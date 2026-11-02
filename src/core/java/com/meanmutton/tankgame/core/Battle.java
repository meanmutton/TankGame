package com.meanmutton.tankgame.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** One round of combat: terrain, tanks, turns, physics, damage, and the world rendering. */
public final class Battle {
    // The world is 720 units tall; gravity and muzzle speed are scaled together so flight times stay snappy.
    public static final float G = 432f, SPEED_K = 0.936f, DT = 1f / 120f;
    public static final float H = 720f;

    public static final int PH_BANNER = 0, PH_AIM = 1, PH_ACTION = 2, PH_OVER = 3;

    // Money and score rules
    public static final int PAY_PER_HP = 20, PAY_PER_SHIELD_HP = 10, PAY_DIRECT = 250, PAY_KILL = 1000, PAY_WIN = 2500;

    final Game game;
    final Settings st;
    final Random rng;
    public final int w;
    public final Terrain terrain;
    public final List<Tank> tanks = new ArrayList<Tank>();
    final List<Tank> order = new ArrayList<Tank>();
    int turnIdx = -1;
    public int turnCount;
    public Tank active;
    public float wind;
    public int phase;
    public float phaseTime;
    public float shake;
    public Player winner;
    public boolean roundOver;
    public String banner = "";
    public boolean suddenDeath;
    int dryTurns;

    final List<Projectile> projs = new ArrayList<Projectile>();
    final List<Projectile> newProjs = new ArrayList<Projectile>();
    final List<Fx> effects = new ArrayList<Fx>();
    final List<Fx> newEffects = new ArrayList<Fx>();
    final List<Fx.Particle> particles = new ArrayList<Fx.Particle>();
    final List<Fx.FloatText> texts = new ArrayList<Fx.FloatText>();
    final int[] actionEarn;
    float accum;
    float engineTimer;
    Ai.Brain brain;

    // Visual theme
    final Theme theme;
    final float[] cloudX = new float[7], cloudY = new float[7], cloudS = new float[7];
    final float[] starX = new float[90], starY = new float[90];
    final float[] farX, farY1, farY2;

    // trail recording for the active shot
    final float[] recX = new float[600], recY = new float[600];
    int recN;
    Projectile recording;

    public Battle(Game game, List<Player> players, int worldW, Random rng) {
        this.game = game;
        this.st = game.settings;
        this.rng = rng;
        this.w = worldW;
        this.terrain = new Terrain(worldW, H);
        terrain.generate(st.terrain, rng);
        this.actionEarn = new int[players.size()];
        this.theme = Theme.pick(rng);

        // Place tanks across the field in a random order.
        List<Player> shuffled = new ArrayList<Player>(players);
        Collections.shuffle(shuffled, rng);
        int n = shuffled.size();
        float slot = (float) w / n;
        for (int i = 0; i < n; i++) {
            Player p = shuffled.get(i);
            Tank t = new Tank(p);
            t.x = slot * (i + 0.5f) + (rng.nextFloat() - 0.5f) * slot * 0.4f;
            t.x = Math.max(Tank.W, Math.min(w - Tank.W, t.x));
            terrain.flatten(t.x, Tank.W / 2 + 4);
            t.y = support(t.x);
            t.angle = t.x < w / 2f ? 60 : 120;
            t.power = 420;
            tanks.add(t);
        }
        // Turn order is a separate shuffle so neighbors don't always go in sequence.
        order.addAll(tanks);
        Collections.shuffle(order, rng);
        for (Tank t : tanks) t.weapon = Items.PEA;

        int maxWind = Settings.WIND_MAX[st.wind];
        wind = maxWind == 0 ? 0 : (rng.nextFloat() * 2 - 1) * maxWind;

        for (int i = 0; i < cloudX.length; i++) {
            cloudX[i] = rng.nextFloat() * w;
            cloudY[i] = 50 + rng.nextFloat() * 190;
            cloudS[i] = 0.6f + rng.nextFloat() * 0.9f;
        }
        for (int i = 0; i < starX.length; i++) {
            starX[i] = rng.nextFloat() * w;
            starY[i] = rng.nextFloat() * H * 0.7f;
        }
        int fn = w / 20 + 2;
        farX = new float[fn + 2];
        farY1 = new float[fn + 2];
        farY2 = new float[fn + 2];
        float p1 = rng.nextFloat() * 6, p2 = rng.nextFloat() * 6;
        for (int i = 0; i < fn; i++) {
            farX[i] = i * 20;
            farY1[i] = H * 0.45f + (float) (Math.sin(i * 0.11 + p1) * 70 + Math.sin(i * 0.37 + p2) * 25);
            farY2[i] = H * 0.58f + (float) (Math.sin(i * 0.07 + p2) * 50 + Math.sin(i * 0.23 + p1) * 30);
        }
        farX[fn] = w;
        farY1[fn] = H;
        farY2[fn] = H;
        farX[fn + 1] = 0;
        farY1[fn + 1] = H;
        farY2[fn + 1] = H;
        nextTurn();
    }

    // =====================================================================
    // Queries
    // =====================================================================

    public float windAccel() {
        return wind * 1.2f;
    }

    public float support(float x) {
        return terrain.highestIn(x - Tank.W / 2 + 3, x + Tank.W / 2 - 3);
    }

    public boolean buried(Tank t) {
        return terrain.at(t.x) < t.y - 12f;
    }

    public int aliveCount() {
        int n = 0;
        for (Tank t : tanks) if (t.alive) n++;
        return n;
    }

    public boolean isHumanAiming() {
        return phase == PH_AIM && active != null && active.p.isHuman() && active.alive;
    }

    public boolean isAiTurn() {
        return active != null && !active.p.isHuman();
    }

    boolean busy() {
        if (!projs.isEmpty() || !newProjs.isEmpty() || !newEffects.isEmpty()) return true;
        for (Fx f : effects) if (f.blocking()) return true;
        for (Tank t : tanks) if (t.alive && t.falling) return true;
        return false;
    }

    float timeScale() {
        if (isAiTurn() || phase == PH_OVER) {
            return st.aiSpeed == 2 ? 2.6f : (st.aiSpeed == 1 ? 1.6f : 1f);
        }
        return 1f;
    }

    // =====================================================================
    // Turn flow
    // =====================================================================

    void nextTurn() {
        brain = null;
        if (checkRoundOver()) return;
        for (int guard = 0; guard < order.size() + 1; guard++) {
            turnIdx = (turnIdx + 1) % order.size();
            Tank t = order.get(turnIdx);
            if (t.alive) {
                active = t;
                break;
            }
        }
        turnCount++;
        dryTurns++;
        if (!suddenDeath && dryTurns > aliveCount() * 3 + 6) {
            suddenDeath = true;
            for (Tank t : tanks) t.fullPower = true;
            text(w / 2f, H * 0.25f, "SUDDEN DEATH: full power restored!", Col.BAD, 30);
        }
        if (st.windChanges && turnCount > 1) {
            int maxWind = Settings.WIND_MAX[st.wind];
            if (maxWind > 0) {
                wind += (float) rng.nextGaussian() * maxWind * 0.25f;
                wind = Math.max(-maxWind, Math.min(maxWind, wind));
            }
        }
        if (active.weapon != Items.PEA && active.p.count(active.weapon) <= 0) active.weapon = Items.PEA;
        active.power = Math.min(active.power, active.maxPower());
        phase = PH_BANNER;
        phaseTime = 0;
        banner = active.p.name;
        if (!active.p.isHuman()) brain = new Ai.Brain(this, active);
    }

