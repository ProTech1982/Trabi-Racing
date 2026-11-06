package de.protech1982.trabiracing;

/** Farbpalette und Stilschalter fuer die drei waehlbaren Designs. */
public class Theme {

    public static final int CYBERPUNK = 0;
    public static final int DDR = 1;
    public static final int MODERN = 2;

    public static final String[] NAMES = {"Cyberpunk", "DDR", "Modern"};

    public final String name;
    public final boolean neon;        // Leuchteffekte an
    public final boolean sunBands;    // Synthwave-Sonne mit Streifen
    public final boolean stars;
    public final boolean grid;        // Gitterlinien auf der Fahrbahn

    public final int[] sky;
    public final int[] sun;
    public final int buildingFar, buildingNear, window;
    public final int[] road;
    public final int ground;
    public final int railLeft, railRight;
    public final int dash, edge;
    public final int gridLine;
    public final int[] tree;
    public final int trunk;
    public final int[] carBody;
    public final int carRoof, carGlass, carGlassHi, carTrim;
    public final int tailLight, underglow;
    public final int accent, accent2, hudText;

    private Theme(String name, boolean neon, boolean sunBands, boolean stars, boolean grid,
                  int[] sky, int[] sun, int buildingFar, int buildingNear, int window,
                  int[] road, int ground, int railLeft, int railRight, int dash, int edge,
                  int gridLine, int[] tree, int trunk, int[] carBody, int carRoof,
                  int carGlass, int carGlassHi, int carTrim, int tailLight, int underglow,
                  int accent, int accent2, int hudText) {
        this.name = name;
        this.neon = neon;
        this.sunBands = sunBands;
        this.stars = stars;
        this.grid = grid;
        this.sky = sky;
        this.sun = sun;
        this.buildingFar = buildingFar;
        this.buildingNear = buildingNear;
        this.window = window;
        this.road = road;
        this.ground = ground;
        this.railLeft = railLeft;
        this.railRight = railRight;
        this.dash = dash;
        this.edge = edge;
        this.gridLine = gridLine;
        this.tree = tree;
        this.trunk = trunk;
        this.carBody = carBody;
        this.carRoof = carRoof;
        this.carGlass = carGlass;
        this.carGlassHi = carGlassHi;
        this.carTrim = carTrim;
        this.tailLight = tailLight;
        this.underglow = underglow;
        this.accent = accent;
        this.accent2 = accent2;
        this.hudText = hudText;
    }

    public static Theme of(int id) {
        switch (id) {
            case DDR:
                return new Theme("DDR", false, false, false, false,
                        new int[]{0xFF6E86A8, 0xFF93A4BC, 0xFFC2B49A, 0xFFE0CBA8},
                        new int[]{0xFFFFE9B8, 0xFFFFD98A, 0xFFEFC071, 0xFFD9A75E},
                        0xFF7C8294, 0xFF5E6373, 0xFFBFC4CF,
                        new int[]{0xFF6B6B6B, 0xFF585858, 0xFF636363}, 0xFF7E7A6A,
                        0xFFD9D4C6, 0xFFD9D4C6, 0xFFF2EFE4, 0xFFBDB8A8,
                        0x00000000,
                        new int[]{0xFF4A6B3C, 0xFF567A45, 0xFF3F5C33}, 0xFF5A4733,
                        new int[]{0xFFE8EFE4, 0xFFBFD0BE, 0xFF8FA790},
                        0xFFD5E0D2, 0xFF3F4A48, 0xFF9FB0AE, 0xFFC8C8C0,
                        0xFFCC3A3A, 0x00000000,
                        0xFFE4B33C, 0xFFB9442F, 0xFF2A2A24);
            case MODERN:
                return new Theme("Modern", false, false, false, false,
                        new int[]{0xFF0B2545, 0xFF1D4E89, 0xFF3E8FC4, 0xFF9CD3E8},
                        new int[]{0xFFFFFFFF, 0xFFFFF3D0, 0xFFFFD9A0, 0xFFFFC078},
                        0xFF2C4A6B, 0xFF1B3550, 0xFFBFE4F5,
                        new int[]{0xFF3A3F46, 0xFF2C3036, 0xFF373B42}, 0xFF2A4A32,
                        0xFFE8EDF2, 0xFFE8EDF2, 0xFFFFFFFF, 0xFFE2A32B,
                        0x00000000,
                        new int[]{0xFF2E7D4F, 0xFF34915C, 0xFF256B43}, 0xFF4B3A2A,
                        new int[]{0xFFFFFFFF, 0xFFD8DEE6, 0xFF9AA5B4},
                        0xFFEFF3F7, 0xFF1C2A3A, 0xFF7FA8C8, 0xFFB9C2CC,
                        0xFFE03B3B, 0x00000000,
                        0xFF2BB3F0, 0xFFFF8A3D, 0xFFFFFFFF);
            default:
                return new Theme("Cyberpunk", true, true, true, true,
                        new int[]{0xFF1A0B2E, 0xFF4A1B5C, 0xFF8E2D62, 0xFFD94B7A},
                        new int[]{0xFFFFE08A, 0xFFFF7A5C, 0xFFFF3D8B, 0xFFB4268C},
                        0xFF2A1140, 0xFF190A2B, 0xCC38E0F0,
                        new int[]{0xFF241A3A, 0xFF171229, 0xFF221B33}, 0xFF0B0718,
                        0xFF31E7FF, 0xFFFF4D9D, 0xFFF4F8FF, 0x66FFFFFF,
                        0x5F56E2FF,
                        new int[]{0xFF1C2E4A, 0xFF22314F, 0xFF1A2742}, 0xFF120B20,
                        new int[]{0xFFEAF8FF, 0xFFA9DCF0, 0xFF4E89A8},
                        0xFF8EC6DE, 0xFF14243A, 0xFF49E8FF, 0xFFDCE9F0,
                        0xFFFF5566, 0x88FF2E7E,
                        0xFF49E8FF, 0xFFFF4D9D, 0xFFFFC24D);
        }
    }
}
