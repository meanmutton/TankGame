package com.meanmutton.tankgame.core;

import java.util.ArrayList;
import java.util.List;

/** The battlefield view: draws the world, the control panel, and handles human input. */
public final class BattleScreen extends Screen {
    private static final int LEFT = 1, RIGHT = 2, ANG_DN = 3, ANG_UP = 4, POW_DN = 5, POW_UP = 6, WEAPON = 7,
            ITEMS = 8, FIRE = 9, PAUSE = 10;
    private static final int O_WEAPON = 100, O_REPAIR = 200, O_SHIELD = 210, O_CLOSE = 300,
            O_RESUME = 301, O_SKIP = 302, O_SURRENDER = 303, O_QUIT = 304, O_SOUND = 305, O_CONFIRM = 306;

    private Battle battle;
    private float scale, pfH, offX;
    private float t;
    private Btn left, right, angDn, angUp, powDn, powUp, weapon, items, fire, pause;
    private int overlayMode; // 0 none, 1 weapons, 2 items, 3 pause, 4 confirm quit
    private final List<Btn> overlay = new ArrayList<Btn>();
    private boolean aiming;
    private int aimPointer = -1;
    private float overTime;
    private boolean finished;

    public BattleScreen(Game game) {
        super(game);
    }

    public Battle battle() {
        return battle;
    }

    protected void layout() {
        pfH = Math.round(h * 0.8f);
        scale = pfH / Battle.H;
        if (battle == null) {
            int worldW = Math.max(700, Math.min(3600, Math.round(w / scale)));
            battle = new Battle(game, game.players, worldW, game.rng);
        }
        offX = (w - battle.w * scale) / 2f;

        buttons.clear();
        float ph = h - pfH;
        float bs = Math.min(ph * 0.72f, w / 16.5f);
        float y = pfH + (ph - bs) / 2;
        float gap = bs * 0.12f;
        float x = u * 2;
        left = add(new Btn(LEFT, "<").set(x, y, bs, bs));
        right = add(new Btn(RIGHT, ">").set(x + bs + gap, y, bs, bs));
        x += 2 * bs + gap * 5;
        angDn = add(new Btn(ANG_DN, "-").set(x, y, bs * 0.85f, bs));
        angUp = add(new Btn(ANG_UP, "+").set(x + bs * 0.85f + bs * 1.3f, y, bs * 0.85f, bs));
        x += bs * 3f + gap * 4;
        powDn = add(new Btn(POW_DN, "-").set(x, y, bs * 0.85f, bs));
        powUp = add(new Btn(POW_UP, "+").set(x + bs * 0.85f + bs * 1.3f, y, bs * 0.85f, bs));
        x += bs * 3f + gap * 4;
        float fireW = bs * 1.7f;
        float itemsW = bs * 1.15f;
        float weaponW = w - x - fireW - itemsW - gap * 2 - u * 2;
        weapon = add(new Btn(WEAPON, "").set(x, y, weaponW, bs));
        weapon.color = 0xFF2A3550;
        items = add(new Btn(ITEMS, "Items").set(x + weaponW + gap, y, itemsW, bs));
        fire = add(new Btn(FIRE, "FIRE").set(w - u * 2 - fireW, y, fireW, bs));
        fire.color = 0xFFB8322A;
        fire.textSize = bs * 0.36f;
        angDn.repeat = angUp.repeat = powDn.repeat = powUp.repeat = true;
        pause = add(new Btn(PAUSE, "II").set(u * 1.5f, u * 1.5f, u * 9, u * 9));
        pause.color = 0x99202A3C;
        layoutOverlay();
    }