    boolean checkRoundOver() {
        int alive = aliveCount();
        if (alive <= 1 || turnCount > Math.max(150, 30 * tanks.size())) {
            roundOver = true;
            phase = PH_OVER;
            phaseTime = 0;
            winner = null;
            if (alive == 1) {
                for (Tank t : tanks) if (t.alive) winner = t.p;
            }
            if (winner != null) {
                winner.wins++;
                winner.roundWinner = true;
                winner.score += 100;
                winner.earn(PAY_WIN);
                text(winner.tank.x, winner.tank.y - 60, "WINNER +$" + PAY_WIN, Col.MONEY, 24);
                banner = winner.name + " wins the round!";
                game.sfx(Platform.SND_WIN, 1f);
            } else {
                banner = alive == 0 ? "Everyone was destroyed!" : "Stalemate!";
            }
            return true;
        }
        return false;
    }

    public void update(float rawDt) {
        float dt = Math.min(rawDt, 0.05f) * timeScale();
        phaseTime += dt;
        if (shake > 0) shake = Math.max(0, shake - dt * 30f);
        for (Tank t : tanks) if (t.hitFlash > 0) t.hitFlash -= dt;
        for (int i = 0; i < cloudX.length; i++) {
            cloudX[i] += (windAccel() * 0.25f + 4f) * dt * (0.6f + cloudS[i] * 0.4f);
            if (cloudX[i] > w + 150) cloudX[i] = -150;
            if (cloudX[i] < -150) cloudX[i] = w + 150;
        }

        // Fixed-step physics so the AI's predictions match reality.
        accum += dt;
        int steps = 0;
        while (accum >= DT && steps < 40) {
            accum -= DT;
            steps++;
            physicsStep();
        }
        if (steps >= 40) accum = 0;

        updateEffects(dt);
        updateParticles(dt);

        switch (phase) {
            case PH_BANNER:
                if (phaseTime > (isAiTurn() ? 0.7f : 0.9f)) {
                    phase = PH_AIM;
                    phaseTime = 0;
                }
                break;
            case PH_AIM:
                if (anyDying()) {
                    phase = PH_ACTION;
                    phaseTime = 0;
                    break;
                }
                if (brain != null) brain.update(dt);
                if (engineTimer > 0) engineTimer -= dt;
                break;
            case PH_ACTION:
                if (!busy()) {
                    if (processDeaths()) break;
                    if (phaseTime > 0.35f) endAction();
                } else {
                    phaseTime = Math.min(phaseTime, 0.2f);
                }
                break;
            case PH_OVER:
                break;
        }
    }

    boolean anyDying() {
        for (Tank t : tanks) if (t.alive && t.dying) return true;
        return false;
    }

    void endAction() {
        for (int i = 0; i < actionEarn.length; i++) {
            if (actionEarn[i] > 0) {
                Player p = game.players.get(i);
                if (p.tank != null) text(p.tank.x, p.tank.y - 58, "+$" + Ui.money(actionEarn[i]), Col.MONEY, 20);
                actionEarn[i] = 0;
            }
        }
        nextTurn();
    }

    /** Explodes any tank whose health hit zero. Returns true if something happened. */
    boolean processDeaths() {
        boolean any = false;
        for (Tank t : tanks) {
            if (!t.alive || !t.dying) continue;
            any = true;
            t.alive = false;
            t.dying = false;
            t.shieldHp = 0;
            t.shieldType = -1;
            t.p.deaths++;
            Player killer = t.killer >= 0 ? game.players.get(t.killer) : null;
            if (killer != null && killer != t.p) {
                killer.kills++;
                killer.roundKills++;
                killer.score += 50;
                killer.earn(PAY_KILL);
                actionEarn[killer.index] += PAY_KILL;
                text(t.x, t.y - 70, killer.name + " destroyed " + t.p.name, Col.ACCENT, 18);
            } else {
                text(t.x, t.y - 70, t.p.name + " self-destructed", Col.ACCENT, 18);
            }
            game.sfx(Platform.SND_DEATH, 1f);
            for (int i = 0; i < 24; i++) {
                float a = rng.nextFloat() * 6.28f, s = 100 + rng.nextFloat() * 350;
                particle(t.x, t.y - 10, (float) Math.cos(a) * s, (float) Math.sin(a) * s - 200, 1.4f, 3 + rng.nextFloat() * 3,
                        rng.nextBoolean() ? t.p.color : 0xFF333333, Fx.Particle.DEBRIS);
            }
            explode(t.x, t.y - 8, 52, 35, killer, null, 1f, true, 0xFFFF7A2A);
        }
        return any;
    }

    // =====================================================================
    // Player actions
    // =====================================================================

    /** Drive the active tank. Returns false if it can't move (no fuel, blocked, buried). */
    public boolean drive(int dir, float dt) {
        Tank t = active;
        if (t == null || phase != PH_AIM || t.falling || buried(t)) return false;
        float dist = 55f * dt;
        boolean moved = false;
        while (dist > 0) {
            float stepLen = Math.min(0.5f, dist);
            dist -= stepLen;
            float nx = t.x + dir * stepLen;
            if (nx < Tank.W / 2 || nx > w - Tank.W / 2) break;
            float ny = support(nx);
            float rise = t.y - ny;
            if (rise > stepLen * 1.3f + 0.05f) break; // too steep
            float cost = stepLen * (1f + Math.max(0, rise) * 2.5f);
            if (t.p.fuel < cost) {
                t.p.fuel = 0;
                break;
            }
            t.p.fuel -= cost;
            t.x = nx;
            moved = true;
            if (ny > t.y + 3) {
                t.falling = true;
                t.fallStartY = t.y;
                t.vy = 0;
                break;
            }
            t.y = ny;
        }
        if (moved && engineTimer <= 0) {
            game.sfx(Platform.SND_ENGINE, 0.35f);
            engineTimer = 0.22f;
            if (rng.nextFloat() < 0.5f) dust(t.x - dir * 18, t.y, 1);
        }
        return moved;
    }

    public void fire() {
        Tank t = active;
        if (t == null || phase != PH_AIM || !t.alive || t.falling) return;
        Item it = Items.ALL[t.weapon];
        if (!it.infinite) {
            if (t.p.inv[it.id] <= 0) {
                t.weapon = Items.PEA;
                it = Items.ALL[Items.PEA];
            } else {
                t.p.inv[it.id]--;
            }
        }
        float rad = (float) Math.toRadians(t.angle);
        float speed = Math.min(t.power, t.maxPower()) * SPEED_K;
        if (it.behavior == Items.B_RAIL) speed *= 1.0f;
        Projectile p = new Projectile(it, t.p, Projectile.SHELL, t.barrelTipX(), t.barrelTipY(),
                (float) Math.cos(rad) * speed, -(float) Math.sin(rad) * speed);
        p.recordTrail = true;
        recording = p;
        recN = 0;
        addProjectile(p);
        game.sfx(it.behavior == Items.B_RAIL ? Platform.SND_ZAP : Platform.SND_FIRE, 0.9f);
        for (int i = 0; i < 6; i++) {
            particle(t.barrelTipX(), t.barrelTipY(), (float) Math.cos(rad) * 60 + (rng.nextFloat() - 0.5f) * 40,
                    -(float) Math.sin(rad) * 60 + (rng.nextFloat() - 0.5f) * 40, 0.6f, 4, 0xFFBBBBBB, Fx.Particle.SMOKE);
        }
        if (t.weapon != Items.PEA && t.p.inv[t.weapon] <= 0) t.weapon = Items.PEA;
        phase = PH_ACTION;
        phaseTime = 0;
    }

    public boolean useRepair() {
        Tank t = active;
        if (t == null || phase != PH_AIM || t.p.inv[Items.REPAIR] <= 0 || t.health >= 100) return false;
        t.p.inv[Items.REPAIR]--;
        float before = t.health;
        t.health = Math.min(100, t.health + 35);
        text(t.x, t.y - 50, "+" + Math.round(t.health - before) + " HP", Col.GOOD, 20);
        effects.add(new Fx.Sparkle(this, t.x, t.centerY(), 0xFF5BD46A));
        game.sfx(Platform.SND_SHIELD, 0.8f);
        return true;
    }

