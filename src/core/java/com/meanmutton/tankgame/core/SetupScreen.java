package com.meanmutton.tankgame.core;

import java.util.ArrayList;
import java.util.List;

/** Match setup: rules on the left, the player roster on the right, with two pop-up editors. */
public final class SetupScreen extends Screen {
    private static final int BACK = 1, START = 2, ADD = 3;
    private static final int SET_DEC = 100, SET_INC = 120;
    private static final int P_TANK = 200, P_NAME = 220, P_TYPE = 240, P_DEL = 260;
    private static final int D_STYLE = 300, D_COLOR = 320, D_DONE = 340, D_PREV = 341, D_NEXT = 342;
    private static final int A_PICK = 400, A_CANCEL = 450;

    private static final String[] SETTING_NAMES = {"Rounds", "Starting cash", "Terrain", "Wind", "Wind shifts",
            "Walls", "Aim guide", "AI speed"};

    private final Settings s;
    private int mode; // 0 main, 1 tank designer, 2 opponent picker
    private int editing = -1;
    private final List<Btn> overlay = new ArrayList<Btn>();
    private float t;
    private float rowH, listX, listY, setX, setY, setW, setRowH;

    public SetupScreen(Game game) {
        super(game);
        this.s = game.settings;
    }

    protected void layout() {
        buttons.clear();
        overlay.clear();
        float top = h * 0.11f;
        // settings column
        setX = u * 3;
        setY = top + u * 4;
        setW = w * 0.33f;
        setRowH = Math.min(u * 9, (h * 0.83f - setY) / SETTING_NAMES.length);
        for (int i = 0; i < SETTING_NAMES.length; i++) {
            float y = setY + i * setRowH;
            float bs = setRowH * 0.78f;
            Btn dec = add(new Btn(SET_DEC + i, "<").set(setX + setW * 0.42f, y + (setRowH - bs) / 2, bs, bs));
            Btn inc = add(new Btn(SET_INC + i, ">").set(setX + setW - bs, y + (setRowH - bs) / 2, bs, bs));
            dec.repeat = i == 0;
            inc.repeat = i == 0;
        }
        // player list
        listX = setX + setW + u * 5;
        listY = top + u * 9;
        rowH = Math.min(u * 9.5f, (h * 0.835f - listY) / 10f);
        float listW = w - listX - u * 3;
        int n = s.players.size();
        for (int i = 0; i < n; i++) {
            Settings.PlayerSetup ps = s.players.get(i);
            float y = listY + i * rowH;
            float bh = rowH * 0.86f;
            float by = y + (rowH - bh) / 2;
            Btn tank = add(new Btn(P_TANK + i, "").set(listX, by, bh * 1.5f, bh));
            tank.color = 0xFF1A2232;
            Btn name = add(new Btn(P_NAME + i, ps.name).set(listX + bh * 1.6f, by, listW * 0.33f, bh));
            name.textSize = bh * 0.4f;
            float typeX = listX + bh * 1.7f + listW * 0.33f;
            float typeW = listW - (typeX - listX) - bh * 1.1f;
            Btn type;
            if (ps.ai < 0) {
                type = add(new Btn(P_TYPE + i, "Human").set(typeX, by, typeW, bh));
                type.color = 0xFF2E5D8F;
            } else {
                Ai.Profile pr = Ai.PROFILES[ps.ai];
                type = add(new Btn(P_TYPE + i, "CPU: " + pr.name).set(typeX, by, typeW, bh));
                type.color = 0xFF5A3A2A;
            }
            type.textSize = bh * 0.38f;
            Btn del = add(new Btn(P_DEL + i, "X").set(listX + listW - bh, by, bh, bh));
            del.color = 0xFF7A2A2A;
            del.enabled = n > 2;
        }
        float bw = u * 34, bh = u * 10;
        add(new Btn(BACK, "Back").set(u * 3, h - bh - u * 2.5f, bw * 0.6f, bh));
        Btn start = add(new Btn(START, "START BATTLE").set(w - bw - u * 3, h - bh - u * 2.5f, bw, bh));
        start.color = 0xFF2E7D4F;
        Btn add = add(new Btn(ADD, "+ Add Tank").set(w - bw - u * 3 - u * 30, h - bh - u * 2.5f, u * 28, bh));
        add.enabled = n < 10;

        if (mode == 1) layoutDesigner();
        if (mode == 2) layoutPicker();
    }