    private void layoutOverlay() {
        overlay.clear();
        if (overlayMode == 0) return;
        Tank tk = battle.active;
        if (overlayMode == 1) {
            List<Integer> ids = new ArrayList<Integer>();
            for (int i = 0; i < Items.FIRST_DEFENSE; i++) if (tk.p.count(i) > 0) ids.add(i);
            int cols = 4;
            float pw = w * 0.86f, cw = (pw - u * 4) / cols, ch = u * 13;
            int rows = (ids.size() + cols - 1) / cols;
            float ph = rows * ch + u * 18;
            float px = (w - pw) / 2, py = Math.max(u * 2, (h - ph) / 2);
            for (int k = 0; k < ids.size(); k++) {
                int id = ids.get(k);
                Btn b = new Btn(O_WEAPON + id, "");
                b.set(px + u * 2 + (k % cols) * cw + u * 0.5f, py + u * 10 + (k / cols) * ch, cw - u, ch - u);
                b.selected = tk.weapon == id;
                b.color = 0xFF1E293C;
                overlay.add(b);
            }
            overlay.add(new Btn(O_CLOSE, "Close").set(px + pw - u * 24, py + u * 1.5f, u * 22, u * 7.5f));
        } else if (overlayMode == 2) {
            float pw = w * 0.7f, ph = h * 0.66f, px = (w - pw) / 2, py = (h - ph) / 2;
            float bw = (pw - u * 8) / 2, bh = u * 13;
            int[] ids = {Items.REPAIR, Items.SHIELD_L, Items.SHIELD_H, Items.SHIELD_D};
            for (int k = 0; k < ids.length; k++) {
                int id = ids[k];
                Btn b = new Btn(id == Items.REPAIR ? O_REPAIR : O_SHIELD + id, "");
                b.set(px + u * 3 + (k % 2) * (bw + u * 2), py + u * 11 + (k / 2) * (bh + u * 2), bw, bh);
                b.enabled = tk.p.inv[id] > 0 && (id != Items.REPAIR || tk.health < 100);
                b.color = 0xFF1E293C;
                b.tag = id;
                overlay.add(b);
            }
            overlay.add(new Btn(O_CLOSE, "Close").set(px + pw - u * 24, py + u * 1.5f, u * 22, u * 7.5f));
        } else if (overlayMode == 3) {
            float bw = u * 52, bh = u * 10, x = (w - bw) / 2, y = h * 0.2f;
            overlay.add(new Btn(O_RESUME, "Resume").set(x, y, bw, bh).tint(0xFF2E7D4F));
            Btn skip = new Btn(O_SKIP, "Skip Turn").set(x, y + bh * 1.2f, bw, bh);
            Btn sur = new Btn(O_SURRENDER, "Surrender (self-destruct)").set(x, y + bh * 2.4f, bw, bh);
            boolean human = battle.active != null && battle.active.p.isHuman() && !battle.roundOver;
            skip.enabled = human && (battle.phase == Battle.PH_AIM);
            sur.enabled = human && (battle.phase == Battle.PH_AIM);
            overlay.add(skip);
            overlay.add(sur);
            overlay.add(new Btn(O_SOUND, "Sound: " + (game.settings.sound ? "On" : "Off")).set(x, y + bh * 3.6f, bw, bh));
            overlay.add(new Btn(O_QUIT, "Quit to Main Menu").set(x, y + bh * 4.8f, bw, bh).tint(0xFF7A2A2A));
        } else if (overlayMode == 4) {
            float bw = u * 30, bh = u * 10, y = h * 0.55f;
            overlay.add(new Btn(O_CONFIRM, "Yes, quit").set(w / 2f - bw - u * 2, y, bw, bh).tint(0xFF7A2A2A));
            overlay.add(new Btn(O_RESUME, "Keep playing").set(w / 2f + u * 2, y, bw, bh).tint(0xFF2E7D4F));
        }
    }

    private void openOverlay(int mode) {
        overlayMode = mode;
        aiming = false;
        layoutOverlay();
    }

    protected List<Btn> activeButtons() {
        return overlayMode == 0 ? buttons : overlay;
    }

    public void update(float dt) {
        super.update(dt);
        t += dt;
        if (overlayMode == 3 || overlayMode == 4) return; // paused
        battle.update(dt);
        Tank a = battle.active;
        boolean human = battle.isHumanAiming() && overlayMode == 0;
        for (Btn b : buttons) if (b != pause) b.enabled = human;
        if (human && a != null) {
            if (left.pressed) battle.drive(-1, dt);
            if (right.pressed) battle.drive(1, dt);
            left.enabled = right.enabled = a.p.fuel > 0.5f && !battle.buried(a);
            items.enabled = a.p.inv[Items.REPAIR] > 0 || a.p.inv[Items.SHIELD_L] > 0 || a.p.inv[Items.SHIELD_H] > 0
                    || a.p.inv[Items.SHIELD_D] > 0;
        }
        if (overlayMode != 0 && !battle.isHumanAiming()) {
            overlayMode = 0;
            overlay.clear();
        }
        if (battle.phase == Battle.PH_OVER) {
            overTime += dt;
            battle.celebrate();
            float wait = game.hasHumans() ? 3.5f : 2.5f;
            if (overTime > wait && !finished) {
                finished = true;
                game.roundFinished();
            }
        }
    }