    public boolean activateShield(int id) {
        Tank t = active;
        if (t == null || phase != PH_AIM || !Items.isShield(id) || t.p.inv[id] <= 0) return false;
        Item it = Items.ALL[id];
        if (t.shieldHp >= it.shieldHp && t.shieldType == id) return false;
        t.p.inv[id]--;
        t.shieldType = id;
        t.shieldHp = it.shieldHp;
        text(t.x, t.y - 50, it.name + " up", Col.SHIELD, 18);
        game.sfx(Platform.SND_SHIELD, 0.8f);
        return true;
    }

    public void skipTurn() {
        if (phase == PH_AIM || phase == PH_BANNER) {
            phase = PH_ACTION;
            phaseTime = 0;
        }
    }

    public void surrender() {
        if (active != null && active.alive && (phase == PH_AIM || phase == PH_BANNER)) {
            active.health = 0;
            active.dying = true;
            active.killer = active.p.index;
            phase = PH_ACTION;
            phaseTime = 0;
        }
    }

    // =====================================================================
    // Physics
    // =====================================================================

    void addProjectile(Projectile p) {
        newProjs.add(p);
    }

    void physicsStep() {
        if (!newProjs.isEmpty()) {
            projs.addAll(newProjs);
            newProjs.clear();
        }
        for (int i = 0; i < projs.size(); i++) {
            Projectile p = projs.get(i);
            if (!p.dead) stepProjectile(p);
        }
        for (int i = projs.size() - 1; i >= 0; i--) if (projs.get(i).dead) projs.remove(i);

        // Falling tanks
        for (Tank t : tanks) {
            if (!t.alive) continue;
            float sup = support(t.x);
            if (!t.falling) {
                if (sup > t.y + 0.5f) {
                    if (sup - t.y < 2.5f) t.y = sup;
                    else {
                        t.falling = true;
                        t.fallStartY = t.y;
                        t.vy = 0;
                    }
                } else if (sup < t.y && t.y - sup < 3f) {
                    t.y = sup; // climb onto a thin layer of dirt instead of counting as buried
                }
            } else {
                if (!t.chute && !t.noFallDamage && sup - t.fallStartY > 45 && t.p.inv[Items.PARACHUTE] > 0) {
                    t.p.inv[Items.PARACHUTE]--;
                    t.chute = true;
                    text(t.x, t.y - 40, "Parachute!", 0xFFFFFFFF, 16);
                }
                if (t.chute) t.vy = Math.min(t.vy + G * DT, 80f);
                else t.vy += G * DT;
                t.y += t.vy * DT;
                if (t.y >= sup) {
                    float d = sup - t.fallStartY;
                    t.y = sup;
                    t.falling = false;
                    if (!t.chute && !t.noFallDamage && d > 20) {
                        Player src = t.lastHitBy >= 0 ? game.players.get(t.lastHitBy) : null;
                        hurt(t, (d - 20) * 0.32f, src, false);
                        dust(t.x, t.y, 8);
                        game.sfx(Platform.SND_BOUNCE, 0.6f);
                    }
                    t.chute = false;
                    t.noFallDamage = false;
                }
            }
        }
    }

    void stepProjectile(Projectile p) {
        p.age += DT;
        switch (p.kind) {
            case Projectile.ROLLING: stepRoller(p); return;
            case Projectile.BURROW: stepBurrow(p); return;
            case Projectile.LAVA: stepLava(p); return;
            default: break;
        }
        float px = p.x, py = p.y;
        p.vx += windAccel() * p.windMul * DT;
        p.vy += G * p.gravMul * DT;
        if (p.kind == Projectile.SHELL && p.item.behavior == Items.B_HOMING && p.vy > 0 && p.age > 0.5f) home(p);
        p.x += p.vx * DT;
        p.y += p.vy * DT;

        // Splitter at the apex
        if (p.kind == Projectile.SHELL && p.item.behavior == Items.B_SPLITTER && !p.split && p.vy >= 0) {
            p.split = true;
            for (int k = -1; k <= 1; k++) {
                Projectile c = new Projectile(p.item, p.owner, Projectile.BOMBLET, p.x, p.y, p.vx + k * 110f, p.vy);
                c.radius = p.item.radius;
                c.damage = p.item.damage;
                c.recordTrail = p.recordTrail && k == 0;
                if (c.recordTrail) recording = c;
                addProjectile(c);
            }
            sparks(p.x, p.y, p.item.color, 8);
            p.dead = true;
            return;
        }

        p.tTimer += DT;
        if (p.tTimer > 0.025f) {
            p.tTimer = 0;
            p.pushTrail();
            if (p == recording && recN < recX.length) {
                recX[recN] = p.x;
                recY[recN] = p.y;
                recN++;
            }
        }

        // Walls
        if (p.x < 0 || p.x >= w) {
            if (st.walls == 1) {
                p.x = p.x < 0 ? -p.x : 2 * w - p.x - 1;
                p.vx = -p.vx * 0.9f;
                game.sfx(Platform.SND_BOUNCE, 0.4f);
            } else if (st.walls == 2) {
                p.x = p.x < 0 ? p.x + w : p.x - w;
                p.tCount = 0;
            } else if (p.x < -400 || p.x > w + 400) {
                miss(p);
                return;
            }
        }
        if (p.y > H + 40) {
            miss(p);
            return;
        }

        // Deflector shields
        for (Tank t : tanks) {
            if (!t.alive || t.shieldType != Items.SHIELD_D || t.shieldHp <= 0) continue;
            float cx = t.x, cy = t.centerY(), r = 36f;
            float d1 = dist(px, py, cx, cy), d2 = dist(p.x, p.y, cx, cy);
            if (d1 >= r && d2 < r) {
                float nx = (p.x - cx) / Math.max(0.01f, d2), ny = (p.y - cy) / Math.max(0.01f, d2);
                float dot = p.vx * nx + p.vy * ny;
                p.vx = (p.vx - 2 * dot * nx) * 0.8f;
                p.vy = (p.vy - 2 * dot * ny) * 0.8f;
                p.x = cx + nx * (r + 1);
                p.y = cy + ny * (r + 1);
                t.shieldHp -= 12;
                if (t.shieldHp <= 0) {
                    t.shieldHp = 0;
                    t.shieldType = -1;
                }
                sparks(p.x, p.y, 0xFFC08AFF, 10);
                game.sfx(Platform.SND_SHIELD, 0.7f);
            }
        }

        // Tanks
        for (Tank t : tanks) {
            if (!t.alive) continue;
            if (t.p == p.owner && p.age < 0.25f) continue;
            if (t.contains(p.x, p.y, 1f)) {
                impact(p, t);
                return;
            }
        }

        // Ground
        if (p.x >= 0 && p.x < w && p.y >= terrain.at(p.x)) {
            impact(p, null);
        }
    }

    void home(Projectile p) {
        Tank best = null;
        float bd = Float.MAX_VALUE;
        for (Tank t : tanks) {
            if (!t.alive || t.p == p.owner) continue;
            float d = dist(p.x, p.y, t.x, t.centerY());
            if (d < bd) {
                bd = d;
                best = t;
            }
        }
        if (best == null) return;
        float speed = (float) Math.sqrt(p.vx * p.vx + p.vy * p.vy);
        float want = (float) Math.atan2(best.centerY() - p.y, best.x - p.x);
        float cur = (float) Math.atan2(p.vy, p.vx);
        float diff = want - cur;
        while (diff > Math.PI) diff -= 2 * Math.PI;
        while (diff < -Math.PI) diff += 2 * Math.PI;
        float turn = 2.6f * DT;
        cur += Math.max(-turn, Math.min(turn, diff));
        speed = Math.min(speed, 700f);
        p.vx = (float) Math.cos(cur) * speed;
        p.vy = (float) Math.sin(cur) * speed - G * p.gravMul * DT * 0.7f;
        if (p.age % 0.05f < DT) particle(p.x, p.y, 0, 0, 0.5f, 3, 0xFFFFAA66, Fx.Particle.SMOKE);
    }

