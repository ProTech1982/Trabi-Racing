package de.protech1982.trabiracing;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

public class MainActivity extends Activity implements GameView.Listener {

    private static final int ACCENT = 0xFF49E8FF;
    private static final int PINK = 0xFFFF4D9D;
    private static final int BACKDROP = 0xFF0E0A1A;

    private FrameLayout root;
    private GameView game;
    private ScrollView menuLayer;
    private LinearLayout card;
    private TextView subtitle;
    private Button btnResume, btnSave, btnLoad, btnClear, btnMusic, btnSound, btnUpdate;
    private final Button[] limitButtons = new Button[3];
    private final int[] limitValues = {60, 120, 300};

    private Store store;
    private Chiptune music;
    private Sfx sfx;
    private boolean gameStarted;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        store = new Store(this);
        music = new Chiptune();
        music.setEnabled(store.isMusicOn());
        sfx = new Sfx();
        sfx.setEnabled(store.isSoundOn());

        root = new FrameLayout(this);
        root.setBackgroundColor(BACKDROP);

        game = new GameView(this);
        game.setListener(this);
        game.setSfx(sfx);
        root.addView(game, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        buildMenu();
        setContentView(root);
        applyInsets();
        showMenu("Hauptmenü");
    }

    private void applyInsets() {
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int l, t, r, b;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars =
                        insets.getInsets(WindowInsets.Type.systemBars()
                                | WindowInsets.Type.displayCutout());
                l = bars.left;
                t = bars.top;
                r = bars.right;
                b = bars.bottom;
            } else {
                l = insets.getSystemWindowInsetLeft();
                t = insets.getSystemWindowInsetTop();
                r = insets.getSystemWindowInsetRight();
                b = insets.getSystemWindowInsetBottom();
            }
            v.setPadding(l, t, r, b);
            return insets;
        });
        root.requestApplyInsets();
    }

    // ------------------------------------------------------------------ Menue

    private void buildMenu() {
        menuLayer = new ScrollView(this);
        menuLayer.setBackgroundColor(0xE60C0818);
        menuLayer.setFillViewport(true);

        LinearLayout holder = new LinearLayout(this);
        holder.setOrientation(LinearLayout.VERTICAL);
        holder.setGravity(Gravity.CENTER);
        holder.setPadding(dp(18), dp(18), dp(18), dp(18));

        card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(22), dp(18), dp(22));
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(0xF2120C22);
        cardBg.setCornerRadius(dp(22));
        cardBg.setStroke(dp(2), PINK);
        card.setBackground(cardBg);

        TextView title = new TextView(this);
        title.setText("Trabi Racing");
        title.setTextColor(PINK);
        title.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD), Typeface.BOLD_ITALIC);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 38);
        title.setGravity(Gravity.CENTER);
        title.setShadowLayer(dp(10), 0, 0, PINK);
        card.addView(title);

        subtitle = new TextView(this);
        subtitle.setTextColor(0xFFBFB6D8);
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, dp(4), 0, dp(14));
        card.addView(subtitle);

        card.addView(buildLimitRow());

        card.addView(menuButton("▶  Neues Spiel", v -> startGame()));
        btnResume = menuButton("Weiterfahren", v -> resumeGame());
        card.addView(btnResume);
        btnSave = menuButton("Spiel speichern", v -> saveGame());
        card.addView(btnSave);
        btnLoad = menuButton("Spiel laden", v -> loadGame());
        card.addView(btnLoad);
        btnClear = menuButton("Spielstand löschen", v -> clearSave());
        card.addView(btnClear);
        card.addView(menuButton("Highscore – Top 10", v -> showHighscores()));
        btnSound = menuButton(soundLabel(), v -> toggleSound());
        card.addView(btnSound);
        btnMusic = menuButton(musicLabel(), v -> toggleMusic());
        card.addView(btnMusic);
        btnUpdate = menuButton("Nach Updates suchen", v -> checkForUpdate());
        card.addView(btnUpdate);
        card.addView(menuButton("Beenden", v -> finish()));

        holder.addView(card, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        menuLayer.addView(holder);
        root.addView(menuLayer, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private View buildLimitRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setPadding(0, 0, 0, dp(10));

        TextView label = new TextView(this);
        label.setText("Zeitlimit:");
        label.setTextColor(0xFFBFB6D8);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        label.setPadding(0, 0, dp(10), 0);
        row.addView(label);

        String[] names = {"1 Min", "2 Min", "5 Min"};
        for (int i = 0; i < 3; i++) {
            final int index = i;
            Button b = new Button(this);
            b.setText(names[i]);
            b.setAllCaps(false);
            b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            b.setPadding(dp(6), dp(4), dp(6), dp(4));
            b.setOnClickListener(v -> {
                store.setLimitSeconds(limitValues[index]);
                refreshLimitButtons();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(dp(3), 0, dp(3), 0);
            row.addView(b, lp);
            limitButtons[i] = b;
        }
        refreshLimitButtons();
        return row;
    }

    private void refreshLimitButtons() {
        int current = store.getLimitSeconds();
        for (int i = 0; i < 3; i++) {
            boolean on = limitValues[i] == current;
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(on ? 0x33FFC24D : 0x22FFFFFF);
            bg.setCornerRadius(dp(12));
            bg.setStroke(dp(on ? 2 : 1), on ? 0xFFFFC24D : 0x55FFFFFF);
            limitButtons[i].setBackground(bg);
            limitButtons[i].setTextColor(on ? 0xFFFFC24D : 0xFFD6CFEA);
        }
    }

    private Button menuButton(String text, View.OnClickListener action) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextColor(0xFFEDE9F8);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x33121A2E);
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(2), ACCENT);
        b.setBackground(bg);
        b.setOnClickListener(action);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(5), 0, dp(5));
        b.setLayoutParams(lp);
        return b;
    }

    private void showMenu(String text) {
        subtitle.setText(text);
        btnResume.setVisibility(gameStarted && !game.isOver() ? View.VISIBLE : View.GONE);
        btnSave.setVisibility(gameStarted && !game.isOver() ? View.VISIBLE : View.GONE);
        btnLoad.setVisibility(store.hasSave() ? View.VISIBLE : View.GONE);
        btnClear.setVisibility(store.hasSave() ? View.VISIBLE : View.GONE);
        menuLayer.setVisibility(View.VISIBLE);
        game.setPaused(true);
        music.stop();
        sfx.stopEngine();
    }

    private void hideMenu() {
        menuLayer.setVisibility(View.GONE);
        if (store.isMusicOn()) music.start();
        if (store.isSoundOn()) sfx.startEngine();
    }

    // ----------------------------------------------------------- Menuaktionen

    private void startGame() {
        game.newGame(store.getLimitSeconds());
        gameStarted = true;
        hideMenu();
    }

    private void resumeGame() {
        if (!gameStarted) {
            startGame();
            return;
        }
        game.setPaused(false);
        hideMenu();
    }

    private void saveGame() {
        store.save(game.score(), game.metres(), game.timeLeft(), game.lane(), game.speed());
        toast("Spielstand gespeichert");
        showMenu("Gespeichert");
    }

    private void loadGame() {
        float[] s = store.load();
        if (s == null) return;
        game.restore((int) s[0], s[1], s[2], (int) s[3], s[4], store.getLimitSeconds());
        gameStarted = true;
        showMenu("Spielstand geladen – weiterfahren?");
    }

    private void clearSave() {
        store.clearSave();
        toast("Spielstand gelöscht");
        showMenu("Hauptmenü");
    }

    private void toggleMusic() {
        boolean on = !store.isMusicOn();
        store.setMusicOn(on);
        music.setEnabled(on);
        if (on && menuLayer.getVisibility() != View.VISIBLE) music.start();
        btnMusic.setText(musicLabel());
    }

    private void toggleSound() {
        boolean on = !store.isSoundOn();
        store.setSoundOn(on);
        sfx.setEnabled(on);
        if (!on) sfx.stopEngine();
        else if (menuLayer.getVisibility() != View.VISIBLE) sfx.startEngine();
        btnSound.setText(soundLabel());
    }

    private String soundLabel() {
        return "Ton: " + (store.isSoundOn() ? "An" : "Aus");
    }

    private String musicLabel() {
        return "Musik: " + (store.isMusicOn() ? "An" : "Aus");
    }

    private void showHighscores() {
        List<Store.Entry> list = store.highscores();
        StringBuilder sb = new StringBuilder();
        if (list.isEmpty()) {
            sb.append("Noch keine Einträge.\nFahr eine Runde!");
        } else {
            for (int i = 0; i < list.size(); i++) {
                sb.append(String.format(java.util.Locale.GERMANY, "%2d.  %-14s %,d%n",
                        i + 1, list.get(i).name, list.get(i).score));
            }
        }
        TextView tv = new TextView(this);
        tv.setText(sb.toString());
        tv.setTypeface(Typeface.MONOSPACE);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tv.setPadding(dp(22), dp(18), dp(22), dp(10));
        new AlertDialog.Builder(this)
                .setTitle("Highscore – Top 10")
                .setView(tv)
                .setPositiveButton("Schließen", null)
                .show();
    }

    private void checkForUpdate() {
        btnUpdate.setEnabled(false);
        btnUpdate.setText("Suche läuft …");
        Updater.check(this, result -> {
            btnUpdate.setEnabled(true);
            btnUpdate.setText("Nach Updates suchen");

            if (result.error != null) {
                new AlertDialog.Builder(this)
                        .setTitle("Update-Prüfung")
                        .setMessage(result.error)
                        .setPositiveButton("Schließen", null)
                        .show();
                return;
            }
            if (!result.available) {
                toast("Du hast bereits die neueste Version");
                return;
            }

            String notes = result.notes == null ? "" : result.notes.trim();
            if (notes.length() > 400) notes = notes.substring(0, 400) + " …";
            new AlertDialog.Builder(this)
                    .setTitle("Update verfügbar")
                    .setMessage(result.version + (notes.isEmpty() ? "" : "\n\n" + notes))
                    .setPositiveButton("Herunterladen", (d, which) -> {
                        try {
                            startActivity(new android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse(result.downloadUrl)));
                        } catch (Exception e) {
                            toast("Download konnte nicht geöffnet werden");
                        }
                    })
                    .setNegativeButton("Später", null)
                    .show();
        });
    }

    private void toast(String msg) {
        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    // ------------------------------------------------------- Rueckmeldungen

    @Override
    public void onPauseRequested() {
        showMenu("Pause");
    }

    @Override
    public void onGameOver(int score, int metres, boolean timeUp) {
        music.stop();
        sfx.stopEngine();
        String head = timeUp ? "Zeit abgelaufen!" : "Zusammenstoß!";
        String body = String.format(java.util.Locale.GERMANY,
                "%s%n%,d Punkte  ·  %d m", head, score, metres);

        List<Store.Entry> list = store.highscores();
        boolean top = list.size() < 10 || score > list.get(list.size() - 1).score;

        if (top && score > 0) {
            EditText input = new EditText(this);
            input.setHint("Dein Name");
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            input.setSingleLine(true);
            new AlertDialog.Builder(this)
                    .setTitle("Top 10!")
                    .setMessage(body + "\n\nTrag dich in die Bestenliste ein:")
                    .setView(input)
                    .setCancelable(false)
                    .setPositiveButton("Eintragen", (d, which) -> {
                        String name = input.getText().toString().trim();
                        if (name.isEmpty()) name = "Trabifahrer";
                        store.addHighscore(name, score);
                        gameStarted = false;
                        showMenu("Hauptmenü");
                    })
                    .setNegativeButton("Überspringen", (d, which) -> {
                        gameStarted = false;
                        showMenu("Hauptmenü");
                    })
                    .show();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Fahrt beendet")
                    .setMessage(body)
                    .setPositiveButton("Weiter", (d, which) -> {
                        gameStarted = false;
                        showMenu("Hauptmenü");
                    })
                    .show();
        }
    }

    @Override
    public void onBackPressed() {
        if (menuLayer.getVisibility() == View.VISIBLE) {
            super.onBackPressed();
        } else {
            showMenu("Pause");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        game.setPaused(true);
        music.stop();
        sfx.stopEngine();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (menuLayer != null && menuLayer.getVisibility() != View.VISIBLE) {
            if (store.isMusicOn()) music.start();
            if (store.isSoundOn()) sfx.startEngine();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        music.stop();
        sfx.release();
    }
}
