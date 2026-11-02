package com.meanmutton.tankgame.core;

/** One shop entry. Weapons have blast parameters; defense items have their own fields. */
public final class Item {
    public int id;
    public String name;
    public String desc;
    public int cat;
    public int price;      // price per bundle
    public int bundle;     // units per purchase
    public int behavior;
    public float radius;
    public float damage;
    public int color;
    public boolean infinite;
    public int shieldHp;
    public int maxStock = 99;

    public int sellPricePerUnit() {
        return price / bundle / 2;
    }
}