    protected void onClick(Btn b) {
        Tank a = battle.active;
        switch (b.id) {
            case ANG_DN: a.angle = Math.max(0, a.angle - 1); return;
            case ANG_UP: a.angle = Math.min(180, a.angle + 1); return;
            case POW_DN: a.power = Math.max(0, a.power - 5); return;
            case POW_UP: a.power = Math.min(a.maxPower(), a.power + 5); return;
            case FIRE: battle.fire(); return;
            case WEAPON: openOverlay(1); return;
            case ITEMS: openOverlay(2); return;
            case PAUSE: openOverlay(3); return;
            case O_CLOSE:
            case O_RESUME:
                overlayMode = 0;
                overlay.clear();
                return;
            case O_SKIP:
                overlayMode = 0;
                battle.skipTurn();
                return;
            case O_SURRENDER:
                overlayMode = 0;
                battle.surrender();
                return;
            case O_SOUND:
                game.settings.sound = !game.settings.sound;
                game.saveSettings();
                layoutOverlay();
                return;
            case O_QUIT:
                openOverlay(4);
                return;
            case O_CONFIRM:
                game.quitToMenu();
                return;
            case O_REPAIR:
                battle.useRepair();
                overlayMode = 0;
                return;
            default:
                break;
        }
        if (b.id >= O_WEAPON && b.id < O_WEAPON + Items.COUNT) {
            a.weapon = b.id - O_WEAPON;
            overlayMode = 0;
        } else if (b.id >= O_SHIELD && b.id < O_SHIELD + Items.COUNT) {
            battle.activateShield(b.id - O_SHIELD);
            overlayMode = 0;
        }
    }

    protected void onTouchDown(int pointer, float x, float y) {
        if (overlayMode != 0) {
            if (overlayMode == 1 || overlayMode == 2) overlayMode = 0; // tap outside closes
            return;
        }
        if (y < pfH && battle.isHumanAiming()) {
            aiming = true;
            aimPointer = pointer;
            aimAt(x, y);
        }
    }

    protected void onTouchMove(int pointer, float x, float y) {
        if (aiming && pointer == aimPointer && battle.isHumanAiming()) aimAt(x, Math.min(y, pfH));
    }

    protected void onTouchUp(int pointer, float x, float y) {
        if (pointer == aimPointer) {
            aiming = false;
            aimPointer = -1;
        }
    }

    private void aimAt(float sx, float sy) {
        Tank a = battle.active;
        float wx = (sx - offX) / scale, wy = sy / scale;
        float dx = wx - a.pivotX(), dy = a.pivotY() - wy;
        float ang = (float) Math.toDegrees(Math.atan2(dy, dx));
        if (ang < 0) ang = dx >= 0 ? 0 : 180;
        a.angle = Math.round(Math.max(0, Math.min(180, ang)));
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        float full = Math.max(300, battle.w * 0.42f);
        a.power = Math.round(Math.min(a.maxPower(), Math.max(0, dist / full * 1000f)));
    }

    public boolean back() {
        if (overlayMode != 0) {
            overlayMode = 0;
            overlay.clear();
        } else {
            openOverlay(3);
        }
        return true;
    }

    // =====================================================================

