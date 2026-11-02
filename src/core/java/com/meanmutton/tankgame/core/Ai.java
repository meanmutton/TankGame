package com.meanmutton.tankgame.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Computer opponents: ten personalities that differ in skill, aggression, and play style. */
public final class Ai {
    private Ai() {}

    static final int T_RANDOM = 0, T_NEAREST = 1, T_WEAKEST = 2, T_REVENGE = 3, T_LEADER = 4, T_BEST = 5;
    static final int W_CHEAP = 0, W_RANDOM = 1, W_BIGGEST = 2, W_PRECISE = 3, W_BALANCED = 4, W_BEST = 5;
    static final int M_NONE = 0, M_RANDOM = 1, M_TOWARD = 2, M_AWAY = 3, M_SMART = 4;
    static final int ARC_LOW = 0, ARC_HIGH = 1, ARC_ANY = 2;

    public static final class Profile {
        public final String name, blurb;
        public final int skill;          // 1..10 for display
        final float noiseDeg, noisePow, windKnow, learn, spend, shieldUse, repairAt;
        final int target, weapons, move, arc;
        final int[] shop = new int[Items.COUNT];

        Profile(String name, int skill, String blurb, float noiseDeg, float noisePow, float windKnow, float learn,
                int target, int weapons, int move, int arc, float spend, float shieldUse, float repairAt, int... weights) {
            this.name = name;
            this.skill = skill;
            this.blurb = blurb;
            this.noiseDeg = noiseDeg;
            this.noisePow = noisePow;
            this.windKnow = windKnow;
            this.learn = learn;
            this.target = target;
            this.weapons = weapons;
            this.move = move;
            this.arc = arc;
            this.spend = spend;
            this.shieldUse = shieldUse;
            this.repairAt = repairAt;
            for (int i = 0; i + 1 < weights.length; i += 2) shop[weights[i]] = weights[i + 1];
        }

        public String targetName() {
            switch (target) {
                case T_NEAREST: return "nearest tank";
                case T_WEAKEST: return "weakest tank";
                case T_REVENGE: return "whoever hurt it";
                case T_LEADER: return "the leader";
                case T_BEST: return "best opportunity";
                default: return "anyone";
            }
        }
    }