    void miss(Projectile p) {
        p.dead = true;
        if (p.recordTrail) {
            Tank t = p.owner.tank;
            if (t != null) {
                t.lastImpactX = Float.NaN;
                saveTrail(t);
            }
        }
    }

    void saveTrail(Tank t) {
        if (t.lastTrailX == null) {
            t.lastTrailX = new float[recX.length];
            t.lastTrailY = new float[recY.length];
        }
        System.arraycopy(recX, 0, t.lastTrailX, 0, recN);
        System.arraycopy(recY, 0, t.lastTrailY, 0, recN);
        t.lastTrailN = recN;
    }

    Player immune;

    /** Shots that burst right at the muzzle (e.g. a buried tank digging out) never hurt the shooter. */
    void impact(Projectile p, Tank direct) {
        immune = (p.kind == Projectile.SHELL && p.age < 0.15f) ? p.owner : null;
        impactInner(p, direct);
        immune = null;
    }

    void impactInner(Projectile p, Tank direct) {
        if (p.recordTrail && (p.kind == Projectile.SHELL || p.kind == Projectile.BOMBLET)) {
            Tank t = p.owner.tank;
            if (t != null) {
                t.lastImpactX = p.x;
                t.lastImpactY = p.y;
                if (recN < recX.length) {
                    recX[recN] = p.x;
                    recY[recN] = p.y;
                    recN++;
                }
                saveTrail(t);
            }
            p.recordTrail = false;
        }
        Item it = p.item;
        float ix = p.x, iy = Math.min(p.y, p.x >= 0 && p.x < w ? terrain.at(p.x) + 2 : p.y);
        if (direct != null) iy = p.y;

        if (p.kind == Projectile.BOMBLET || p.kind == Projectile.AIRBOMB) {
            explode(ix, iy, p.radius, p.damage, p.owner, direct, 1.5f, true, it.color);
            p.dead = true;
            return;
        }

        switch (it.behavior) {
            case Items.B_CLUSTER: {
                explode(ix, iy, 18, 14, p.owner, direct, 1.5f, true, it.color);
                for (int k = 0; k < 6; k++) {
                    Projectile c = new Projectile(it, p.owner, Projectile.BOMBLET, ix, iy - 6,
                            (rng.nextFloat() - 0.5f) * 440f, -220 - rng.nextFloat() * 220f);
                    c.radius = it.radius;
                    c.damage = it.damage;
                    addProjectile(c);
                }
                p.dead = true;
                return;
            }
            case Items.B_BOUNCER: {
                explode(ix, iy, it.radius, it.damage, p.owner, direct, 1.5f, true, it.color);
                if (direct == null && p.bounces < 3 && ix >= 0 && ix < w) {
                    float s = terrain.slope(ix);
                    float len = (float) Math.sqrt(s * s + 1);
                    float nx = s / len, ny = -1 / len;
                    float dot = p.vx * nx + p.vy * ny;
                    p.vx = (p.vx - 2 * dot * nx) * 0.72f;
                    p.vy = (p.vy - 2 * dot * ny) * 0.72f;
                    if (p.vy > -180) p.vy = -220;
                    p.y = terrain.at(ix) - 4;
                    p.bounces++;
                    game.sfx(Platform.SND_BOUNCE, 0.6f);
                } else p.dead = true;
                return;
            }
            case Items.B_LEAPFROG: {
                explode(ix, iy, it.radius, it.damage, p.owner, direct, 1.5f, true, it.color);
                if (direct == null && p.hops < 2 && ix >= 0 && ix < w) {
                    float dir = Math.signum(p.vx == 0 ? 1 : p.vx);
                    p.vx = dir * Math.max(130f, Math.abs(p.vx) * 0.7f);
                    p.vy = -380;
                    p.y = terrain.at(ix) - 4;
                    p.hops++;
                } else p.dead = true;
                return;
            }
            case Items.B_ROLLER: {
                if (direct != null || ix < 0 || ix >= w) {
                    explode(ix, iy, it.radius, it.damage, p.owner, direct, 1.5f, true, it.color);
                    p.dead = true;
                } else {
                    p.kind = Projectile.ROLLING;
                    p.vx = Math.max(-260, Math.min(260, p.vx * 0.45f));
                    p.vy = 0;
                    p.timer = 0;
                    p.y = terrain.atSmooth(ix) - 5;
                }
                return;
            }
            case Items.B_DIGGER: {
                if (direct != null) {
                    explode(ix, iy, it.radius, it.damage, p.owner, direct, 1.5f, true, it.color);
                    p.dead = true;
                } else {
                    p.kind = Projectile.BURROW;
                    float sp = (float) Math.sqrt(p.vx * p.vx + p.vy * p.vy);
                    p.vx = p.vx / sp * 280f;
                    p.vy = Math.max(0.3f, p.vy / sp) * 280f;
                    p.timer = 0;
                    dust(ix, iy, 6);
                }
                return;
            }
            case Items.B_TESLA: {
                explode(ix, iy, it.radius, it.damage, p.owner, direct, 1.5f, true, it.color);
                List<Tank> targets = Fx.nearbyEnemies(this, ix, iy, 260, p.owner, 4);
                if (!targets.isEmpty()) newEffects.add(new Fx.Lightning(this, ix, iy, targets, p.owner));
                p.dead = true;
                return;
            }
            case Items.B_SHOCKWAVE: {
                explode(ix, iy, it.radius, 20, p.owner, direct, 1.5f, true, it.color);
                newEffects.add(new Fx.Shockwave(this, ix, iy, p.owner));
                game.sfx(Platform.SND_BOOM_L, 0.9f);
                p.dead = true;
                return;
            }
            case Items.B_LAVA: {
                explode(ix, iy, it.radius, it.damage, p.owner, direct, 1.5f, true, it.color);
                for (int k = 0; k < 22; k++) {
                    Projectile c = new Projectile(it, p.owner, Projectile.LAVA, ix, iy - 4,
                            (rng.nextFloat() - 0.5f) * 300f, -100 - rng.nextFloat() * 240f);
                    addProjectile(c);
                }
                game.sfx(Platform.SND_FIZZ, 0.9f);
                p.dead = true;
                return;
            }
            case Items.B_AIRSTRIKE: {
                newEffects.add(new Fx.AirStrike(this, ix, iy, p.owner, it));
                sparks(ix, iy, 0xFFFF4040, 10);
                p.dead = true;
                return;
            }
            case Items.B_BLACKHOLE: {
                newEffects.add(new Fx.BlackHole(this, ix, Math.min(iy, terrain.at(ix) - 10), p.owner));
                game.sfx(Platform.SND_TELEPORT, 0.9f);
                p.dead = true;
                return;
            }
            case Items.B_DIRT: {
                float dx = direct != null ? direct.x : ix, dy = direct != null ? direct.y - 10 : iy;
                terrain.addDirt(dx, dy, it.radius);
                dust(dx, dy, 30);
                game.sfx(Platform.SND_BOOM_S, 0.5f);
                p.dead = true;
                return;
            }
            case Items.B_TELEPORT: {
                Tank t = p.owner.tank;
                if (t != null && t.alive) {
                    newEffects.add(new Fx.Sparkle(this, t.x, t.centerY(), 0xFFFF9AF0));
                    t.x = Math.max(Tank.W / 2, Math.min(w - Tank.W / 2, ix));
                    float sup = support(t.x);
                    t.y = Math.min(sup, iy - 4);
                    t.noFallDamage = true;
                    t.falling = t.y < sup - 1;
                    t.fallStartY = t.y;
                    t.vy = 0;
                    newEffects.add(new Fx.Sparkle(this, t.x, t.centerY(), 0xFFFF9AF0));
                    game.sfx(Platform.SND_TELEPORT, 1f);
                }
                p.dead = true;
                return;
            }
            case Items.B_RAIL:
                explode(ix, iy, it.radius, it.damage, p.owner, direct, 2.0f, true, it.color);
                p.dead = true;
                return;
            default:
                explode(ix, iy, it.radius, it.damage, p.owner, direct, 1.5f, true, it.color);
                p.dead = true;
        }
    }