    public void render(Gfx g) {
        // world
        g.save();
        g.clipRect(0, 0, w, pfH);
        float sx = 0, sy = 0;
        if (battle.shake > 0) {
            sx = (float) (Math.random() - 0.5) * battle.shake * scale;
            sy = (float) (Math.random() - 0.5) * battle.shake * scale;
        }
        g.translate(offX + sx, sy);
        g.scale(scale, scale);
        battle.render(g, t);
        g.restore();

        renderTopHud(g);
        renderPanel(g);

        if (aiming && battle.isHumanAiming()) {
            Tank a = battle.active;
            float px = offX + a.pivotX() * scale, py = a.pivotY() * scale;
            float rad = (float) Math.toRadians(a.angle);
            float len = a.power / 1000f * Math.max(300, battle.w * 0.42f) * scale;
            g.line(px, py, px + (float) Math.cos(rad) * len, py - (float) Math.sin(rad) * len, u * 0.5f, 0x66FFFFFF);
        }

        if (battle.phase == Battle.PH_BANNER && battle.active != null) {
            Tank a = battle.active;
            String s = a.p.isHuman() ? a.p.name + ", your turn!" : a.p.name + "'s turn";
            float k = Math.min(1f, battle.phaseTime * 5f);
            banner(g, s, a.p.color, k);
        }
        if (battle.phase == Battle.PH_OVER) {
            banner(g, battle.banner, battle.winner != null ? battle.winner.color : Col.ACCENT, Math.min(1f, overTime * 4));
        }

        if (overlayMode != 0) renderOverlay(g);
    }

    private void banner(Gfx g, String s, int color, float k) {
        float bh = u * 12, y = pfH * 0.32f;
        g.fillRect(0, y - bh / 2, w, bh, Col.alpha(0xCC000000, k));
        g.fillRect(0, y - bh / 2, w, u * 0.6f, Col.alpha(color, k));
        g.fillRect(0, y + bh / 2 - u * 0.6f, w, u * 0.6f, Col.alpha(color, k));
        g.text(s, w / 2f, y, u * 6, Col.alpha(0xFFFFFFFF, k), Gfx.ALIGN_CENTER, true);
    }

    private void renderTopHud(Gfx g) {
        // wind
        float cx = w / 2f, cy = u * 5.5f;
        float pw = u * 34, ph = u * 8;
        g.fillRoundRect(cx - pw / 2, cy - ph / 2, pw, ph, ph / 2, 0x99000000);
        int wind = Math.round(battle.wind);
        if (wind == 0) {
            g.text("No wind", cx, cy, u * 3.4f, Col.TEXT, Gfx.ALIGN_CENTER, true);
        } else {
            float len = Math.abs(wind) / 100f * pw * 0.32f + u * 2;
            float dir = Math.signum(wind);
            float ax = cx + dir * u * 6;
            g.line(ax - dir * len / 2, cy, ax + dir * len / 2, cy, u * 1.1f, Col.ACCENT);
            float[] xs = {ax + dir * (len / 2 + u * 2), ax + dir * len / 2, ax + dir * len / 2};
            float[] ys = {cy, cy - u * 1.8f, cy + u * 1.8f};
            g.fillPoly(xs, ys, 3, Col.ACCENT);
            g.text("Wind " + Math.abs(wind), cx - dir * u * 9, cy, u * 3.2f, Col.TEXT, Gfx.ALIGN_CENTER, true);
        }
        // round
        g.text("Round " + game.round + "/" + game.settings.rounds, w - u * 3, u * 4.5f, u * 3.4f, Col.TEXT, Gfx.ALIGN_RIGHT, true);
        g.text("Tanks left: " + battle.aliveCount(), w - u * 3, u * 9, u * 2.8f, Col.TEXT_DIM, Gfx.ALIGN_RIGHT, false);

        // active player
        Tank a = battle.active;
        if (a != null && a.alive) {
            float x = pause.x + pause.w + u * 2.5f;
            g.fillRoundRect(x - u, u * 1.5f, u * 46, u * 9, u * 2, 0x99000000);
            g.fillCircle(x + u * 2, u * 6, u * 1.8f, a.p.color);
            g.text(a.p.name, x + u * 5, u * 4.3f, u * 3.3f, Col.TEXT, Gfx.ALIGN_LEFT, true);
            String info = "HP " + Math.round(a.health) + (a.shieldHp > 0 ? "  Shield " + Math.round(a.shieldHp) : "")
                    + "   $" + Ui.money(a.p.money);
            g.text(info, x + u * 5, u * 8.2f, u * 2.6f, Col.TEXT_DIM, Gfx.ALIGN_LEFT, false);
        }
    }