    private void layoutDesigner() {
        Settings.PlayerSetup ps = s.players.get(editing);
        float pw = w * 0.82f, ph = h * 0.84f, px = (w - pw) / 2, py = (h - ph) / 2;
        int ns = TankArt.STYLE_NAMES.length;
        float sbw = (pw * 0.5f - u * 4) / 3f, sbh = u * 9;
        for (int i = 0; i < ns; i++) {
            Btn b = new Btn(D_STYLE + i, TankArt.STYLE_NAMES[i]);
            b.set(px + pw * 0.48f + (i % 3) * (sbw + u), py + u * 13 + (i / 3) * (sbh + u), sbw, sbh);
            b.selected = ps.style == i;
            b.textSize = u * 3.2f;
            overlay.add(b);
        }
        int nc = TankArt.COLORS.length;
        float cs = (pw * 0.5f - u * 6) / 6f;
        for (int i = 0; i < nc; i++) {
            Btn b = new Btn(D_COLOR + i, "");
            b.set(px + pw * 0.48f + (i % 6) * (cs + u), py + u * 13 + 3 * (sbh + u) + u * 7 + (i / 6) * (cs * 0.7f + u), cs, cs * 0.7f);
            b.color = TankArt.COLORS[i];
            b.selected = ps.color == i;
            overlay.add(b);
        }
        Btn done = new Btn(D_DONE, "Done").set(px + pw - u * 30, py + ph - u * 13, u * 26, u * 10);
        done.color = 0xFF2E7D4F;
        overlay.add(done);
        float pvx = px + pw * 0.06f, pvw = pw * 0.36f;
        overlay.add(new Btn(D_PREV, "<").set(pvx, py + ph - u * 13, u * 10, u * 10));
        overlay.add(new Btn(D_NEXT, ">").set(pvx + pvw - u * 10, py + ph - u * 13, u * 10, u * 10));
    }

    private void layoutPicker() {
        float pw = w * 0.9f, ph = h * 0.9f, px = (w - pw) / 2, py = (h - ph) / 2;
        float cw = (pw - u * 6) / 2, ch = (ph - u * 20) / 6f;
        for (int i = -1; i < Ai.PROFILES.length; i++) {
            int k = i + 1;
            float x = px + u * 2 + (k % 2) * (cw + u * 2);
            float y = py + u * 11 + (k / 2) * ch;
            Btn b = new Btn(A_PICK + k, "");
            b.set(x, y + u * 0.5f, cw, ch - u);
            b.tag = i;
            b.selected = s.players.get(editing).ai == i;
            b.color = i < 0 ? 0xFF22406A : 0xFF2A2E3E;
            overlay.add(b);
        }
        Btn c = new Btn(A_CANCEL, "Cancel").set(px + pw - u * 26, py + u * 1.5f, u * 23, u * 8);
        overlay.add(c);
    }

    protected List<Btn> activeButtons() {
        return mode == 0 ? buttons : overlay;
    }

    private void rebuild() {
        w = -1;
        game.setScreen(this);
    }

    public void update(float dt) {
        super.update(dt);
        t += dt;
    }