    void stepRoller(Projectile p) {
        p.timer += DT;
        float s = terrain.slope(p.x);
        p.vx += s * 700f * DT;
        p.vx *= (1f - 0.5f * DT);
        p.x += p.vx * DT;
        if (p.x < 1 || p.x >= w - 1) {
            if (st.walls == 1) {
                p.vx = -p.vx;
                p.x = Math.max(1, Math.min(w - 2, p.x));
            } else if (st.walls == 2) {
                p.x = p.x < 1 ? p.x + w - 3 : p.x - w + 3;
            } else {
                p.dead = true;
                return;
            }
        }
        float gy = terrain.atSmooth(p.x) - 5;
        p.y = gy;
        p.tTimer += DT;
        if (p.tTimer > 0.04f) {
            p.tTimer = 0;
            p.pushTrail();
        }
        for (Tank t : tanks) {
            if (!t.alive || (t.p == p.owner && p.timer < 0.6f)) continue;
            if (t.contains(p.x, p.y, 3f)) {
                explode(p.x, p.y, p.radius, p.damage, p.owner, t, 1.5f, true, p.item.color);
                p.dead = true;
                return;
            }
        }
        if (Math.abs(p.vx) < 10f) p.stopTimer += DT;
        else p.stopTimer = 0;
        if (p.stopTimer > 0.45f || p.timer > 6f) {
            explode(p.x, p.y, p.radius, p.damage, p.owner, null, 1.5f, true, p.item.color);
            p.dead = true;
        }
    }

    void stepBurrow(Projectile p) {
        p.timer += DT;
        p.x += p.vx * DT;
        p.y += p.vy * DT;
        if (((int) (p.timer / DT)) % 3 == 0) terrain.crater(p.x, p.y, 9);
        for (Tank t : tanks) {
            if (t.alive && t.contains(p.x, p.y, 2f) && !(t.p == p.owner && p.age < 0.3f)) {
                explode(p.x, p.y, p.radius, p.damage, p.owner, t, 1.5f, true, p.item.color);
                p.dead = true;
                return;
            }
        }
        if (p.timer > 0.3f || p.y > H - 8 || p.x < 0 || p.x >= w) {
            explode(p.x, Math.min(p.y, H - 8), p.radius, p.damage, p.owner, null, 1.5f, true, p.item.color);
            p.dead = true;
        }
    }

    void stepLava(Projectile p) {
        p.timer += DT;
        if (!p.onGround) {
            p.vy += G * DT;
            p.vx += windAccel() * 0.3f * DT;
            p.x += p.vx * DT;
            p.y += p.vy * DT;
            if (p.x >= 0 && p.x < w && p.y >= terrain.at(p.x) - 2) {
                p.onGround = true;
                p.vy = 0;
                p.vx *= 0.4f;
            }
        } else {
            p.vx += terrain.slope(p.x) * 520f * DT;
            p.vx *= (1f - 1.2f * DT);
            p.x += p.vx * DT;
            if (p.x >= 0 && p.x < w) {
                float gy = terrain.atSmooth(p.x) - 2;
                if (gy > p.y + 5) p.onGround = false;
                else p.y = gy;
            }
        }
        if (p.x < 0 || p.x >= w || p.y > H) {
            p.dead = true;
            return;
        }
        for (Tank t : tanks) {
            if (!t.alive) continue;
            if (t.p == p.owner && p.timer < 0.4f) continue;
            if (t.contains(p.x, p.y, 4f)) {
                hurt(t, 2.5f, p.owner, false);
                sparks(p.x, p.y, 0xFFFF8A2A, 3);
                p.dead = true;
                return;
            }
        }
        if (p.timer > 4.5f) p.dead = true;
        if (rng.nextFloat() < 0.01f) particle(p.x, p.y, 0, -20, 0.8f, 4, 0x88555555, Fx.Particle.SMOKE);
    }

    // =====================================================================
    // Damage
    // =====================================================================

    void explode(float x, float y, float r, float dmg, Player owner, Tank direct, float directMul, boolean crater, int color) {
        if (crater) {
            float removed = terrain.crater(x, y, r);
            int chunks = Math.min(22, (int) (removed / 40f) + 3);
            for (int i = 0; i < chunks; i++) {
                float a = (float) (-Math.PI / 2 + (rng.nextFloat() - 0.5f) * 2.2f);
                float s = 120 + rng.nextFloat() * 260;
                particle(x + (rng.nextFloat() - 0.5f) * r, y, (float) Math.cos(a) * s, (float) Math.sin(a) * s,
                        1.2f + rng.nextFloat() * 0.6f, 2 + rng.nextFloat() * 2.5f, theme.dirtTop, Fx.Particle.DEBRIS);
            }
        }
        newEffects.add(new Fx.Explosion(this, x, y, r, color == 0xFFD8D8D8 ? 0xFFFF9A3A : blendFire(color)));
        for (int i = 0; i < Math.min(10, (int) (r / 8)); i++) {
            particle(x + (rng.nextFloat() - 0.5f) * r, y + (rng.nextFloat() - 0.5f) * r * 0.6f,
                    (rng.nextFloat() - 0.5f) * 40, -30 - rng.nextFloat() * 40, 1.2f + rng.nextFloat(),
                    r * 0.25f, 0x66404040, Fx.Particle.SMOKE);
        }
        shake = Math.max(shake, Math.min(16f, r * 0.12f));
        game.sfx(r >= 60 ? Platform.SND_BOOM_L : Platform.SND_BOOM_S, Math.min(1f, 0.5f + r / 100f));
        if (dmg <= 0) return;

        for (Tank t : tanks) {
            if (!t.alive || t.p == immune) continue;
            float d;
            boolean isDirect = (t == direct);
            if (isDirect) {
                d = dmg * directMul;
            } else {
                float dist = t.distanceTo(x, y);
                if (dist >= r) continue;
                float frac = 1f - dist / r;
                d = dmg * (0.15f + 0.85f * frac);
            }
            if (d < 0.5f) continue;
            hurt(t, d, owner, isDirect);
        }
    }

    private int blendFire(int c) {
        return Col.lerp(c, 0xFFFF8A2A, 0.45f);
    }

