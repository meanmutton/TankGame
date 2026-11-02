package com.meanmutton.tankgame.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Top-level game object: owns the current screen, the match state, and the input queue.
 * Input arrives on the UI thread and is replayed on the game thread inside tick().
 */
public final class Game {
    public static final int TOUCH_DOWN = 0, TOUCH_MOVE = 1, TOUCH_UP = 2, BACK = 3;

    public final Platform platform;
    public Settings settings;
    public final List<Player> players = new ArrayList<Player>();
    public int round;
    public final Random rng;
    public float time;

    private Screen screen;
    private int w = 1, h = 1;
    private final List<float[]> input = new ArrayList<float[]>();
    private final List<Runnable> posted = new ArrayList<Runnable>();
    private final List<Player> shopQueue = new ArrayList<Player>();

    public Game(Platform platform, long seed) {
        this.platform = platform;
        this.rng = new Random(seed);
        this.settings = Settings.parse(platform.load("settings"));
        this.screen = new MenuScreen(this);
    }

    public Screen screen() {
        return screen;
    }

    public void setScreen(Screen s) {
        screen = s;
        s.resize(w, h);
    }

    public int width() {
        return w;
    }

    public int height() {
        return h;
    }

    // ---------------- threading ----------------

    public void post(Runnable r) {
        synchronized (posted) {
            posted.add(r);
        }
    }

    public void touch(int type, int pointer, float x, float y) {
        synchronized (input) {
            input.add(new float[]{type, pointer, x, y});
        }
    }

    public void tick(float dt, int width, int height) {
        if (width != w || height != h) {
            w = Math.max(1, width);
            h = Math.max(1, height);
        }
        screen.resize(w, h);
        List<Runnable> run = null;
        synchronized (posted) {
            if (!posted.isEmpty()) {
                run = new ArrayList<Runnable>(posted);
                posted.clear();
            }
        }
        if (run != null) for (Runnable r : run) r.run();
        List<float[]> ev = null;
        synchronized (input) {
            if (!input.isEmpty()) {
                ev = new ArrayList<float[]>(input);
                input.clear();
            }
        }
        if (ev != null) {
            for (float[] e : ev) {
                Screen s = screen;
                switch ((int) e[0]) {
                    case TOUCH_DOWN: s.touchDown((int) e[1], e[2], e[3]); break;
                    case TOUCH_MOVE: s.touchMove((int) e[1], e[2], e[3]); break;
                    case TOUCH_UP: s.touchUp((int) e[1], e[2], e[3]); break;
                    case BACK:
                        if (!s.back()) platform.exit();
                        break;
                }
            }
        }
        time += dt;
        screen.update(dt);
    }

    public void render(Gfx g) {
        screen.resize(w, h);
        screen.render(g);
    }

    public void sfx(int id, float vol) {
        if (settings.sound) platform.play(id, vol);
    }

    public void saveSettings() {
        platform.save("settings", settings.serialize());
    }

    // ---------------- match flow ----------------

    public void startMatch() {
        saveSettings();
        players.clear();
        for (int i = 0; i < settings.players.size(); i++) {
            players.add(new Player(i, settings.players.get(i), settings.startCash()));
        }
        round = 0;
        nextRound();
    }

    public boolean hasHumans() {
        for (Player p : players) if (p.isHuman()) return true;
        return false;
    }

    void nextRound() {
        round++;
        for (Player p : players) {
            p.roundEarn = 0;
            p.roundDamage = 0;
            p.roundKills = 0;
            p.roundWinner = false;
            p.lastTargetIdx = -1;
            for (int i = 0; i < p.aimOffset.length; i++) p.aimOffset[i] = 0;
            p.tank = null;
            if (!p.isHuman()) Ai.shop(p, rng);
        }
        shopQueue.clear();
        for (Player p : players) if (p.isHuman()) shopQueue.add(p);
        nextShopper();
    }

    void nextShopper() {
        if (shopQueue.isEmpty()) {
            setScreen(new BattleScreen(this));
        } else {
            setScreen(new ShopScreen(this, shopQueue.remove(0)));
        }
    }

    void shopDone() {
        nextShopper();
    }

    void roundFinished() {
        setScreen(new ScoreScreen(this, round >= settings.rounds));
    }

    void scoresDone(boolean finalScreen) {
        if (finalScreen) setScreen(new MenuScreen(this));
        else nextRound();
    }

    void quitToMenu() {
        setScreen(new MenuScreen(this));
    }
}
