package com.meanmutton.tankgame.core;

/** Services the host (Android or desktop harness) provides to the game. */
public interface Platform {
    int SND_FIRE = 0, SND_BOOM_S = 1, SND_BOOM_L = 2, SND_BOUNCE = 3, SND_ZAP = 4, SND_CLICK = 5,
            SND_BUY = 6, SND_FIZZ = 7, SND_WIN = 8, SND_DEATH = 9, SND_TELEPORT = 10, SND_ENGINE = 11,
            SND_SHIELD = 12, SND_COUNT = 13;

    void play(int sound, float volume);

    String load(String key);

    void save(String key, String value);

    /** Ask the user for a line of text. The callback may run on another thread; use Game.post. */
    void promptText(String title, String initial, TextCallback callback);

    void exit();

    interface TextCallback {
        void done(String text);
    }
}
