package com.meanmutton.tankgame.core;

import java.util.Random;

/** Generates every sound effect procedurally (16-bit mono PCM), so the game ships no audio files. */
public final class SoundSynth {
    public static final int RATE = 22050;

    private SoundSynth() {}

    public static short[] generate(int id) {
        Random r = new Random(1234 + id);
        switch (id) {
            case Platform.SND_FIRE: return thump(r, 0.32f, 140, 45, 0.55f);
            case Platform.SND_BOOM_S: return boom(r, 0.55f, 0.9f, 900);
            case Platform.SND_BOOM_L: return boom(r, 1.3f, 1.0f, 500);
            case Platform.SND_DEATH: return boom(r, 1.6f, 1.0f, 380);
            case Platform.SND_BOUNCE: return tone(0.12f, 520, 260, 0.5f, false);
            case Platform.SND_ZAP: return zap(r);
            case Platform.SND_CLICK: return tone(0.04f, 1400, 1100, 0.35f, false);
            case Platform.SND_BUY: return chord(new float[]{988, 1319}, 0.09f, 0.45f);
            case Platform.SND_FIZZ: return fizz(r);
            case Platform.SND_WIN: return chord(new float[]{523, 659, 784, 1047}, 0.13f, 0.5f);
            case Platform.SND_TELEPORT: return tone(0.45f, 300, 1800, 0.45f, true);
            case Platform.SND_ENGINE: return engine(r);
            case Platform.SND_SHIELD: return tone(0.3f, 400, 900, 0.4f, true);
            default: return new short[100];
        }
    }

    private static short[] out(float[] f) {
        short[] s = new short[f.length];
        for (int i = 0; i < f.length; i++) {
            float v = Math.max(-1f, Math.min(1f, f[i]));
            s[i] = (short) (v * 32000);
        }
        return s;
    }

    private static short[] thump(Random r, float dur, float f0, float f1, float vol) {
        int n = (int) (dur * RATE);
        float[] f = new float[n];
        double ph = 0;
        float lp = 0;
        for (int i = 0; i < n; i++) {
            float t = i / (float) n;
            float freq = f0 + (f1 - f0) * t;
            ph += 2 * Math.PI * freq / RATE;
            float env = (float) Math.exp(-t * 6);
            float noise = r.nextFloat() * 2 - 1;
            lp += (noise - lp) * 0.3f;
            f[i] = (float) (Math.sin(ph) * 0.8 + lp * 0.6 * Math.exp(-t * 14)) * env * vol;
        }
        return out(f);
    }

    private static short[] boom(Random r, float dur, float vol, float cutoff) {
        int n = (int) (dur * RATE);
        float[] f = new float[n];
        float lp1 = 0, lp2 = 0;
        double ph = 0;
        for (int i = 0; i < n; i++) {
            float t = i / (float) n;
            float noise = r.nextFloat() * 2 - 1;
            float a = (float) (cutoff * (1 - t * 0.7) / RATE * 6.28);
            lp1 += (noise - lp1) * a;
            lp2 += (lp1 - lp2) * a;
            ph += 2 * Math.PI * (60 - 30 * t) / RATE;
            float attack = Math.min(1f, i / (RATE * 0.004f));
            float env = (float) Math.exp(-t * 4.5) * attack;
            f[i] = (lp2 * 3.2f + (float) Math.sin(ph) * 0.5f * (float) Math.exp(-t * 8)) * env * vol;
        }
        return out(f);
    }

    private static short[] tone(float dur, float f0, float f1, float vol, boolean wobble) {
        int n = (int) (dur * RATE);
        float[] f = new float[n];
        double ph = 0;
        for (int i = 0; i < n; i++) {
            float t = i / (float) n;
            float freq = f0 + (f1 - f0) * t;
            if (wobble) freq *= 1 + 0.08f * (float) Math.sin(t * 60);
            ph += 2 * Math.PI * freq / RATE;
            float env = Math.min(1f, i / (RATE * 0.003f)) * (1 - t);
            f[i] = (float) Math.sin(ph) * env * vol;
        }
        return out(f);
    }

