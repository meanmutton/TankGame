package com.meanmutton.tankgame.core;

/** A participant in the match. Persists across rounds (money, inventory, stats). */
public final class Player {
    public final int index;
    public String name;
    public int colorIndex;
    public int color;
    public int style;
    public int ai; // -1 human

    public int money;
    public float fuel;
    public final int[] inv = new int[Items.COUNT];

    // Match stats
    public float score, damage;
    public int kills, wins, earned, deaths;
    // This round
    public int roundEarn, roundKills;
    public float roundDamage;
    public boolean roundWinner;

    /** Damage received from each other player this match (for grudge-holding AIs). */
    public float[] damageFrom = new float[10];

    /** AI aim memory: correction offset (world x) against each target. */
    public float[] aimOffset = new float[10];

    /** AI: which player it aimed at last shot and where that target stood (-1 = none). */
    public int lastTargetIdx = -1;
    public float lastAimX;

    public Tank tank;

    public Player(int index, Settings.PlayerSetup s, int startCash) {
        this.index = index;
        this.name = s.name;
        this.colorIndex = s.color;
        this.color = TankArt.COLORS[s.color];
        this.style = s.style;
        this.ai = s.ai;
        this.money = startCash;
    }

    public boolean isHuman() {
        return ai < 0;
    }

    public void earn(int amount) {
        money += amount;
        if (amount > 0) {
            earned += amount;
            roundEarn += amount;
        }
    }

    public int count(int item) {
        if (Items.ALL[item].infinite) return 999;
        return inv[item];
    }

    public boolean canBuy(int item) {
        Item it = Items.ALL[item];
        if (it.infinite || it.price > money) return false;
        if (item == Items.FUEL) return fuel + 80 <= it.maxStock * 80;
        return inv[item] + it.bundle <= it.maxStock;
    }

    public boolean buy(int item) {
        if (!canBuy(item)) return false;
        Item it = Items.ALL[item];
        money -= it.price;
        if (item == Items.FUEL) fuel += 80;
        else inv[item] += it.bundle;
        return true;
    }

    public boolean canSell(int item) {
        Item it = Items.ALL[item];
        if (it.infinite) return false;
        if (item == Items.FUEL) return fuel >= 80;
        return inv[item] > 0;
    }

    /** Sells one unit (or one can of fuel) at half price. */
    public boolean sell(int item) {
        if (!canSell(item)) return false;
        Item it = Items.ALL[item];
        if (item == Items.FUEL) {
            fuel -= 80;
            money += it.price / 2;
        } else {
            inv[item]--;
            money += it.sellPricePerUnit();
        }
        return true;
    }
}