    /** Applies damage to a tank, routing it through shields and paying the attacker. */
    void hurt(Tank t, float d, Player src, boolean direct) {
        if (!t.alive || d <= 0) return;
        float absorbed = 0;
        if (t.shieldHp > 0) {
            absorbed = Math.min(t.shieldHp, d);
            t.shieldHp -= absorbed;
            d -= absorbed;
            if (t.shieldHp <= 0.5f) {
                t.shieldHp = 0;
                t.shieldType = -1;
                text(t.x, t.y - 64, "Shield down", Col.SHIELD, 16);
            }
        }
        float before = t.health;
        t.health = Math.max(0, t.health - d);
        float lost = before - t.health;
        t.hitFlash = 0.35f;
        if (lost + absorbed > 0.5f) dryTurns = 0;
        if (src != null && src != t.p) t.lastHitBy = src.index;
        if (lost >= 0.5f) text(t.x + (rng.nextFloat() - 0.5f) * 16, t.y - 38, "-" + Math.round(lost), Col.BAD, 22);
        if (absorbed >= 0.5f) text(t.x + (rng.nextFloat() - 0.5f) * 16, t.y - 54, "-" + Math.round(absorbed), Col.SHIELD, 18);

        if (src != null && src != t.p) {
            int pay = Math.round(lost * PAY_PER_HP + absorbed * PAY_PER_SHIELD_HP);
            if (direct) {
                pay += PAY_DIRECT;
                text(t.x, t.y - 80, "DIRECT HIT!", Col.ACCENT, 22);
            }
            src.earn(pay);
            actionEarn[src.index] += pay;
            src.roundDamage += lost;
            src.damage += lost;
            src.score += lost;
            t.p.damageFrom[src.index] += lost + absorbed;
        }
        if (t.health <= 0 && !t.dying) {
            t.dying = true;
            t.killer = src != null ? src.index : (t.lastHitBy >= 0 ? t.lastHitBy : t.p.index);
        }
    }

    // =====================================================================
    // Effects and particles
    // =====================================================================

    void updateEffects(float dt) {
        if (!newEffects.isEmpty()) {
            effects.addAll(newEffects);
            newEffects.clear();
        }
        for (int i = 0; i < effects.size(); i++) effects.get(i).update(dt);
        for (int i = effects.size() - 1; i >= 0; i--) if (effects.get(i).done) effects.remove(i);
        for (int i = texts.size() - 1; i >= 0; i--) {
            Fx.FloatText t = texts.get(i);
            t.life -= dt;
            t.y -= 28 * dt;
            if (t.life <= 0) texts.remove(i);
        }
    }

    void updateParticles(float dt) {
        for (int i = particles.size() - 1; i >= 0; i--) {
            Fx.Particle p = particles.get(i);
            p.life -= dt;
            if (p.life <= 0) {
                particles.remove(i);
                continue;
            }
            switch (p.type) {
                case Fx.Particle.DEBRIS:
                case Fx.Particle.CONFETTI:
                    p.vy += G * 0.8f * dt;
                    if (p.type == Fx.Particle.CONFETTI) {
                        p.vy = Math.min(p.vy, 90);
                        p.vx *= (1 - 1.5f * dt);
                    }
                    p.x += p.vx * dt;
                    p.y += p.vy * dt;
                    if (p.type == Fx.Particle.DEBRIS && p.x >= 0 && p.x < w && p.y > terrain.at(p.x)) {
                        p.y = terrain.at(p.x);
                        p.vx *= 0.3f;
                        p.vy = -p.vy * 0.2f;
                    }
                    break;
                case Fx.Particle.SMOKE:
                    p.x += (p.vx + windAccel() * 0.2f) * dt;
                    p.y += p.vy * dt;
                    p.size += 8 * dt;
                    break;
                case Fx.Particle.SPARK:
                    p.vy += G * 0.5f * dt;
                    p.x += p.vx * dt;
                    p.y += p.vy * dt;
                    break;
                case Fx.Particle.SWIRL: {
                    float dx = p.ox - p.x, dy = p.oy - p.y;
                    p.x += (dx * 2.5f - dy * 3f) * dt;
                    p.y += (dy * 2.5f + dx * 3f) * dt;
                    break;
                }
            }
        }
    }

    Fx.Particle particle(float x, float y, float vx, float vy, float life, float size, int color, int type) {
        if (particles.size() > 700) return null;
        Fx.Particle p = new Fx.Particle();
        p.x = x;
        p.y = y;
        p.vx = vx;
        p.vy = vy;
        p.life = life;
        p.maxLife = life;
        p.size = size;
        p.color = color;
        p.type = type;
        particles.add(p);
        return p;
    }

    void sparks(float x, float y, int color, int n) {
        for (int i = 0; i < n; i++) {
            float a = rng.nextFloat() * 6.28f, s = 60 + rng.nextFloat() * 220;
            particle(x, y, (float) Math.cos(a) * s, (float) Math.sin(a) * s, 0.4f + rng.nextFloat() * 0.3f, 2f, color, Fx.Particle.SPARK);
        }
    }

    void dust(float x, float y, int n) {
        for (int i = 0; i < n; i++) {
            particle(x + (rng.nextFloat() - 0.5f) * 30, y - 2, (rng.nextFloat() - 0.5f) * 60, -10 - rng.nextFloat() * 30,
                    0.8f + rng.nextFloat() * 0.5f, 5 + rng.nextFloat() * 5, Col.alpha(theme.dirtTop, 0.5f), Fx.Particle.SMOKE);
        }
    }

    void text(float x, float y, String s, int color, float size) {
        Fx.FloatText t = new Fx.FloatText();
        t.x = Math.max(60, Math.min(w - 60, x));
        t.y = Math.max(30, y);
        t.text = s;
        t.color = color;
        t.size = size;
        t.life = 1.6f;
        texts.add(t);
    }

    public void celebrate() {
        if (winner == null || winner.tank == null) return;
        if (rng.nextFloat() < 0.35f) {
            Tank t = winner.tank;
            particle(t.x + (rng.nextFloat() - 0.5f) * 200, -10, (rng.nextFloat() - 0.5f) * 80, 40, 3f, 4f,
                    TankArt.COLORS[rng.nextInt(TankArt.COLORS.length)], Fx.Particle.CONFETTI);
        }
    }

