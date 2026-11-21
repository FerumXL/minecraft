package ru.codex.offhand1710;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Block states of 1.12.2 for 1.7.10 blocks and metadata, from data/states112.tsv (taken from the 1.12.2 jar:
 * Block.getStateFromMeta for every block and metadata 0..15; "c" marks the metadata getMetaFromState gives back).
 * Structure files use it to write the Name and Properties a 1.12.2 template has, to read 1.12.2 templates, and to
 * mirror and rotate blocks by their properties (IBlockState.withMirror / withRotation of 1.12.2).
 */
public final class States112 {
    /** "minecraft:log#4" -> {"axis":"x","variant":"oak"} */
    private static final Map<String, Map<String, String>> PROPS = new HashMap<String, Map<String, String>>();
    /** "minecraft:log|axis=x,variant=oak" -> 4 (canonical metadata only) */
    private static final Map<String, Integer> META = new HashMap<String, Integer>();
    private static boolean loaded;
    /** name -> the properties that change with the metadata (the rest come from neighbours in 1.12.2) */
    private static final Map<String, java.util.Set<String>> STORED = new HashMap<String, java.util.Set<String>>();
    private static final Map<String, Map<String, String>> FIRST = new HashMap<String, Map<String, String>>();

    private States112() {
    }

    private static synchronized void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        try {
            InputStream in = States112.class.getResourceAsStream("/assets/mc112bridge/data/states112.tsv");
            BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String line;
            while ((line = r.readLine()) != null) {
                String[] f = line.split("\t", -1);
                if (f.length < 5) {
                    continue;
                }
                Map<String, String> p = new TreeMap<String, String>();
                if (!f[3].isEmpty()) {
                    for (String kv : f[3].split(",")) {
                        int eq = kv.indexOf('=');
                        p.put(kv.substring(0, eq), kv.substring(eq + 1));
                    }
                }
                PROPS.put(f[0] + "#" + f[2], p);
                Map<String, String> first = FIRST.get(f[0]);
                if (first == null) {
                    FIRST.put(f[0], p);
                    STORED.put(f[0], new java.util.HashSet<String>());
                } else {
                    for (Map.Entry<String, String> e : p.entrySet()) {
                        if (!e.getValue().equals(first.get(e.getKey()))) {
                            STORED.get(f[0]).add(e.getKey());
                        }
                    }
                }
                if ("c".equals(f[4])) {
                    META.put(f[0] + "|" + key(p), Integer.valueOf(f[2]));
                }
            }
            r.close();
        } catch (Exception e) {
            org.apache.logging.log4j.LogManager.getLogger("mc112bridge").warn("states112.tsv not read: " + e);
        }
    }

    private static String key(Map<String, String> p) {
        StringBuilder b = new StringBuilder();
        for (Map.Entry<String, String> e : new TreeMap<String, String>(p).entrySet()) {
            if (b.length() > 0) {
                b.append(',');
            }
            b.append(e.getKey()).append('=').append(e.getValue());
        }
        return b.toString();
    }

    /** The 1.12.2 name of a 1.7.10 block: its own vanilla name, or minecraft:x for a block of this mod named x. */
    public static String name112(Block b, int meta) {
        load();
        String n = Block.blockRegistry.getNameForObject(b);
        if (n == null) {
            return null;
        }
        if (PROPS.containsKey(n + "#" + meta)) {
            return n;
        }
        if (n.startsWith(MC112Bridge.MOD_ID + ":")) {
            String v = "minecraft:" + n.substring(MC112Bridge.MOD_ID.length() + 1);
            if (PROPS.containsKey(v + "#" + meta)) {
                return v;
            }
        }
        return null;
    }

    public static Map<String, String> props(String name112, int meta) {
        load();
        Map<String, String> p = PROPS.get(name112 + "#" + meta);
        return p == null ? null : new TreeMap<String, String>(p);
    }

    /** The 1.7.10 block for a 1.12.2 name: the vanilla block, or this mod's block of that path. */
    public static Block block17(String name112) {
        Block b = Block.getBlockFromName(name112);
        if (b != null && b != net.minecraft.init.Blocks.air || "minecraft:air".equals(name112)) {
            return b;
        }
        if (name112.startsWith("minecraft:")) {
            return Block.getBlockFromName(MC112Bridge.MOD_ID + ":" + name112.substring(10));
        }
        return null;
    }

    /** The metadata of a 1.12.2 state, or -1 when the table does not have it. */
    public static int meta(String name112, Map<String, String> props) {
        load();
        Integer m = META.get(name112 + "|" + key(props));
        return m == null ? -1 : m.intValue();
    }

    /** Whether 1.12.2 keeps this property in the metadata of this block. */
    public static boolean stored(String name112, String prop) {
        load();
        java.util.Set<String> s = STORED.get(name112);
        return s != null && s.contains(prop);
    }

    public static NBTTagCompound propsTag(Map<String, String> p) {
        NBTTagCompound t = new NBTTagCompound();
        for (Map.Entry<String, String> e : p.entrySet()) {
            t.setString(e.getKey(), e.getValue());
        }
        return t;
    }

    public static Map<String, String> fromTag(NBTTagCompound t) {
        Map<String, String> p = new TreeMap<String, String>();
        for (Object k : t.func_150296_c()) {
            p.put((String) k, t.getString((String) k));
        }
        return p;
    }

    // ---------------------------------------------------------------------------------- mirror and rotation

    private static final String[] HORIZONTAL = {"south", "west", "north", "east"};

    private static String rotFacing(String f, int quarter) {
        for (int i = 0; i < 4; i++) {
            if (HORIZONTAL[i].equals(f)) {
                return HORIZONTAL[(i + quarter) & 3];
            }
        }
        return f;
    }

    /**
     * IBlockState.withMirror then withRotation of 1.12.2 on the common properties: facing (horizontal), axis,
     * rotation (0-15 of signs and banners), hinge and stair shape under a mirror. mirror: 0 none, 1 LEFT_RIGHT (z),
     * 2 FRONT_BACK (x); rotation: quarter turns clockwise.
     */
    public static Map<String, String> transform(Map<String, String> in, int mirror, int rotation) {
        Map<String, String> p = new TreeMap<String, String>(in);
        if (mirror != 0) {
            String f = p.get("facing");
            if (f != null) {
                if (mirror == 1 && (f.equals("north") || f.equals("south"))) {
                    p.put("facing", f.equals("north") ? "south" : "north");
                } else if (mirror == 2 && (f.equals("east") || f.equals("west"))) {
                    p.put("facing", f.equals("east") ? "west" : "east");
                }
            }
            String r = p.get("rotation");
            if (r != null) {
                int v = Integer.parseInt(r);
                p.put("rotation", String.valueOf(mirror == 1 ? (16 - v) % 16 : (24 - v) % 16));
            }
            String h = p.get("hinge");
            if (h != null) {
                p.put("hinge", h.equals("left") ? "right" : "left");
            }
            String s = p.get("shape");
            if (s != null && (s.startsWith("inner_") || s.startsWith("outer_"))) {
                p.put("shape", s.endsWith("left") ? s.replace("left", "right") : s.replace("right", "left"));
            }
        }
        if (rotation != 0) {
            String f = p.get("facing");
            if (f != null) {
                p.put("facing", rotFacing(f, rotation));
            }
            String a = p.get("axis");
            if (a != null && (rotation & 1) == 1) {
                p.put("axis", a.equals("x") ? "z" : a.equals("z") ? "x" : a);
            }
            String r = p.get("rotation");
            if (r != null) {
                p.put("rotation", String.valueOf((Integer.parseInt(r) + rotation * 4) % 16));
            }
            for (String side : new String[] {"north", "east", "south", "west"}) {
                // fences and panes keep their connections in the actual state, not in the stored one
                if (p.containsKey(side) && !p.containsKey("facing")) {
                    break;
                }
            }
        }
        return p;
    }

    public static List<String> names() {
        load();
        List<String> out = new ArrayList<String>();
        for (String k : PROPS.keySet()) {
            out.add(k);
        }
        return out;
    }
}
