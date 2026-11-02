package com.meanmutton.tankgame.core;

/** The shop catalog: every weapon and defensive item in the game. */
public final class Items {
    private Items() {}

    public static final int CAT_WEAPON = 0, CAT_DEFENSE = 1;

    // Weapon behaviors
    public static final int B_STANDARD = 0, B_CLUSTER = 1, B_SPLITTER = 2, B_BOUNCER = 3, B_ROLLER = 4,
            B_LEAPFROG = 5, B_RAIL = 6, B_HOMING = 7, B_TESLA = 8, B_SHOCKWAVE = 9, B_LAVA = 10,
            B_AIRSTRIKE = 11, B_BLACKHOLE = 12, B_DIGGER = 13, B_DIRT = 14, B_TELEPORT = 15, B_NONE = 99;

    // Item ids (index into ALL and Player.inv). Never reorder: saved setups do not store these,
    // but AI preference tables do.
    public static final int PEA = 0, HEAVY = 1, MEGATON = 2, SUNBURST = 3, CLUSTER = 4, TRIDENT = 5,
            BOUNCER = 6, ROLLER = 7, LEAPFROG = 8, RAIL = 9, SEEKER = 10, TESLA = 11, QUAKE = 12,
            LAVA = 13, AIRSTRIKE = 14, SINGULARITY = 15, MOLE = 16, DIRT = 17, TELEPORT = 18,
            FUEL = 19, REPAIR = 20, PARACHUTE = 21, SHIELD_L = 22, SHIELD_H = 23, SHIELD_D = 24;

    public static final int COUNT = 25;
    public static final int FIRST_DEFENSE = FUEL;

    public static final Item[] ALL = new Item[COUNT];

    static {
        w(PEA, "Pea Shooter", 0, 1, B_STANDARD, 24, 30, 0xFFD8D8D8,
                "Basic shell. Free and unlimited. Small blast.");
        ALL[PEA].infinite = true;
        w(HEAVY, "Heavy Shell", 1200, 5, B_STANDARD, 34, 48, 0xFFB0B8C8,
                "A meaner shell with a bigger, harder blast.");
        w(MEGATON, "Megaton", 4500, 2, B_STANDARD, 70, 80, 0xFFFF8A2A,
                "Huge explosion that digs a deep crater.");
        w(SUNBURST, "Sunburst", 11000, 1, B_STANDARD, 135, 115, 0xFFFFE04A,
                "Miniature sun. Flattens everything nearby. Don't stand close.");
        w(CLUSTER, "Cluster Bomb", 3200, 3, B_CLUSTER, 24, 26, 0xFFE05AE0,
                "Bursts on impact, scattering six bomblets around the target.");
        w(TRIDENT, "Trident", 3800, 3, B_SPLITTER, 32, 44, 0xFF5AE0D2,
                "Splits into three heavy shells at the top of its arc.");
        w(BOUNCER, "Bouncer", 2400, 4, B_BOUNCER, 28, 32, 0xFF7CFF5A,
                "Explodes and bounces off the ground three times.");
        w(ROLLER, "Roller", 2400, 4, B_ROLLER, 40, 54, 0xFF9A7A5A,
                "Lands and rolls downhill until it hits a tank or stops.");
        w(LEAPFROG, "Leapfrog", 3000, 3, B_LEAPFROG, 30, 38, 0xFF5AFF9A,
                "Hops forward three times, exploding on every landing.");
        w(RAIL, "Railgun Slug", 3000, 4, B_RAIL, 14, 40, 0xFF6AB4FF,
                "Very fast, flat, ignores wind. Direct hits do double damage.");
        w(SEEKER, "Seeker Missile", 5500, 2, B_HOMING, 36, 55, 0xFFFF5A5A,
                "After the peak of its arc it locks onto the nearest enemy.");
        w(TESLA, "Tesla Orb", 4800, 3, B_TESLA, 22, 24, 0xFFA6E8FF,
                "Lightning arcs from the impact to up to four nearby enemy tanks.");
        w(QUAKE, "Quake Charge", 4200, 2, B_SHOCKWAVE, 22, 45, 0xFFC89A5A,
                "Sends a shockwave along the ground that rattles every tank nearby.");
        w(LAVA, "Lava Bomb", 3800, 3, B_LAVA, 20, 15, 0xFFFF6A1A,
                "Splashes molten lava that flows downhill and burns what it touches.");
        w(AIRSTRIKE, "Air Strike", 6500, 1, B_AIRSTRIKE, 34, 40, 0xFFFFD27A,
                "Marks the spot. Seven bombs rain down on it moments later.");
        w(SINGULARITY, "Singularity", 8500, 1, B_BLACKHOLE, 85, 95, 0xFF9A5AFF,
                "Opens a black hole that drags tanks in, then collapses violently.");
        w(MOLE, "Mole Bomb", 1800, 4, B_DIGGER, 45, 45, 0xFFB08A60,
                "Burrows into the ground before exploding. Great for undermining.");
        w(DIRT, "Dirt Clod", 1200, 4, B_DIRT, 65, 0, 0xFF8A6A40,
                "No damage. Dumps a ball of dirt that can bury a tank.");
        w(TELEPORT, "Teleporter", 2500, 2, B_TELEPORT, 0, 0, 0xFFFF9AF0,
                "Fire it and your tank teleports to wherever it lands.");

        d(FUEL, "Fuel Can", 600, 1, 0xFFFFC23A, "Adds 80 fuel. Fuel lets your tank drive during its turn.");
        d(REPAIR, "Repair Kit", 2000, 1, 0xFF5BD46A, "Restores 35 health. Use it during your turn.");
        d(PARACHUTE, "Parachute", 900, 2, 0xFFF0F0F0, "Deploys automatically on a big fall. No fall damage.");
        d(SHIELD_L, "Light Shield", 2000, 1, 0xFF7FD8FF, "Absorbs 40 damage. Activate during your turn.");
        d(SHIELD_H, "Heavy Shield", 5000, 1, 0xFF3A9CFF, "Absorbs 110 damage. Activate during your turn.");
        d(SHIELD_D, "Deflector", 5500, 1, 0xFFC08AFF,
                "Absorbs 60 damage and bounces incoming shells off its bubble.");
        ALL[SHIELD_L].shieldHp = 40;
        ALL[SHIELD_H].shieldHp = 110;
        ALL[SHIELD_D].shieldHp = 60;
        ALL[FUEL].maxStock = 30;
    }

