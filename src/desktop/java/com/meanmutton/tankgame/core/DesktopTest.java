package com.meanmutton.tankgame.core;

import com.meanmutton.tankgame.desktop.AwtGfx;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Desktop harness (not part of the APK).
 *   shots <dir>   render every screen and each weapon in action to PNG files
 *   sim <n>       play n all-computer matches headless and report crashes, stalls, and win rates
 */
public final class DesktopTest {

    static final class FakePlatform implements Platform {
        final Map<String, String> store = new HashMap<String, String>();
        public void play(int sound, float volume) {}
        public String load(String key) { return store.get(key); }
        public void save(String key, String value) { store.put(key, value); }
        public void promptText(String title, String initial, TextCallback cb) { cb.done(initial + "!"); }
        public void exit() {}
    }

    static int W = 2400, H = 1080;
    static File out;

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("icons")) {
            icons(new File(args[1]));
            return;
        }
        if (args.length > 0 && args[0].equals("sim")) {
            sim(args.length > 1 ? Integer.parseInt(args[1]) : 20);
        } else {
            out = new File(args.length > 1 ? args[1] : "shots");
            out.mkdirs();
            shots();
        }
    }

    /** Renders the launcher icon at every density into an Android res directory. */
    static void icons(File res) throws Exception {
        String[] dens = {"mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi"};
        int[] px = {48, 72, 96, 144, 192};
        for (int i = 0; i < dens.length; i++) {
            int n = px[i];
            float s = n / 48f;
            BufferedImage mask = new BufferedImage(n, n, BufferedImage.TYPE_INT_ARGB);
            AwtGfx m = new AwtGfx(mask);
            m.fillCircle(n / 2f, n / 2f, n * 0.47f, 0xFF1A2440);
            m.fillRadial(n * 0.7f, n * 0.3f, n * 0.2f, 0xFFFFE08A, 0x00FFE08A);
            m.fillCircle(n * 0.7f, n * 0.3f, n * 0.08f, 0xFFFFE08A);
            float[] xs = new float[22], ys = new float[22];
            for (int k = 0; k <= 19; k++) {
                xs[k] = n * k / 19f;
                ys[k] = n * (0.68f + 0.05f * (float) Math.sin(k * 0.6));
            }
            xs[20] = n; ys[20] = n; xs[21] = 0; ys[21] = n;
            m.fillPolyVGradient(xs, ys, 22, n * 0.6f, 0xFF6B8E3A, n, 0xFF3E2A1A);
            m.save();
            m.translate(n * 0.45f, n * 0.70f);
            m.scale(s * 0.75f, s * 0.75f);
            TankArt.draw(m, 0, TankArt.COLORS[0], 0, 0, 40, 0);
            m.restore();
            m.strokeCircle(n / 2f, n / 2f, n * 0.47f, n * 0.03f, 0xFFFFB02E);
            m.dispose();
            // keep only the inside of the circle
            for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) {
                float dx = x + 0.5f - n / 2f, dy = y + 0.5f - n / 2f;
                if (dx * dx + dy * dy > (n * 0.49f) * (n * 0.49f)) mask.setRGB(x, y, 0);
            }
            File d = new File(res, "mipmap-" + dens[i]);
            d.mkdirs();
            ImageIO.write(mask, "png", new File(d, "ic_launcher.png"));
        }
    }

    // ------------------------------------------------------------------

    static void tick(Game g, int frames) {
        for (int i = 0; i < frames; i++) g.tick(1 / 60f, W, H);
    }

    static void shot(Game g, String name) throws Exception {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        AwtGfx gfx = new AwtGfx(img);
        g.tick(0, W, H);
        g.render(gfx);
        gfx.dispose();
        ImageIO.write(img, "png", new File(out, name + ".png"));
        System.out.println("wrote " + name);
    }

    @SuppressWarnings("unchecked")
    static Btn find(Game g, int id) throws Exception {
        Screen s = g.screen();
        Method m = Screen.class.getDeclaredMethod("activeButtons");
        m.setAccessible(true);
        for (Btn b : (List<Btn>) m.invoke(s)) if (b.id == id) return b;
        throw new RuntimeException("no button " + id + " on " + s.getClass().getSimpleName());
    }

    static void click(Game g, int id) throws Exception {
        Btn b = find(g, id);
        g.touch(Game.TOUCH_DOWN, 0, b.x + b.w / 2, b.y + b.h / 2);
        g.touch(Game.TOUCH_UP, 0, b.x + b.w / 2, b.y + b.h / 2);
        tick(g, 1);
    }

    static void shots() throws Exception {
        FakePlatform fp = new FakePlatform();
        Game g = new Game(fp, 7);
        tick(g, 30);
        shot(g, "01_menu");
        click(g, 1);
        Settings s = g.settings;
        s.players.clear();
        s.players.add(new Settings.PlayerSetup("Player 1", 0, 0, -1));
        String[] names = {"Rookie", "Turtle", "Berserker", "Sniper", "Overlord", "Wildcard", "Bully", "Avenger", "Tactician"};
        int[] ais = {0, 2, 4, 7, 9, 3, 5, 6, 8};
        for (int i = 0; i < 9; i++) s.players.add(new Settings.PlayerSetup(names[i], (i + 1) % 12, (i + 1) % 9, ais[i]));
        g.setScreen(new SetupScreen(g));
        tick(g, 2);
        shot(g, "02_setup_10_tanks");
        click(g, 200);
        tick(g, 10);
        shot(g, "03_tank_designer");
        click(g, 340);
        click(g, 241);
        shot(g, "04_opponent_picker");
        click(g, 450);

        // 6-tank match
        s.players.clear();
        s.players.add(new Settings.PlayerSetup("Player 1", 0, 0, -1));
        for (int i = 0; i < 5; i++) s.players.add(new Settings.PlayerSetup(names[i], (i + 2) % 12, (i + 1) % 9, ais[i]));
        s.cashIndex = 4;
        s.terrain = 2;
        g.startMatch();
        tick(g, 2);
        shot(g, "05_shop");
        click(g, 2);
        shot(g, "06_shop_defense");
        Player me = g.players.get(0);
        for (int i = 1; i < Items.COUNT; i++) me.inv[i] = 5;
        me.fuel = 300;
        click(g, 5);
        BattleScreen bs = (BattleScreen) g.screen();
        Battle b = bs.battle();
        // run until it is the human's turn
        for (int i = 0; i < 60 * 120 && !(b.isHumanAiming()); i++) {
            tick(g, 1);
            if (i % 400 == 200) shot(g, "07_ai_turn_" + i);
        }
        shot(g, "08_human_turn");
        click(g, 7);
        shot(g, "09_weapon_picker");
        click(g, 300);
        click(g, 8);
        shot(g, "10_items");
        click(g, 300);
        click(g, 10);
        shot(g, "11_pause");
        click(g, 301);

        // every weapon in action on its own fresh battlefield
        for (int wpn = 0; wpn < Items.FIRST_DEFENSE; wpn++) {
            weaponShot(fp, wpn);
        }

        // score screens
        g.setScreen(new ScoreScreen(g, false));
        tick(g, 5);
        shot(g, "30_scores");
        g.setScreen(new ScoreScreen(g, true));
        tick(g, 30);
        shot(g, "31_final");
        for (int p = 0; p < 4; p++) {
            HelpScreen hs = new HelpScreen(g);
            g.setScreen(hs);
            for (int k = 0; k < p; k++) click(g, 3);
            shot(g, "40_help_" + p);
        }
        // smaller, 16:9 and 4:3-ish screens
        W = 1280;
        H = 720;
        g.setScreen(new SetupScreen(g));
        tick(g, 2);
        shot(g, "50_setup_720p");
        g.setScreen(new BattleScreen(g));
        tick(g, 90);
        shot(g, "51_battle_720p");
        W = 1600;
        H = 1200;
        g.setScreen(new BattleScreen(g));
        tick(g, 90);
        shot(g, "52_battle_4x3");
        W = 2400;
        H = 1080;
    }

    static void weaponShot(FakePlatform fp, int wpn) throws Exception {
        Game g = new Game(fp, 100 + wpn);
        g.settings = Settings.defaults();
        g.settings.players.clear();
        g.settings.players.add(new Settings.PlayerSetup("Gunner", 0, wpn % 9, -1));
        g.settings.players.add(new Settings.PlayerSetup("Target A", 7, 1, 0));
        g.settings.players.add(new Settings.PlayerSetup("Target B", 4, 2, 0));
        g.settings.terrain = 1;
        g.settings.wind = 0;
        g.settings.walls = 0;
        g.startMatch();
        tick(g, 1);
        click(g, 5);
        BattleScreen bs = (BattleScreen) g.screen();
        Battle b = bs.battle();
        Player me = g.players.get(0);
        // force the gunner to go first
        while (b.active.p != me) b.nextTurn();
        b.phase = Battle.PH_AIM;
        me.inv[wpn] = 3;
        me.inv[Items.SHIELD_D] = 1;
        Tank t = me.tank;
        t.weapon = wpn;
        // aim at the nearest enemy using the AI solver with perfect skill
        Tank target = null;
        for (Tank o : b.tanks) if (o != t && (target == null || Math.abs(o.x - t.x) < Math.abs(target.x - t.x))) target = o;
        if (wpn == Items.SINGULARITY || wpn == Items.SUNBURST) {
            // keep the gunner safe from its own blast
            for (Tank o : b.tanks) if (o != t && (Math.abs(o.x - t.x) > Math.abs(target.x - t.x))) target = o;
        }
        me.ai = 9;
        Ai.Brain brain = new Ai.Brain(b, t);
        me.ai = -1;
        float tx = wpn == Items.TELEPORT ? (t.x + target.x) / 2 : target.x;
        if (!brain.aimAt(tx, target.y - 6, Items.ALL[wpn], target)) {
            brain.goalAngle = 60;
            brain.goalPower = 600;
        }
        t.angle = brain.goalAngle;
        t.power = brain.goalPower;
        String nm = Items.ALL[wpn].name.replace(' ', '_');
        b.fire();
        int frames = 0;
        boolean shotTaken = false;
        float lastHp = sumHp(b);
        while (b.phase == Battle.PH_ACTION && frames < 60 * 30) {
            tick(g, 1);
            frames++;
            if (!shotTaken && (!b.effects.isEmpty() || frames == 140)) {
                // capture a little after the first effect appears
                tick(g, wpn == Items.SINGULARITY ? 50 : (wpn == Items.LAVA ? 40 : 8));
                shot(g, String.format("2%d_weapon_%02d_%s", wpn / 10, wpn, nm));
                shotTaken = true;
            }
        }
        System.out.println("   " + nm + ": action took " + frames + " frames, damage dealt " + (lastHp - sumHp(b)) +
                ", gunner earned $" + me.roundEarn);
        if (frames >= 60 * 30) throw new RuntimeException("weapon " + nm + " never finished");
    }

    static float sumHp(Battle b) {
        float s = 0;
        for (Tank t : b.tanks) if (t.p.index != 0) s += Math.max(0, t.health) + t.shieldHp;
        return s;
    }

    // ------------------------------------------------------------------

    static void sim(int matches) throws Exception {
        FakePlatform fp = new FakePlatform();
        Random r = new Random(99);
        int[] played = new int[10], won = new int[10], roundsWon = new int[10], roundsPlayed = new int[10];
        long worstTick = 0;
        int totalRounds = 0, totalTurns = 0, stalemates = 0;
        BufferedImage img = new BufferedImage(960, 432, BufferedImage.TYPE_INT_ARGB);
        for (int m = 0; m < matches; m++) {
            Game g = new Game(fp, r.nextLong());
            Settings s = g.settings;
            s.players.clear();
            int n = 2 + r.nextInt(9);
            for (int i = 0; i < n; i++) {
                int ai = r.nextInt(10);
                s.players.add(new Settings.PlayerSetup(Ai.PROFILES[ai].name, i % 12, i % 9, ai));
            }
            s.rounds = 3;
            s.cashIndex = r.nextInt(Settings.CASH_OPTIONS.length);
            s.terrain = r.nextInt(Settings.TERRAIN_NAMES.length);
            s.wind = r.nextInt(4);
            s.walls = r.nextInt(3);
            s.aiSpeed = 2;
            g.startMatch();
            long frames = 0;
            int lastRound = 0;
            while (!(g.screen() instanceof ScoreScreen && g.round >= s.rounds) && !(g.screen() instanceof MenuScreen)) {
                if (g.screen() instanceof BattleScreen) {
                    Battle bb = ((BattleScreen) g.screen()).battle();
                    if (bb.brain != null && bb.brain.planning && !bb.brain.planned) Thread.sleep(1);
                }
                long t0 = System.nanoTime();
                g.tick(1 / 30f, 960, 432);
                long dt = System.nanoTime() - t0;
                if (frames > 10) worstTick = Math.max(worstTick, dt);
                frames++;
                if (frames % 500 == 0) {
                    AwtGfx gfx = new AwtGfx(img);
                    g.render(gfx);
                    gfx.dispose();
                }
                if (g.screen() instanceof BattleScreen) {
                    Battle b = ((BattleScreen) g.screen()).battle();
                    if (b.turnCount > 400) {
                        System.out.println("walls=" + s.walls + " wind=" + b.wind + " terrain=" + s.terrain);
                        for (Tank t : b.tanks) System.out.println("  " + t.p.name + " alive=" + t.alive + " hp=" + t.health
                                + " x=" + t.x + " y=" + t.y + " buried=" + b.buried(t) + " lastImpact=" + t.lastImpactX + " shield=" + t.shieldHp);
                        throw new RuntimeException("round never ends: match " + m);
                    }
                }
                if (frames > 30L * 60 * 60 && g.screen() instanceof BattleScreen) {
                    Battle b = ((BattleScreen) g.screen()).battle();
                    System.out.println("phase=" + b.phase + " active=" + b.active.p.name + " brain=" + (b.brain == null ? "null" : b.brain.stage + " planned=" + b.brain.planned)
                            + " projs=" + b.projs.size() + " effects=" + b.effects.size() + " falling=" + b.active.falling + " busy=" + b.busy());
                    for (Fx f : b.effects) System.out.println("  fx " + f.getClass().getSimpleName() + " t=" + f.t + " blocking=" + f.blocking());
                    for (Projectile p : b.projs) System.out.println("  proj kind=" + p.kind + " item=" + p.item.name + " x=" + p.x + " y=" + p.y + " vx=" + p.vx + " vy=" + p.vy);
                    for (Tank t : b.tanks) if (t.alive) System.out.println("  tank " + t.p.name + " falling=" + t.falling + " y=" + t.y + " sup=" + b.support(t.x) + " vy=" + t.vy);
                }
                if (frames > 30L * 60 * 60) throw new RuntimeException("match " + m + " stalled on " + g.screen().getClass().getSimpleName());
                if (g.round != lastRound && g.screen() instanceof BattleScreen) {
                    lastRound = g.round;
                }
                if (g.screen() instanceof ScoreScreen && lastRound > 0) {
                    // count once per round
                    if (lastRound != -g.round) {
                        totalRounds++;
                        boolean any = false;
                        for (Player p : g.players) {
                            roundsPlayed[p.ai]++;
                            if (p.roundWinner) {
                                roundsWon[p.ai]++;
                                any = true;
                            }
                        }
                        if (!any) stalemates++;
                        lastRound = -g.round;
                    }
                }
            }
            Player best = null;
            for (Player p : g.players) {
                played[p.ai]++;
                if (best == null || p.score > best.score) best = p;
            }
            won[best.ai]++;
            System.out.printf("match %2d: %d tanks, %s won (score %d)  [%d s game time]%n", m, n, best.name,
                    Math.round(best.score), frames / 30);
        }
        System.out.println();
        System.out.printf("%-14s %8s %10s %12s%n", "AI", "matches", "match win%", "round win%");
        for (int i = 0; i < 10; i++) {
            System.out.printf("%-14s %8d %9.0f%% %11.0f%%%n", Ai.PROFILES[i].name, played[i],
                    played[i] == 0 ? 0 : 100.0 * won[i] / played[i],
                    roundsPlayed[i] == 0 ? 0 : 100.0 * roundsWon[i] / roundsPlayed[i]);
        }
        System.out.printf("rounds %d, no-winner rounds %d, worst tick %.1f ms%n", totalRounds, stalemates, worstTick / 1e6);
    }
}
