package com.meanmutton.tankgame.core;

/** Title screen with a little animated skirmish behind the buttons. */
public final class MenuScreen extends Screen {
    private static final int NEW = 1, HELP = 2, SOUND = 3, QUIT = 4;
    private Btn sound;
    private float t;
    private float shellX, shellY, shellVx, shellVy, boomT = -1, boomX, boomY;
    private boolean shellLive;
    private float fireTimer = 1.2f;
    private int shooter;
    private final float[] hx = new float[66], hy = new float[66];

    public MenuScreen(Game game) {
        super(game);
    }

    protected void layout() {
        buttons.clear();
        float bw = u * 48, bh = u * 11, x = w / 2f - bw / 2, y = h * 0.42f;
        add(new Btn(NEW, "New Game").set(x, y, bw, bh)).color = 0xFF2E7D4F;
        add(new Btn(HELP, "How to Play").set(x, y + bh * 1.25f, bw, bh));
        sound = add(new Btn(SOUND, "").set(x, y + bh * 2.5f, bw * 0.48f, bh));
        sound.textSize = u * 3.4f;
        add(new Btn(QUIT, "Quit").set(x + bw * 0.52f, y + bh * 2.5f, bw * 0.48f, bh));
        for (int i = 0; i < hx.length; i++) {
            hx[i] = i * w / (hx.length - 3f);
            hy[i] = h * 0.86f + (float) (Math.sin(i * 0.35) * h * 0.04 + Math.sin(i * 0.9) * h * 0.015);
        }
        hx[hx.length - 2] = w;
        hy[hy.length - 2] = h;
        hx[hx.length - 1] = 0;
        hy[hy.length - 1] = h;
    }

    public void update(float dt) {
        super.update(dt);
        t += dt;
        sound.label = "Sound: " + (game.settings.sound ? "On" : "Off");
        float s = h / 300f;
        if (shellLive) {
            shellVy += 160 * s * dt;
            shellX += shellVx * dt;
            shellY += shellVy * dt;
            float gy = groundAt(shellX);
            if (shellY >= gy) {
                shellLive = false;
                boomT = 0;
                boomX = shellX;
                boomY = gy;
            }
        } else {
            fireTimer -= dt;
            if (fireTimer <= 0) {
                fireTimer = 2.2f;
                shooter = 1 - shooter;
                float sx = shooter == 0 ? w * 0.12f : w * 0.88f;
                shellX = sx;
                shellY = groundAt(sx) - 14 * s;
                float dir = shooter == 0 ? 1 : -1;
                float range = w * (0.55f + (float) Math.sin(t) * 0.2f);
                float vy = -115 * s;
                float flight = 2 * -vy / (160 * s);
                shellVx = dir * range / flight;
                shellVy = vy;
                shellLive = true;
            }
        }
        if (boomT >= 0) {
            boomT += dt;
            if (boomT > 0.6f) boomT = -1;
        }
    }

    private float groundAt(float x) {
        int i = (int) (x / (w / (hx.length - 3f)));
        i = Math.max(0, Math.min(hx.length - 4, i));
        float f = (x - hx[i]) / Math.max(1, hx[i + 1] - hx[i]);
        return hy[i] * (1 - f) + hy[i + 1] * f;
    }

    protected void onClick(Btn b) {
        switch (b.id) {
            case NEW: game.setScreen(new SetupScreen(game)); break;
            case HELP: game.setScreen(new HelpScreen(game)); break;
            case SOUND:
                game.settings.sound = !game.settings.sound;
                game.saveSettings();
                break;
            case QUIT: game.platform.exit(); break;
        }
    }

    public void render(Gfx g) {
        g.fillRectVGradient(0, 0, w, h, 0xFF0A1028, 0xFF3A2A5A);
        for (int i = 0; i < 60; i++) {
            float sx = (i * 137.5f) % w, sy = (i * 61.3f) % (h * 0.6f);
            g.fillCircle(sx, sy, 1 + (i % 3) * 0.5f, Col.alpha(0xFFFFFFFF, 0.3f + 0.4f * (float) Math.abs(Math.sin(t + i))));
        }
        g.fillPoly(hx, hy, hx.length, 0xFF24304A);
        float s = h / 300f;
        TankArt.drawIcon(g, 0, TankArt.COLORS[0], w * 0.12f, groundAt(w * 0.12f) - 12 * s, 26 * s, 50, t);
        TankArt.drawIcon(g, 1, TankArt.COLORS[7], w * 0.88f, groundAt(w * 0.88f) - 12 * s, 26 * s, 130, t);
        if (shellLive) {
            g.fillRadial(shellX, shellY, 6 * s, 0xFFFFE0A0, 0x00FFA040);
            g.fillCircle(shellX, shellY, 1.6f * s, 0xFFFFFFFF);
        }
        if (boomT >= 0) {
            float k = boomT / 0.6f;
            g.fillRadial(boomX, boomY, 18 * s * (0.5f + k), Col.alpha(0xFFFFE0A0, 1 - k), Col.alpha(0xFFFF6A2A, 0));
        }
        Ui.title(g, "TANK BARRAGE", w / 2f, h * 0.2f, u * 13);
        g.text("Turn-based artillery for 2 to 10 tanks", w / 2f, h * 0.31f, u * 3.6f, Col.TEXT_DIM, Gfx.ALIGN_CENTER, false);
        renderButtons(g);
    }

    public boolean back() {
        return false;
    }
}
