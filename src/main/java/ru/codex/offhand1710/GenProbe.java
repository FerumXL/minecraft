package ru.codex.offhand1710;

import java.io.File;

import cpw.mods.fml.common.FMLLog;

/**
 * Diagnostic only: when mc112bridge-genprobe.txt exists in the game directory, logs every 200 chunks how long the
 * world generators of this mod took and how fast chunks were being generated overall.
 */
final class GenProbe {
    static final boolean ENABLED = new File("mc112bridge-genprobe.txt").exists();
    private static final int REPORT_EVERY = 200;

    private static long stoneNanos;
    private static long iglooNanos;
    private static int chunks;
    private static long windowStart = System.nanoTime();

    private GenProbe() {
    }

    static void stone(long nanos) {
        stoneNanos += nanos;
        chunks++;
        if (chunks >= REPORT_EVERY) {
            long now = System.nanoTime();
            FMLLog.info("[mc112bridge gen probe] %d chunks in %d ms: stone veins %.2f ms/chunk, igloo %.3f ms/chunk",
                    Integer.valueOf(chunks), Long.valueOf((now - windowStart) / 1000000L),
                    Double.valueOf(stoneNanos / 1000000.0D / chunks), Double.valueOf(iglooNanos / 1000000.0D / chunks));
            stoneNanos = 0L;
            iglooNanos = 0L;
            chunks = 0;
            windowStart = now;
        }
    }

    static void igloo(long nanos) {
        iglooNanos += nanos;
    }
}
