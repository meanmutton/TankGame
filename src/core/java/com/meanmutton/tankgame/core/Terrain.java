package com.meanmutton.tankgame.core;

import java.util.Random;

/**
 * Height-map terrain: one surface height per world-unit column (y grows downward).
 * Explosions remove the part of each column inside the blast and the dirt above
 * settles down, which is the classic artillery-game behavior.
 */
public final class Terrain {
    public final int w;
    public final float h;
    public final float[] y;
    public final float minY, maxY;

    public Terrain(int w, float h) {
        this.w = w;
        this.h = h;
        this.y = new float[w];
        this.minY = 40f;
        this.maxY = h - 6f;
    }

    public float at(float x) {
        int i = (int) x;
        if (i < 0) i = 0;
        if (i >= w) i = w - 1;
        return y[i];
    }

    /** Smooth interpolated height. */
    public float atSmooth(float x) {
        if (x <= 0) return y[0];
        if (x >= w - 1) return y[w - 1];
        int i = (int) x;
        float t = x - i;
        return y[i] * (1 - t) + y[i + 1] * t;
    }

    /** The highest surface point (smallest y) under the span [x0, x1]. */
    public float highestIn(float x0, float x1) {
        int a = Math.max(0, (int) x0), b = Math.min(w - 1, (int) x1);
        float best = Float.MAX_VALUE;
        for (int i = a; i <= b; i++) if (y[i] < best) best = y[i];
        return best == Float.MAX_VALUE ? at(x0) : best;
    }

    public float slope(float x) {
        return (atSmooth(x + 3) - atSmooth(x - 3)) / 6f;
    }

    /** Removes a circle of terrain. Returns the amount of dirt removed (for debris). */
    public float crater(float cx, float cy, float r) {
        float removed = 0;
        int a = Math.max(0, (int) (cx - r)), b = Math.min(w - 1, (int) (cx + r));
        for (int i = a; i <= b; i++) {
            float dx = i - cx;
            float d2 = r * r - dx * dx;
            if (d2 <= 0) continue;
            float dy = (float) Math.sqrt(d2);
            float top = cy - dy, bot = cy + dy;
            float s = y[i];
            if (bot <= s) continue;
            float cut = bot - Math.max(s, top);
            if (cut <= 0) continue;
            float ny = Math.min(maxY, s + cut);
            removed += ny - s;
            y[i] = ny;
        }
        return removed;
    }

    /** Adds a ball of dirt that settles onto the surface. */
    public void addDirt(float cx, float cy, float r) {
        int a = Math.max(0, (int) (cx - r)), b = Math.min(w - 1, (int) (cx + r));
        for (int i = a; i <= b; i++) {
            float dx = i - cx;
            float d2 = r * r - dx * dx;
            if (d2 <= 0) continue;
            float thick = 2f * (float) Math.sqrt(d2);
            y[i] = Math.max(minY, y[i] - thick);
        }
    }

    public void flatten(float cx, float halfW) {
        int a = Math.max(0, (int) (cx - halfW)), b = Math.min(w - 1, (int) (cx + halfW));
        float sum = 0;
        int n = 0;
        for (int i = a; i <= b; i++) {
            sum += y[i];
            n++;
        }
        if (n == 0) return;
        float avg = sum / n;
        for (int i = a; i <= b; i++) y[i] = avg;
        // ease the edges
        for (int k = 1; k <= 10; k++) {
            float t = k / 11f;
            int l = a - k, rr = b + k;
            if (l >= 0) y[l] = avg * (1 - t) + y[l] * t;
            if (rr < w) y[rr] = avg * (1 - t) + y[rr] * t;
        }
    }

    // ---------------- generation ----------------

    public void generate(int type, Random r) {
        if (type == 0) type = 1 + r.nextInt(Settings.TERRAIN_NAMES.length - 1);
        switch (type) {
            case 2: mountains(r); break;
            case 3: sines(r, h * 0.74f, 26f); break;
            case 4: mesas(r); break;
            case 5: valley(r); break;
            case 6: islands(r); break;
            default: sines(r, h * 0.64f, 95f); break;
        }
        for (int i = 0; i < w; i++) y[i] = Math.max(h * 0.2f, Math.min(h * 0.95f, y[i]));
    }

