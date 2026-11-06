package de.protech1982.trabiracing;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Die eigentliche Spielflaeche: Spielschleife, Physik und Darstellung. */
public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    public interface Listener {
        void onPauseRequested();

        void onGameOver(int score, int metres, boolean timeUp);
    }

    private static final int LANES = 3;
    private static final float CAR_Z = 0.05f;
    private static final float HIT_Z = 0.055f;

    private static class Obstacle {
        float z;
        int lane;
        int kind;
        boolean counted;
    }

    private static class Tree {
        float z;
        int side;
        float scale;
        int kind;
    }

    private static class Building {
        float x, w, h;
        long seed;
    }

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rect = new RectF();
    private final Random rnd = new Random();

    private final List<Obstacle> obstacles = new ArrayList<>();
    private final List<Tree> trees = new ArrayList<>();
    private final List<Building> skyline = new ArrayList<>();

    private Thread loop;
    private volatile boolean looping;
    private volatile boolean paused = true;
    private boolean over;

    private int w, h;
    private float horizon, cx, roadHalf;
    private LinearGradient skyShader, roadShader;

    private int lane = 1;
    private float laneVisual = 1f;
    private float speed = 1f;
    private float metres;
    private float scoreF;
    private float timeLeft = 120f;
    private float limit = 120f;
    private float dashOffset;
    private float spawnCountdown = 1.2f;
    private float shake;
    private float flash;

    private RectF btnPause = new RectF();
    private RectF btnLeft = new RectF();
    private RectF btnRight = new RectF();

    private Listener listener;
    private Sfx sfx;

    private BlurMaskFilter glowSoft, glowWide;
    private LinearGradient carShader, sunShader, mirrorShader;
    private float sunY, sunR;

    public GameView(Context ctx) {
        super(ctx);
        getHolder().addCallback(this);
        setFocusable(true);
        textPaint.setTypeface(android.graphics.Typeface.create(
                android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));
        buildSkyline();
    }

    public void setListener(Listener l) {
        listener = l;
    }

    public void setSfx(Sfx s) {
        sfx = s;
    }

    // ---------------------------------------------------------------- Zustand

    public void newGame(float limitSeconds) {
        limit = limitSeconds;
        timeLeft = limitSeconds;
        lane = 1;
        laneVisual = 1f;
        speed = 1f;
        metres = 0;
        scoreF = 0;
        shake = 0;
        flash = 0;
        over = false;
        obstacles.clear();
        trees.clear();
        spawnCountdown = 1.2f;
        paused = false;
    }

    public void restore(int score, float metresDone, float left, int savedLane, float savedSpeed, float limitSeconds) {
        newGame(limitSeconds);
        scoreF = score;
        metres = metresDone;
        timeLeft = left;
        lane = Math.max(0, Math.min(LANES - 1, savedLane));
        laneVisual = lane;
        speed = Math.max(1f, savedSpeed);
        paused = true;
    }

    public void setPaused(boolean value) {
        paused = value;
    }

    public boolean isPaused() {
        return paused;
    }

    public boolean isOver() {
        return over;
    }

    public int score() {
        return (int) scoreF;
    }

    public int metres() {
        return (int) metres;
    }

    public float timeLeft() {
        return timeLeft;
    }

    public int lane() {
        return lane;
    }

    public float speed() {
        return speed;
    }

    public float limit() {
        return limit;
    }

    public void moveLeft() {
        if (!paused && !over && lane > 0) {
            lane--;
            if (sfx != null) sfx.laneChange();
        }
    }

    public void moveRight() {
        if (!paused && !over && lane < LANES - 1) {
            lane++;
            if (sfx != null) sfx.laneChange();
        }
    }

    // ------------------------------------------------------------ Oberflaeche

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        looping = true;
        loop = new Thread(this, "game");
        loop.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        w = width;
        h = height;
        horizon = h * 0.34f;
        cx = w * 0.5f;
        roadHalf = w * 0.60f;

        skyShader = new LinearGradient(0, 0, 0, horizon,
                new int[]{0xFF1A0B2E, 0xFF4A1B5C, 0xFF8E2D62, 0xFFD94B7A},
                new float[]{0f, 0.45f, 0.78f, 1f}, Shader.TileMode.CLAMP);
        roadShader = new LinearGradient(0, horizon, 0, h,
                new int[]{0xFF241A3A, 0xFF171229, 0xFF221B33},
                new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP);

        sunR = Math.min(w, h) * 0.20f;
        sunY = horizon - sunR * 0.28f;
        sunShader = new LinearGradient(0, sunY - sunR, 0, sunY + sunR,
                new int[]{0xFFFFE08A, 0xFFFF7A5C, 0xFFFF3D8B, 0xFFB4268C},
                new float[]{0f, 0.38f, 0.72f, 1f}, Shader.TileMode.CLAMP);

        float carScale = scaleAt(CAR_Z) * 1.15f;
        float carY = yAt(CAR_Z) - h * 0.045f;
        float carBh = 150f * carScale;
        carShader = new LinearGradient(0, carY - carBh * 0.42f, 0, carY + carBh * 0.06f,
                new int[]{0xFFEAF8FF, 0xFFA9DCF0, 0xFF4E89A8},
                new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP);

        mirrorShader = new LinearGradient(0, horizon, 0, horizon + (h - horizon) * 0.35f,
                0x44FF4D9D, 0x00FF4D9D, Shader.TileMode.CLAMP);

        glowSoft = new BlurMaskFilter(Math.max(3f, Math.min(w, h) * 0.012f),
                BlurMaskFilter.Blur.NORMAL);
        glowWide = new BlurMaskFilter(Math.max(6f, Math.min(w, h) * 0.035f),
                BlurMaskFilter.Blur.NORMAL);

        float bs = Math.min(w, h) * 0.13f;
        float pad = Math.min(w, h) * 0.035f;
        btnPause.set(pad, pad, pad + bs * 0.72f, pad + bs * 0.72f);
        btnLeft.set(pad, h - pad - bs, pad + bs * 1.25f, h - pad);
        btnRight.set(w - pad - bs * 1.25f, h - pad - bs, w - pad, h - pad);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        looping = false;
        try {
            if (loop != null) loop.join(500);
        } catch (InterruptedException ignored) {
        }
    }

    // --------------------------------------------------------------- Schleife

    @Override
    public void run() {
        long last = System.nanoTime();
        while (looping) {
            long now = System.nanoTime();
            float dt = (now - last) / 1_000_000_000f;
            last = now;
            if (dt > 0.05f) dt = 0.05f;

            if (w > 0) {
                update(dt);
                Canvas c = null;
                try {
                    c = getHolder().lockCanvas();
                    if (c != null) drawFrame(c);
                } finally {
                    if (c != null) getHolder().unlockCanvasAndPost(c);
                }
            }

            long sleep = 16_000_000L - (System.nanoTime() - now);
            if (sleep > 0) {
                try {
                    Thread.sleep(sleep / 1_000_000L, (int) (sleep % 1_000_000L));
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    private void update(float dt) {
        laneVisual += (lane - laneVisual) * Math.min(1f, dt * 12f);
        if (shake > 0) shake = Math.max(0, shake - dt * 2.5f);
        if (flash > 0) flash = Math.max(0, flash - dt * 2f);

        if (paused || over) {
            dashOffset += dt * 0.05f;
            return;
        }

        speed += dt * 0.055f;
        if (speed > 6.5f) speed = 6.5f;
        if (sfx != null) sfx.setEngineSpeed(speed);

        float advance = speed * dt;
        metres += advance * 26f;
        scoreF += advance * 48f;
        dashOffset += advance * 0.55f;

        timeLeft -= dt;
        if (timeLeft <= 0) {
            timeLeft = 0;
            finish(true);
            return;
        }

        for (int i = obstacles.size() - 1; i >= 0; i--) {
            Obstacle o = obstacles.get(i);
            o.z -= advance * 0.30f;
            if (!o.counted && o.z < HIT_Z) {
                o.counted = true;
                if (o.lane == lane) {
                    shake = 1f;
                    flash = 1f;
                    if (sfx != null) sfx.crash();
                    finish(false);
                    return;
                }
                scoreF += 60;
                if (sfx != null) sfx.passed();
            }
            if (o.z < -0.05f) obstacles.remove(i);
        }

        for (int i = trees.size() - 1; i >= 0; i--) {
            Tree t = trees.get(i);
            t.z -= advance * 0.30f;
            if (t.z < -0.05f) trees.remove(i);
        }

        spawnCountdown -= dt * speed;
        if (spawnCountdown <= 0) {
            spawnObstacles();
            spawnCountdown = 1.35f + rnd.nextFloat() * 0.5f;
        }

        if (trees.size() < 16 && rnd.nextFloat() < dt * speed * 6f) {
            Tree t = new Tree();
            t.z = 1.0f;
            t.side = rnd.nextBoolean() ? -1 : 1;
            t.scale = 0.75f + rnd.nextFloat() * 0.6f;
            t.kind = rnd.nextInt(3);
            trees.add(t);
        }
    }

    private void spawnObstacles() {
        int blocked = rnd.nextFloat() < 0.35f ? 2 : 1;
        boolean[] used = new boolean[LANES];
        for (int i = 0; i < blocked; i++) {
            int l = rnd.nextInt(LANES);
            if (used[l]) continue;
            used[l] = true;
            Obstacle o = new Obstacle();
            o.z = 1.0f;
            o.lane = l;
            o.kind = rnd.nextInt(3);
            obstacles.add(o);
        }
    }

    private void finish(boolean timeUp) {
        over = true;
        paused = true;
        final int s = (int) scoreF;
        final int m = (int) metres;
        post(() -> {
            if (listener != null) listener.onGameOver(s, m, timeUp);
        });
    }

    // ------------------------------------------------------------- Projektion

    private float scaleAt(float z) {
        return 1f / (1f + z * 9f);
    }

    private float yAt(float z) {
        return horizon + (h - horizon) * scaleAt(z);
    }

    private float halfAt(float z) {
        return roadHalf * scaleAt(z);
    }

    private float laneX(float laneIndex, float z) {
        return cx + (laneIndex - 1f) * halfAt(z) * 0.66f;
    }

    // -------------------------------------------------------------- Zeichnung

    private void drawFrame(Canvas c) {
        c.save();
        if (shake > 0) {
            c.translate((rnd.nextFloat() - 0.5f) * 18f * shake,
                    (rnd.nextFloat() - 0.5f) * 18f * shake);
        }

        drawSky(c);
        drawSkyline(c);
        drawRoad(c);
        drawRoadside(c);
        drawObstacles(c);
        drawCar(c);

        c.restore();

        if (flash > 0) {
            p.setShader(null);
            p.setColor(Color.argb((int) (140 * flash), 255, 60, 90));
            c.drawRect(0, 0, w, h, p);
        }

        drawHud(c);
        drawButtons(c);
    }

    private void drawSky(Canvas c) {
        p.setShader(skyShader);
        c.drawRect(0, 0, w, horizon, p);
        p.setShader(null);

        // Sternenfeld
        long seed = 1337;
        for (int i = 0; i < 46; i++) {
            seed = seed * 6364136223846793005L + 1442695040888963407L;
            float sx = ((seed >>> 33) % 10000) / 10000f * w;
            seed = seed * 6364136223846793005L + 1442695040888963407L;
            float sy = ((seed >>> 33) % 10000) / 10000f * horizon * 0.62f;
            seed = seed * 6364136223846793005L + 1442695040888963407L;
            int a = (int) (60 + ((seed >>> 33) % 140));
            p.setColor(Color.argb(a, 255, 255, 255));
            c.drawCircle(sx, sy, Math.max(1f, w * 0.0016f), p);
        }

        // Synthwave-Sonne mit Querstreifen
        float sx = w * 0.5f;
        p.setShader(null);
        p.setColor(0x33FF5C9E);
        p.setMaskFilter(glowWide);
        c.drawCircle(sx, sunY, sunR * 1.1f, p);
        p.setMaskFilter(null);

        p.setShader(sunShader);
        c.drawCircle(sx, sunY, sunR, p);
        p.setShader(null);

        p.setColor(0xFF1A0B2E);
        float band = sunR * 0.085f;
        float y = sunY + sunR * 0.12f;
        int n = 0;
        while (y < sunY + sunR) {
            float thick = band * (0.45f + n * 0.26f);
            c.drawRect(sx - sunR, y, sx + sunR, y + thick, p);
            y += thick + band * 0.78f;
            n++;
        }
    }

    private void buildSkyline() {
        skyline.clear();
        float x = -0.05f;
        long seed = 99;
        while (x < 1.15f) {
            Building b = new Building();
            b.x = x;
            seed = seed * 6364136223846793005L + 1442695040888963407L;
            float r1 = ((seed >> 33) % 1000) / 1000f;
            seed = seed * 6364136223846793005L + 1442695040888963407L;
            float r2 = ((seed >> 33) % 1000) / 1000f;
            b.w = 0.045f + Math.abs(r1) * 0.055f;
            b.h = 0.22f + Math.abs(r2) * 0.62f;
            b.seed = seed;
            skyline.add(b);
            x += b.w + 0.012f;
        }
    }

    private void drawSkyline(Canvas c) {
        float shift = (metres * 0.00012f) % 1f;

        for (int pass = 0; pass < 2; pass++) {
            float depth = pass == 0 ? 0.55f : 1f;
            int colour = pass == 0 ? 0xFF2A1140 : 0xFF190A2B;
            float factor = pass == 0 ? 0.5f : 1f;

            for (Building b : skyline) {
                float bx = ((b.x - shift * factor) % 1.25f + 1.25f) % 1.25f - 0.12f;
                float left = bx * w;
                float bw = b.w * w;
                float bh = b.h * horizon * depth;
                float top = horizon - bh;
                if (left > w || left + bw < 0) continue;

                p.setShader(null);
                p.setColor(colour);
                c.drawRect(left, top, left + bw, horizon, p);

                if (pass == 1) {
                    p.setColor(0xCC38E0F0);
                    long s = b.seed;
                    float cell = Math.max(5f, bw / 5f);
                    for (float wy = top + cell; wy < horizon - cell; wy += cell * 1.9f) {
                        for (float wx = left + cell * 0.5f; wx < left + bw - cell * 0.5f; wx += cell * 1.6f) {
                            s = s * 6364136223846793005L + 1442695040888963407L;
                            if (((s >> 35) & 7) < 3) {
                                c.drawRect(wx, wy, wx + cell * 0.55f, wy + cell * 0.4f, p);
                            }
                        }
                    }
                }
            }
        }

        drawTower(c, w * 0.30f);
    }

    /** Markanter Fernsehturm als Silhouette. */
    private void drawTower(Canvas c, float tx) {
        float base = horizon;
        float height = horizon * 0.95f;
        float top = base - height;

        p.setShader(null);
        p.setColor(0xFF140821);
        path.reset();
        path.moveTo(tx - height * 0.040f, base);
        path.lineTo(tx - height * 0.016f, top + height * 0.30f);
        path.lineTo(tx + height * 0.016f, top + height * 0.30f);
        path.lineTo(tx + height * 0.040f, base);
        path.close();
        c.drawPath(path, p);

        float ballY = top + height * 0.27f;
        float br = height * 0.065f;
        c.drawCircle(tx, ballY, br, p);
        p.setColor(0x5538E0F0);
        c.drawCircle(tx, ballY, br * 0.55f, p);

        p.setColor(0xFF140821);
        c.drawRect(tx - height * 0.009f, top, tx + height * 0.009f, ballY, p);
        p.setColor(0xFFFF4D6D);
        c.drawCircle(tx, top + height * 0.012f, Math.max(2f, height * 0.011f), p);
    }

    private void drawRoad(Canvas c) {
        p.setShader(null);
        p.setColor(0xFF0B0718);
        c.drawRect(0, horizon, w, h, p);

        // Neon-Widerschein des Himmels auf dem nassen Asphalt
        p.setShader(mirrorShader);
        c.drawRect(0, horizon, w, horizon + (h - horizon) * 0.35f, p);
        p.setShader(null);

        float yFar = yAt(1f), yNear = h;
        float hFar = halfAt(1f), hNear = halfAt(0f);

        p.setShader(roadShader);
        path.reset();
        path.moveTo(cx - hFar, yFar);
        path.lineTo(cx + hFar, yFar);
        path.lineTo(cx + hNear, yNear);
        path.lineTo(cx - hNear, yNear);
        path.close();
        c.drawPath(path, p);
        p.setShader(null);

        // Gitterlinien quer zur Fahrtrichtung
        p.setStyle(Paint.Style.STROKE);
        for (int i = 0; i < 24; i++) {
            float z = ((i * 0.05f) - (dashOffset % 0.05f));
            if (z <= 0.006f || z > 1f) continue;
            float sc = scaleAt(z);
            int alpha = (int) (95 * sc * 4f);
            if (alpha > 95) alpha = 95;
            p.setColor(Color.argb(alpha, 86, 226, 255));
            p.setStrokeWidth(Math.max(1f, 3.2f * sc));
            float gy = yAt(z), gh = halfAt(z);
            c.drawLine(cx - gh, gy, cx + gh, gy, p);
        }
        p.setStyle(Paint.Style.FILL);

        drawRail(c, -1, 0xFF31E7FF);
        drawRail(c, 1, 0xFFFF4D9D);

        // Fahrbahnmarkierungen zwischen den Spuren
        for (int d = 0; d < 2; d++) {
            float laneEdge = d == 0 ? 0.33f : 1.67f;
            for (int i = 0; i < 26; i++) {
                float z = ((i * 0.045f) - (dashOffset % 0.045f));
                if (z <= 0.004f || z > 1f) continue;
                float z2 = z + 0.020f;
                if (z2 > 1f) continue;
                float s1 = scaleAt(z), s2 = scaleAt(z2);
                float x1 = cx + (laneEdge - 1f) * halfAt(z) * 0.66f;
                float x2 = cx + (laneEdge - 1f) * halfAt(z2) * 0.66f;
                float wdt1 = Math.max(1.2f, 9f * s1);
                float wdt2 = Math.max(1f, 9f * s2);
                path.reset();
                path.moveTo(x1 - wdt1, yAt(z));
                path.lineTo(x1 + wdt1, yAt(z));
                path.lineTo(x2 + wdt2, yAt(z2));
                path.lineTo(x2 - wdt2, yAt(z2));
                path.close();
                p.setColor(0x66B9F2FF);
                p.setMaskFilter(glowSoft);
                c.drawPath(path, p);
                p.setMaskFilter(null);
                p.setColor(0xFFF4F8FF);
                c.drawPath(path, p);
            }
        }
    }

    private void drawRail(Canvas c, int side, int colour) {
        for (int pass = 0; pass < 2; pass++) {
            if (pass == 0) {
                p.setColor(Color.argb(120, Color.red(colour), Color.green(colour), Color.blue(colour)));
                p.setMaskFilter(glowWide);
            } else {
                p.setColor(colour);
                p.setMaskFilter(null);
            }
            for (int i = 0; i < 20; i++) {
                float z = ((i * 0.055f) - (dashOffset * 0.8f % 0.055f));
                if (z <= 0.01f || z > 0.95f) continue;
                float z2 = z + 0.030f;
                float y1 = yAt(z), y2 = yAt(z2);
                float x1 = cx + side * halfAt(z) * 1.12f;
                float x2 = cx + side * halfAt(z2) * 1.12f;
                float t1 = Math.max(1.8f, 8f * scaleAt(z));
                path.reset();
                path.moveTo(x1, y1);
                path.lineTo(x1 + side * t1, y1);
                path.lineTo(x2 + side * t1 * 0.6f, y2);
                path.lineTo(x2, y2);
                path.close();
                c.drawPath(path, p);
            }
        }
        p.setMaskFilter(null);

        // Spiegelung auf der Fahrbahn
        p.setColor(Color.argb(40, Color.red(colour), Color.green(colour), Color.blue(colour)));
        path.reset();
        path.moveTo(cx + side * halfAt(1f) * 0.98f, yAt(1f));
        path.lineTo(cx + side * halfAt(1f) * 0.86f, yAt(1f));
        path.lineTo(cx + side * halfAt(0f) * 0.80f, h);
        path.lineTo(cx + side * halfAt(0f) * 0.98f, h);
        path.close();
        c.drawPath(path, p);
    }

    private void drawRoadside(Canvas c) {
        for (int i = trees.size() - 1; i >= 0; i--) {
            Tree t = trees.get(i);
            if (t.z <= 0.01f || t.z > 1f) continue;
            float s = scaleAt(t.z);
            float x = cx + t.side * halfAt(t.z) * (1.45f + t.kind * 0.18f);
            float y = yAt(t.z);
            float size = 150f * s * t.scale;
            if (size < 2f) continue;

            p.setShader(null);
            p.setColor(0xFF120B20);
            c.drawRect(x - size * 0.07f, y - size * 0.55f, x + size * 0.07f, y, p);

            p.setColor(t.kind == 0 ? 0xFF1C2E4A : (t.kind == 1 ? 0xFF22314F : 0xFF1A2742));
            if (t.kind == 2) {
                path.reset();
                path.moveTo(x, y - size * 1.5f);
                path.lineTo(x + size * 0.42f, y - size * 0.45f);
                path.lineTo(x - size * 0.42f, y - size * 0.45f);
                path.close();
                c.drawPath(path, p);
            } else {
                c.drawCircle(x, y - size * 0.95f, size * 0.46f, p);
                c.drawCircle(x - size * 0.3f, y - size * 0.68f, size * 0.33f, p);
                c.drawCircle(x + size * 0.3f, y - size * 0.68f, size * 0.33f, p);
            }
        }
    }

    private void drawObstacles(Canvas c) {
        for (int i = obstacles.size() - 1; i >= 0; i--) {
            Obstacle o = obstacles.get(i);
            if (o.z <= 0.005f || o.z > 1f) continue;
            float s = scaleAt(o.z);
            float x = laneX(o.lane, o.z);
            float y = yAt(o.z);
            float bw = 190f * s;
            float bh = 95f * s;
            if (bw < 3f) continue;

            p.setShader(null);
            p.setColor(0x66000000);
            rect.set(x - bw * 0.55f, y - bh * 0.1f, x + bw * 0.55f, y + bh * 0.28f);
            c.drawOval(rect, p);

            if (o.kind == 2) {
                // Oelfass
                p.setColor(0xFFCC3344);
                rect.set(x - bw * 0.28f, y - bh * 1.5f, x + bw * 0.28f, y);
                c.drawRect(rect, p);
                p.setColor(0xFFF0E6D2);
                c.drawRect(x - bw * 0.28f, y - bh * 1.05f, x + bw * 0.28f, y - bh * 0.85f, p);
                c.drawRect(x - bw * 0.28f, y - bh * 0.5f, x + bw * 0.28f, y - bh * 0.3f, p);
            } else {
                // Bake mit rot-weissen Schraegstreifen
                float top = y - bh * (o.kind == 0 ? 1.25f : 1.6f);
                float bot = y - bh * 0.15f;
                p.setColor(0xFFF2EADF);
                c.drawRect(x - bw * 0.5f, top, x + bw * 0.5f, bot, p);

                p.setColor(0xFFE23B4E);
                c.save();
                c.clipRect(x - bw * 0.5f, top, x + bw * 0.5f, bot);
                float stripe = bw * 0.22f;
                for (float sx = x - bw * 0.9f; sx < x + bw * 0.9f; sx += stripe * 2f) {
                    path.reset();
                    path.moveTo(sx, bot);
                    path.lineTo(sx + stripe, bot);
                    path.lineTo(sx + stripe + (bot - top) * 0.6f, top);
                    path.lineTo(sx + (bot - top) * 0.6f, top);
                    path.close();
                    c.drawPath(path, p);
                }
                c.restore();

                p.setColor(0xFF2B2438);
                c.drawRect(x - bw * 0.5f, bot, x + bw * 0.5f, y, p);

                p.setColor(0xFFFFC24D);
                c.drawCircle(x + bw * 0.34f, top - bh * 0.16f, Math.max(1.5f, bw * 0.09f), p);
            }
        }
    }

    private void drawCar(Canvas c) {
        float s = scaleAt(CAR_Z) * 1.15f;
        float x = laneX(laneVisual, CAR_Z);
        float y = yAt(CAR_Z) - h * 0.045f;
        float bw = 300f * s;
        float bh = 150f * s;
        float tilt = (laneVisual - lane) * 0.16f;

        c.save();
        c.rotate((float) Math.toDegrees(tilt), x, y);
        p.setShader(null);

        // Unterbodenbeleuchtung
        p.setColor(0x88FF2E7E);
        p.setMaskFilter(glowWide);
        rect.set(x - bw * 0.50f, y + bh * 0.02f, x + bw * 0.50f, y + bh * 0.24f);
        c.drawOval(rect, p);
        p.setMaskFilter(null);

        p.setColor(0x66000000);
        rect.set(x - bw * 0.50f, y + bh * 0.06f, x + bw * 0.50f, y + bh * 0.26f);
        c.drawOval(rect, p);

        // Dach
        p.setColor(0xFF8EC6DE);
        path.reset();
        path.moveTo(x - bw * 0.33f, y - bh * 0.94f);
        path.lineTo(x + bw * 0.33f, y - bh * 0.94f);
        path.lineTo(x + bw * 0.40f, y - bh * 0.36f);
        path.lineTo(x - bw * 0.40f, y - bh * 0.36f);
        path.close();
        c.drawPath(path, p);

        // Heckscheibe
        p.setColor(0xFF14243A);
        path.reset();
        path.moveTo(x - bw * 0.285f, y - bh * 0.88f);
        path.lineTo(x + bw * 0.285f, y - bh * 0.88f);
        path.lineTo(x + bw * 0.335f, y - bh * 0.46f);
        path.lineTo(x - bw * 0.335f, y - bh * 0.46f);
        path.close();
        c.drawPath(path, p);
        p.setColor(0x3349E8FF);
        c.drawPath(path, p);

        // Karosserie mit Verlauf
        p.setShader(carShader);
        rect.set(x - bw * 0.46f, y - bh * 0.42f, x + bw * 0.46f, y + bh * 0.06f);
        c.drawRoundRect(rect, bw * 0.045f, bw * 0.045f, p);
        p.setShader(null);

        // Neon-Kante entlang der Karosserie
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(1.5f, bw * 0.012f));
        p.setColor(0xCC49E8FF);
        c.drawRoundRect(rect, bw * 0.045f, bw * 0.045f, p);
        p.setStyle(Paint.Style.FILL);

        // Chrom-Stossstange
        p.setColor(0xFFDCE9F0);
        rect.set(x - bw * 0.48f, y + bh * 0.03f, x + bw * 0.48f, y + bh * 0.14f);
        c.drawRoundRect(rect, bw * 0.02f, bw * 0.02f, p);
        p.setColor(0x66FFFFFF);
        c.drawRect(x - bw * 0.48f, y + bh * 0.045f, x + bw * 0.48f, y + bh * 0.07f, p);

        // Rueckleuchten mit Schein
        for (int side = -1; side <= 1; side += 2) {
            float lx = x + side * bw * 0.385f;
            p.setColor(0xAAFF2E4E);
            p.setMaskFilter(glowSoft);
            rect.set(lx - bw * 0.075f, y - bh * 0.36f, lx + bw * 0.075f, y - bh * 0.06f);
            c.drawRoundRect(rect, bw * 0.02f, bw * 0.02f, p);
            p.setMaskFilter(null);
            p.setColor(0xFFFF5566);
            rect.set(lx - bw * 0.055f, y - bh * 0.34f, lx + bw * 0.055f, y - bh * 0.08f);
            c.drawRoundRect(rect, bw * 0.015f, bw * 0.015f, p);
            p.setColor(0xFFFFC9CF);
            c.drawRect(lx - bw * 0.03f, y - bh * 0.30f, lx + bw * 0.03f, y - bh * 0.14f, p);
        }

        // Kennzeichen
        p.setColor(0xFFF6F6F8);
        rect.set(x - bw * 0.20f, y - bh * 0.28f, x + bw * 0.20f, y - bh * 0.08f);
        c.drawRoundRect(rect, bw * 0.015f, bw * 0.015f, p);
        textPaint.setColor(0xFF15151F);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(bh * 0.165f);
        c.drawText("DDR  TR 601", x, y - bh * 0.125f, textPaint);

        // Raeder
        p.setColor(0xFF100C18);
        c.drawRect(x - bw * 0.505f, y - bh * 0.18f, x - bw * 0.43f, y + bh * 0.08f, p);
        c.drawRect(x + bw * 0.43f, y - bh * 0.18f, x + bw * 0.505f, y + bh * 0.08f, p);

        // Auspuffglimmen
        p.setColor(0x9949E8FF);
        p.setMaskFilter(glowSoft);
        c.drawCircle(x - bw * 0.30f, y + bh * 0.15f, bw * 0.03f, p);
        p.setMaskFilter(null);

        c.restore();
    }

    private void drawHud(Canvas c) {
        float pad = Math.min(w, h) * 0.035f;
        float size = Math.min(w, h) * 0.052f;
        textPaint.setTextSize(size);
        textPaint.setTextAlign(Paint.Align.RIGHT);

        float right = w - pad;
        float y = pad + size;

        hudLine(c, "Punkte", formatScore((int) scoreF), right, y, 0xFFFFC24D);
        y += size * 1.35f;
        hudLine(c, "Zeit", formatTime(timeLeft) + " / " + formatTime(limit), right, y, 0xFF49E8FF);
        y += size * 1.35f;
        hudLine(c, "Strecke", (int) metres + " m", right, y, 0xFFFFFFFF);
    }

    private void hudLine(Canvas c, String label, String value, float right, float y, int colour) {
        textPaint.setColor(colour);
        textPaint.setTextAlign(Paint.Align.RIGHT);
        c.drawText(value, right, y, textPaint);
        float vw = textPaint.measureText(value);
        textPaint.setColor(0xFFCBC3E0);
        c.drawText(label + ":", right - vw - textPaint.getTextSize() * 0.45f, y, textPaint);
    }

    private void drawButtons(Canvas c) {
        drawGlassButton(c, btnPause, 0xFF49E8FF);
        p.setColor(0xFFEAF6FF);
        float bx = btnPause.centerX(), by = btnPause.centerY();
        float bw = btnPause.width() * 0.13f, bh = btnPause.height() * 0.3f;
        c.drawRect(bx - bw * 2.1f, by - bh, bx - bw * 0.3f, by + bh, p);
        c.drawRect(bx + bw * 0.3f, by - bh, bx + bw * 2.1f, by + bh, p);

        drawGlassButton(c, btnLeft, 0xFF49E8FF);
        drawGlassButton(c, btnRight, 0xFF49E8FF);
        p.setColor(0xFFEAF6FF);
        arrow(c, btnLeft, -1);
        arrow(c, btnRight, 1);
    }

    private void drawGlassButton(Canvas c, RectF r, int colour) {
        p.setShader(null);
        p.setStyle(Paint.Style.FILL);
        p.setColor(0x55140F24);
        c.drawRoundRect(r, r.height() * 0.28f, r.height() * 0.28f, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(2f, r.height() * 0.045f));
        p.setColor(colour);
        c.drawRoundRect(r, r.height() * 0.28f, r.height() * 0.28f, p);
        p.setStyle(Paint.Style.FILL);
    }

    private void arrow(Canvas c, RectF r, int dir) {
        float ax = r.centerX(), ay = r.centerY(), s = r.height() * 0.26f;
        path.reset();
        path.moveTo(ax + dir * s, ay);
        path.lineTo(ax - dir * s * 0.75f, ay - s * 0.85f);
        path.lineTo(ax - dir * s * 0.75f, ay + s * 0.85f);
        path.close();
        c.drawPath(path, p);
    }

    private static String formatScore(int value) {
        return String.format(java.util.Locale.GERMANY, "%,d", value);
    }

    private static String formatTime(float seconds) {
        int s = (int) Math.ceil(seconds);
        return String.format(java.util.Locale.GERMANY, "%d:%02d", s / 60, s % 60);
    }

    // ----------------------------------------------------------- Eingaben

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() != MotionEvent.ACTION_DOWN) return true;
        float x = e.getX(), y = e.getY();

        if (btnPause.contains(x, y)) {
            if (listener != null) listener.onPauseRequested();
            return true;
        }
        if (btnLeft.contains(x, y)) {
            moveLeft();
            return true;
        }
        if (btnRight.contains(x, y)) {
            moveRight();
            return true;
        }
        if (x < w * 0.5f) moveLeft();
        else moveRight();
        return true;
    }
}
