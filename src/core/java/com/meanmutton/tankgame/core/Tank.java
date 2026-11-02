package com.meanmutton.tankgame.core;

/** A player's tank during one round. */
public final class Tank {
    public static final float W = 40f, H = 18f;
    public static final float PIVOT_Y = 16f;   // turret pivot height above tank base
    public static final float BARREL = 24f;

    public final Player p;
    public float x, y;          // y is the bottom of the treads (world coords, y down)
    public float health = 100f;
    public float angle = 60f;   // degrees: 0 = right, 90 = up, 180 = left
    public float power = 450f;
    public boolean alive = true;
    public boolean dying;
    public int killer = -1;     // player index credited with the kill
    public int lastHitBy = -1;

    public boolean falling;
    public float fallStartY, vy;
    public boolean chute;
    public boolean noFallDamage;

    public float shieldHp;
    public int shieldType = -1;
    public int weapon = Items.PEA;

    public float hitFlash;
    public float lastImpactX = Float.NaN, lastImpactY = Float.NaN;
    public float[] lastTrailX, lastTrailY;
    public int lastTrailN;

    public Tank(Player p) {
        this.p = p;
        p.tank = this;
    }

    /** Set by sudden death: the health-based power limit no longer applies. */
    public boolean fullPower;

    public float maxPower() {
        return fullPower ? 1000f : 450f + 5.5f * Math.max(0f, health);
    }

    public float pivotX() {
        return x;
    }

    public float pivotY() {
        return y - PIVOT_Y;
    }

    public float barrelTipX() {
        return x + (float) Math.cos(Math.toRadians(angle)) * BARREL;
    }

    public float barrelTipY() {
        return pivotY() - (float) Math.sin(Math.toRadians(angle)) * BARREL;
    }

    public float centerY() {
        return y - H * 0.55f;
    }

    public boolean contains(float px, float py, float pad) {
        return px >= x - W / 2 - pad && px <= x + W / 2 + pad && py >= y - H - 4 - pad && py <= y + 2 + pad;
    }

    /** Distance from a point to the tank's body (0 if inside). */
    public float distanceTo(float px, float py) {
        float left = x - W / 2, right = x + W / 2, top = y - H - 2, bottom = y;
        float dx = px < left ? left - px : (px > right ? px - right : 0);
        float dy = py < top ? top - py : (py > bottom ? py - bottom : 0);
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
}
