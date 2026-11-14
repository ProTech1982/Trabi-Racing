package de.protech1982.trabiracing;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Sicherung und Wiederherstellung von Highscore, Spielstand und Einstellungen
 * als JSON-Datei. Die Datei liegt dort, wo sie im Android-Dateidialog
 * abgelegt wird, und uebersteht deshalb eine Neuinstallation.
 */
final class Backup {

    private static final String PREFS_NAME = "trabi";
    private static final String FORMAT = "trabi-racing-backup";
    private static final int VERSION = 1;

    private Backup() {
    }

    static String suggestedName() {
        return "trabi-racing-sicherung-"
                + new SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY).format(new Date()) + ".json";
    }

    /** Schreibt alle Werte in die Datei und gibt die Anzahl zurueck. */
    static int write(Context context, Uri uri) throws Exception {
        SharedPreferences sp = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        JSONArray items = new JSONArray();
        for (Map.Entry<String, ?> e : sp.getAll().entrySet()) {
            Object v = e.getValue();
            String type;
            Object json;
            if (v instanceof Boolean) {
                type = "boolean";
                json = v;
            } else if (v instanceof Integer) {
                type = "int";
                json = v;
            } else if (v instanceof Long) {
                type = "long";
                json = v;
            } else if (v instanceof Float) {
                type = "float";
                json = ((Float) v).doubleValue();
            } else if (v instanceof String) {
                type = "string";
                json = v;
            } else if (v instanceof Set) {
                type = "set";
                JSONArray arr = new JSONArray();
                for (Object o : (Set<?>) v) {
                    arr.put(String.valueOf(o));
                }
                json = arr;
            } else {
                continue;
            }
            items.put(new JSONObject().put("k", e.getKey()).put("t", type).put("v", json));
        }
        JSONObject root = new JSONObject()
                .put("format", FORMAT)
                .put("version", VERSION)
                .put("erstellt", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.GERMANY).format(new Date()))
                .put("eintraege", items);
        try (OutputStream out = context.getContentResolver().openOutputStream(uri, "wt")) {
            if (out == null) {
                throw new IllegalStateException("Datei konnte nicht geschrieben werden");
            }
            out.write(root.toString(2).getBytes(StandardCharsets.UTF_8));
        }
        return items.length();
    }

    /** Ersetzt alle Werte durch die der Sicherung und gibt die Anzahl zurueck. */
    static int restore(Context context, Uri uri) throws Exception {
        String text;
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in == null) {
                throw new IllegalStateException("Datei konnte nicht gelesen werden");
            }
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int n;
            while ((n = in.read(chunk)) > 0) {
                buf.write(chunk, 0, n);
            }
            text = buf.toString("UTF-8");
        }
        JSONObject root = new JSONObject(text);
        if (!FORMAT.equals(root.optString("format"))) {
            throw new IllegalArgumentException("Das ist keine Sicherung von Trabi Racing.");
        }
        if (root.optInt("version", 0) > VERSION) {
            throw new IllegalArgumentException("Die Sicherung stammt von einer neueren App-Version.");
        }
        JSONArray items = root.getJSONArray("eintraege");

        // Erst alles in den Editor legen; geschrieben wird erst am Ende.
        SharedPreferences.Editor editor = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.clear();
        int count = 0;
        for (int i = 0; i < items.length(); i++) {
            JSONObject o = items.getJSONObject(i);
            String key = o.getString("k");
            switch (o.getString("t")) {
                case "boolean":
                    editor.putBoolean(key, o.getBoolean("v"));
                    break;
                case "int":
                    editor.putInt(key, o.getInt("v"));
                    break;
                case "long":
                    editor.putLong(key, o.getLong("v"));
                    break;
                case "float":
                    editor.putFloat(key, (float) o.getDouble("v"));
                    break;
                case "string":
                    editor.putString(key, o.getString("v"));
                    break;
                case "set":
                    JSONArray arr = o.getJSONArray("v");
                    Set<String> set = new HashSet<>();
                    for (int j = 0; j < arr.length(); j++) {
                        set.add(arr.getString(j));
                    }
                    editor.putStringSet(key, set);
                    break;
                default:
                    continue;
            }
            count++;
        }
        if (!editor.commit()) {
            throw new IllegalStateException("Einstellungen konnten nicht gespeichert werden");
        }
        return count;
    }
}
