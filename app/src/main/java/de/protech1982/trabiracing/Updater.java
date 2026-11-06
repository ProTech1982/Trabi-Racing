package de.protech1982.trabiracing;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Prueft ueber die oeffentliche GitHub-Releases-API, ob ein neuerer Build
 * bereitsteht. Bewusst ohne Token: das Repository ist oeffentlich, und ein
 * eingebetteter Token waere aus der APK auslesbar.
 */
public class Updater {

    private static final String API =
            "https://api.github.com/repos/ProTech1982/Trabi-Racing/releases/latest";

    public static class Result {
        public boolean available;
        public String version;
        public String notes;
        public String downloadUrl;
        public String error;
    }

    public interface Callback {
        void onResult(Result result);
    }

    public static void check(Context ctx, Callback callback) {
        final long installed = installedVersion(ctx);
        final Handler main = new Handler(Looper.getMainLooper());

        new Thread(() -> {
            Result r = new Result();
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(API).openConnection();
                conn.setRequestProperty("Accept", "application/vnd.github+json");
                conn.setRequestProperty("User-Agent", "TrabiRacing");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

                int code = conn.getResponseCode();
                if (code != 200) {
                    r.error = "GitHub antwortete mit Code " + code;
                } else {
                    StringBuilder sb = new StringBuilder();
                    try (BufferedReader in = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                        String line;
                        while ((line = in.readLine()) != null) sb.append(line);
                    }
                    JSONObject json = new JSONObject(sb.toString());
                    String tag = json.optString("tag_name", "");
                    r.version = json.optString("name", tag);
                    r.notes = json.optString("body", "");

                    JSONArray assets = json.optJSONArray("assets");
                    if (assets != null) {
                        for (int i = 0; i < assets.length(); i++) {
                            JSONObject a = assets.getJSONObject(i);
                            if (a.optString("name", "").endsWith(".apk")) {
                                r.downloadUrl = a.optString("browser_download_url", null);
                                break;
                            }
                        }
                    }
                    if (r.downloadUrl == null) {
                        r.downloadUrl = json.optString("html_url", null);
                    }

                    long remote = buildNumber(tag);
                    r.available = remote > installed && r.downloadUrl != null;
                }
            } catch (Exception e) {
                r.error = "Keine Verbindung zu GitHub";
            } finally {
                if (conn != null) conn.disconnect();
            }
            main.post(() -> callback.onResult(r));
        }, "updater").start();
    }

    /** Aus einem Tag wie "build-17" die Nummer ziehen. */
    private static long buildNumber(String tag) {
        if (tag == null) return -1;
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < tag.length(); i++) {
            char c = tag.charAt(i);
            if (c >= '0' && c <= '9') digits.append(c);
        }
        if (digits.length() == 0) return -1;
        try {
            return Long.parseLong(digits.toString());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static long installedVersion(Context ctx) {
        try {
            PackageInfo info = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
            if (Build.VERSION.SDK_INT >= 28) return info.getLongVersionCode();
            return info.versionCode;
        } catch (Exception e) {
            return 0;
        }
    }
}
