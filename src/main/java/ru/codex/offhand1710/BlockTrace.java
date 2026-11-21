package ru.codex.offhand1710;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.world.chunk.Chunk;

/**
 * Debugging aid: -Dmc112.traceBlocks=x0,z0,x1,z1[,prefix] logs, once per distinct call stack, who sets a block whose
 * registry name starts with prefix (default "hbm:") inside that block square. Called from Chunk.func_150807_a, the
 * method every World.setBlock goes through. Off (one null check) without the property.
 */
public final class BlockTrace {
    private static final int[] AREA;
    private static final String PREFIX;
    private static final Set<String> SEEN = new HashSet<String>();
    public static int hits;

    static {
        String p = System.getProperty("mc112.traceBlocks");
        int[] a = null;
        String prefix = "hbm:";
        if (p != null) {
            String[] s = p.split(",");
            a = new int[] {Integer.parseInt(s[0].trim()), Integer.parseInt(s[1].trim()), Integer.parseInt(s[2].trim()), Integer.parseInt(s[3].trim())};
            if (s.length > 4) {
                prefix = s[4].trim();
            }
        }
        AREA = a;
        PREFIX = prefix;
    }

    private BlockTrace() {
    }

    public static void onSet(Chunk chunk, int x, int y, int z, Block block) {
        if (AREA == null || block == null) {
            return;
        }
        int wx = chunk.xPosition * 16 + x, wz = chunk.zPosition * 16 + z;
        if (wx < AREA[0] || wz < AREA[1] || wx > AREA[2] || wz > AREA[3]) {
            return;
        }
        String name = Block.blockRegistry.getNameForObject(block);
        if (name == null || !name.startsWith(PREFIX)) {
            return;
        }
        hits++;
        StackTraceElement[] st = new Throwable().getStackTrace();
        StringBuilder key = new StringBuilder();
        for (int i = 1; i < Math.min(st.length, 40); i++) {
            String c = st[i].getClassName();
            if (c.startsWith("net.minecraft.") || c.startsWith("com.") || c.startsWith("cpw.") || c.startsWith("ru.")) {
                key.append("\n    at ").append(c).append('.').append(st[i].getMethodName());
            }
        }
        String k = key.toString();
        synchronized (SEEN) {
            if (SEEN.add(k)) {
                org.apache.logging.log4j.LogManager.getLogger("mc112bridge").info("TRACE " + name + " at " + wx + "," + y + "," + wz + k);
            }
        }
    }
}