    private static void w(int id, String name, int price, int bundle, int behavior, float radius, float damage,
                          int color, String desc) {
        Item it = new Item();
        it.id = id;
        it.name = name;
        it.price = price;
        it.bundle = bundle;
        it.behavior = behavior;
        it.radius = radius;
        it.damage = damage;
        it.color = color;
        it.desc = desc;
        it.cat = CAT_WEAPON;
        ALL[id] = it;
    }

    private static void d(int id, String name, int price, int bundle, int color, String desc) {
        Item it = new Item();
        it.id = id;
        it.name = name;
        it.price = price;
        it.bundle = bundle;
        it.behavior = B_NONE;
        it.color = color;
        it.desc = desc;
        it.cat = CAT_DEFENSE;
        ALL[id] = it;
    }

    public static boolean isShield(int id) {
        return id == SHIELD_L || id == SHIELD_H || id == SHIELD_D;
    }

    /** Short stat line for the shop. */
    public static String stats(Item it) {
        if (it.cat == CAT_DEFENSE) {
            if (it.shieldHp > 0) return "Shield " + it.shieldHp + " HP";
            if (it.id == FUEL) return "+80 fuel";
            if (it.id == REPAIR) return "+35 health";
            return "Auto-deploys";
        }
        switch (it.behavior) {
            case B_DIRT: return "Adds terrain";
            case B_TELEPORT: return "Moves your tank";
            case B_CLUSTER: return "6 x " + (int) it.damage + " dmg";
            case B_SPLITTER: return "3 x " + (int) it.damage + " dmg";
            case B_AIRSTRIKE: return "7 x " + (int) it.damage + " dmg";
            case B_BOUNCER: return "4 x " + (int) it.damage + " dmg";
            case B_LEAPFROG: return "3 x " + (int) it.damage + " dmg";
            case B_TESLA: return "Chain 32 dmg";
            case B_LAVA: return "Burns over time";
            default: return (int) it.damage + " dmg, radius " + (int) it.radius;
        }
    }
}
