package de.protech1982.trabiracing;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

/**
 * Erzeugt die Hintergrundmusik direkt im Code: Rechteck-Melodie, Rechteck-Bass
 * und ein Rausch-Hi-Hat, wie bei alten Heimcomputern. Keine Audiodateien noetig.
 */
public class Chiptune {

    private static final int RATE = 22050;
    private static final int BPM = 148;

    /** Melodie in MIDI-Noten, ein Eintrag je Sechzehntel, -1 = Pause. */
    private static final int[] MELODY = {
            69, -1, 76, -1, 72, -1, 76, -1, 81, -1, 79, -1, 76, -1, -1, -1,
            67, -1, 74, -1, 71, -1, 74, -1, 79, -1, 78, -1, 74, -1, -1, -1,
            65, -1, 72, -1, 69, -1, 72, -1, 77, -1, 76, -1, 72, -1, -1, -1,
            64, -1, 71, -1, 68, -1, 71, -1, 76, 75, 76, 78, 80, -1, -1, -1
    };

    private static final int[] BASS = {
            45, -1, 45, -1, 52, -1, 45, -1, 45, -1, 45, -1, 52, -1, 57, -1,
            43, -1, 43, -1, 50, -1, 43, -1, 43, -1, 43, -1, 50, -1, 55, -1,
            41, -1, 41, -1, 48, -1, 41, -1, 41, -1, 41, -1, 48, -1, 53, -1,
            40, -1, 40, -1, 47, -1, 40, -1, 40, -1, 47, -1, 52, -1, 52, -1
    };

    private volatile boolean running;
    private volatile boolean enabled = true;
    private Thread thread;
    private AudioTrack track;

    public void setEnabled(boolean on) {
        enabled = on;
        if (on) start();
        else stop();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public synchronized void start() {
        if (!enabled || running) return;
        running = true;
        thread = new Thread(this::loop, "chiptune");
        thread.start();
    }

    public synchronized void stop() {
        running = false;
        Thread t = thread;
        thread = null;
        if (t != null) t.interrupt();
    }

    private void loop() {
        int stepSamples = (RATE * 15) / BPM;          // Laenge einer Sechzehntel
        int minBuf = AudioTrack.getMinBufferSize(RATE,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        int buf = Math.max(minBuf, stepSamples * 4);

        AudioTrack t;
        try {
            t = new AudioTrack(AudioManager.STREAM_MUSIC, RATE,
                    AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                    buf, AudioTrack.MODE_STREAM);
            t.play();
        } catch (Exception e) {
            running = false;
            return;
        }
        track = t;

        short[] chunk = new short[stepSamples];
        double melPhase = 0, bassPhase = 0;
        int step = 0;
        long noise = 0x5eed;

        int melNote = -1, bassNote = -1;
        int melAge = 0, bassAge = 0;

        while (running) {
            int m = MELODY[step % MELODY.length];
            int b = BASS[step % BASS.length];
            if (m >= 0) { melNote = m; melAge = 0; } else { melAge++; }
            if (b >= 0) { bassNote = b; bassAge = 0; } else { bassAge++; }

            double melFreq = melNote < 0 ? 0 : midiToHz(melNote);
            double bassFreq = bassNote < 0 ? 0 : midiToHz(bassNote);
            double melStep = 2 * Math.PI * melFreq / RATE;
            double bassStep = 2 * Math.PI * bassFreq / RATE;

            boolean hat = (step % 2) == 1;

            for (int i = 0; i < stepSamples; i++) {
                double pos = (double) i / stepSamples;
                double sample = 0;

                if (melFreq > 0) {
                    double env = Math.max(0, 1.0 - (melAge + pos) * 0.42);
                    sample += square(melPhase, 0.5) * 0.26 * env;
                    melPhase += melStep;
                }
                if (bassFreq > 0) {
                    double env = Math.max(0, 1.0 - (bassAge + pos) * 0.75);
                    sample += square(bassPhase, 0.25) * 0.30 * env;
                    bassPhase += bassStep;
                }
                if (hat && pos < 0.18) {
                    noise = noise * 6364136223846793005L + 1442695040888963407L;
                    double n = ((noise >> 40) & 0xFF) / 127.5 - 1.0;
                    sample += n * 0.07 * (1.0 - pos / 0.18);
                }

                if (sample > 1) sample = 1;
                if (sample < -1) sample = -1;
                chunk[i] = (short) (sample * 9000);
            }

            try {
                t.write(chunk, 0, chunk.length);
            } catch (Exception e) {
                break;
            }
            step++;
        }

        try {
            t.stop();
            t.release();
        } catch (Exception ignored) {
        }
        track = null;
    }

    private static double midiToHz(int note) {
        return 440.0 * Math.pow(2.0, (note - 69) / 12.0);
    }

    private static double square(double phase, double duty) {
        double p = (phase / (2 * Math.PI)) % 1.0;
        if (p < 0) p += 1.0;
        return p < duty ? 1.0 : -1.0;
    }
}
