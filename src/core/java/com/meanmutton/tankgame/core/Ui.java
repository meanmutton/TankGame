package com.meanmutton.tankgame.core;

/** Shared UI drawing helpers. */
public final class Ui {
    private Ui() {}

    public static String money(int v) {
        boolean neg = v < 0;
        String s = Integer.toString(Math.abs(v));
        StringBuilder b = new StringBuilder();
        int c = 0;
        for (int i = s.length() - 1; i >= 0; i--) {
            b.append(s.charAt(i));
            if (++c % 3 == 0 && i > 0) b.append(',');
        }
        if (neg) b.append('-');
        return b.reverse().toString();
    }

    public static void panel(Gfx g, float x, float y, float w, float h, float r) {
        g.fillRoundRect(x, y + r * 0.25f, w, h, r, 0x55000000);
        g.fillRoundRect(x, y, w, h, r, Col.PANEL);
        g.strokeRoundRect(x, y, w, h, r, Math.max(1f, r * 0.12f), Col.BORDER);
    }

    public static void button(Gfx g, Btn b, float u) {
        if (!b.visible) return;
        float r = Math.min(b.h, b.w) * 0.22f;
        int base = b.color != 0 ? b.color : Col.PANEL_HI;
        if (b.selected && b.label.length() > 0) base = Col.lerp(base, Col.ACCENT, 0.55f);
        if (!b.enabled) base = Col.lerp(base, Col.BG, 0.6f);
        int top = Col.lighter(base, b.pressed ? 0.0f : 0.12f);
        int bot = Col.darker(base, b.pressed ? 0.05f : 0.25f);
        float off = b.pressed ? r * 0.15f : 0;
        g.fillRoundRect(b.x, b.y + r * 0.3f, b.w, b.h, r, 0x66000000);
        g.save();
        g.translate(0, off);
        g.fillRoundRect(b.x, b.y, b.w, b.h, r, bot);
        g.fillRoundRect(b.x, b.y, b.w, b.h * 0.6f, r, top);
        g.fillRect(b.x + r * 0.5f, b.y + b.h * 0.45f, b.w - r, b.h * 0.15f, Col.lerp(top, bot, 0.5f));
        g.strokeRoundRect(b.x, b.y, b.w, b.h, r, Math.max(1f, u * (b.selected ? 0.6f : 0.25f)),
                b.selected ? Col.ACCENT : Col.alpha(0xFFFFFFFF, 0.18f));
        int tc = b.enabled ? (b.textColor != 0 ? b.textColor : Col.TEXT) : Col.TEXT_DIM;
        float size = b.textSize > 0 ? b.textSize : Math.min(b.h * 0.4f, u * 3.6f);
        if (b.sub != null) {
            g.text(b.label, b.x + b.w / 2, b.y + b.h * 0.38f, size * 0.92f, tc, Gfx.ALIGN_CENTER, true);
            g.text(b.sub, b.x + b.w / 2, b.y + b.h * 0.72f, size * 0.62f, b.enabled ? Col.TEXT_DIM : Col.alpha(Col.TEXT_DIM, 0.6f), Gfx.ALIGN_CENTER, false);
        } else {
            g.text(fit(g, b.label, size, b.w * 0.92f), b.x + b.w / 2, b.y + b.h / 2, size, tc, Gfx.ALIGN_CENTER, true);
        }
        g.restore();
    }

    /** Shrinks nothing; just ellipsizes text that would overflow. */
    public static String fit(Gfx g, String s, float size, float maxW) {
        if (g.textWidth(s, size, true) <= maxW) return s;
        String t = s;
        while (t.length() > 1 && g.textWidth(t + "..", size, true) > maxW) t = t.substring(0, t.length() - 1);
        return t + "..";
    }

    /** Draws word-wrapped text starting at (x, y) (center of first line). Returns the height used. */
    public static float wrap(Gfx g, String s, float x, float y, float size, float maxW, int color) {
        String[] words = s.split(" ");
        StringBuilder line = new StringBuilder();
        float lh = size * 1.3f, cy = y;
        for (String word : words) {
            String cand = line.length() == 0 ? word : line + " " + word;
            if (g.textWidth(cand, size, false) > maxW && line.length() > 0) {
                g.text(line.toString(), x, cy, size, color, Gfx.ALIGN_LEFT, false);
                cy += lh;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(cand);
            }
        }
        if (line.length() > 0) g.text(line.toString(), x, cy, size, color, Gfx.ALIGN_LEFT, false);
        return cy - y + lh;
    }

