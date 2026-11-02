package com.meanmutton.tankgame.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Standings after each round, and the final results after the last one. */
public final class ScoreScreen extends Screen {
    private static final int CONTINUE = 1;
    private final boolean finalScreen;
    private final List<Player> sorted;
    private float t;

    public ScoreScreen(Game game, boolean finalScreen) {
        super(game);
        this.finalScreen = finalScreen;
        sorted = new ArrayList<Player>(game.players);
        Collections.sort(sorted, new Comparator<Player>() {
            public int compare(Player a, Player b) {
                if (b.score != a.score) return b.score > a.score ? 1 : -1;
                return b.money - a.money;
            }
        });
        if (finalScreen) game.sfx(Platform.SND_WIN, 1f);
    }

    protected void layout() {
        buttons.clear();
        float bw = u * 40, bh = u * 10;
        Btn b = add(new Btn(CONTINUE, finalScreen ? "Main Menu" : "Continue to Shop").set(w - bw - u * 3, h - bh - u * 3, bw, bh));
        b.color = 0xFF2E7D4F;
    }

    public void update(float dt) {
        super.update(dt);
        t += dt;
        if (!game.hasHumans() && !finalScreen && t > 4f) game.scoresDone(false);
    }

    protected void onClick(Btn b) {
        if (b.id == CONTINUE) game.scoresDone(finalScreen);
    }

    public boolean back() {
        game.scoresDone(finalScreen);
        return true;
    }

    public void render(Gfx g) {
        background(g);
        if (finalScreen) {
            Player win = sorted.get(0);
            Ui.title(g, win.name + " WINS THE WAR!", w / 2f, h * 0.08f, u * 7);
            for (int i = 0; i < 40; i++) {
                float x = (i * 97.3f + t * (30 + i % 7 * 10)) % w;
                float y = (i * 53.1f + t * (60 + i % 5 * 25)) % h;
                g.fillRect(x, y, u, u * 0.6f, TankArt.COLORS[i % TankArt.COLORS.length]);
            }
        } else {
            Ui.title(g, "ROUND " + game.round + " OF " + game.settings.rounds + " COMPLETE", w / 2f, h * 0.08f, u * 6);
        }
        float x0 = u * 4, y0 = h * 0.17f, tw = w - u * 8;
        int n = sorted.size();
        float rowH = Math.min(u * 7.6f, (h * 0.68f) / (n + 1));
        Ui.panel(g, x0 - u, y0 - u, tw + u * 2, rowH * (n + 1) + u * 2, u * 2);
        float[] cols = {0.02f, 0.07f, 0.12f, 0.38f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f};
        String[] heads = {"#", "", "Tank", "Round", "Kills", "Damage", "Earned", "Wins", "Score"};
        float hy = y0 + rowH / 2;
        for (int c = 0; c < heads.length; c++) {
            g.text(heads[c], x0 + tw * cols[c], hy, u * 2.8f, Col.TEXT_DIM, c >= 3 ? Gfx.ALIGN_LEFT : Gfx.ALIGN_LEFT, true);
        }
        for (int i = 0; i < n; i++) {
            Player p = sorted.get(i);
            float y = y0 + rowH * (i + 1) + rowH / 2;
            if (i % 2 == 0) g.fillRect(x0, y - rowH / 2, tw, rowH, 0x10FFFFFF);
            float ts = rowH * 0.72f;
            g.text(Integer.toString(i + 1), x0 + tw * cols[0], y, u * 3.4f, i == 0 ? Col.ACCENT : Col.TEXT, Gfx.ALIGN_LEFT, true);
            TankArt.drawIcon(g, p.style, p.color, x0 + tw * cols[1] + ts * 0.4f, y, ts, 60, t);
            String type = p.isHuman() ? "Human" : Ai.PROFILES[p.ai].name;
            g.text(p.name, x0 + tw * cols[2], y - rowH * 0.16f, u * 3.2f, Col.TEXT, Gfx.ALIGN_LEFT, true);
            g.text(type + "   $" + Ui.money(p.money), x0 + tw * cols[2], y + rowH * 0.22f, u * 2.3f, Col.TEXT_DIM, Gfx.ALIGN_LEFT, false);
            String res = p.roundWinner ? "WON" : (p.tank != null && p.tank.alive ? "Survived" : "Destroyed");
            int rc = p.roundWinner ? Col.ACCENT : (p.tank != null && p.tank.alive ? Col.GOOD : Col.BAD);
            g.text(res, x0 + tw * cols[3], y, u * 3f, rc, Gfx.ALIGN_LEFT, true);
            g.text(p.roundKills + " / " + p.kills, x0 + tw * cols[4], y, u * 3f, Col.TEXT, Gfx.ALIGN_LEFT, false);
            g.text(Math.round(p.roundDamage) + "", x0 + tw * cols[5], y, u * 3f, Col.TEXT, Gfx.ALIGN_LEFT, false);
            g.text("$" + Ui.money(p.roundEarn), x0 + tw * cols[6], y, u * 3f, Col.MONEY, Gfx.ALIGN_LEFT, false);
            g.text(Integer.toString(p.wins), x0 + tw * cols[7], y, u * 3f, Col.TEXT, Gfx.ALIGN_LEFT, false);
            g.text(Integer.toString(Math.round(p.score)), x0 + tw * cols[8], y, u * 3.4f, Col.ACCENT, Gfx.ALIGN_LEFT, true);
        }
        g.text("Score: 1 point per damage dealt, 50 per kill, 100 per round won.  Kills shown as round / total.",
                u * 4, h - u * 8, u * 2.5f, Col.TEXT_DIM, Gfx.ALIGN_LEFT, false);
        if (!game.hasHumans() && !finalScreen) {
            g.text("Next round starts automatically...", u * 4, h - u * 4, u * 2.5f, Col.TEXT_DIM, Gfx.ALIGN_LEFT, false);
        }
        renderButtons(g);
    }
}
