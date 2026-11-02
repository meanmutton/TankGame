package com.meanmutton.tankgame.core;

/** Between-round shop for one human player. */
public final class ShopScreen extends Screen {
    private static final int TAB_W = 1, TAB_D = 2, BUY = 3, SELL = 4, DONE = 5, ITEM = 100;

    private final Player p;
    private int tab;
    private int selected = Items.HEAVY;
    private float t, flash;
    private Btn buy, sell;
    private float detX, detY, detW, detH;

    public ShopScreen(Game game, Player p) {
        super(game);
        this.p = p;
    }

    protected void layout() {
        buttons.clear();
        float top = h * 0.15f;
        Btn tw = add(new Btn(TAB_W, "Weapons").set(u * 3, top, u * 30, u * 8));
        Btn td = add(new Btn(TAB_D, "Defense & Fuel").set(u * 35, top, u * 34, u * 8));
        tw.selected = tab == 0;
        td.selected = tab == 1;
        float gx = u * 3, gy = top + u * 10, gw = w * 0.6f, gh = h - gy - u * 3;
        int from = tab == 0 ? 0 : Items.FIRST_DEFENSE;
        int to = tab == 0 ? Items.FIRST_DEFENSE : Items.COUNT;
        int cols = 4;
        int rows = (int) Math.ceil((to - from) / (float) cols);
        rows = Math.max(rows, 4);
        float cw = gw / cols, ch = Math.min(gh / rows, u * 17);
        for (int i = from; i < to; i++) {
            int k = i - from;
            Btn b = add(new Btn(ITEM + i, "").set(gx + (k % cols) * cw + u * 0.5f, gy + (k / cols) * ch + u * 0.5f, cw - u, ch - u));
            b.selected = selected == i;
            b.color = 0xFF1E293C;
        }
        detX = gx + gw + u * 3;
        detY = top;
        detW = w - detX - u * 3;
        detH = h - detY - u * 16;
        float bw = (detW - u * 2) / 2;
        buy = add(new Btn(BUY, "Buy").set(detX, detY + detH - u * 11, bw, u * 10));
        buy.color = 0xFF2E7D4F;
        sell = add(new Btn(SELL, "Sell").set(detX + bw + u * 2, detY + detH - u * 11, bw, u * 10));
        sell.color = 0xFF7A4A2A;
        Btn done = add(new Btn(DONE, "Done - To Battle!").set(detX, h - u * 13, detW, u * 10));
        done.color = 0xFF2E5D8F;
        refresh();
    }

    private void refresh() {
        Item it = Items.ALL[selected];
        buy.enabled = p.canBuy(selected);
        sell.enabled = p.canSell(selected);
        buy.label = it.infinite ? "Free" : "Buy $" + Ui.money(it.price);
        buy.sub = it.infinite ? null : (it.id == Items.FUEL ? "+80 fuel" : "+" + it.bundle);
        int back = it.id == Items.FUEL ? it.price / 2 : it.sellPricePerUnit();
        sell.label = "Sell";
        sell.sub = it.infinite ? null : "+$" + Ui.money(back);
    }

    public void update(float dt) {
        super.update(dt);
        t += dt;
        if (flash > 0) flash -= dt;
    }

    protected void onClick(Btn b) {
        if (b.id == TAB_W || b.id == TAB_D) {
            tab = b.id == TAB_W ? 0 : 1;
            selected = tab == 0 ? Items.HEAVY : Items.FUEL;
            w = -1;
            game.setScreen(this);
        } else if (b.id >= ITEM) {
            selected = b.id - ITEM;
            for (Btn o : buttons) if (o.id >= ITEM) o.selected = o.id == b.id;
            refresh();
        } else if (b.id == BUY) {
            if (p.buy(selected)) {
                game.sfx(Platform.SND_BUY, 0.8f);
                flash = 0.3f;
            }
            refresh();
        } else if (b.id == SELL) {
            if (p.sell(selected)) game.sfx(Platform.SND_BUY, 0.5f);
            refresh();
        } else if (b.id == DONE) {
            game.shopDone();
        }
    }

    public boolean back() {
        return true; // shopping must be finished with the Done button
    }