    protected void onClick(Btn b) {
        int id = b.id;
        if (id == BACK) {
            game.saveSettings();
            game.setScreen(new MenuScreen(game));
        } else if (id == START) {
            game.startMatch();
        } else if (id == ADD) {
            addPlayer();
            rebuild();
        } else if (id >= SET_DEC && id < SET_DEC + 20) {
            changeSetting(id - SET_DEC, -1);
        } else if (id >= SET_INC && id < SET_INC + 20) {
            changeSetting(id - SET_INC, 1);
        } else if (id >= P_TANK && id < P_TANK + 10) {
            editing = id - P_TANK;
            mode = 1;
            rebuild();
        } else if (id >= P_NAME && id < P_NAME + 10) {
            final int idx = id - P_NAME;
            game.platform.promptText("Tank name", s.players.get(idx).name, new Platform.TextCallback() {
                public void done(final String text) {
                    game.post(new Runnable() {
                        public void run() {
                            String c = Settings.clean(text);
                            if (c.length() > 0 && idx < s.players.size()) {
                                s.players.get(idx).name = c;
                                game.saveSettings();
                                rebuild();
                            }
                        }
                    });
                }
            });
        } else if (id >= P_TYPE && id < P_TYPE + 10) {
            editing = id - P_TYPE;
            mode = 2;
            rebuild();
        } else if (id >= P_DEL && id < P_DEL + 10) {
            if (s.players.size() > 2) s.players.remove(id - P_DEL);
            rebuild();
        } else if (id >= D_STYLE && id < D_STYLE + 20) {
            s.players.get(editing).style = id - D_STYLE;
            rebuild();
        } else if (id >= D_COLOR && id < D_COLOR + 20) {
            s.players.get(editing).color = id - D_COLOR;
            rebuild();
        } else if (id == D_PREV || id == D_NEXT) {
            Settings.PlayerSetup ps = s.players.get(editing);
            int n = TankArt.STYLE_NAMES.length;
            ps.style = (ps.style + (id == D_NEXT ? 1 : n - 1)) % n;
            rebuild();
        } else if (id == D_DONE) {
            mode = 0;
            game.saveSettings();
            rebuild();
        } else if (id >= A_PICK && id < A_PICK + 20) {
            setType(editing, (Integer) b.tag);
            mode = 0;
            game.saveSettings();
            rebuild();
        } else if (id == A_CANCEL) {
            mode = 0;
            rebuild();
        }
    }

    private void setType(int idx, int ai) {
        Settings.PlayerSetup ps = s.players.get(idx);
        boolean defaultName = ps.name.startsWith("Player") || ps.name.startsWith("CPU");
        for (Ai.Profile p : Ai.PROFILES) if (ps.name.startsWith(p.name)) defaultName = true;
        ps.ai = ai;
        if (defaultName) {
            ps.name = "";
            ps.name = uniqueName(ai < 0 ? "Player" : Ai.PROFILES[ai].name, ai < 0);
        }
    }

    private String uniqueName(String base, boolean alwaysNumber) {
        for (int n = alwaysNumber ? 1 : 0; n < 20; n++) {
            String cand = n == 0 ? base : base + " " + n;
            boolean used = false;
            for (Settings.PlayerSetup p : s.players) if (p.name.equals(cand)) used = true;
            if (!used) return cand;
        }
        return base;
    }

    private void addPlayer() {
        if (s.players.size() >= 10) return;
        boolean[] used = new boolean[TankArt.COLORS.length];
        for (Settings.PlayerSetup p : s.players) used[p.color] = true;
        int color = 0;
        for (int i = 0; i < used.length; i++) {
            if (!used[i]) {
                color = i;
                break;
            }
        }
        int ai = game.rng.nextInt(Ai.PROFILES.length);
        Settings.PlayerSetup ps = new Settings.PlayerSetup("", color, game.rng.nextInt(TankArt.STYLE_NAMES.length), ai);
        s.players.add(ps);
        ps.name = uniqueName(Ai.PROFILES[ai].name, false);
    }

    private void changeSetting(int i, int d) {
        switch (i) {
            case 0: s.rounds = Settings.clampInt(s.rounds + d, 1, 20); break;
            case 1: s.cashIndex = Settings.clampInt(s.cashIndex + d, 0, Settings.CASH_OPTIONS.length - 1); break;
            case 2: s.terrain = (s.terrain + d + Settings.TERRAIN_NAMES.length) % Settings.TERRAIN_NAMES.length; break;
            case 3: s.wind = Settings.clampInt(s.wind + d, 0, Settings.WIND_NAMES.length - 1); break;
            case 4: s.windChanges = !s.windChanges; break;
            case 5: s.walls = (s.walls + d + Settings.WALL_NAMES.length) % Settings.WALL_NAMES.length; break;
            case 6: s.aimGuide = Settings.clampInt(s.aimGuide + d, 0, Settings.AIM_NAMES.length - 1); break;
            case 7: s.aiSpeed = Settings.clampInt(s.aiSpeed + d, 0, Settings.SPEED_NAMES.length - 1); break;
        }
    }

