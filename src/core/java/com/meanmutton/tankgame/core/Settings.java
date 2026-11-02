package com.meanmutton.tankgame.core;

import java.util.ArrayList;
import java.util.List;

/** Match settings chosen on the setup screen. Remembered between launches. */
public final class Settings {
    public static final int[] CASH_OPTIONS = {0, 2500, 5000, 10000, 20000, 35000, 50000, 100000};
    public static final String[] TERRAIN_NAMES = {"Random", "Rolling Hills", "Mountains", "Plains", "Mesas", "Valley", "Islands"};
    public static final String[] WIND_NAMES = {"None", "Calm", "Breezy", "Gusty"};
    public static final int[] WIND_MAX = {0, 20, 50, 100};
    public static final String[] WALL_NAMES = {"Open", "Bounce", "Wrap"};
    public static final String[] AIM_NAMES = {"Off", "Short", "Long"};
    public static final String[] SPEED_NAMES = {"Normal", "Fast", "Very Fast"};

    public int rounds = 5;
    public int cashIndex = 3;
    public int terrain = 0;
    public int wind = 2;
    public boolean windChanges = true;
    public int walls = 0;
    public int aimGuide = 0;
    public int aiSpeed = 1;
    public boolean sound = true;
    public final List<PlayerSetup> players = new ArrayList<PlayerSetup>();

    public static final class PlayerSetup {
        public String name;
        public int color;   // index into TankArt.COLORS
        public int style;   // index into TankArt.STYLE_NAMES
        public int ai;      // -1 = human, else index into Ai.PROFILES

        public PlayerSetup(String name, int color, int style, int ai) {
            this.name = name;
            this.color = color;
            this.style = style;
            this.ai = ai;
        }
    }

    public int startCash() {
        return CASH_OPTIONS[Math.max(0, Math.min(CASH_OPTIONS.length - 1, cashIndex))];
    }

    public static Settings defaults() {
        Settings s = new Settings();
        s.players.add(new PlayerSetup("Player 1", 0, 0, -1));
        s.players.add(new PlayerSetup(Ai.PROFILES[2].name, 7, 1, 2));
        s.players.add(new PlayerSetup(Ai.PROFILES[5].name, 4, 6, 5));
        s.players.add(new PlayerSetup(Ai.PROFILES[7].name, 8, 3, 7));
        return s;
    }

    // ---- persistence (simple, dependency free) ----

    public String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append("v1;").append(rounds).append(';').append(cashIndex).append(';').append(terrain).append(';')
                .append(wind).append(';').append(windChanges ? 1 : 0).append(';').append(walls).append(';')
                .append(aimGuide).append(';').append(aiSpeed).append(';').append(sound ? 1 : 0);
        for (PlayerSetup p : players) {
            sb.append(';').append(clean(p.name)).append(',').append(p.color).append(',').append(p.style)
                    .append(',').append(p.ai);
        }
        return sb.toString();
    }

    public static String clean(String s) {
        if (s == null) return "";
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length() && b.length() < 14; i++) {
            char c = s.charAt(i);
            if (c == ';' || c == ',' || c < 32) continue;
            b.append(c);
        }
        return b.toString().trim();
    }

    public static Settings parse(String data) {
        if (data == null || !data.startsWith("v1;")) return defaults();
        try {
            String[] f = data.split(";");
            Settings s = new Settings();
            s.rounds = clampInt(Integer.parseInt(f[1]), 1, 20);
            s.cashIndex = clampInt(Integer.parseInt(f[2]), 0, CASH_OPTIONS.length - 1);
            s.terrain = clampInt(Integer.parseInt(f[3]), 0, TERRAIN_NAMES.length - 1);
            s.wind = clampInt(Integer.parseInt(f[4]), 0, WIND_NAMES.length - 1);
            s.windChanges = Integer.parseInt(f[5]) != 0;
            s.walls = clampInt(Integer.parseInt(f[6]), 0, WALL_NAMES.length - 1);
            s.aimGuide = clampInt(Integer.parseInt(f[7]), 0, AIM_NAMES.length - 1);
            s.aiSpeed = clampInt(Integer.parseInt(f[8]), 0, SPEED_NAMES.length - 1);
            s.sound = Integer.parseInt(f[9]) != 0;
            for (int i = 10; i < f.length && s.players.size() < 10; i++) {
                String[] p = f[i].split(",");
                if (p.length < 4) continue;
                String name = p[0].length() == 0 ? "Player" : p[0];
                s.players.add(new PlayerSetup(name,
                        clampInt(Integer.parseInt(p[1]), 0, TankArt.COLORS.length - 1),
                        clampInt(Integer.parseInt(p[2]), 0, TankArt.STYLE_NAMES.length - 1),
                        clampInt(Integer.parseInt(p[3]), -1, Ai.PROFILES.length - 1)));
            }
            if (s.players.size() < 2) return defaults();
            return s;
        } catch (RuntimeException e) {
            return defaults();
        }
    }

    static int clampInt(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
