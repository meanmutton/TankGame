package com.meanmutton.tankgame.core;

/** How-to-play pages, including the weapon list. */
public final class HelpScreen extends Screen {
    private static final int BACK = 1, PREV = 2, NEXT = 3;
    private int page;
    private static final int PAGES = 4;

    public HelpScreen(Game game) {
        super(game);
    }

    protected void layout() {
        buttons.clear();
        float bh = u * 10;
        add(new Btn(BACK, "Back").set(u * 3, h - bh - u * 3, u * 24, bh));
        Btn p = add(new Btn(PREV, "<").set(w - u * 30, h - bh - u * 3, u * 12, bh));
        Btn n = add(new Btn(NEXT, ">").set(w - u * 15, h - bh - u * 3, u * 12, bh));
        p.enabled = page > 0;
        n.enabled = page < PAGES - 1;
    }

    protected void onClick(Btn b) {
        if (b.id == BACK) game.setScreen(new MenuScreen(game));
        else {
            page = Math.max(0, Math.min(PAGES - 1, page + (b.id == NEXT ? 1 : -1)));
            w = -1;
            game.setScreen(this);
        }
    }

    public boolean back() {
        game.setScreen(new MenuScreen(game));
        return true;
    }

    public void render(Gfx g) {
        background(g);
        String[] titles = {"HOW TO PLAY", "MONEY & SCORING", "WEAPONS", "DEFENSE & OPPONENTS"};
        Ui.title(g, titles[page], w / 2f, h * 0.08f, u * 6.5f);
        g.text("Page " + (page + 1) + " of " + PAGES, w - u * 33, h - u * 8, u * 2.8f, Col.TEXT_DIM, Gfx.ALIGN_RIGHT, false);
        float x = u * 6, y = h * 0.18f, size = u * 3.2f, maxW = w - u * 12;
        switch (page) {
            case 0: {
                String[] lines = {
                        "Tanks take turns. On your turn you can drive, aim, pick a weapon, use items, then FIRE.",
                        "Aim: drag anywhere on the battlefield. The direction from your tank sets the angle, the distance sets the power. Use - and + to fine tune.",
                        "Drive: hold < or >. Driving burns fuel, and climbing burns more. No fuel, no driving. Steep cliffs can't be climbed.",
                        "Damaged tanks can't fire as hard: max power drops with health (the red mark on the power bar). If nobody lands a hit for a long stretch, SUDDEN DEATH restores full power to everyone.",
                        "Wind (top of the screen) pushes shells sideways. Watch the clouds drift.",
                        "Blasts destroy the ground. Tanks fall when the ground under them goes, and long falls hurt unless you have a parachute.",
                        "Your last shot's path is shown faintly on your next turn so you can adjust.",
                        "The last tank standing wins the round. After each round everyone shops, then the next round begins."
                };
                for (String s : lines) y += Ui.wrap(g, "- " + s, x, y, size, maxW, Col.TEXT) + u;
                break;
            }
            case 1: {
                String[] lines = {
                        "You earn $" + Battle.PAY_PER_HP + " for every point of damage you do to enemy tanks, and $" + Battle.PAY_PER_SHIELD_HP + " for every point their shields absorb.",
                        "Better hits do more damage: a blast hurts most at its center and falls off toward its edge.",
                        "A DIRECT HIT (the shell strikes the tank itself) does 50% extra damage (Railgun: double) and pays a $" + Battle.PAY_DIRECT + " bonus.",
                        "Destroying a tank pays $" + Battle.PAY_KILL + ". Winning the round pays $" + Battle.PAY_WIN + ".",
                        "A destroyed tank explodes and can damage its neighbors. Those hits are credited to whoever destroyed it.",
                        "Hurting yourself pays nothing. Shots that burst right at your own barrel (digging out when buried) never hurt you.",
                        "Score: 1 point per damage dealt, 50 per kill, 100 per round won. Highest score after the last round wins.",
                        "Unused weapons and items carry over between rounds. You can sell items back for half price."
                };
                for (String s : lines) y += Ui.wrap(g, "- " + s, x, y, size, maxW, Col.TEXT) + u;
                break;
            }
            case 2: {
                int cols = 2;
                float cw = (w - u * 12) / cols;
                float rowH = (h * 0.68f) / 10f;
                for (int i = 0; i < Items.FIRST_DEFENSE; i++) {
                    Item it = Items.ALL[i];
                    float cx = x + (i % cols) * cw, cy = y + (i / cols) * rowH;
                    Ui.itemIcon(g, i, cx + rowH * 0.4f, cy + rowH * 0.4f, rowH * 0.75f);
                    g.text(it.name + "  " + (it.infinite ? "(free)" : "$" + Ui.money(it.price) + "/" + it.bundle), cx + rowH, cy + rowH * 0.2f, u * 2.7f, Col.ACCENT, Gfx.ALIGN_LEFT, true);
                    g.text(Ui.fit(g, it.desc, u * 2.3f, cw - rowH - u), cx + rowH, cy + rowH * 0.58f, u * 2.3f, Col.TEXT, Gfx.ALIGN_LEFT, false);
                }
                break;
            }
            default: {
                for (int i = Items.FIRST_DEFENSE; i < Items.COUNT; i++) {
                    Item it = Items.ALL[i];
                    Ui.itemIcon(g, i, x + u * 3, y + u * 2, u * 5);
                    g.text(it.name + " ($" + Ui.money(it.price) + ")", x + u * 8, y + u * 0.5f, u * 2.8f, Col.ACCENT, Gfx.ALIGN_LEFT, true);
                    g.text(it.desc, x + u * 8, y + u * 3.8f, u * 2.4f, Col.TEXT, Gfx.ALIGN_LEFT, false);
                    y += u * 8.2f;
                }
                float ox = w * 0.52f, oy = h * 0.18f;
                g.text("Computer opponents (skill 1 to 10)", ox, oy, u * 3f, Col.ACCENT, Gfx.ALIGN_LEFT, true);
                oy += u * 5;
                for (Ai.Profile p : Ai.PROFILES) {
                    g.text(p.skill + ". " + p.name, ox, oy, u * 2.7f, Col.TEXT, Gfx.ALIGN_LEFT, true);
                    g.text(Ui.fit(g, p.blurb, u * 2.2f, w - ox - u * 26), ox + u * 24, oy, u * 2.2f, Col.TEXT_DIM, Gfx.ALIGN_LEFT, false);
                    oy += u * 5.6f;
                }
            }
        }
        renderButtons(g);
    }
}
