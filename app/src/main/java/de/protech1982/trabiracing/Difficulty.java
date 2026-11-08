package de.protech1982.trabiracing;

/**
 * Schwierigkeitsstufen. Tempo-Einheit ist wie im Spiel "speed";
 * km/h = speed * 17.5. Je hoeher die Stufe, desto schneller der Start,
 * desto steiler der Anstieg und desto dichter die Hindernisse.
 */
public final class Difficulty {

    public static final int EASY = 0;
    public static final int MEDIUM = 1;
    public static final int HARD = 2;
    public static final int VERY_HARD = 3;
    public static final int COUNT = 4;

    public static final String[] NAMES = {"Einfach", "Mittel", "Schwer", "Sehr schwer"};

    /** Punkte-Multiplikator je Stufe. */
    public static final float[] SCORE_MULT = {1f, 1.5f, 2f, 3f};

    /** Starttempo. */
    public static final float[] START_SPEED = {1.0f, 1.0f, 3.0f, 4.5f};

    /** Tempozuwachs pro Sekunde. */
    public static final float[] ACCEL = {0.025f, 0.055f, 0.085f, 0.12f};

    /** Hoechsttempo. */
    public static final float[] MAX_SPEED = {4.0f, 6.5f, 8.0f, 9.5f};

    /** Mindestabstand zwischen zwei Hindernisreihen. */
    public static final float[] GAP_MIN = {1.60f, 1.35f, 1.20f, 1.05f};

    /** Zufaelliger Zusatzabstand. */
    public static final float[] GAP_RANDOM = {0.60f, 0.50f, 0.40f, 0.35f};

    /** Wahrscheinlichkeit, dass zwei Spuren gleichzeitig blockiert sind. */
    public static final float[] DOUBLE_CHANCE = {0.20f, 0.35f, 0.45f, 0.55f};

    /** KI: Reaktionszeit in Sekunden, bis sie ein Hindernis bemerkt. */
    public static final float[] AI_REACT = {0.32f, 0.22f, 0.15f, 0.10f};

    /** KI: Pause zwischen zwei Spurwechseln in Sekunden. */
    public static final float[] AI_STEP = {0.26f, 0.19f, 0.14f, 0.11f};

    /** KI: Wahrscheinlichkeit, ein Hindernis zu uebersehen. */
    public static final float[] AI_MISTAKE = {0.12f, 0.06f, 0.025f, 0.008f};

    private Difficulty() {
    }

    public static int clamp(int level) {
        return Math.max(0, Math.min(COUNT - 1, level));
    }

    public static String label(int level) {
        return NAMES[clamp(level)];
    }

    /** Kurzform fuer die Bestenliste, z. B. "x1,5". */
    public static String multiplierText(int level) {
        float m = SCORE_MULT[clamp(level)];
        if (m == (int) m) return "×" + (int) m;
        return "×" + String.valueOf(m).replace('.', ',');
    }
}
