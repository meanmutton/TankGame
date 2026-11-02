package com.meanmutton.tankgame.core;

/** Procedural tank drawings: nine body styles in twelve colors. */
public final class TankArt {
    private TankArt() {}

    public static final String[] STYLE_NAMES = {
            "Classic", "Heavy", "Scout", "Dome", "Artillery", "Hover", "Spiker", "Retro", "Twin Cannon"
    };

    public static final int[] COLORS = {
            0xFFE84A3C, 0xFFFF8E2B, 0xFFF5D33B, 0xFF9BE043, 0xFF3DAA4F, 0xFF2BB8A6,
            0xFF45C8F0, 0xFF3F6FE0, 0xFF9A5AE6, 0xFFF06AB4, 0xFFEDEDED, 0xFF5A5F6B
    };
    public static final String[] COLOR_NAMES = {
            "Red", "Orange", "Yellow", "Lime", "Green", "Teal", "Sky", "Blue", "Purple", "Pink", "White", "Steel"
    };

    private static final float[] PX = new float[16], PY = new float[16];

    /**
     * Draws a tank whose tread bottom is at (x, y), in world units (W = 40 wide).
     * Use g.scale for bigger previews.
     */
    public static void draw(Gfx g, int style, int color, float x, float y, float angle, float time) {
        int dark = Col.darker(color, 0.45f);
        int darker = Col.darker(color, 0.65f);
        int light = Col.lighter(color, 0.35f);
        int tread = 0xFF2A2D33;
        int treadHi = 0xFF4A4F58;

        // Barrel first so the body overlaps its base.
        float rad = (float) Math.toRadians(angle);
        float cx = x, cy = y - Tank.PIVOT_Y;
        float ca = (float) Math.cos(rad), sa = (float) -Math.sin(rad);
        switch (style) {
            case 1: barrel(g, cx, cy, ca, sa, 22, 5.5f, dark); break;
            case 2: barrel(g, cx, cy, ca, sa, 27, 2.6f, dark); break;
            case 4: barrel(g, cx, cy, ca, sa, 30, 4f, dark);
                g.fillCircle(cx + ca * 30, cy + sa * 30, 2.6f, darker);
                break;
            case 8: {
                float nx = -sa * 2.6f, ny = ca * 2.6f;
                barrel(g, cx + nx, cy + ny, ca, sa, 24, 2.6f, dark);
                barrel(g, cx - nx, cy - ny, ca, sa, 24, 2.6f, dark);
                break;
            }
            default: barrel(g, cx, cy, ca, sa, Tank.BARREL, 3.6f, dark);
        }

        switch (style) {
            case 0: { // Classic
                treads(g, x, y, 40, 8, tread, treadHi, 5);
                poly(g, color, x - 19, y - 7, x + 19, y - 7, x + 14, y - 14, x - 14, y - 14);
                g.fillRect(x - 14, y - 14, 28, 2, light);
                g.fillCircle(cx, cy + 1, 8, color);
                g.fillCircle(cx - 2, cy - 1, 3, light);
                break;
            }
            case 1: { // Heavy
                treads(g, x, y, 42, 9, tread, treadHi, 6);
                g.fillRect(x - 21, y - 9, 42, 4, dark);
                poly(g, color, x - 20, y - 8, x + 20, y - 8, x + 17, y - 16, x - 17, y - 16);
                g.fillRect(x - 17, y - 16, 34, 2, light);
                g.fillRoundRect(cx - 9, cy - 6, 18, 11, 2, color);
                g.fillRect(cx - 9, cy - 6, 18, 2, light);
                g.fillRect(cx + 3, cy - 9, 4, 4, darker);
                break;
            }
            case 2: { // Scout
                treads(g, x, y, 36, 6, tread, treadHi, 4);
                poly(g, color, x - 19, y - 6, x + 20, y - 6, x + 12, y - 12, x - 15, y - 11);
                g.fillRect(x - 15, y - 11.5f, 26, 1.5f, light);
                g.fillCircle(cx, cy + 2, 6, color);
                g.line(x - 12, y - 11, x - 16, y - 26, 1f, darker);
                g.fillCircle(x - 16, y - 26, 1.5f, (((int) (time * 3)) & 1) == 0 ? 0xFFFF4A4A : 0xFF661A1A);
                break;
            }
            case 3: { // Dome
                treads(g, x, y, 38, 7, tread, treadHi, 5);
                domeShape(g, x, y - 7, 18, 12, color);
                g.fillCircle(x - 6, y - 14, 4, Col.alpha(light, 0.7f));
                g.fillRect(x - 18, y - 8, 36, 2, dark);
                break;
            }
            case 4: { // Artillery
                treads(g, x, y, 42, 7, tread, treadHi, 6);
                g.fillRect(x - 20, y - 11, 40, 4, dark);
                g.fillRoundRect(x - 18, y - 17, 15, 8, 2, color);
                g.fillRect(x - 16, y - 16, 5, 3, 0xFF9ED8FF);
                g.fillCircle(cx, cy + 3, 6, color);
                g.line(x + 6, y - 9, cx, cy + 3, 2f, darker);
                g.line(x + 14, y - 9, cx + 2, cy + 3, 2f, darker);
                break;
            }
            case 5: { // Hover
                float bob = (float) Math.sin(time * 5f + x) * 1.2f;
                g.fillRadial(x - 11, y - 1, 7, 0xCC6AE8FF, 0x006AE8FF);
                g.fillRadial(x + 11, y - 1, 7, 0xCC6AE8FF, 0x006AE8FF);
                g.fillRoundRect(x - 16, y - 5 + bob, 10, 3, 1.5f, darker);
                g.fillRoundRect(x + 6, y - 5 + bob, 10, 3, 1.5f, darker);
                poly(g, color, x - 21, y - 8 + bob, x + 21, y - 8 + bob, x + 15, y - 4 + bob, x - 15, y - 4 + bob);
                domeShape(g, x, y - 8 + bob, 14, 7, light);
                g.fillCircle(cx, cy + 1 + bob, 5.5f, color);
                break;
            }
            case 6: { // Spiker
                treads(g, x, y, 40, 8, tread, treadHi, 5);
                for (int i = 0; i < 5; i++) {
                    float sx = x - 16 + i * 8;
                    poly(g, 0xFFD0D4DA, sx - 2.5f, y - 13, sx + 2.5f, y - 13, sx, y - 20);
                }
                poly(g, color, x - 20, y - 7, x + 20, y - 7, x + 16, y - 14, x - 16, y - 14);
                poly(g, dark, cx - 9, cy + 4, cx + 9, cy + 4, cx + 5, cy - 5, cx - 5, cy - 5);
                g.fillRect(x - 16, y - 14, 32, 1.5f, light);
                break;
            }
            case 7: { // Retro
                g.fillRoundRect(x - 20, y - 9, 40, 9, 4.5f, tread);
                for (int i = 0; i < 3; i++) {
                    float wx = x - 13 + i * 13;
                    g.fillCircle(wx, y - 4.5f, 4.2f, treadHi);
                    g.fillCircle(wx, y - 4.5f, 1.6f, tread);
                }
                g.fillRoundRect(x - 19, y - 16, 38, 9, 4f, color);
                for (int i = 0; i < 6; i++) g.fillCircle(x - 15 + i * 6, y - 12, 0.9f, darker);
                g.fillCircle(cx, cy + 1, 8.5f, color);
                star(g, cx, cy + 1, 4, 0xFFFFFFFF);
                break;
            }
            default: { // Twin cannon
                treads(g, x, y, 40, 8, tread, treadHi, 5);
                poly(g, color, x - 20, y - 7, x + 20, y - 7, x + 15, y - 14, x - 15, y - 14);
                g.fillRoundRect(cx - 10, cy - 5, 20, 10, 5, color);
                g.fillRect(cx - 10, cy - 5, 20, 2, light);
                g.fillCircle(cx, cy, 2.5f, darker);
                break;
            }
        }
    }