    public static final Profile[] PROFILES = {
            new Profile("Rookie", 1, "Fresh out of training. Shoots wildly and rarely learns.",
                    10f, 95f, 0f, 0.15f, T_RANDOM, W_RANDOM, M_NONE, ARC_ANY, 0.6f, 0.25f, 30,
                    Items.HEAVY, 4, Items.BOUNCER, 2, Items.MOLE, 2, Items.DIRT, 2, Items.PARACHUTE, 1, Items.SHIELD_L, 1),
            new Profile("Penny Pincher", 2, "Hoards cash and plinks away with cheap shells.",
                    6.5f, 60f, 0.25f, 0.45f, T_NEAREST, W_CHEAP, M_NONE, ARC_LOW, 0.35f, 0.3f, 35,
                    Items.HEAVY, 6, Items.MOLE, 2, Items.REPAIR, 2, Items.PARACHUTE, 1),
            new Profile("Turtle", 3, "Hides behind shields and only strikes back at attackers.",
                    4.8f, 45f, 0.4f, 0.5f, T_REVENGE, W_BALANCED, M_AWAY, ARC_HIGH, 0.9f, 0.95f, 60,
                    Items.SHIELD_L, 4, Items.SHIELD_H, 4, Items.SHIELD_D, 3, Items.REPAIR, 5, Items.PARACHUTE, 3,
                    Items.FUEL, 3, Items.HEAVY, 3, Items.DIRT, 2, Items.TELEPORT, 2),
            new Profile("Wildcard", 4, "Pure chaos. Random targets, random toys, random driving.",
                    4.2f, 42f, 0.5f, 0.5f, T_RANDOM, W_RANDOM, M_RANDOM, ARC_ANY, 1.0f, 0.4f, 40,
                    Items.CLUSTER, 2, Items.BOUNCER, 3, Items.ROLLER, 3, Items.LEAPFROG, 3, Items.LAVA, 3,
                    Items.TESLA, 2, Items.QUAKE, 2, Items.DIRT, 3, Items.TELEPORT, 2, Items.SINGULARITY, 1,
                    Items.MOLE, 2, Items.FUEL, 2, Items.SHIELD_L, 1),
            new Profile("Berserker", 5, "Charges the nearest enemy with the biggest guns it can afford.",
                    3.6f, 35f, 0.6f, 0.6f, T_NEAREST, W_BIGGEST, M_TOWARD, ARC_LOW, 1.0f, 0f, 20,
                    Items.MEGATON, 5, Items.SUNBURST, 3, Items.CLUSTER, 3, Items.AIRSTRIKE, 2, Items.SINGULARITY, 2,
                    Items.HEAVY, 2, Items.FUEL, 3),
            new Profile("Bully", 6, "Picks on whoever is weakest. Smells blood.",
                    2.6f, 28f, 0.7f, 0.7f, T_WEAKEST, W_BALANCED, M_RANDOM, ARC_ANY, 0.85f, 0.5f, 40,
                    Items.HEAVY, 4, Items.CLUSTER, 2, Items.TRIDENT, 2, Items.ROLLER, 2, Items.RAIL, 2,
                    Items.SHIELD_L, 2, Items.REPAIR, 2, Items.FUEL, 1),
            new Profile("Avenger", 7, "Never forgets. Hunts down whoever hurt it most.",
                    2.0f, 22f, 0.8f, 0.75f, T_REVENGE, W_BALANCED, M_SMART, ARC_ANY, 0.85f, 0.6f, 45,
                    Items.HEAVY, 3, Items.MEGATON, 2, Items.TESLA, 2, Items.LEAPFROG, 2, Items.SEEKER, 2,
                    Items.SHIELD_H, 1, Items.SHIELD_L, 2, Items.REPAIR, 2, Items.FUEL, 2),
            new Profile("Sniper", 8, "Patient and precise. Finishes off wounded tanks.",
                    0.9f, 10f, 0.95f, 0.9f, T_WEAKEST, W_PRECISE, M_NONE, ARC_LOW, 0.8f, 0.5f, 40,
                    Items.RAIL, 6, Items.HEAVY, 4, Items.SEEKER, 3, Items.TRIDENT, 2, Items.SHIELD_L, 2, Items.REPAIR, 2),
            new Profile("Tactician", 9, "Reads the field, picks the best target and the right tool.",
                    0.7f, 8f, 1.0f, 0.9f, T_BEST, W_BEST, M_SMART, ARC_ANY, 0.9f, 0.8f, 50,
                    Items.HEAVY, 3, Items.MEGATON, 3, Items.CLUSTER, 3, Items.TRIDENT, 3, Items.TESLA, 3,
                    Items.AIRSTRIKE, 2, Items.RAIL, 2, Items.QUAKE, 2, Items.SHIELD_H, 2, Items.SHIELD_D, 2,
                    Items.REPAIR, 3, Items.FUEL, 2, Items.PARACHUTE, 2),
            new Profile("Overlord", 10, "Deadly accurate. Always goes after whoever is winning.",
                    0.3f, 4f, 1.0f, 1.0f, T_LEADER, W_BEST, M_SMART, ARC_ANY, 1.0f, 0.9f, 55,
                    Items.MEGATON, 3, Items.SUNBURST, 2, Items.SINGULARITY, 2, Items.AIRSTRIKE, 2, Items.RAIL, 3,
                    Items.SEEKER, 2, Items.TESLA, 2, Items.SHIELD_H, 3, Items.SHIELD_D, 2, Items.REPAIR, 3,
                    Items.PARACHUTE, 2, Items.FUEL, 2),
    };

    // =====================================================================
    // Shopping
    // =====================================================================

