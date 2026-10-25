package com.mateuscelestino.antigravitymobile;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

public class MainActivity extends Activity implements KeepAliveService.NetworkStateListener {
    private static final String TAG = "AntigravityMainActivity";
    private static final String TARGET_URL = "https://antigravity.google.com";

    private WebView webView;
    private ProgressBar progressBar;
    private Handler handler;
    private Vibrator vibrator;
    private ValueCallback<Uri[]> filePathCallback;
    private static final int FILE_CHOOSER_REQUEST_CODE = 2026;

    // Variáveis de controle de toque e Pull-to-Refresh nativo
    private float touchStartY = 0;
    private boolean isPulling = false;
    private static final float PULL_THRESHOLD_DP = 130f;
    private float pullThresholdPx;

    private boolean isReconnecting = false;
    private long lastReconnectTime = 0;

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        configureWindowDecorations();
        setContentView(R.layout.activity_main);

        pullThresholdPx = PULL_THRESHOLD_DP * getResources().getDisplayMetrics().density;
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        webView = findViewById(R.id.webview);
        progressBar = findViewById(R.id.progress_bar);
        handler = new Handler(Looper.getMainLooper());

        startPersistentService();
        configureWebView();
        configureTouchAndGestures();

        if (savedInstanceState == null) {
            webView.loadUrl(TARGET_URL);
        } else {
            webView.restoreState(savedInstanceState);
        }

