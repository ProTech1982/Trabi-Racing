package de.protech1982.trabiracing;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Geraeusche des Spiels, ebenfalls im Code erzeugt: Motorbrummen als
 * Dauerton sowie kurze Effekte fuer Spurwechsel, Vorbeifahrt und Crash.
 * Laeuft getrennt von der Musik und hat einen eigenen Schalter.
 */
public class Sfx {

    private static final int RATE = 22050;

    private volatile boolean enabled = true;
    private volatile boolean engineWanted;
    private volatile float engineSpeed = 1f;

    private final BlockingQueue<short[]> queue = new ArrayBlockingQueue<>(8);
    private Thread fxThread;
    private Thread engineThread;

    public void setEnabled(boolean on) {
        enabled = on;
        if (!on) {
            queue.clear();
            stopEngine();
        } else if (engineWanted) {
            startEngine();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    // ------------------------------------------------------------- Effekte

    public void laneChange() {
        play(sweep(0.085f, 620, 980, 0.22f, 0.5f));
    }

    public void passed() {
        short[] a = tone(0.055f, 880, 0.18f, 0.5f);
        short[] b = tone(0.075f, 1320, 0.18f, 0.5f);
        short[] both = new short[a.length + b.length];
        System.arraycopy(a, 0, both, 0, a.length);
        System.arraycopy(b, 0, both, a.length, b.length);
        play(both);
    }

    public void crash() {
        int n = (int) (RATE * 0.42f);
        short[] out = new short[n];
        long seed = 0x9e37;
        double phase = 0;
        for (int i = 0; i < n; i++) {
            double t = (double) i / n;
            double env = Math.pow(1.0 - t, 1.8);
            seed = seed * 6364136223846793005L + 1442695040888963407L;
            double noise = ((seed >> 41) & 0x7FF) / 1023.5 - 1.0;
            double freq = 320 - 240 * t;
            phase += 2 * Math.PI * freq / RATE;
            double sq = ((phase / (2 * Math.PI)) % 1.0) < 0.5 ? 1 : -1;
            double s = (noise * 0.6 + sq * 0.4) * env * 0.85;
            out[i] = (short) (Math.max(-1, Math.min(1, s)) * 12000);
        }
        play(out);
    }

    private short[] tone(float seconds, double freq, double amp, double duty) {
        int n = (int) (RATE * seconds);
        short[] out = new short[n];
        double phase = 0, step = 2 * Math.PI * freq / RATE;
        for (int i = 0; i < n; i++) {
            double t = (double) i / n;
            double env = Math.min(1.0, (1.0 - t) * 3.0);
            double p = (phase / (2 * Math.PI)) % 1.0;
            out[i] = (short) ((p < duty ? 1 : -1) * amp * env * 12000);
            phase += step;
        }
        return out;
    }

    private short[] sweep(float seconds, double from, double to, double amp, double duty) {
        int n = (int) (RATE * seconds);
        short[] out = new short[n];
        double phase = 0;
        for (int i = 0; i < n; i++) {
            double t = (double) i / n;
            double freq = from + (to - from) * t;
            double env = Math.min(1.0, (1.0 - t) * 2.6);
            double p = (phase / (2 * Math.PI)) % 1.0;
            out[i] = (short) ((p < duty ? 1 : -1) * amp * env * 12000);
            phase += 2 * Math.PI * freq / RATE;
        }
        return out;
    }

    private void play(short[] buffer) {
        if (!enabled) return;
        ensureFxThread();
        queue.offer(buffer);
    }

    private synchronized void ensureFxThread() {
        if (fxThread != null) return;
        fxThread = new Thread(() -> {
            int min = AudioTrack.getMinBufferSize(RATE,
                    AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
            AudioTrack t;
            try {
                t = new AudioTrack(AudioManager.STREAM_MUSIC, RATE,
                        AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                        Math.max(min, RATE / 2), AudioTrack.MODE_STREAM);
                t.play();
            } catch (Exception e) {
                return;
            }
            try {
                while (true) {
                    short[] buf = queue.take();
                    t.write(buf, 0, buf.length);
                }
            } catch (InterruptedException | IllegalStateException ignored) {
            } finally {
                try {
                    t.stop();
                    t.release();
                } catch (Exception ignored) {
                }
            }
        }, "sfx");
        fxThread.setDaemon(true);
        fxThread.start();
    }

    // -------------------------------------------------------------- Motor

    public void setEngineSpeed(float speed) {
        engineSpeed = speed;
    }

    public synchronized void startEngine() {
        engineWanted = true;
        if (!enabled || engineThread != null) return;
        engineThread = new Thread(this::engineLoop, "engine");
        engineThread.setDaemon(true);
        engineThread.start();
    }

    public synchronized void stopEngine() {
        engineWanted = false;
        Thread t = engineThread;
        engineThread = null;
        if (t != null) t.interrupt();
    }

    private void engineLoop() {
        int min = AudioTrack.getMinBufferSize(RATE,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        int chunk = Math.max(min / 2, RATE / 20);
        AudioTrack t;
        try {
            t = new AudioTrack(AudioManager.STREAM_MUSIC, RATE,
                    AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                    Math.max(min, chunk * 4), AudioTrack.MODE_STREAM);
            t.play();
        } catch (Exception e) {
            return;
        }

        short[] buf = new short[chunk];
        double phase = 0, putter = 0;
        long seed = 0x1234;

        while (engineWanted && enabled) {
            double freq = 46 + engineSpeed * 15;
            double step = 2 * Math.PI * freq / RATE;
            double putterStep = 2 * Math.PI * (freq / 3.0) / RATE;

            for (int i = 0; i < chunk; i++) {
                double p = (phase / (2 * Math.PI)) % 1.0;
                double saw = p * 2 - 1;
                double sq = p < 0.35 ? 1 : -1;
                double mod = 0.65 + 0.35 * Math.sin(putter);
                seed = seed * 6364136223846793005L + 1442695040888963407L;
                double noise = ((seed >> 45) & 0x3F) / 31.5 - 1.0;
                double s = (saw * 0.5 + sq * 0.5) * mod * 0.08 + noise * 0.012;
                buf[i] = (short) (Math.max(-1, Math.min(1, s)) * 12000);
                phase += step;
                putter += putterStep;
            }
            try {
                t.write(buf, 0, buf.length);
            } catch (Exception e) {
                break;
            }
        }

        try {
            t.stop();
            t.release();
        } catch (Exception ignored) {
        }
    }

    public void release() {
        stopEngine();
        queue.clear();
        Thread t = fxThread;
        fxThread = null;
        if (t != null) t.interrupt();
    }
}