    public static void shop(Player p, Random r) {
        Profile pr = PROFILES[p.ai];
        int reserve = (int) (p.money * (1f - pr.spend));
        if (pr.move != M_NONE) {
            int cans = 0;
            while (p.fuel < 200 && cans < 3 && p.money - Items.ALL[Items.FUEL].price >= reserve && p.buy(Items.FUEL)) cans++;
        }
        if (pr.shop[Items.PARACHUTE] > 0 && p.inv[Items.PARACHUTE] == 0 && p.money - 900 >= reserve) p.buy(Items.PARACHUTE);
        for (int guard = 0; guard < 80; guard++) {
            int total = 0;
            for (int i = 0; i < Items.COUNT; i++) {
                if (pr.shop[i] > 0 && p.canBuy(i) && p.money - Items.ALL[i].price >= reserve && notHoarding(p, i)) {
                    total += pr.shop[i];
                }
            }
            if (total == 0) break;
            int pick = r.nextInt(total);
            for (int i = 0; i < Items.COUNT; i++) {
                if (pr.shop[i] > 0 && p.canBuy(i) && p.money - Items.ALL[i].price >= reserve && notHoarding(p, i)) {
                    pick -= pr.shop[i];
                    if (pick < 0) {
                        p.buy(i);
                        break;
                    }
                }
            }
        }
    }

    private static boolean notHoarding(Player p, int i) {
        if (i == Items.FUEL) return p.fuel < 400;
        if (i == Items.REPAIR || Items.isShield(i) || i == Items.PARACHUTE) return p.inv[i] < 4;
        return p.inv[i] < 12;
    }

    // =====================================================================
    // Turn logic
    // =====================================================================

    static final class Brain {
        static final int THINK = 0, MOVE = 1, SETTLE = 2, AIM = 3, DONE = 4;
        final Battle b;
        final Tank me;
        final Profile pr;
        final Random r;
        int stage;
        float t, aimT, aimDur;
        float moveLeft;
        int moveDir;
        float goalAngle, goalPower, startAngle, startPower;
        int goalWeapon;
        final float[] res = new float[6];
        boolean planning;
        volatile boolean planned;

        Brain(Battle b, Tank me) {
            this.b = b;
            this.me = me;
            this.pr = PROFILES[me.p.ai];
            this.r = b.rng;
            learnFromLastShot();
        }

        private void learnFromLastShot() {
            Player p = me.p;
            if (p.lastTargetIdx >= 0 && !Float.isNaN(me.lastImpactX)) {
                float err = me.lastImpactX - p.lastAimX;
                if (Math.abs(err) < 900) {
                    p.aimOffset[p.lastTargetIdx] += pr.learn * err;
                    p.aimOffset[p.lastTargetIdx] = Math.max(-500, Math.min(500, p.aimOffset[p.lastTargetIdx]));
                }
            }
            p.lastTargetIdx = -1;
        }

        void update(float dt) {
            t += dt;
            switch (stage) {
                case THINK:
                    if (t > 0.35f) {
                        defense();
                        planMove();
                        stage = MOVE;
                    }
                    break;
                case MOVE: {
                    if (moveLeft <= 0 || me.falling) {
                        stage = SETTLE;
                        break;
                    }
                    float nx = me.x + moveDir * 4;
                    if (b.support(nx) - me.y > 28) { // cliff ahead
                        moveLeft = 0;
                        break;
                    }
                    float before = me.x;
                    boolean ok = b.drive(moveDir, dt);
                    moveLeft -= Math.abs(me.x - before);
                    if (!ok) moveLeft = 0;
                    break;
                }
                case SETTLE:
                    if (!me.falling && !planning) {
                        // The aim search simulates hundreds of shots; run it off the frame thread.
                        // Nothing it reads (terrain, tank positions) changes while it runs.
                        planning = true;
                        Thread th = new Thread(new Runnable() {
                            public void run() {
                                try {
                                    plan();
                                } catch (Throwable e) {
                                    e.printStackTrace();
                                    goalAngle = me.angle;
                                    goalPower = me.power;
                                    goalWeapon = Items.PEA;
                                }
                                planned = true;
                            }
                        }, "ai-plan");
                        th.setDaemon(true);
                        th.start();
                    }
                    if (planned) {
                        stage = AIM;
                        aimT = 0;
                        startAngle = me.angle;
                        startPower = me.power;
                        aimDur = 0.5f + Math.min(0.7f, Math.abs(goalAngle - startAngle) / 120f);
                    }
                    break;
                case AIM: {
                    aimT += dt;
                    float k = Math.min(1f, aimT / aimDur);
                    float e = k * k * (3 - 2 * k);
                    me.angle = startAngle + (goalAngle - startAngle) * e;
                    me.power = startPower + (goalPower - startPower) * e;
                    me.weapon = goalWeapon;
                    if (aimT > aimDur + 0.25f) {
                        me.angle = goalAngle;
                        me.power = goalPower;
                        stage = DONE;
                        b.fire();
                    }
                    break;
                }
                default:
                    break;
            }
        }

