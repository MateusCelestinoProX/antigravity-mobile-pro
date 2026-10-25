package com.mateuscelestino.antigravitymobile;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

public class KeepAliveService extends Service {
    private static final String TAG = "AntigravityKeepAlive";
    private static final String CHANNEL_ID = "antigravity_link_channel";
    private static final int NOTIFICATION_ID = 20261025;

    private PowerManager.WakeLock wakeLock;
    private WifiManager.WifiLock wifiLock;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    public interface NetworkStateListener {
        void onNetworkAvailable();
        void onNetworkLost();
    }

    private static NetworkStateListener networkListener;

    public static void setNetworkListener(NetworkStateListener listener) {
        networkListener = listener;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        acquireLocks();
        registerNetworkMonitoring();
        Log.i(TAG, "KeepAliveService criado com sucesso.");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = buildNotification();
        try {
            startForeground(NOTIFICATION_ID, notification);
        } catch (Exception e) {
            Log.e(TAG, "Erro ao iniciar startForeground: " + e.getMessage());
        }
        return START_STICKY;
    }

    private void acquireLocks() {
        try {
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (powerManager != null) {
                wakeLock = powerManager.newWakeLock(
                        PowerManager.PARTIAL_WAKE_LOCK,
                        "AntigravityMobile::PersistentWakeLock"
                );
                wakeLock.setReferenceCounted(false);
                wakeLock.acquire();
            }

            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                wifiLock = wifiManager.createWifiLock(
                        WifiManager.WIFI_MODE_FULL_HIGH_PERF,
                        "AntigravityMobile::PersistentWifiLock"
                );
                wifiLock.setReferenceCounted(false);
                wifiLock.acquire();
            }
            Log.i(TAG, "WakeLock e WifiLock de alta performance adquiridos.");
        } catch (Exception e) {
            Log.e(TAG, "Falha ao adquirir locks de hardware: " + e.getMessage());
        }
    }

    private void registerNetworkMonitoring() {
        try {
            connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                NetworkRequest request = new NetworkRequest.Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .build();

                networkCallback = new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(Network network) {
                        Log.i(TAG, "Rede ativa detectada pelo Sentinel.");
                        if (networkListener != null) {
                            networkListener.onNetworkAvailable();
                        }
                    }

                    @Override
                    public void onLost(Network network) {
                        Log.w(TAG, "Conexão de rede perdida.");
                        if (networkListener != null) {
                            networkListener.onNetworkLost();
                        }
                    }
                };
                connectivityManager.registerNetworkCallback(request, networkCallback);
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao registrar NetworkCallback: " + e.getMessage());
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Antigravity Connection Sentinel",
                    NotificationManager.IMPORTANCE_MIN
            );
            channel.setDescription("Mantém a sincronização de baixa latência ativa com o desktop");
            channel.setShowBadge(false);
            channel.enableVibration(false);
            channel.enableLights(false);
            channel.setSound(null, null);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
        );

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        builder.setContentTitle("Antigravity Link Ativo")
                .setContentText("Sincronização de alta estabilidade com Mac OS")
                .setSmallIcon(R.drawable.ic_stat_antigravity)
                .setContentIntent(pendingIntent)
                .setOngoing(true);

        return builder.build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
            if (wifiLock != null && wifiLock.isHeld()) wifiLock.release();
            if (connectivityManager != null && networkCallback != null) {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            }
        } catch (Exception ignored) {}
        Log.i(TAG, "KeepAliveService destruído.");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