    public void render(Gfx g) {
        background(g);
        // header
        TankArt.drawIcon(g, p.style, p.color, u * 10, h * 0.07f, u * 11, 60, t);
        g.text(p.name + "'s Shop", u * 18, h * 0.055f, u * 5.2f, Col.TEXT, Gfx.ALIGN_LEFT, true);
        g.text("Round " + game.round + " of " + game.settings.rounds + "   |   Fuel " + Math.round(p.fuel),
                u * 18, h * 0.105f, u * 3f, Col.TEXT_DIM, Gfx.ALIGN_LEFT, false);
        int mc = flash > 0 ? Col.lerp(Col.MONEY, 0xFFFFFFFF, flash / 0.3f) : Col.MONEY;
        g.text("$" + Ui.money(p.money), w - u * 3, h * 0.075f, u * 7, mc, Gfx.ALIGN_RIGHT, true);

        renderButtons(g);
        for (Btn b : buttons) {
            if (b.id < ITEM) continue;
            int id = b.id - ITEM;
            Item it = Items.ALL[id];
            float is = Math.min(b.h * 0.55f, b.w * 0.3f);
            Ui.itemIcon(g, id, b.x + is * 0.5f + u * 1.5f, b.y + b.h * 0.42f, is);
            float tx = b.x + is + u * 3;
            float maxW = b.x + b.w - tx - u;
            g.text(Ui.fit(g, it.name, u * 2.9f, maxW), tx, b.y + b.h * 0.28f, u * 2.9f, Col.TEXT, Gfx.ALIGN_LEFT, true);
            String price = it.infinite ? "Free" : "$" + Ui.money(it.price) + (it.bundle > 1 ? " / " + it.bundle : "");
            g.text(price, tx, b.y + b.h * 0.55f, u * 2.5f, p.money >= it.price || it.infinite ? Col.MONEY : Col.BAD, Gfx.ALIGN_LEFT, false);
            String own = it.infinite ? "Owned: unlimited" : (id == Items.FUEL ? "Fuel: " + Math.round(p.fuel) : "Owned: " + p.inv[id]);
            g.text(own, tx, b.y + b.h * 0.8f, u * 2.3f, Col.TEXT_DIM, Gfx.ALIGN_LEFT, false);
        }

        // detail panel
        Ui.panel(g, detX - u * 1.5f, detY - u * 1.5f, detW + u * 3, detH + u * 3, u * 2);
        Item it = Items.ALL[selected];
        float is = u * 16;
        Ui.itemIcon(g, selected, detX + is / 2 + u, detY + is / 2 + u, is);
        float tx = detX + is + u * 4;
        g.text(Ui.fit(g, it.name, u * 4.4f, detX + detW - tx), tx, detY + u * 5, u * 4.4f, Col.ACCENT, Gfx.ALIGN_LEFT, true);
        g.text(Items.stats(it), tx, detY + u * 11, u * 3f, Col.TEXT, Gfx.ALIGN_LEFT, false);
        String own = it.infinite ? "Unlimited" : (selected == Items.FUEL ? "Fuel: " + Math.round(p.fuel) : "You own " + p.inv[selected]);
        g.text(own, tx, detY + u * 15.5f, u * 3f, Col.TEXT_DIM, Gfx.ALIGN_LEFT, false);
        Ui.wrap(g, it.desc, detX + u, detY + is + u * 6, u * 3.1f, detW - u * 2, Col.TEXT);
        if (it.cat == Items.CAT_WEAPON && it.damage > 0) {
            float by = detY + detH - u * 18;
            g.text("Damage", detX + u, by, u * 2.6f, Col.TEXT_DIM, Gfx.ALIGN_LEFT, false);
            Ui.bar(g, detX + u * 14, by - u * 1.2f, detW - u * 15, u * 2.4f, it.damage / 120f, Col.BAD);
            g.text("Blast", detX + u, by + u * 4.2f, u * 2.6f, Col.TEXT_DIM, Gfx.ALIGN_LEFT, false);
            Ui.bar(g, detX + u * 14, by + u * 3f, detW - u * 15, u * 2.4f, it.radius / 135f, Col.ACCENT);
        }
    }
}