        void defense() {
            Player p = me.p;
            if (me.health < pr.repairAt && p.inv[Items.REPAIR] > 0) {
                b.useRepair();
                if (me.health < pr.repairAt - 20 && p.inv[Items.REPAIR] > 0) b.useRepair();
            }
            if (me.shieldHp <= 0 && r.nextFloat() < pr.shieldUse) {
                int[] pref = {Items.SHIELD_H, Items.SHIELD_D, Items.SHIELD_L};
                for (int id : pref) {
                    if (p.inv[id] > 0) {
                        b.activateShield(id);
                        break;
                    }
                }
            }
        }

        void planMove() {
            moveLeft = 0;
            if (me.p.fuel < 5 || b.buried(me)) return;
            Tank near = nearestEnemy();
            switch (pr.move) {
                case M_RANDOM:
                    if (r.nextFloat() < 0.45f) {
                        moveDir = r.nextBoolean() ? 1 : -1;
                        moveLeft = 25 + r.nextFloat() * 90;
                    }
                    break;
                case M_TOWARD:
                    if (near != null && Math.abs(near.x - me.x) > 220 && r.nextFloat() < 0.8f) {
                        moveDir = near.x > me.x ? 1 : -1;
                        moveLeft = Math.min(140, Math.abs(near.x - me.x) - 200);
                    }
                    break;
                case M_AWAY:
                    if (near != null && Math.abs(near.x - me.x) < 320) {
                        moveDir = near.x > me.x ? -1 : 1;
                        moveLeft = 60 + r.nextFloat() * 80;
                    }
                    break;
                case M_SMART:
                    if (near != null && Math.abs(near.x - me.x) < 200) {
                        moveDir = near.x > me.x ? -1 : 1;
                        moveLeft = 80 + r.nextFloat() * 60;
                    } else if (me.health < 60 && r.nextFloat() < 0.35f) {
                        moveDir = r.nextBoolean() ? 1 : -1; // dodge so enemies' corrections go stale
                        moveLeft = 30 + r.nextFloat() * 50;
                    }
                    break;
                default:
                    break;
            }
            if (moveDir > 0 && me.x > b.w - 60) moveDir = -1;
            if (moveDir < 0 && me.x < 60) moveDir = 1;
            moveLeft = Math.min(moveLeft, me.p.fuel);
        }

        Tank nearestEnemy() {
            Tank best = null;
            float bd = Float.MAX_VALUE;
            for (Tank t : b.tanks) {
                if (!t.alive || t == me) continue;
                float d = Math.abs(t.x - me.x);
                if (d < bd) {
                    bd = d;
                    best = t;
                }
            }
            return best;
        }