        startWatchdogLoop();
        startStealthRadioKeepAlive();
    }

    private void configureWindowDecorations() {
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.BLACK);
        window.setNavigationBarColor(Color.BLACK);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(true);
        }
    }

    private void startPersistentService() {
        try {
            KeepAliveService.setNetworkListener(this);
            Intent serviceIntent = new Intent(this, KeepAliveService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Falha ao iniciar KeepAliveService: " + e.getMessage());
        }
    }

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);

        // Otimização de renderização para telas de alta taxa (120Hz do S22 Ultra)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.setSafeBrowsingEnabled(true);
        }
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            settings.setOffscreenPreRaster(true); // Pré-rasterização para rolagem 120Hz sem engasgos
        }

        // Sanitização de User-Agent: remove o marcador "; wv" para identificação como Chrome Mobile padrão
        String defaultUA = settings.getUserAgentString();
        String sanitizedUA = defaultUA.replace("; wv", "");
        settings.setUserAgentString(sanitizedUA);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(webView, true);
        }

        webView.setBackgroundColor(Color.BLACK);
        webView.addJavascriptInterface(new AntigravityNativeBridge(), "AntigravityNative");

        webView.setWebViewClient(new AntigravityWebViewClient());
        webView.setWebChromeClient(new AntigravityWebChromeClient());
    }

    @SuppressLint("ClickableViewAccessibility")
    private void configureTouchAndGestures() {
        // Implementação nativa de Pull-to-Refresh para a WebView
        webView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (webView.getScrollY() == 0) {
                        touchStartY = event.getY();
                        isPulling = true;
                    } else {
                        isPulling = false;
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (isPulling && webView.getScrollY() == 0) {
                        float deltaY = event.getY() - touchStartY;
                        if (deltaY > 0) {
                            int progress = (int) Math.min(100, (deltaY / pullThresholdPx) * 100);
                            progressBar.setVisibility(View.VISIBLE);
                            progressBar.setProgress(progress);
                            if (deltaY >= pullThresholdPx) {
                                triggerHapticFeedback();
                            }
                        }
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (isPulling) {
                        float deltaY = event.getY() - touchStartY;
                        if (deltaY >= pullThresholdPx && webView.getScrollY() == 0) {
                            Log.i(TAG, "Pull-to-refresh disparado pelo usuário.");
                            triggerHapticFeedback();
                            performSmartReconnect(true);
                        } else {
                            progressBar.setVisibility(View.GONE);
                        }
                        isPulling = false;
                    }
                    break;
            }
            return false;
        });
    }

    private void triggerHapticFeedback() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && vibrator != null) {
                vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE));
            } else if (webView != null) {
                webView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            }
        } catch (Exception ignored) {}
    }

    private class AntigravityWebViewClient extends WebViewClient {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            progressBar.setVisibility(View.GONE);
            CookieManager.getInstance().flush();
            injectWatchdogScript();
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
            if (request.isForMainFrame()) {
                Log.w(TAG, "Erro de rede no frame principal: " + error.getDescription());
            }
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            Uri uri = request.getUrl();
            String host = uri.getHost();
            if (host != null && (host.endsWith("google.com") || host.endsWith("googleusercontent.com") || host.endsWith("googleapis.com"))) {
                return false;
            }
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }

    private class AntigravityWebChromeClient extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            if (newProgress < 100) {
                progressBar.setProgress(newProgress);
            } else {
                progressBar.setVisibility(View.GONE);
            }
        }

        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
            if (MainActivity.this.filePathCallback != null) {
                MainActivity.this.filePathCallback.onReceiveValue(null);
            }
            MainActivity.this.filePathCallback = filePathCallback;

            try {
                Intent intent = fileChooserParams.createIntent();
                startActivityForResult(intent, FILE_CHOOSER_REQUEST_CODE);
                return true;
            } catch (Exception e) {
                MainActivity.this.filePathCallback = null;
                return false;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_CHOOSER_REQUEST_CODE) {
            if (filePathCallback != null) {
                Uri[] results = null;
                if (resultCode == Activity.RESULT_OK && data != null) {
                    if (data.getData() != null) {
                        results = new Uri[]{data.getData()};
                    } else if (data.getClipData() != null) {
                        int count = data.getClipData().getItemCount();
                        results = new Uri[count];
                        for (int i = 0; i < count; i++) {
                            results[i] = data.getClipData().getItemAt(i).getUri();
                        }
                    }
                }
                filePathCallback.onReceiveValue(results);
                filePathCallback = null;
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public class AntigravityNativeBridge {
        @JavascriptInterface
        public void onInstanceDisconnected() {
            Log.w(TAG, "[Bridge] Notificação 'Instance disconnected' recebida do DOM.");
            handler.post(() -> performSmartReconnect(false));
        }

        @JavascriptInterface
        public void onSocketClosed(int code) {
            Log.w(TAG, "[Bridge] WebSocket fechado: " + code);
            if (code == 1006 || code == 1001 || code == 1005) {
                handler.postDelayed(() -> performSmartReconnect(false), 800);
            }
        }

        @JavascriptInterface
        public void log(String message) {
            Log.d(TAG, "[JS] " + message);
        }
    }

    private void injectWatchdogScript() {
        String js = "(function() {\n" +
                "  if (window.__antigravitySentinelInjected) return;\n" +
                "  window.__antigravitySentinelInjected = true;\n" +
                "  console.log('[Sentinel Pro v2.6] Ativado com monitoramento resiliente.');\n" +
                "  \n" +
                "  // 1. Interceptor de WebSocket nativo\n" +
                "  if (window.WebSocket) {\n" +
                "    const OriginalWS = window.WebSocket;\n" +
                "    window.WebSocket = function(...args) {\n" +
                "      const ws = new OriginalWS(...args);\n" +
                "      ws.addEventListener('close', function(e) {\n" +
                "        if (window.AntigravityNative) window.AntigravityNative.onSocketClosed(e.code);\n" +
                "      });\n" +
                "      return ws;\n" +
                "    };\n" +
                "    window.WebSocket.prototype = OriginalWS.prototype;\n" +
                "  }\n" +
                "  \n" +
                "  // 2. Auto-recuperação do botão de Reconexão no DOM\n" +
                "  setInterval(function() {\n" +
                "    // Tenta encontrar botões de reconexão ou retry automáticos\n" +
                "    const buttons = document.querySelectorAll('button, [role=\"button\"]');\n" +
                "    for (let i = 0; i < buttons.length; i++) {\n" +
                "      const label = (buttons[i].innerText || buttons[i].getAttribute('aria-label') || '').toLowerCase();\n" +
                "      if (label.includes('reconnect') || label.includes('retry') || label.includes('reconectar') || label.includes('tentar novamente')) {\n" +
                "        console.log('[Sentinel] Auto-clicando no botão de reconexão...');\n" +
                "        buttons[i].click();\n" +
                "        return;\n" +
                "      }\n" +
                "    }\n" +
                "    \n" +
                "    const text = document.body ? document.body.innerText : '';\n" +
                "    if (text.includes('Instance disconnected') || text.includes('Waiting for connection or Wi-Fi to resume')) {\n" +
                "      if (window.AntigravityNative) {\n" +
                "        window.AntigravityNative.onInstanceDisconnected();\n" +
                "      }\n" +
                "    }\n" +
                "  }, 1800);\n" +
                "})();";

        webView.evaluateJavascript(js, null);
    }

    private void performSmartReconnect(boolean force) {
        long now = System.currentTimeMillis();
        long minInterval = force ? 1500 : 3500;
        if (now - lastReconnectTime < minInterval) {
            return;
        }
        lastReconnectTime = now;

        if (isNetworkConnected()) {
            Log.i(TAG, "Rede ativa. Disparando soft-reconnect do Antigravity...");
            Toast.makeText(this, "Reconectando instância Antigravity...", Toast.LENGTH_SHORT).show();
            webView.reload();
        } else {
            Log.w(TAG, "Sem rede no momento. Aguardando conectividade.");
        }
    }

    // Mantém a camada de rádio celular e Wi-Fi sempre ativas (evita expiração da tabela NAT da operadora/roteador)
    private void startStealthRadioKeepAlive() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (webView != null && isNetworkConnected()) {
                    // Executa um fetch ultraleve sem CORS para favicon que refresca a tabela NAT sem travar a thread
                    String keepAliveJs = "fetch('https://antigravity.google.com/favicon.ico', { mode: 'no-cors', cache: 'no-store' }).catch(function(){});";
                    webView.evaluateJavascript(keepAliveJs, null);
                }
                handler.postDelayed(this, 18000); // Executa a cada 18 segundos
            }
        }, 10000);
    }

    private boolean isNetworkConnected() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                return activeNetwork != null && activeNetwork.isConnected();
            }
        } catch (Exception ignored) {}
        return false;
    }

    private void startWatchdogLoop() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (webView != null) {
                    injectWatchdogScript();
                }
                handler.postDelayed(this, 8000);
            }
        }, 8000);
    }

    @Override
    public void onNetworkAvailable() {
        Log.i(TAG, "Notificação de rede disponível recebida do KeepAliveService.");
        handler.post(() -> {
            String currentUrl = webView.getUrl();
            if (currentUrl == null || currentUrl.isEmpty() || currentUrl.startsWith("data:") || currentUrl.startsWith("about:blank")) {
                webView.loadUrl(TARGET_URL);
            } else {
                performSmartReconnect(true);
            }
        });
    }

    @Override
    public void onNetworkLost() {
        Log.w(TAG, "Notificação de rede perdida recebida.");
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            moveTaskToBack(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
            CookieManager.getInstance().flush();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) {
            CookieManager.getInstance().flush();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        KeepAliveService.setNetworkListener(null);
    }
}