    private void sines(Random r, float base, float amp) {
        float[] f = new float[5], p = new float[5], a = new float[5];
        for (int k = 0; k < 5; k++) {
            f[k] = (float) ((k + 1) * (0.6 + r.nextFloat() * 0.9) * Math.PI * 2 / w);
            p[k] = r.nextFloat() * 6.28f;
            a[k] = amp / (1 + k * 1.3f) * (0.6f + r.nextFloat() * 0.8f);
        }
        for (int i = 0; i < w; i++) {
            float v = base;
            for (int k = 0; k < 5; k++) v += (float) Math.sin(i * f[k] + p[k]) * a[k];
            y[i] = v;
        }
    }

    private void mountains(Random r) {
        int n = 1;
        while (n < w) n <<= 1;
        float[] m = new float[n + 1];
        m[0] = h * (0.45f + r.nextFloat() * 0.35f);
        m[n] = h * (0.45f + r.nextFloat() * 0.35f);
        float rough = h * 0.42f;
        for (int step = n; step > 1; step >>= 1) {
            for (int i = 0; i < n; i += step) {
                int mid = i + step / 2;
                m[mid] = (m[i] + m[i + step]) / 2 + (r.nextFloat() - 0.5f) * rough;
            }
            rough *= 0.56f;
        }
        float lo = Float.MAX_VALUE, hi = -Float.MAX_VALUE;
        for (int i = 0; i < w; i++) {
            lo = Math.min(lo, m[i]);
            hi = Math.max(hi, m[i]);
        }
        float span = Math.max(1, hi - lo);
        for (int i = 0; i < w; i++) {
            float t = (m[i] - lo) / span; // 0 = peak
            y[i] = h * 0.26f + t * h * 0.64f;
        }
        smooth(2);
    }

    private void mesas(Random r) {
        float x = 0;
        float level = h * (0.55f + r.nextFloat() * 0.3f);
        while (x < w) {
            float len = 120 + r.nextFloat() * 260;
            for (int i = (int) x; i < Math.min(w, x + len); i++) y[i] = level;
            x += len;
            level = h * (0.38f + r.nextFloat() * 0.5f);
        }
        smooth(6);
        for (int i = 0; i < w; i++) y[i] += (float) Math.sin(i * 0.05f) * 3f;
    }

    private void valley(Random r) {
        float depth = h * (0.3f + r.nextFloat() * 0.15f);
        float center = w * (0.4f + r.nextFloat() * 0.2f);
        float ph = r.nextFloat() * 6f;
        for (int i = 0; i < w; i++) {
            float t = (i - center) / (w * 0.55f);
            y[i] = h * 0.85f - depth * Math.min(1.2f, t * t * 2.2f) + (float) Math.sin(i * 0.012f + ph) * 22f;
        }
    }

    private void islands(Random r) {
        sines(r, h * 0.8f, 40f);
        int count = 3 + r.nextInt(3);
        for (int k = 0; k < count; k++) {
            float cx = (k + 0.5f) * w / count + (r.nextFloat() - 0.5f) * 80;
            float rad = 90 + r.nextFloat() * 90;
            float height = h * (0.18f + r.nextFloat() * 0.25f);
            for (int i = 0; i < w; i++) {
                float d = Math.abs(i - cx) / rad;
                if (d < 1) y[i] -= height * (float) Math.pow(Math.cos(d * Math.PI / 2), 0.7);
            }
        }
    }

    private void smooth(int passes) {
        float[] t = new float[w];
        for (int p = 0; p < passes; p++) {
            for (int i = 0; i < w; i++) {
                float s = 0;
                int n = 0;
                for (int k = -3; k <= 3; k++) {
                    int j = i + k;
                    if (j >= 0 && j < w) {
                        s += y[j];
                        n++;
                    }
                }
                t[i] = s / n;
            }
            System.arraycopy(t, 0, y, 0, w);
        }
    }
}