        Tank chooseTarget() {
            List<Tank> enemies = new ArrayList<Tank>();
            for (Tank t : b.tanks) if (t.alive && t != me) enemies.add(t);
            if (enemies.isEmpty()) return null;
            Tank best = null;
            float bs = -Float.MAX_VALUE;
            for (Tank t : enemies) {
                float s;
                float dist = Math.abs(t.x - me.x);
                switch (pr.target) {
                    case T_NEAREST: s = -dist; break;
                    case T_WEAKEST: s = -(t.health + t.shieldHp) - dist * 0.02f; break;
                    case T_REVENGE: s = me.p.damageFrom[t.p.index] * 2 - dist * 0.05f; break;
                    case T_LEADER: s = t.p.score + t.p.wins * 150 + (t.p.isHuman() ? 5 : 0) - dist * 0.01f; break;
                    case T_BEST: {
                        s = (100 - t.health) * 0.6f - t.shieldHp * 0.4f - dist * 0.03f;
                        for (Tank o : enemies) if (o != t && Math.abs(o.x - t.x) < 90) s += 30;
                        if (t.health < 30) s += 25;
                        break;
                    }
                    default: s = r.nextFloat(); break;
                }
                s += r.nextFloat() * 0.5f;
                if (s > bs) {
                    bs = s;
                    best = t;
                }
            }
            return best;
        }

        boolean safe(Item it, Tank target) {
            if (it.cat != Items.CAT_WEAPON || it.behavior == Items.B_DIRT || it.behavior == Items.B_TELEPORT) return true;
            float r0 = it.radius;
            if (it.behavior == Items.B_BLACKHOLE) r0 = 330;
            if (it.behavior == Items.B_SHOCKWAVE) r0 = 300;
            if (it.behavior == Items.B_LAVA) r0 = 140;
            if (it.behavior == Items.B_AIRSTRIKE || it.behavior == Items.B_CLUSTER) r0 = it.radius + 140;
            return Math.abs(target.x - me.x) > r0 + 45;
        }

        int chooseWeapon(Tank target) {
            Player p = me.p;
            List<Integer> avail = new ArrayList<Integer>();
            for (int i = 0; i < Items.FIRST_DEFENSE; i++) {
                if (i == Items.TELEPORT || i == Items.DIRT) continue;
                if (p.count(i) > 0 && safe(Items.ALL[i], target)) avail.add(i);
            }
            if (avail.isEmpty()) return Items.PEA;
            float hp = target.health + target.shieldHp;
            switch (pr.weapons) {
                case W_CHEAP: {
                    if (r.nextFloat() < 0.75f) return Items.PEA;
                    int best = Items.PEA;
                    for (int i : avail) if (i != Items.PEA && (best == Items.PEA || Items.ALL[i].price < Items.ALL[best].price)) best = i;
                    return best;
                }
                case W_RANDOM: {
                    if (p.count(Items.DIRT) > 0 && r.nextFloat() < 0.12f) return Items.DIRT;
                    return avail.get(r.nextInt(avail.size()));
                }
                case W_BIGGEST: {
                    int best = Items.PEA;
                    float bv = 0;
                    for (int i : avail) {
                        Item it = Items.ALL[i];
                        float v = it.damage * it.radius;
                        if (v > bv) {
                            bv = v;
                            best = i;
                        }
                    }
                    return best;
                }
                case W_PRECISE: {
                    int[] pref = {Items.RAIL, Items.SEEKER, Items.TRIDENT, Items.HEAVY};
                    if (hp < 25) return Items.PEA;
                    for (int i : pref) if (avail.contains(i)) return i;
                    return Items.PEA;
                }
                case W_BALANCED: {
                    if (hp < 30 || r.nextFloat() < 0.3f) return Items.PEA;
                    return avail.get(r.nextInt(avail.size()));
                }
                default: { // W_BEST: expected damage per dollar spent, but never overkill
                    float err = pr.noiseDeg * Math.abs(target.x - me.x) * 0.017f + (1 - pr.windKnow) * Math.abs(b.wind) * 3 + 6;
                    int best = Items.PEA;
                    float bv = -Float.MAX_VALUE;
                    for (int i : avail) {
                        Item it = Items.ALL[i];
                        float dmg = expectedDamage(it, target, err);
                        float kill = dmg >= hp ? 40 : 0;
                        float cost = it.infinite ? 0 : (it.price / (float) it.bundle) / 400f;
                        float v = Math.min(dmg, hp + 10) + kill - cost;
                        if (v > bv) {
                            bv = v;
                            best = i;
                        }
                    }
                    if (p.count(Items.DIRT) > 0 && target.health > 70 && r.nextFloat() < 0.08f) return Items.DIRT;
                    return best;
                }
            }
        }

