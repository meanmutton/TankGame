package com.meanmutton.tankgame.core;

import java.util.ArrayList;
import java.util.List;

/** Visual effects and timed weapon behaviors. */
abstract class Fx {
    final Battle b;
    float t;
    boolean done;

    Fx(Battle b) {
        this.b = b;
    }

    /** Effects that block the end of an action (the turn waits for them). */
    boolean blocking() {
        return true;
    }

    abstract void update(float dt);

    abstract void render(Gfx g);

    // ------------------------------------------------------------------

    static final class Explosion extends Fx {
        final float x, y, r, dur;
        final int core, edge;

        Explosion(Battle b, float x, float y, float r, int color) {
            super(b);
            this.x = x;
            this.y = y;
            this.r = r;
            this.dur = 0.35f + r / 260f;
            this.core = Col.lighter(color, 0.6f);
            this.edge = color;
        }

        boolean blocking() {
            return t < 0.3f;
        }

        void update(float dt) {
            t += dt;
            if (t >= dur) done = true;
        }

        void render(Gfx g) {
            float k = t / dur;
            float grow = Math.min(1f, t / 0.12f);
            float rr = r * (0.35f + 0.65f * grow) * (1f + 0.15f * k);
            float a = 1f - k;
            g.fillRadial(x, y, rr * 1.35f, Col.alpha(edge, a * 0.55f), Col.withAlpha(edge, 0));
            g.fillRadial(x, y, rr, Col.alpha(0xFFFFFFFF, a), Col.alpha(edge, a * 0.8f));
            g.fillCircle(x, y, rr * 0.45f * (1 - k), Col.alpha(core, a));
            if (k < 0.6f) g.strokeCircle(x, y, r * (0.6f + k * 1.4f), 2.5f * (1 - k), Col.alpha(0xFFFFFFFF, 0.6f * (1 - k / 0.6f)));
        }
    }

    // ------------------------------------------------------------------

    static final class Lightning extends Fx {
        final float x, y;
        final List<Tank> targets;
        final Player owner;
        int fired;
        float[] seg = new float[20];

        Lightning(Battle b, float x, float y, List<Tank> targets, Player owner) {
            super(b);
            this.x = x;
            this.y = y;
            this.targets = targets;
            this.owner = owner;
        }

        void update(float dt) {
            t += dt;
            while (fired < targets.size() && t > 0.15f + fired * 0.22f) {
                Tank tk = targets.get(fired);
                float dmg = 32f - fired * 4f;
                if (tk.alive) b.hurt(tk, dmg, owner, false);
                b.sparks(tk.x, tk.centerY(), 0xFFA6E8FF, 14);
                b.game.sfx(Platform.SND_ZAP, 0.8f);
                fired++;
            }
            if (t > 0.35f + targets.size() * 0.22f) done = true;
        }

        void render(Gfx g) {
            float sx = x, sy = y;
            for (int i = 0; i < targets.size(); i++) {
                if (t < 0.15f + i * 0.22f) break;
                Tank tk = targets.get(i);
                float age = t - (0.15f + i * 0.22f);
                float a = Math.max(0.25f, 1f - age * 2.5f);
                bolt(g, sx, sy, tk.x, tk.centerY(), a);
                sx = tk.x;
                sy = tk.centerY();
            }
        }

        private void bolt(Gfx g, float x0, float y0, float x1, float y1, float a) {
            int n = 9;
            float px = x0, py = y0;
            float dx = x1 - x0, dy = y1 - y0;
            float len = (float) Math.sqrt(dx * dx + dy * dy) + 0.001f;
            float nx = -dy / len, ny = dx / len;
            for (int i = 1; i <= n; i++) {
                float f = i / (float) n;
                float off = (i == n) ? 0 : (float) (b.rng.nextGaussian() * len * 0.05f);
                float qx = x0 + dx * f + nx * off, qy = y0 + dy * f + ny * off;
                g.line(px, py, qx, qy, 5f, Col.alpha(0xFF6AB4FF, a * 0.5f));
                g.line(px, py, qx, qy, 2f, Col.alpha(0xFFFFFFFF, a));
                px = qx;
                py = qy;
            }
        }
    }

    // ------------------------------------------------------------------

    static final class Shockwave extends Fx {
        final float x, y, range = 420f, speed = 520f;
        final Player owner;
        final boolean[] hit;

        Shockwave(Battle b, float x, float y, Player owner) {
            super(b);
            this.x = x;
            this.y = y;
            this.owner = owner;
            this.hit = new boolean[b.tanks.size()];
            b.shake += 10;
        }

        void update(float dt) {
            t += dt;
            float front = t * speed;
            for (int i = 0; i < b.tanks.size(); i++) {
                Tank tk = b.tanks.get(i);
                if (hit[i] || !tk.alive) continue;
                float d = Math.abs(tk.x - x);
                if (d <= front && d < range) {
                    hit[i] = true;
                    if (tk.p == owner && d < 30) continue;
                    float dmg = 45f * (1f - d / range) + 5f;
                    b.hurt(tk, dmg, owner, false);
                    b.dust(tk.x, tk.y, 10);
                }
            }
            if (front > range) done = true;
            b.shake = Math.max(b.shake, 4f * (1f - front / range));
        }

        void render(Gfx g) {
            float front = t * speed;
            float a = 1f - front / range;
            for (int s = -1; s <= 1; s += 2) {
                float fx = x + s * front;
                if (fx < 0 || fx >= b.w) continue;
                float gy = b.terrain.at(fx);
                g.fillRadial(fx, gy, 34, Col.alpha(0xFFFFE0A0, a * 0.8f), 0x00FFE0A0);
                g.line(fx, gy - 30, fx, gy + 4, 3f, Col.alpha(0xFFFFFFFF, a * 0.7f));
            }
        }
    }