    private static void barrel(Gfx g, float x, float y, float ca, float sa, float len, float w, int color) {
        g.line(x, y, x + ca * len, y + sa * len, w, color);
    }

    private static void treads(Gfx g, float x, float y, float w, float h, int tread, int hi, int wheels) {
        g.fillRoundRect(x - w / 2, y - h, w, h, h / 2, tread);
        float step = (w - h) / (wheels - 1);
        for (int i = 0; i < wheels; i++) {
            g.fillCircle(x - w / 2 + h / 2 + i * step, y - h / 2, h * 0.32f, hi);
        }
    }

    private static void poly(Gfx g, int color, float... pts) {
        int n = pts.length / 2;
        for (int i = 0; i < n; i++) {
            PX[i] = pts[i * 2];
            PY[i] = pts[i * 2 + 1];
        }
        g.fillPoly(PX, PY, n, color);
    }

    private static void domeShape(Gfx g, float cx, float baseY, float rx, float ry, int color) {
        int n = 12;
        for (int i = 0; i <= n - 2; i++) {
            double a = Math.PI * i / (n - 2);
            PX[i] = cx + (float) Math.cos(a) * rx;
            PY[i] = baseY - (float) Math.sin(a) * ry;
        }
        PX[n - 1] = cx - rx;
        PY[n - 1] = baseY;
        g.fillPoly(PX, PY, n, color);
    }

    private static void star(Gfx g, float cx, float cy, float r, int color) {
        for (int i = 0; i < 10; i++) {
            double a = -Math.PI / 2 + i * Math.PI / 5;
            float rr = (i % 2 == 0) ? r : r * 0.42f;
            PX[i] = cx + (float) Math.cos(a) * rr;
            PY[i] = cy + (float) Math.sin(a) * rr;
        }
        g.fillPoly(PX, PY, 10, color);
    }

    /** Draws a tank centered in a box (for menus). */
    public static void drawIcon(Gfx g, int style, int color, float cx, float cy, float size, float angle, float time) {
        float s = size / 48f;
        g.save();
        g.translate(cx, cy + 14 * s);
        g.scale(s, s);
        draw(g, style, color, 0, 0, angle, time);
        g.restore();
    }
}