        float expectedDamage(Item it, Tank target, float err) {
            float hitChance = Math.min(1f, (it.radius + 10) / (err + 10));
            float base = it.damage * (0.5f + 0.5f * hitChance) * hitChance;
            switch (it.behavior) {
                case Items.B_CLUSTER: base = it.damage * 2.4f * Math.min(1f, (it.radius + 60) / (err + 10)); break;
                case Items.B_SPLITTER: base = it.damage * 1.3f * Math.min(1f, (it.radius + 40) / (err + 10)); break;
                case Items.B_AIRSTRIKE: base = it.damage * 2.6f * Math.min(1f, 120 / (err + 10)); break;
                case Items.B_HOMING: base = it.damage * 0.9f; break;
                case Items.B_TESLA: base = 24 + 32 * Math.min(1f, 260 / (err + 10)); break;
                case Items.B_SHOCKWAVE: base = 38; break;
                case Items.B_LAVA: base = 30 * Math.min(1f, 80 / (err + 10)); break;
                case Items.B_BLACKHOLE: base = it.damage * 0.85f; break;
                case Items.B_RAIL: base = it.damage * (hitChance > 0.6f ? 1.5f : 0.6f) * hitChance; break;
                default: break;
            }
            // splash bonus for other enemies near the target
            for (Tank o : b.tanks) {
                if (!o.alive || o == target || o == me) continue;
                if (Math.abs(o.x - target.x) < it.radius) base += it.damage * 0.4f;
            }
            return base;
        }

        void plan() {
            Tank target = chooseTarget();
            if (target == null) {
                goalAngle = me.angle;
                goalPower = me.power;
                goalWeapon = Items.PEA;
                return;
            }
            goalWeapon = chooseWeapon(target);

            // Turtle-style escape with a teleporter when an enemy is close.
            if (me.p.count(Items.TELEPORT) > 0 && (pr.move == M_AWAY || pr.weapons == W_RANDOM)) {
                Tank near = nearestEnemy();
                boolean wantEscape = near != null && Math.abs(near.x - me.x) < 180 && (pr.move == M_AWAY || r.nextFloat() < 0.4f);
                if (wantEscape) {
                    float bestX = me.x, bestD = -1;
                    for (int k = 0; k < 24; k++) {
                        float x = 60 + r.nextFloat() * (b.w - 120);
                        float md = Float.MAX_VALUE;
                        for (Tank o : b.tanks) if (o.alive && o != me) md = Math.min(md, Math.abs(o.x - x));
                        if (md > bestD) {
                            bestD = md;
                            bestX = x;
                        }
                    }
                    if (aimAt(bestX, b.terrain.at(bestX), Items.ALL[Items.TELEPORT], null)) {
                        goalWeapon = Items.TELEPORT;
                        applyNoise(0.3f);
                        return;
                    }
                }
            }

            Item it = Items.ALL[goalWeapon];
            int ti = b.tanks.indexOf(target);
            float offset = me.p.aimOffset[target.p.index];
            float tx = target.x - offset;
            if (!aimAt(tx, target.y - 6, it, target)) {
                // Nothing reaches: lob as far as possible toward the target
                goalAngle = target.x > me.x ? 50 : 130;
                goalPower = me.maxPower();
            }
            me.p.lastTargetIdx = target.p.index;
            me.p.lastAimX = tx + offset; // where the target really is; error is measured against it
            if (ti < 0) me.p.lastTargetIdx = -1;
            applyNoise(1f);
        }

