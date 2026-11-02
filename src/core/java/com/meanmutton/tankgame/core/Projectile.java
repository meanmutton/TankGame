package com.meanmutton.tankgame.core;

/** Anything flying, rolling, burrowing, or flowing that the action phase must wait for. */
final class Projectile {
    static final int SHELL = 0, BOMBLET = 1, AIRBOMB = 2, ROLLING = 3, BURROW = 4, LAVA = 5;

    float x, y, vx, vy;
    final Item item;
    final Player owner;
    int kind;
    float age, timer, stopTimer;
    int bounces, hops;
    boolean split, dead, onGround;
    float radius, damage;   // for sub-munitions
    float gravMul = 1f, windMul = 1f;
    boolean recordTrail;    // the shooter's main shell: remembered for the "last shot" trace

    // Short visual trail (ring buffer)
    final float[] tx = new float[24], ty = new float[24];
    int tHead, tCount;
    float tTimer;

    Projectile(Item item, Player owner, int kind, float x, float y, float vx, float vy) {
        this.item = item;
        this.owner = owner;
        this.kind = kind;
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.radius = item.radius;
        this.damage = item.damage;
        if (item.behavior == Items.B_RAIL && kind == SHELL) {
            gravMul = 0.18f;
            windMul = 0f;
        }
    }

    void pushTrail() {
        tx[tHead] = x;
        ty[tHead] = y;
        tHead = (tHead + 1) % tx.length;
        if (tCount < tx.length) tCount++;
    }
}