    public static void title(Gfx g, String s, float x, float y, float size) {
        g.text(s, x + size * 0.06f, y + size * 0.08f, size, 0xAA000000, Gfx.ALIGN_CENTER, true);
        g.text(s, x, y, size, Col.ACCENT, Gfx.ALIGN_CENTER, true);
    }

    public static void bar(Gfx g, float x, float y, float w, float h, float frac, int color) {
        g.fillRoundRect(x, y, w, h, h / 2, 0xFF0A0F18);
        if (frac > 0) g.fillRoundRect(x, y, Math.max(h, w * Math.min(1f, frac)), h, h / 2, color);
    }

    /** Small picture for a shop item. */
    public static void itemIcon(Gfx g, int id, float cx, float cy, float s) {
        Item it = Items.ALL[id];
        int c = it.color;
        float r = s * 0.5f;
        g.fillCircle(cx, cy, r, 0xFF0E1420);
        g.strokeCircle(cx, cy, r, s * 0.05f, Col.alpha(c, 0.8f));
        float k = s / 40f;
        switch (it.behavior) {
            case Items.B_NONE:
                if (Items.isShield(id)) {
                    g.fillRadial(cx, cy, r * 0.8f, Col.alpha(c, 0.15f), Col.alpha(c, 0.7f));
                    g.strokeCircle(cx, cy, r * 0.8f, 2 * k, c);
                } else if (id == Items.FUEL) {
                    g.fillRoundRect(cx - 7 * k, cy - 9 * k, 14 * k, 19 * k, 2 * k, c);
                    g.fillRect(cx - 3 * k, cy - 13 * k, 6 * k, 5 * k, Col.darker(c, 0.3f));
                    g.text("F", cx, cy + 1 * k, 11 * k, 0xFF000000, Gfx.ALIGN_CENTER, true);
                } else if (id == Items.REPAIR) {
                    g.fillRoundRect(cx - 10 * k, cy - 8 * k, 20 * k, 16 * k, 3 * k, 0xFFF0F0F0);
                    g.fillRect(cx - 2.5f * k, cy - 6 * k, 5 * k, 12 * k, 0xFFE84A3C);
                    g.fillRect(cx - 6 * k, cy - 2.5f * k, 12 * k, 5 * k, 0xFFE84A3C);
                } else {
                    float[] xs = new float[9], ys = new float[9];
                    for (int i = 0; i <= 8; i++) {
                        double a = Math.PI * i / 8;
                        xs[i] = cx + (float) Math.cos(a) * 12 * k;
                        ys[i] = cy - 2 * k - (float) Math.sin(a) * 9 * k;
                    }
                    g.fillPoly(xs, ys, 9, c);
                    g.line(cx - 12 * k, cy - 2 * k, cx, cy + 10 * k, 1.2f * k, c);
                    g.line(cx + 12 * k, cy - 2 * k, cx, cy + 10 * k, 1.2f * k, c);
                    g.fillRect(cx - 3 * k, cy + 8 * k, 6 * k, 5 * k, 0xFF8A6A40);
                }
                break;
            case Items.B_CLUSTER:
            case Items.B_AIRSTRIKE:
                for (int i = 0; i < 5; i++) {
                    double a = i * Math.PI * 2 / 5;
                    g.fillCircle(cx + (float) Math.cos(a) * 8 * k, cy + (float) Math.sin(a) * 8 * k, 3.5f * k, c);
                }
                g.fillCircle(cx, cy, 4 * k, 0xFFFFFFFF);
                break;
            case Items.B_SPLITTER:
                for (int i = -1; i <= 1; i++) {
                    g.line(cx, cy + 9 * k, cx + i * 9 * k, cy - 8 * k, 2 * k, c);
                    g.fillCircle(cx + i * 9 * k, cy - 8 * k, 3.5f * k, c);
                }
                break;
            case Items.B_TESLA:
                g.fillRadial(cx, cy, 11 * k, 0xFFFFFFFF, Col.alpha(c, 0.3f));
                g.line(cx - 10 * k, cy - 4 * k, cx - 2 * k, cy + 2 * k, 1.8f * k, c);
                g.line(cx - 2 * k, cy + 2 * k, cx + 4 * k, cy - 6 * k, 1.8f * k, c);
                g.line(cx + 4 * k, cy - 6 * k, cx + 11 * k, cy + 3 * k, 1.8f * k, c);
                break;
            case Items.B_BLACKHOLE:
                g.fillRadial(cx, cy, 13 * k, Col.alpha(c, 0.9f), Col.alpha(c, 0f));
                g.fillCircle(cx, cy, 6 * k, 0xFF000000);
                g.strokeCircle(cx, cy, 7 * k, 1.5f * k, 0xFFE0C0FF);
                break;
            case Items.B_LAVA:
                g.fillRadial(cx, cy, 13 * k, 0xFFFFE070, 0x00FF4A00);
                g.fillCircle(cx - 4 * k, cy + 4 * k, 4 * k, 0xFFFF8A2A);
                g.fillCircle(cx + 5 * k, cy + 3 * k, 3 * k, 0xFFFF8A2A);
                break;
            case Items.B_DIRT:
                g.fillCircle(cx, cy + 2 * k, 11 * k, c);
                g.fillCircle(cx - 4 * k, cy - 2 * k, 3 * k, Col.lighter(c, 0.3f));
                break;
            case Items.B_TELEPORT:
                g.strokeCircle(cx, cy, 10 * k, 2 * k, c);
                g.strokeCircle(cx, cy, 5 * k, 2 * k, c);
                g.fillCircle(cx, cy, 2 * k, 0xFFFFFFFF);
                break;
            case Items.B_SHOCKWAVE:
                for (int i = 0; i < 3; i++) g.strokeCircle(cx, cy + 8 * k, (5 + i * 5) * k, 1.6f * k, Col.alpha(c, 1f - i * 0.25f));
                break;
            case Items.B_RAIL:
                g.line(cx - 12 * k, cy, cx + 12 * k, cy, 4 * k, c);
                g.line(cx - 12 * k, cy, cx + 12 * k, cy, 1.5f * k, 0xFFFFFFFF);
                break;
            case Items.B_HOMING: {
                g.line(cx - 10 * k, cy + 6 * k, cx + 8 * k, cy - 6 * k, 5 * k, c);
                g.fillCircle(cx + 9 * k, cy - 7 * k, 3 * k, 0xFFFFFFFF);
                g.fillCircle(cx - 11 * k, cy + 7 * k, 3 * k, 0xFFFFAA44);
                break;
            }
            case Items.B_ROLLER:
                g.fillCircle(cx, cy, 9 * k, c);
                g.line(cx - 9 * k, cy, cx + 9 * k, cy, 1.5f * k, 0xFF2A2A2A);
                g.line(cx, cy - 9 * k, cx, cy + 9 * k, 1.5f * k, 0xFF2A2A2A);
                break;
            case Items.B_BOUNCER:
            case Items.B_LEAPFROG:
                g.line(cx - 12 * k, cy + 8 * k, cx - 6 * k, cy - 4 * k, 1.5f * k, Col.alpha(c, 0.6f));
                g.line(cx - 6 * k, cy - 4 * k, cx, cy + 8 * k, 1.5f * k, Col.alpha(c, 0.6f));
                g.line(cx, cy + 8 * k, cx + 6 * k, cy - 2 * k, 1.5f * k, Col.alpha(c, 0.6f));
                g.fillCircle(cx + 7 * k, cy - 3 * k, 4.5f * k, c);
                break;
            case Items.B_DIGGER:
                g.fillCircle(cx, cy - 2 * k, 7 * k, c);
                float[] xs = {cx - 5 * k, cx + 5 * k, cx};
                float[] ys = {cy + 3 * k, cy + 3 * k, cy + 13 * k};
                g.fillPoly(xs, ys, 3, 0xFFC8C8C8);
                break;
            default: {
                float rr = (5 + Math.min(10, it.radius / 12f)) * k;
                g.fillRadial(cx, cy, rr * 1.6f, Col.alpha(c, 0.5f), Col.alpha(c, 0f));
                g.fillCircle(cx, cy, rr, c);
                g.fillCircle(cx - rr * 0.3f, cy - rr * 0.3f, rr * 0.35f, 0xFFFFFFFF);
            }
        }
    }
}