    private static short[] chord(float[] notes, float step, float vol) {
        int n = (int) ((step * notes.length + 0.25f) * RATE);
        float[] f = new float[n];
        for (int k = 0; k < notes.length; k++) {
            int start = (int) (k * step * RATE);
            double ph = 0;
            for (int i = start; i < n; i++) {
                float t = (i - start) / (float) RATE;
                ph += 2 * Math.PI * notes[k] / RATE;
                float env = (float) Math.exp(-t * 7) * Math.min(1f, t / 0.005f);
                f[i] += ((float) Math.sin(ph) + 0.3f * (float) Math.sin(ph * 2)) * env * vol / 1.6f;
            }
        }
        return out(f);
    }

    private static short[] zap(Random r) {
        int n = (int) (0.25f * RATE);
        float[] f = new float[n];
        double ph = 0;
        for (int i = 0; i < n; i++) {
            float t = i / (float) n;
            ph += 2 * Math.PI * (2400 * (1 - t) + 200) / RATE;
            float sq = Math.sin(ph) > 0 ? 1 : -1;
            f[i] = (sq * 0.3f + (r.nextFloat() - 0.5f) * 0.3f) * (1 - t) * 0.6f;
        }
        return out(f);
    }

    private static short[] fizz(Random r) {
        int n = (int) (0.8f * RATE);
        float[] f = new float[n];
        float hp = 0, prev = 0;
        for (int i = 0; i < n; i++) {
            float t = i / (float) n;
            float noise = r.nextFloat() * 2 - 1;
            hp = 0.7f * (hp + noise - prev);
            prev = noise;
            float crackle = r.nextFloat() < 0.01f ? (r.nextFloat() - 0.5f) * 2 : 0;
            f[i] = (hp * 0.35f + crackle) * (1 - t) * 0.7f;
        }
        return out(f);
    }

    private static short[] engine(Random r) {
        int n = (int) (0.22f * RATE);
        float[] f = new float[n];
        double ph = 0;
        for (int i = 0; i < n; i++) {
            float t = i / (float) n;
            ph += 2 * Math.PI * 55 / RATE;
            float saw = (float) ((ph / (2 * Math.PI)) % 1.0) * 2 - 1;
            float env = (float) Math.sin(Math.PI * t);
            f[i] = (saw * 0.5f + (r.nextFloat() - 0.5f) * 0.2f) * env * 0.5f;
        }
        return out(f);
    }

    /** Wraps PCM in a WAV container. */
    public static byte[] wav(short[] pcm) {
        int dataLen = pcm.length * 2;
        byte[] b = new byte[44 + dataLen];
        writeStr(b, 0, "RIFF");
        writeInt(b, 4, 36 + dataLen);
        writeStr(b, 8, "WAVE");
        writeStr(b, 12, "fmt ");
        writeInt(b, 16, 16);
        writeShort(b, 20, 1);
        writeShort(b, 22, 1);
        writeInt(b, 24, RATE);
        writeInt(b, 28, RATE * 2);
        writeShort(b, 32, 2);
        writeShort(b, 34, 16);
        writeStr(b, 36, "data");
        writeInt(b, 40, dataLen);
        for (int i = 0; i < pcm.length; i++) writeShort(b, 44 + i * 2, pcm[i]);
        return b;
    }

    private static void writeStr(byte[] b, int o, String s) {
        for (int i = 0; i < s.length(); i++) b[o + i] = (byte) s.charAt(i);
    }

    private static void writeInt(byte[] b, int o, int v) {
        b[o] = (byte) v;
        b[o + 1] = (byte) (v >> 8);
        b[o + 2] = (byte) (v >> 16);
        b[o + 3] = (byte) (v >> 24);
    }

    private static void writeShort(byte[] b, int o, int v) {
        b[o] = (byte) v;
        b[o + 1] = (byte) (v >> 8);
    }
}