    private void renderPanel(Gfx g) {
        float ph = h - pfH;
        g.fillRectVGradient(0, pfH, w, ph, 0xFF1A2232, 0xFF0C111A);
        g.fillRect(0, pfH, w, u * 0.5f, 0xFF3D4E6E);
        Tank a = battle.active;
        if (a == null) return;
        boolean human = a.p.isHuman() && !battle.roundOver;

        renderButtons(g);

        float lab = u * 2.4f;
        // fuel under move buttons
        float mx = (left.x + right.x + right.w) / 2;
        g.text("Fuel " + Math.round(a.p.fuel), mx, left.y - lab * 0.1f + left.h + (h - left.y - left.h) / 2, lab, Col.TEXT_DIM, Gfx.ALIGN_CENTER, false);
        g.text("Move", mx, pfH + (left.y - pfH) / 2, lab, Col.TEXT_DIM, Gfx.ALIGN_CENTER, false);
        // angle
        float ax = (angDn.x + angDn.w + angUp.x) / 2;
        g.text("Angle", ax, pfH + (left.y - pfH) / 2, lab, Col.TEXT_DIM, Gfx.ALIGN_CENTER, false);
        g.text(Math.round(a.angle) + "°", ax, angDn.y + angDn.h / 2, u * 5, Col.TEXT, Gfx.ALIGN_CENTER, true);
        // power
        float px = (powDn.x + powDn.w + powUp.x) / 2;
        g.text("Power", px, pfH + (left.y - pfH) / 2, lab, Col.TEXT_DIM, Gfx.ALIGN_CENTER, false);
        g.text(Integer.toString(Math.round(a.power)), px, powDn.y + powDn.h / 2 - u * 0.8f, u * 5, Col.TEXT, Gfx.ALIGN_CENTER, true);
        float bw = powUp.x - powDn.x - powDn.w - u * 2;
        Ui.bar(g, powDn.x + powDn.w + u, powDn.y + powDn.h - u * 2.2f, bw, u * 1.2f, a.power / 1000f, Col.ACCENT);
        float maxMark = powDn.x + powDn.w + u + bw * (a.maxPower() / 1000f);
        g.fillRect(maxMark - u * 0.25f, powDn.y + powDn.h - u * 2.8f, u * 0.5f, u * 2.4f, Col.BAD);

        // weapon button contents
        Item it = Items.ALL[a.weapon];
        float is = weapon.h * 0.62f;
        Ui.itemIcon(g, a.weapon, weapon.x + is * 0.5f + u * 2, weapon.y + weapon.h / 2, is);
        float tx = weapon.x + is + u * 4;
        g.text(Ui.fit(g, it.name, u * 3.4f, weapon.x + weapon.w - tx - u), tx, weapon.y + weapon.h * 0.36f, u * 3.4f, Col.TEXT, Gfx.ALIGN_LEFT, true);
        g.text(it.infinite ? "unlimited" : "x" + a.p.inv[a.weapon], tx, weapon.y + weapon.h * 0.68f, u * 2.8f, Col.TEXT_DIM, Gfx.ALIGN_LEFT, false);

        if (!human && !battle.roundOver) {
            g.fillRect(0, pfH + u * 0.5f, w, ph, 0xC8000000);
            String s = battle.phase == Battle.PH_ACTION ? a.p.name + " fired!" : a.p.name + " (" + Ai.PROFILES[a.p.ai].name + ") is thinking...";
            g.text(s, w / 2f, pfH + ph / 2, u * 4, Col.TEXT, Gfx.ALIGN_CENTER, true);
        } else if (human && battle.phase == Battle.PH_ACTION) {
            g.fillRect(0, pfH + u * 0.5f, w, ph, 0x66000000);
        }
        if (human && battle.phase == Battle.PH_AIM && battle.buried(a)) {
            g.text("Buried! Fire to dig yourself out.", w / 2f, pfH - u * 4, u * 3.4f, 0xFFFFE08A, Gfx.ALIGN_CENTER, true);
        } else if (human && battle.phase == Battle.PH_AIM && t % 8 < 4 && game.round == 1 && battle.turnCount <= battle.tanks.size()) {
            g.text("Tip: drag on the battlefield to aim. Farther from your tank = more power.", w / 2f, pfH - u * 4, u * 3f, 0xCCFFFFFF, Gfx.ALIGN_CENTER, false);
        }
    }