    private String settingValue(int i) {
        switch (i) {
            case 0: return Integer.toString(s.rounds);
            case 1: return "$" + Ui.money(s.startCash());
            case 2: return Settings.TERRAIN_NAMES[s.terrain];
            case 3: return Settings.WIND_NAMES[s.wind];
            case 4: return s.windChanges ? "Each turn" : "Per round";
            case 5: return Settings.WALL_NAMES[s.walls];
            case 6: return Settings.AIM_NAMES[s.aimGuide];
            default: return Settings.SPEED_NAMES[s.aiSpeed];
        }
    }

    public boolean back() {
        if (mode != 0) {
            mode = 0;
            rebuild();
        } else {
            game.saveSettings();
            game.setScreen(new MenuScreen(game));
        }
        return true;
    }

    public void render(Gfx g) {
        background(g);
        Ui.title(g, "GAME SETUP", w / 2f, h * 0.06f, u * 7);

        // settings panel
        Ui.panel(g, setX - u * 1.5f, setY - u * 3, setW + u * 3, setRowH * SETTING_NAMES.length + u * 4.5f, u * 2);
        for (int i = 0; i < SETTING_NAMES.length; i++) {
            float y = setY + i * setRowH + setRowH / 2;
            g.text(SETTING_NAMES[i], setX + u, y, u * 3.4f, Col.TEXT_DIM, Gfx.ALIGN_LEFT, false);
            float bs = setRowH * 0.78f;
            float mid = (setX + setW * 0.42f + bs + setX + setW - bs) / 2;
            g.text(settingValue(i), mid, y, u * 3.5f, Col.TEXT, Gfx.ALIGN_CENTER, true);
        }

        // roster
        int n = s.players.size();
        float listW = w - listX - u * 3;
        Ui.panel(g, listX - u * 1.5f, listY - u * 7.5f, listW + u * 3, rowH * 10 + u * 9, u * 2);
        String head = "Tanks  (" + n + " of 10)";
        g.text(head, listX, listY - u * 4, u * 3.8f, Col.TEXT, Gfx.ALIGN_LEFT, true);
        String hint = "tap a tank to restyle it, a name to rename it, the type to pick human or computer";
        float room = listW - g.textWidth(head, u * 3.8f, true) - u * 3;
        float hs = u * 2.4f;
        if (g.textWidth(hint, hs, false) > room) hs = hs * room / g.textWidth(hint, hs, false);
        if (hs > u * 1.6f) g.text(hint, listX + listW, listY - u * 4, hs, Col.TEXT_DIM, Gfx.ALIGN_RIGHT, false);
        renderButtons(g);
        for (int i = 0; i < n; i++) {
            Btn b = buttons.get(SETTING_NAMES.length * 2 + i * 4);
            Settings.PlayerSetup ps = s.players.get(i);
            TankArt.drawIcon(g, ps.style, TankArt.COLORS[ps.color], b.x + b.w / 2, b.y + b.h * 0.42f, b.h * 1.25f, 55, t);
            if (ps.ai >= 0) {
                Btn type = buttons.get(SETTING_NAMES.length * 2 + i * 4 + 2);
                skillDots(g, type.x + type.w - u * 1.5f, type.y + type.h / 2, Ai.PROFILES[ps.ai].skill, u * 0.55f, true);
            }
        }

        if (mode == 1) renderDesigner(g);
        if (mode == 2) renderPicker(g);
    }

