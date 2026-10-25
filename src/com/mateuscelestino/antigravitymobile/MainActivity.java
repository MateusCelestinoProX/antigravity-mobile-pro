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
import android.util.Log;
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
    private ValueCallback<Uri[]> filePathCallback;
    private static final int FILE_CHOOSER_REQUEST_CODE = 2026;

    private boolean isReconnecting = false;
    private long lastReconnectTime = 0;

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        configureWindowDecorations();
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webview);
        progressBar = findViewById(R.id.progress_bar);
        handler = new Handler(Looper.getMainLooper());

        startPersistentService();
        configureWebView();

        if (savedInstanceState == null) {
            webView.loadUrl(TARGET_URL);
        } else {
            webView.restoreState(savedInstanceState);
        }

        startWatchdogLoop();
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

    @SuppressLint("SetJavaScriptEnabled")
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        // Sanitização de User-Agent: remove o marcador "; wv" (WebView) para que os serviços Google
        // identifiquem a engine como Chrome Mobile nativo, liberando login sem 403 disallowed_useragent.
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
                return false; // Permite navegação interna no WebView
            }
            // Links externos abrem no navegador do sistema
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
            Log.w(TAG, "[Bridge] Mensagem 'Instance disconnected' detectada.");
            handler.post(() -> performSmartReconnect());
        }

        @JavascriptInterface
        public void onSocketClosed(int code) {
            Log.w(TAG, "[Bridge] WebSocket fechado com código: " + code);
            if (code == 1006 || code == 1001) {
                handler.postDelayed(() -> performSmartReconnect(), 1000);
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
                "  console.log('[Sentinel] Injected into Antigravity Mobile.');\n" +
                "  \n" +
                "  // Interceptor de WebSocket para notificar o app nativo\n" +
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
                "  // Monitor contínuo de desconexão da instância no DOM\n" +
                "  setInterval(function() {\n" +
                "    const text = document.body ? document.body.innerText : '';\n" +
                "    if (text.includes('Instance disconnected') || text.includes('Waiting for connection or Wi-Fi to resume')) {\n" +
                "      if (window.AntigravityNative) {\n" +
                "        window.AntigravityNative.onInstanceDisconnected();\n" +
                "      }\n" +
                "    }\n" +
                "  }, 2000);\n" +
                "})();";

        webView.evaluateJavascript(js, null);
    }

    private void performSmartReconnect() {
        long now = System.currentTimeMillis();
        if (now - lastReconnectTime < 4000) {
            return; // Debounce de 4 segundos para evitar spam de reload
        }
        lastReconnectTime = now;

        if (isNetworkConnected()) {
            Log.i(TAG, "Rede ativa. Disparando soft-reconnect do Antigravity...");
            Toast.makeText(this, "Reconectando instância Antigravity...", Toast.LENGTH_SHORT).show();
            // Dispara reload suave preservando cookies
            webView.reload();
        } else {
            Log.w(TAG, "Sem rede no momento. Aguardando sinal para reconectar.");
        }
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
                handler.postDelayed(this, 10000);
            }
        }, 10000);
    }

    @Override
    public void onNetworkAvailable() {
        Log.i(TAG, "Notificação de rede disponível recebida do KeepAliveService.");
        handler.post(() -> {
            String currentUrl = webView.getUrl();
            if (currentUrl == null || currentUrl.isEmpty() || currentUrl.startsWith("data:") || currentUrl.startsWith("about:blank")) {
                webView.loadUrl(TARGET_URL);
            } else {
                performSmartReconnect();
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
            // Em vez de finalizar o app e destruir a sessão de background, move para segundo plano
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