    private void renderOverlay(Gfx g) {
        g.fillRect(0, 0, w, h, 0xA0000000);
        Tank a = battle.active;
        if (overlayMode == 1) {
            float px = w * 0.07f, pw = w * 0.86f;
            Btn close = overlay.get(overlay.size() - 1);
            float py = close.y - u * 1.5f;
            float bottom = 0;
            for (Btn b : overlay) bottom = Math.max(bottom, b.y + b.h);
            Ui.panel(g, px, py, pw, bottom - py + u * 3, u * 2.5f);
            g.text("Choose weapon", px + u * 3, py + u * 5.5f, u * 4.4f, Col.ACCENT, Gfx.ALIGN_LEFT, true);
            for (Btn b : overlay) {
                Ui.button(g, b, u);
                if (b.id < O_WEAPON || b.id >= O_WEAPON + Items.COUNT) continue;
                int id = b.id - O_WEAPON;
                Item it = Items.ALL[id];
                float is = b.h * 0.62f;
                Ui.itemIcon(g, id, b.x + is * 0.5f + u * 1.5f, b.y + b.h / 2, is);
                float tx = b.x + is + u * 3;
                g.text(Ui.fit(g, it.name, u * 3f, b.x + b.w - tx - u), tx, b.y + b.h * 0.34f, u * 3f, Col.TEXT, Gfx.ALIGN_LEFT, true);
                g.text((it.infinite ? "unlimited" : "x" + a.p.inv[id]) + "  " + Items.stats(it), tx, b.y + b.h * 0.68f, u * 2.2f, Col.TEXT_DIM, Gfx.ALIGN_LEFT, false);
            }
        } else if (overlayMode == 2) {
            float pw = w * 0.7f, ph = h * 0.66f, px = (w - pw) / 2, py = (h - ph) / 2;
            Ui.panel(g, px, py, pw, ph, u * 2.5f);
            g.text("Items", px + u * 3, py + u * 5.5f, u * 4.4f, Col.ACCENT, Gfx.ALIGN_LEFT, true);
            for (Btn b : overlay) {
                Ui.button(g, b, u);
                if (b.tag == null) continue;
                int id = (Integer) b.tag;
                Item it = Items.ALL[id];
                float is = b.h * 0.62f;
                Ui.itemIcon(g, id, b.x + is * 0.5f + u * 1.5f, b.y + b.h / 2, is);
                float tx = b.x + is + u * 3;
                g.text(it.name + "  (x" + a.p.inv[id] + ")", tx, b.y + b.h * 0.34f, u * 3.1f, b.enabled ? Col.TEXT : Col.TEXT_DIM, Gfx.ALIGN_LEFT, true);
                String sub = id == Items.REPAIR ? "Restore 35 health now" : (a.shieldType == id && a.shieldHp > 0 ? "Active: " + Math.round(a.shieldHp) + " left" : "Activate: absorbs " + it.shieldHp);
                g.text(sub, tx, b.y + b.h * 0.68f, u * 2.4f, Col.TEXT_DIM, Gfx.ALIGN_LEFT, false);
            }
            g.text("Parachutes: " + a.p.inv[Items.PARACHUTE] + " (deploy automatically)   Fuel: " + Math.round(a.p.fuel)
                    + "   Using items does not end your turn.", px + pw / 2, py + ph - u * 5, u * 2.6f, Col.TEXT_DIM, Gfx.ALIGN_CENTER, false);
        } else if (overlayMode == 3) {
            Ui.title(g, "PAUSED", w / 2f, h * 0.11f, u * 8);
            for (Btn b : overlay) Ui.button(g, b, u);
        } else if (overlayMode == 4) {
            Ui.panel(g, w * 0.2f, h * 0.25f, w * 0.6f, h * 0.48f, u * 3);
            g.text("Quit this game?", w / 2f, h * 0.35f, u * 5.5f, Col.ACCENT, Gfx.ALIGN_CENTER, true);
            g.text("The current match will be lost.", w / 2f, h * 0.44f, u * 3.2f, Col.TEXT_DIM, Gfx.ALIGN_CENTER, false);
            for (Btn b : overlay) Ui.button(g, b, u);
        }
    }
}
