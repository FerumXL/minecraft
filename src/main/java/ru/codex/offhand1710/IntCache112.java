package ru.codex.offhand1710;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;

/**
 * net.minecraft.world.gen.layer.IntCache with one cache per thread (the coremod sends the three static methods here).
 * The vanilla cache is one static pool: resetIntCache on one thread hands the arrays another thread is still filling
 * to the next caller. In single player the client thread asks the biome layers for chunks it has not received
 * (WorldClient.getBiomeGenForCoords of an unloaded chunk) while the integrated server generates terrain, and the
 * server then reads garbage biome ids ("Biome ID is out of bounds: 267973" / -1, or a null biome that crashes a world
 * generator). The logic per thread is the vanilla one.
 */
public final class IntCache112 {
    private static final class State {
        int size = 256;
        final List<int[]> freeSmall = new ArrayList<int[]>();
        final List<int[]> inUseSmall = new ArrayList<int[]>();
        final List<int[]> freeLarge = new ArrayList<int[]>();
        final List<int[]> inUseLarge = new ArrayList<int[]>();
    }

    private static final ThreadLocal<State> STATE = new ThreadLocal<State>() {
        @Override
        protected State initialValue() {
            return new State();
        }
    };

    private static final Set<String> SEEN = new HashSet<String>();

    private IntCache112() {
    }

    /** IntCache.getIntCache. */
    public static int[] get(int n) {
        State s = STATE.get();
        note();
        if (n <= 256) {
            int[] a = s.freeSmall.isEmpty() ? new int[256] : s.freeSmall.remove(s.freeSmall.size() - 1);
            s.inUseSmall.add(a);
            return a;
        }
        if (n > s.size) {
            s.size = n;
            s.freeLarge.clear();
            s.inUseLarge.clear();
            int[] a = new int[s.size];
            s.inUseLarge.add(a);
            return a;
        }
        int[] a = s.freeLarge.isEmpty() ? new int[s.size] : s.freeLarge.remove(s.freeLarge.size() - 1);
        s.inUseLarge.add(a);
        return a;
    }

    /** IntCache.resetIntCache. */
    public static void reset() {
        State s = STATE.get();
        if (!s.freeLarge.isEmpty()) {
            s.freeLarge.remove(s.freeLarge.size() - 1);
        }
        if (!s.freeSmall.isEmpty()) {
            s.freeSmall.remove(s.freeSmall.size() - 1);
        }
        s.freeLarge.addAll(s.inUseLarge);
        s.freeSmall.addAll(s.inUseSmall);
        s.inUseLarge.clear();
        s.inUseSmall.clear();
    }

    /** IntCache.getCacheSizes (crash reports). */
    public static String sizes() {
        State s = STATE.get();
        return "cache: " + s.freeLarge.size() + ", tcache: " + s.freeSmall.size() + ", allocated: " + s.inUseLarge.size() + ", tallocated: " + s.inUseSmall.size();
    }

    /** Logs once per thread name which threads use the biome layers (the second one is the one that raced before). */
    private static void note() {
        String name = Thread.currentThread().getName();
        synchronized (SEEN) {
            if (!SEEN.add(name)) {
                return;
            }
        }
        StringBuilder b = new StringBuilder();
        StackTraceElement[] st = Thread.currentThread().getStackTrace();
        int i = 3;
        while (i < st.length && st[i].getClassName().startsWith("net.minecraft.world.gen.layer.")) {
            i++;
        }
        for (int k = i; k < Math.min(st.length, i + 14); k++) {
            b.append(" <- ").append(st[k].getClassName()).append('.').append(st[k].getMethodName());
        }
        LogManager.getLogger("mc112bridge").info("IntCache used by thread \"" + name + "\" (own cache):" + b);
    }
}