    static float dist(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2, dy = y1 - y2;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    // =====================================================================
    // Simulation for the AI and the aim guide
    // =====================================================================

    /**
     * Predicts where a shot lands. result[0..1] = impact x,y; result[2] = index in tanks of a tank hit
     * directly, or -1; result[3] = flight time. Returns false if the shot leaves the field.
     */
    boolean simulate(float x0, float y0, float angle, float power, Item it, float windA, Tank shooter,
                     float[] result, float[] pathX, float[] pathY, int maxPath, float maxTime) {
        float rad = (float) Math.toRadians(angle);
        float speed = power * SPEED_K;
        float x = x0, y = y0, vx = (float) Math.cos(rad) * speed, vy = -(float) Math.sin(rad) * speed;
        float gm = 1f, wm = 1f;
        if (it.behavior == Items.B_RAIL) {
            gm = 0.18f;
            wm = 0f;
        }
        float t = 0, rec = 0;
        int n = 0;
        result[2] = -1;
        while (t < maxTime) {
            t += DT;
            vx += windA * wm * DT;
            vy += G * gm * DT;
            x += vx * DT;
            y += vy * DT;
            if (pathX != null) {
                rec += DT;
                if (rec > 0.03f && n < maxPath) {
                    rec = 0;
                    pathX[n] = x;
                    pathY[n] = y;
                    n++;
                }
            }
            if (x < 0 || x >= w) {
                if (st.walls == 1) {
                    x = x < 0 ? -x : 2 * w - x - 1;
                    vx = -vx * 0.9f;
                } else if (st.walls == 2) {
                    x = x < 0 ? x + w : x - w;
                } else if (x < -400 || x > w + 400) {
                    result[0] = x;
                    result[1] = y;
                    result[3] = t;
                    if (pathX != null) result[4] = n;
                    return false;
                }
            }
            if (y > H + 40) {
                result[0] = x;
                result[1] = y;
                result[3] = t;
                if (pathX != null) result[4] = n;
                return false;
            }
            for (int i = 0; i < tanks.size(); i++) {
                Tank tk = tanks.get(i);
                if (!tk.alive || (tk == shooter && t < 0.25f)) continue;
                if (tk.contains(x, y, 1f)) {
                    result[0] = x;
                    result[1] = y;
                    result[2] = i;
                    result[3] = t;
                    if (pathX != null) result[4] = n;
                    return true;
                }
            }
            if (x >= 0 && x < w && y >= terrain.at(x)) {
                result[0] = x;
                result[1] = y;
                result[3] = t;
                if (pathX != null) result[4] = n;
                return true;
            }
        }
        result[0] = x;
        result[1] = y;
        result[3] = t;
        if (pathX != null) result[4] = n;
        return false;
    }

    // =====================================================================
    // Rendering (world coordinates; caller applies the world transform)
    // =====================================================================

    final float[] polyX = new float[2600], polyY = new float[2600];
    final float[] guideX = new float[200], guideY = new float[200];
    final float[] simRes = new float[6];

    public void render(Gfx g, float time) {
        // Sky
        g.fillRectVGradient(-60, -400, w + 120, H + 460, theme.skyTop, theme.skyBottom);
        if (theme.stars) {
            for (int i = 0; i < starX.length; i++) {
                float tw = 0.5f + 0.5f * (float) Math.sin(time * 2 + i * 1.7f);
                g.fillCircle(starX[i], starY[i], 1.2f + (i % 3) * 0.5f, Col.alpha(0xFFFFFFFF, 0.3f + 0.6f * tw));
            }
        }
        if (theme.sunColor != 0) {
            float sx = w * theme.sunX, sy = H * theme.sunY;
            g.fillRadial(sx, sy, 140, Col.alpha(theme.sunColor, 0.45f), Col.withAlpha(theme.sunColor, 0));
            g.fillCircle(sx, sy, 46, theme.sunColor);
        }
        // Distant ranges
        int fn = farX.length;
        g.fillPoly(farX, farY1, fn, theme.far1);
        g.fillPoly(farX, farY2, fn, theme.far2);
        // Clouds
        for (int i = 0; i < cloudX.length; i++) cloud(g, cloudX[i], cloudY[i], cloudS[i]);

        // Tanks are drawn before the ground so buried tanks are hidden by dirt.
        for (Tank t : tanks) {
            if (!t.alive) continue;
            float ang = t.angle;
            int col = t.p.color;
            if (t.hitFlash > 0 && ((int) (t.hitFlash * 20)) % 2 == 0) col = 0xFFFFFFFF;
            if (t.chute) {
                g.line(t.x - 14, t.y - 18, t.x, t.y - 62, 1f, 0xFFDDDDDD);
                g.line(t.x + 14, t.y - 18, t.x, t.y - 62, 1f, 0xFFDDDDDD);
                for (int i = 0; i <= 10; i++) {
                    double a = Math.PI * i / 10;
                    polyX[i] = t.x + (float) Math.cos(a) * 24;
                    polyY[i] = t.y - 60 - (float) Math.sin(a) * 16;
                }
                g.fillPoly(polyX, polyY, 11, 0xFFF0F0F0);
                g.line(t.x - 24, t.y - 60, t.x + 24, t.y - 60, 2f, 0xFFE84A3C);
            }
            TankArt.draw(g, t.p.style, col, t.x, t.y, ang, time);
        }

        // Ground
        int n = 0;
        int step = Math.max(1, w / 1200);
        polyX[n] = 0;
        polyY[n] = H + 50;
        n++;
        for (int x = 0; x < w; x += step) {
            polyX[n] = x;
            polyY[n] = terrain.y[x];
            n++;
        }
        polyX[n] = w - 1;
        polyY[n] = terrain.y[w - 1];
        n++;
        polyX[n] = w - 1;
        polyY[n] = H + 50;
        n++;
        g.fillPolyVGradient(polyX, polyY, n, H * 0.3f, theme.dirtTop, H, theme.dirtBottom);
        for (int x = 0; x < w - step; x += step * 2) {
            g.line(x, terrain.y[x] + 1.5f, x + step * 2, terrain.y[Math.min(w - 1, x + step * 2)] + 1.5f, 4f, theme.grass);
        }

        // Previous shot trace for the human whose turn it is
        if (active != null && active.p.isHuman() && phase == PH_AIM && active.lastTrailN > 1) {
            for (int i = 0; i < active.lastTrailN; i += 2) {
                g.fillCircle(active.lastTrailX[i], active.lastTrailY[i], 1.6f, Col.alpha(active.p.color, 0.45f));
            }
            if (!Float.isNaN(active.lastImpactX)) {
                float ix = active.lastImpactX, iy = active.lastImpactY;
                g.line(ix - 6, iy - 6, ix + 6, iy + 6, 2f, Col.alpha(0xFFFFFFFF, 0.7f));
                g.line(ix - 6, iy + 6, ix + 6, iy - 6, 2f, Col.alpha(0xFFFFFFFF, 0.7f));
            }
        }

        // Projectiles
        for (Projectile p : projs) renderProjectile(g, p, time);

        for (Fx f : effects) f.render(g);

        for (Fx.Particle p : particles) {
            float a = Math.min(1f, p.life / p.maxLife * 1.5f);
            switch (p.type) {
                case Fx.Particle.SMOKE:
                    g.fillCircle(p.x, p.y, p.size, Col.alpha(p.color, a * 0.8f));
                    break;
                case Fx.Particle.SPARK:
                    g.line(p.x, p.y, p.x - p.vx * 0.02f, p.y - p.vy * 0.02f, 1.6f, Col.alpha(p.color, a));
                    break;
                case Fx.Particle.CONFETTI:
                    g.fillRect(p.x - p.size / 2, p.y - p.size / 4, p.size, p.size / 2 + (float) Math.abs(Math.sin(time * 9 + p.x)) * p.size / 2, p.color);
                    break;
                default:
                    g.fillRect(p.x - p.size / 2, p.y - p.size / 2, p.size, p.size, Col.alpha(p.color, a));
            }
        }

        // Tank overlays
        for (Tank t : tanks) {
            if (!t.alive) continue;
            if (t.shieldHp > 0) {
                Item si = Items.ALL[t.shieldType];
                float pulse = 0.75f + 0.25f * (float) Math.sin(time * 4 + t.x);
                g.fillRadial(t.x, t.centerY(), 36, Col.alpha(si.color, 0.05f), Col.alpha(si.color, 0.32f * pulse));
                g.strokeCircle(t.x, t.centerY(), 36, 1.5f, Col.alpha(si.color, 0.8f * pulse));
            }
            if (buried(t)) {
                g.text("BURIED", t.x, terrain.at(t.x) - 12, 13, 0xFFFFE08A, Gfx.ALIGN_CENTER, true);
            }
            // name + health
            float by = Math.min(t.y, terrain.at(t.x)) - 44;
            if (t.shieldHp > 0) by -= 10;
            float bw = 44;
            g.fillRoundRect(t.x - bw / 2 - 1, by - 1, bw + 2, 7, 3, 0xAA000000);
            float hp = Math.max(0, t.health) / 100f;
            int hc = hp > 0.6f ? Col.GOOD : (hp > 0.3f ? Col.ACCENT : Col.BAD);
            g.fillRoundRect(t.x - bw / 2, by, bw * hp, 5, 2.5f, hc);
            if (t.shieldHp > 0) g.fillRect(t.x - bw / 2, by + 5.5f, bw * Math.min(1f, t.shieldHp / 110f), 1.5f, Col.SHIELD);
            g.text(t.p.name, t.x, by - 9, 15, 0xFFFFFFFF, Gfx.ALIGN_CENTER, t == active);
        }

        // Aim guide
        if (isHumanAiming() && st.aimGuide > 0 && !active.falling) {
            float maxT = st.aimGuide == 1 ? 0.45f : 1.1f;
            Item it = Items.ALL[active.weapon];
            simRes[4] = 0;
            simulate(active.barrelTipX(), active.barrelTipY(), active.angle, Math.min(active.power, active.maxPower()), it,
                    windAccel(), active, simRes, guideX, guideY, guideX.length, maxT);
            int gn = (int) simRes[4];
            for (int i = 0; i < gn; i++) {
                float a = 1f - i / (float) Math.max(1, gn);
                g.fillCircle(guideX[i], guideY[i], 2.4f, Col.alpha(0xFFFFFFFF, 0.25f + 0.6f * a));
            }
        }

        // Active tank marker
        if (active != null && active.alive && (phase == PH_AIM || phase == PH_BANNER)) {
            float bob = (float) Math.sin(time * 6) * 4;
            float my = Math.min(active.y, terrain.at(active.x)) - 76 + bob - (active.shieldHp > 0 ? 10 : 0);
            TankArtArrow.draw(g, active.x, my, active.p.color);
        }

        for (Fx.FloatText t : texts) {
            float a = Math.min(1f, t.life * 1.5f);
            g.text(t.text, t.x + 1.5f, t.y + 1.5f, t.size, Col.alpha(0xFF000000, a * 0.7f), Gfx.ALIGN_CENTER, true);
            g.text(t.text, t.x, t.y, t.size, Col.alpha(t.color, a), Gfx.ALIGN_CENTER, true);
        }
    }

    void renderProjectile(Gfx g, Projectile p, float time) {
        int c = p.item.color;
        if (p.kind == Projectile.LAVA) {
            float a = Math.min(1f, (4.5f - p.timer) * 2);
            g.fillRadial(p.x, p.y, 7, Col.alpha(0xFFFFD040, a), Col.alpha(0x00FF4A00, 0));
            g.fillCircle(p.x, p.y, 2.6f, Col.alpha(0xFFFFF0A0, a));
            return;
        }
        // trail
        int cnt = p.tCount;
        for (int i = 0; i < cnt; i++) {
            int idx = (p.tHead - 1 - i + p.tx.length) % p.tx.length;
            float a = 1f - i / (float) p.tx.length;
            g.fillCircle(p.tx[idx], p.ty[idx], 2.2f * a + 0.5f, Col.alpha(p.kind == Projectile.BURROW ? theme.dirtTop : 0xFFFFE0B0, a * 0.5f));
        }
        if (p.kind == Projectile.BURROW) return;
        if (p.y < 0) {
            // off the top of the screen: show a marker with height
            float x = Math.max(10, Math.min(w - 10, p.x));
            float[] ax = {x, x - 8, x + 8}, ay = {4, 18, 18};
            g.fillPoly(ax, ay, 3, c);
            g.text(Math.round(-p.y) + "", x, 30, 12, 0xFFFFFFFF, Gfx.ALIGN_CENTER, false);
            return;
        }
        float r = p.kind == Projectile.BOMBLET || p.kind == Projectile.AIRBOMB ? 3.2f : 4.2f;
        g.fillRadial(p.x, p.y, r * 2.4f, Col.alpha(c, 0.6f), Col.withAlpha(c, 0));
        g.fillCircle(p.x, p.y, r, c);
        g.fillCircle(p.x - r * 0.3f, p.y - r * 0.3f, r * 0.4f, 0xFFFFFFFF);
    }

    void cloud(Gfx g, float x, float y, float s) {
        int c = theme.cloud;
        g.fillCircle(x, y, 28 * s, c);
        g.fillCircle(x + 30 * s, y + 6 * s, 22 * s, c);
        g.fillCircle(x - 30 * s, y + 8 * s, 20 * s, c);
        g.fillCircle(x + 10 * s, y - 14 * s, 22 * s, c);
        g.fillRoundRect(x - 48 * s, y + 4 * s, 98 * s, 22 * s, 11 * s, c);
    }

    // =====================================================================

    static final class Theme {
        int skyTop, skyBottom, far1, far2, dirtTop, dirtBottom, grass, cloud, sunColor;
        float sunX, sunY;
        boolean stars;

        static Theme pick(Random r) {
            Theme t = new Theme();
            switch (r.nextInt(6)) {
                case 0: // meadow day
                    t.skyTop = 0xFF3A7BD5; t.skyBottom = 0xFFA8D8F0; t.far1 = 0xFF7FA8C8; t.far2 = 0xFF6A9A8A;
                    t.dirtTop = 0xFF6B8E3A; t.dirtBottom = 0xFF3E2A1A; t.grass = 0xFF9BD05A; t.cloud = 0xDDFFFFFF;
                    t.sunColor = 0xFFFFF4B0; t.sunX = 0.8f; t.sunY = 0.14f;
                    break;
                case 1: // desert sunset
                    t.skyTop = 0xFF4A2A6A; t.skyBottom = 0xFFFF9A5A; t.far1 = 0xFF9A5A6A; t.far2 = 0xFFB86A50;
                    t.dirtTop = 0xFFD8A060; t.dirtBottom = 0xFF7A4A2A; t.grass = 0xFFF0C080; t.cloud = 0xAAFFC8A0;
                    t.sunColor = 0xFFFFD27A; t.sunX = 0.25f; t.sunY = 0.42f;
                    break;
                case 2: // snowy night
                    t.skyTop = 0xFF050A1E; t.skyBottom = 0xFF2A3A6A; t.far1 = 0xFF2A3658; t.far2 = 0xFF3A4A70;
                    t.dirtTop = 0xFFDDE6F0; t.dirtBottom = 0xFF5A6A88; t.grass = 0xFFFFFFFF; t.cloud = 0x55AABBDD;
                    t.sunColor = 0xFFE8EEF8; t.sunX = 0.7f; t.sunY = 0.12f; t.stars = true;
                    break;
                case 3: // alien world
                    t.skyTop = 0xFF120A2A; t.skyBottom = 0xFF5A2A7A; t.far1 = 0xFF3A2060; t.far2 = 0xFF4A2A70;
                    t.dirtTop = 0xFF4AC8A0; t.dirtBottom = 0xFF1A3A4A; t.grass = 0xFF9AFFD8; t.cloud = 0x55FF9AF0;
                    t.sunColor = 0xFFFF9AF0; t.sunX = 0.3f; t.sunY = 0.18f; t.stars = true;
                    break;
                case 4: // volcanic dusk
                    t.skyTop = 0xFF1A0A0A; t.skyBottom = 0xFF8A2A1A; t.far1 = 0xFF3A1A1A; t.far2 = 0xFF4A2018;
                    t.dirtTop = 0xFF5A4A48; t.dirtBottom = 0xFF1A1010; t.grass = 0xFFFF6A2A; t.cloud = 0x66402020;
                    t.sunColor = 0xFFFF5A2A; t.sunX = 0.6f; t.sunY = 0.3f; t.stars = true;
                    break;
                default: // lunar
                    t.skyTop = 0xFF000000; t.skyBottom = 0xFF14141E; t.far1 = 0xFF24242E; t.far2 = 0xFF32323E;
                    t.dirtTop = 0xFF9A9AA4; t.dirtBottom = 0xFF3A3A44; t.grass = 0xFFC8C8D0; t.cloud = 0x00000000;
                    t.sunColor = 0xFF5A9AFF; t.sunX = 0.82f; t.sunY = 0.16f; t.stars = true;
                    break;
            }
            return t;
        }
    }

    /** The bouncing "your turn" arrow. */
    static final class TankArtArrow {
        static final float[] X = new float[7], Y = new float[7];

        static void draw(Gfx g, float x, float y, int color) {
            float[] px = {-6, 6, 6, 13, 0, -13, -6};
            float[] py = {-16, -16, 0, 0, 14, 0, 0};
            for (int i = 0; i < 7; i++) {
                X[i] = x + px[i] * 1.15f + 1.5f;
                Y[i] = y + py[i] * 1.15f + 1.5f;
            }
            g.fillPoly(X, Y, 7, 0x88000000);
            for (int i = 0; i < 7; i++) {
                X[i] = x + px[i];
                Y[i] = y + py[i];
            }
            g.fillPoly(X, Y, 7, color);
        }
    }
}
