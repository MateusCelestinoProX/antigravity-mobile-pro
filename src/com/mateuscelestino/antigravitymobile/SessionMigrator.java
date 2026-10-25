package com.mateuscelestino.antigravitymobile;

import android.content.Context;
import android.util.Log;
import android.webkit.CookieManager;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class SessionMigrator {
    private static final String TAG = "AntigravitySessionMigrator";

    public static void applySessionCookies(Context context) {
        try {
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptCookie(true);

            InputStream is = context.getAssets().open("session_cookies.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            int read = is.read(buffer);
            is.close();

            if (read <= 0) return;

            String jsonStr = new String(buffer, StandardCharsets.UTF_8);
            JSONArray array = new JSONArray(jsonStr);

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String name = obj.getString("name");
                String value = obj.getString("value");
                String domain = obj.getString("domain");
                String path = obj.optString("path", "/");
                boolean secure = obj.optBoolean("secure", true);
                boolean httpOnly = obj.optBoolean("httpOnly", false);

                StringBuilder sb = new StringBuilder();
                sb.append(name).append("=").append(value).append("; ");
                sb.append("domain=").append(domain).append("; ");
                sb.append("path=").append(path);
                if (secure) sb.append("; Secure");
                if (httpOnly) sb.append("; HttpOnly");

                String targetUrl = domain.startsWith(".") ? "https://" + domain.substring(1) : "https://" + domain;
                cookieManager.setCookie(targetUrl, sb.toString());
            }

            cookieManager.flush();
            Log.i(TAG, "Sessão migrada com sucesso: " + array.length() + " cookies oficiais injetados.");
        } catch (Exception e) {
            Log.e(TAG, "Erro ao migrar cookies de sessão: " + e.getMessage(), e);
        }
    }
}