    private void skillDots(Gfx g, float right, float cy, int skill, float r, boolean alignRight) {
        for (int k = 0; k < 10; k++) {
            float x = alignRight ? right - (9 - k) * r * 2.6f : right + k * r * 2.6f;
            int c = k < skill ? Col.lerp(0xFF5BD46A, 0xFFFF5A4E, k / 9f) : 0x33FFFFFF;
            g.fillCircle(x, cy, r, c);
        }
    }

    private void renderDesigner(Gfx g) {
        g.fillRect(0, 0, w, h, 0xB0000000);
        Settings.PlayerSetup ps = s.players.get(editing);
        float pw = w * 0.82f, ph = h * 0.84f, px = (w - pw) / 2, py = (h - ph) / 2;
        Ui.panel(g, px, py, pw, ph, u * 3);
        g.text("Customize " + ps.name, px + pw / 2, py + u * 6, u * 5, Col.ACCENT, Gfx.ALIGN_CENTER, true);
        float pvx = px + pw * 0.06f, pvw = pw * 0.36f, pvy = py + u * 12, pvh = ph - u * 28;
        g.fillRectVGradient(pvx, pvy, pvw, pvh, 0xFF3A7BD5, 0xFFA8D8F0);
        g.fillRect(pvx, pvy + pvh * 0.78f, pvw, pvh * 0.22f, 0xFF6B8E3A);
        g.fillRect(pvx, pvy + pvh * 0.78f, pvw, u * 0.8f, 0xFF9BD05A);
        float ang = 90 + (float) Math.sin(t * 1.3f) * 50;
        float size = pvw * 0.72f;
        TankArt.drawIcon(g, ps.style, TankArt.COLORS[ps.color], pvx + pvw / 2, pvy + pvh * 0.78f - size * 0.29f, size, ang, t);
        g.text(TankArt.STYLE_NAMES[ps.style], pvx + pvw / 2, py + ph - u * 8, u * 4, Col.TEXT, Gfx.ALIGN_CENTER, true);
        g.text("Body style", px + pw * 0.48f, py + u * 10.5f, u * 3.2f, Col.TEXT_DIM, Gfx.ALIGN_LEFT, false);
        float sbh = u * 9;
        g.text("Color: " + TankArt.COLOR_NAMES[ps.color], px + pw * 0.48f, py + u * 13 + 3 * (sbh + u) + u * 3.5f, u * 3.2f, Col.TEXT_DIM, Gfx.ALIGN_LEFT, false);
        for (Btn b : overlay) Ui.button(g, b, u);
    }

    private void renderPicker(Gfx g) {
        g.fillRect(0, 0, w, h, 0xB0000000);
        float pw = w * 0.9f, ph = h * 0.9f, px = (w - pw) / 2, py = (h - ph) / 2;
        Ui.panel(g, px, py, pw, ph, u * 3);
        g.text("Who drives " + s.players.get(editing).name + "?", px + u * 3, py + u * 5.5f, u * 4.6f, Col.ACCENT, Gfx.ALIGN_LEFT, true);
        for (Btn b : overlay) {
            Ui.button(g, b, u);
            if (b.tag == null) continue;
            int ai = (Integer) b.tag;
            float tx = b.x + u * 2.5f;
            float cy = b.y + b.h / 2;
            if (ai < 0) {
                g.text("Human", tx, cy - b.h * 0.18f, u * 3.6f, Col.TEXT, Gfx.ALIGN_LEFT, true);
                g.text("You (or a friend) play this tank. Turns pass the phone around.", tx, cy + b.h * 0.2f, u * 2.5f, Col.TEXT_DIM, Gfx.ALIGN_LEFT, false);
            } else {
                Ai.Profile p = Ai.PROFILES[ai];
                g.text(p.skill + ". " + p.name, tx, cy - b.h * 0.18f, u * 3.6f, Col.TEXT, Gfx.ALIGN_LEFT, true);
                g.text(p.blurb, tx, cy + b.h * 0.2f, u * 2.3f, Col.TEXT_DIM, Gfx.ALIGN_LEFT, false);
                skillDots(g, b.x + b.w - u * 2, cy - b.h * 0.18f, p.skill, u * 0.6f, true);
            }
        }
    }
}
