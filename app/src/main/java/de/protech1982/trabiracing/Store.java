package de.protech1982.trabiracing;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Speichert Spielstand, Highscore und Einstellungen. */
public class Store {

    public static class Entry {
        public final String name;
        public final int score;

        Entry(String name, int score) {
            this.name = name;
            this.score = score;
        }
    }

    private static final String FILE = "trabi";
    private final SharedPreferences prefs;

    public Store(Context ctx) {
        prefs = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public boolean isMusicOn() {
        return prefs.getBoolean("music", true);
    }

    public void setMusicOn(boolean on) {
        prefs.edit().putBoolean("music", on).apply();
    }

    public int getThemeId() {
        return prefs.getInt("theme", 0);
    }

    public void setThemeId(int id) {
        prefs.edit().putInt("theme", id).apply();
    }

    public boolean isSoundOn() {
        return prefs.getBoolean("sound", true);
    }

    public void setSoundOn(boolean on) {
        prefs.edit().putBoolean("sound", on).apply();
    }

    public int getLimitSeconds() {
        return prefs.getInt("limit", 120);
    }

    public void setLimitSeconds(int seconds) {
        prefs.edit().putInt("limit", seconds).apply();
    }

    public boolean hasSave() {
        return prefs.contains("save_score");
    }

    public void save(int score, float metres, float secondsLeft, int lane, float speed) {
        prefs.edit()
                .putInt("save_score", score)
                .putFloat("save_metres", metres)
                .putFloat("save_left", secondsLeft)
                .putInt("save_lane", lane)
                .putFloat("save_speed", speed)
                .apply();
    }

    public float[] load() {
        if (!hasSave()) return null;
        return new float[]{
                prefs.getInt("save_score", 0),
                prefs.getFloat("save_metres", 0f),
                prefs.getFloat("save_left", 120f),
                prefs.getInt("save_lane", 1),
                prefs.getFloat("save_speed", 1f)
        };
    }

    public void clearSave() {
        prefs.edit()
                .remove("save_score").remove("save_metres").remove("save_left")
                .remove("save_lane").remove("save_speed")
                .apply();
    }

    public List<Entry> highscores() {
        List<Entry> list = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            int score = prefs.getInt("hs_score_" + i, 0);
            if (score <= 0) continue;
            list.add(new Entry(prefs.getString("hs_name_" + i, "Trabifahrer"), score));
        }
        return list;
    }

    /** Traegt einen Punktestand ein und meldet den Platz (1..10) oder 0. */
    public int addHighscore(String name, int score) {
        List<Entry> list = highscores();
        list.add(new Entry(name, score));
        Collections.sort(list, (a, b) -> Integer.compare(b.score, a.score));
        while (list.size() > 10) list.remove(list.size() - 1);

        SharedPreferences.Editor e = prefs.edit();
        for (int i = 0; i < 10; i++) {
            if (i < list.size()) {
                e.putString("hs_name_" + i, list.get(i).name);
                e.putInt("hs_score_" + i, list.get(i).score);
            } else {
                e.remove("hs_name_" + i).remove("hs_score_" + i);
            }
        }
        e.apply();

        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).score == score && list.get(i).name.equals(name)) return i + 1;
        }
        return 0;
    }
}