    // ------------------------------------------------------------------

    static final class BlackHole extends Fx {
        final float x, y;
        final Player owner;
        static final float DUR = 2.3f, PULL = 320f;

        BlackHole(Battle b, float x, float y, Player owner) {
            super(b);
            this.x = x;
            this.y = y;
            this.owner = owner;
        }

        void update(float dt) {
            t += dt;
            if (t < DUR) {
                float k = Math.min(1f, t / 0.6f);
                for (Tank tk : b.tanks) {
                    if (!tk.alive) continue;
                    float dx = x - tk.x;
                    float dist = Math.abs(dx);
                    if (dist < 6 || dist > PULL) continue;
                    float speed = 110f * k * (1f - dist / PULL) + 15f;
                    float step = Math.min(dist - 4, speed * dt);
                    tk.x += Math.signum(dx) * step;
                    tk.x = Math.max(Tank.W / 2, Math.min(b.w - Tank.W / 2, tk.x));
                    float sup = b.support(tk.x);
                    if (sup < tk.y && tk.y - sup < 30) tk.y = sup;
                }
                if (b.rng.nextFloat() < 0.8f) {
                    double a = b.rng.nextDouble() * Math.PI * 2;
                    float rr = 80 + b.rng.nextFloat() * 120;
                    Particle p = b.particle(x + (float) Math.cos(a) * rr, y + (float) Math.sin(a) * rr,
                            0, 0, 0.6f, 2.5f, 0xFFC8A0FF, Particle.SWIRL);
                    if (p != null) {
                        p.ox = x;
                        p.oy = y;
                    }
                }
                b.shake = Math.max(b.shake, 2f);
            } else if (!done) {
                done = true;
                b.explode(x, y, 85, 95, owner, null, 1f, true, 0xFF9A5AFF);
                b.game.sfx(Platform.SND_BOOM_L, 1f);
            }
        }

        void render(Gfx g) {
            float k = Math.min(1f, t / 0.5f);
            float pulse = 1f + 0.08f * (float) Math.sin(t * 18);
            g.fillRadial(x, y, 90 * k * pulse, 0x889A5AFF, 0x009A5AFF);
            g.fillCircle(x, y, 18 * k * pulse, 0xFF000000);
            g.strokeCircle(x, y, 20 * k * pulse, 2.5f, 0xFFE0C0FF);
            for (int i = 0; i < 3; i++) {
                float rr = (60 - ((t * 70 + i * 22) % 60)) * k;
                g.strokeCircle(x, y, rr + 20, 1.5f, Col.alpha(0xFFC8A0FF, rr / 60f));
            }
        }
    }

    // ------------------------------------------------------------------

    static final class AirStrike extends Fx {
        final float x, y;
        final Player owner;
        int dropped;
        final Item item;

        AirStrike(Battle b, float x, float y, Player owner, Item item) {
            super(b);
            this.x = x;
            this.y = y;
            this.owner = owner;
            this.item = item;
        }

        void update(float dt) {
            t += dt;
            while (dropped < 7 && t > 0.7f + dropped * 0.12f) {
                float bx = x + (dropped - 3) * 38 + (b.rng.nextFloat() - 0.5f) * 14;
                Projectile p = new Projectile(item, owner, Projectile.AIRBOMB, bx, -40 - dropped * 10,
                        b.windAccel() * 0.05f, 420);
                p.radius = 34;
                p.damage = 40;
                b.addProjectile(p);
                dropped++;
            }
            if (dropped >= 7) done = true;
        }

        void render(Gfx g) {
            if (t < 0.9f) {
                float a = ((int) (t * 10) % 2 == 0) ? 1f : 0.4f;
                g.fillRadial(x, y - 4, 18, Col.alpha(0xFFFF4040, a), 0x00FF4040);
                g.line(x, y, x, y - 30, 1.5f, 0xFFDDDDDD);
            }
        }
    }

    // ------------------------------------------------------------------

    static final class Sparkle extends Fx {
        final float x, y;
        final int color;

        Sparkle(Battle b, float x, float y, int color) {
            super(b);
            this.x = x;
            this.y = y;
            this.color = color;
        }

        boolean blocking() {
            return false;
        }

        void update(float dt) {
            t += dt;
            if (t > 0.8f) done = true;
        }

        void render(Gfx g) {
            float k = t / 0.8f;
            g.fillRadial(x, y, 40 * (1 - k * 0.5f), Col.alpha(color, 1 - k), Col.withAlpha(color, 0));
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4 + t * 3;
                float rr = 10 + k * 40;
                g.fillCircle(x + (float) Math.cos(a) * rr, y + (float) Math.sin(a) * rr, 2.5f * (1 - k), 0xFFFFFFFF);
            }
        }
    }

    // ------------------------------------------------------------------

    static final class Particle {
        static final int DEBRIS = 0, SMOKE = 1, SPARK = 2, SWIRL = 3, CONFETTI = 4;
        float x, y, vx, vy, life, maxLife, size, ox, oy, spin;
        int color, type;
    }

    static final class FloatText {
        float x, y, life, size;
        String text;
        int color;
    }

    static List<Tank> nearbyEnemies(Battle b, float x, float y, float range, Player owner, int max) {
        List<Tank> out = new ArrayList<Tank>();
        for (Tank t : b.tanks) {
            if (!t.alive || t.p == owner) continue;
            float d = t.distanceTo(x, y);
            if (d > range) continue;
            int i = 0;
            while (i < out.size() && out.get(i).distanceTo(x, y) < d) i++;
            out.add(i, t);
        }
        while (out.size() > max) out.remove(out.size() - 1);
        return out;
    }
}