        void applyNoise(float scale) {
            goalAngle += (float) r.nextGaussian() * pr.noiseDeg * scale;
            goalPower += (float) r.nextGaussian() * pr.noisePow * scale;
            goalAngle = Math.max(0, Math.min(180, goalAngle));
            goalPower = Math.max(50, Math.min(me.maxPower(), goalPower));
        }

        /** Searches angle/power so the shot lands at (tx, ty), using this AI's knowledge of the wind. */
        boolean aimAt(float tx, float ty, Item it, Tank target) {
            float windA = b.windAccel() * pr.windKnow;
            float x0 = me.pivotX(), y0 = me.pivotY();
            float maxP = me.maxPower();
            boolean right = tx >= me.x;
            float bestCost = Float.MAX_VALUE, bestA = 0, bestP = 0;
            int targetIdx = target == null ? -1 : b.tanks.indexOf(target);
            boolean bothSides = b.st.walls != 0;
            for (int side = 0; side < (bothSides ? 2 : 1); side++) {
                boolean goRight = side == 0 ? right : !right;
                for (float a = 6; a <= 86; a += 4) {
                    float ang = goRight ? a : 180 - a;
                    float prevErr = Float.NaN, prevP = 0;
                    for (float pw = 80; pw <= maxP + 1; pw += 40) {
                        float err = shotError(x0, y0, ang, pw, it, windA, tx, targetIdx);
                        float c = cost(err, ang, tx);
                        if (c < bestCost) {
                            bestCost = c;
                            bestA = ang;
                            bestP = pw;
                        }
                        if (!Float.isNaN(prevErr) && Math.signum(err) != Math.signum(prevErr) && Math.abs(err - prevErr) < 900) {
                            // refine between prevP and pw
                            float lo = prevP, hi = pw, eLo = prevErr;
                            for (int k = 0; k < 8; k++) {
                                float mid = (lo + hi) / 2;
                                float em = shotError(x0, y0, ang, mid, it, windA, tx, targetIdx);
                                float cm = cost(em, ang, tx);
                                if (cm < bestCost) {
                                    bestCost = cm;
                                    bestA = ang;
                                    bestP = mid;
                                }
                                if (Math.signum(em) == Math.signum(eLo)) {
                                    lo = mid;
                                    eLo = em;
                                } else hi = mid;
                            }
                        }
                        prevErr = err;
                        prevP = pw;
                    }
                }
            }
            if (bestCost > 2000) return false;
            goalAngle = bestA;
            goalPower = bestP;
            return true;
        }

        float lastHitSelf;

        float shotError(float x0, float y0, float ang, float pw, Item it, float windA, float tx, int targetIdx) {
            float rad = (float) Math.toRadians(ang);
            float sx = x0 + (float) Math.cos(rad) * Tank.BARREL, sy = y0 - (float) Math.sin(rad) * Tank.BARREL;
            lastHitSelf = 0;
            boolean landed = b.simulate(sx, sy, ang, pw, it, windA, me, res, null, null, 0, 14f);
            if (!landed) return res[0] < tx ? -5000 : 5000;
            int hit = (int) res[2];
            if (targetIdx >= 0 && hit == targetIdx) return 0.001f * (res[0] >= tx ? 1 : -1);
            float err = res[0] - tx;
            if (b.st.walls == 2) {
                if (err > b.w / 2f) err -= b.w;
                if (err < -b.w / 2f) err += b.w;
            }
            lastHitSelf = (hit >= 0 && b.tanks.get(hit) == me) || Battle.dist(res[0], res[1], me.x, me.y) < 40 ? 1 : 0;
            return err;
        }

        float cost(float err, float ang, float tx) {
            float c = Math.abs(err);
            if (lastHitSelf > 0) c += 3000;
            float elev = ang > 90 ? 180 - ang : ang;
            if (pr.arc == ARC_LOW) c += elev * 0.15f;
            else if (pr.arc == ARC_HIGH) c += (90 - elev) * 0.15f;
            else c += Math.abs(elev - 45) * 0.05f;
            return c;
        }
    }
}
