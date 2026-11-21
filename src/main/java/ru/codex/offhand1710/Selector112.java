package ru.codex.offhand1710;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * EntitySelector of 1.12.2: @p @a @r @e @s with r, rm, l, lm, x, y, z, dx, dy, dz, rx, rxm, ry, rym, c, m, team,
 * name, type, tag, score_NAME and score_NAME_min, "!" for the negations. The coremod sends PlayerSelector of
 * 1.7.10 here, and the loop of CommandHandler gets every matched entity (a player by name, any other by UUID), so
 * every command takes @e as in 1.12.2. Off with -Dmc112.selectors17=1 (asm side).
 */
public final class Selector112 {
    public static final boolean OLD = System.getProperty("mc112.selectors17") != null;
    private static final Pattern TOKEN = Pattern.compile("^@([pares])(?:\\[([^ ]*)\\])?$");
    private static final String[] KEYS = {"r", "rm", "l", "lm", "x", "y", "z", "dx", "dy", "dz", "rx", "rxm", "ry", "rym", "c", "m", "team", "name", "type", "tag"};
    /** Most tags an entity takes (Entity.addTag). */
    public static final int MAX_TAGS = 1024;

    private Selector112() {
    }

    public static boolean isSelector(String s) {
        return s != null && TOKEN.matcher(s).matches();
    }

    private static Map<String, String> args(String token, String inner) {
        Map<String, String> m = new HashMap<String, String>();
        if (inner == null || inner.isEmpty()) {
            return m;
        }
        for (String part : inner.split(",", -1)) {
            int eq = part.indexOf('=');
            String k = eq < 0 ? part : part.substring(0, eq);
            boolean known = k.startsWith("score_") && k.length() > 6;
            for (String key : KEYS) {
                known |= key.equals(k);
            }
            if (eq < 0 || !known) {
                throw new CommandException("commands.generic.selector_argument", token);
            }
            m.put(k, part.substring(eq + 1));
        }
        return m;
    }

    private static int intArg(Map<String, String> m, String key, int def) {
        String v = m.get(key);
        if (v == null) {
            return def;
        }
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /** EntitySelector.getCoordinate: a given block coordinate is the middle of the block on x and z. */
    private static double coord(Map<String, String> m, String key, double def, boolean centre) {
        return m.containsKey(key) ? intArg(m, key, MathHelper.floor_double(def)) + (centre ? 0.5D : 0.0D) : def;
    }

    private static Entity senderEntity(ICommandSender sender) {
        if (sender instanceof Entity) {
            return (Entity) sender;
        }
        return Content120.executor;
    }

    /** The name an entity has on the scoreboard: a player's name, any other entity's UUID. */
    public static String holder(Entity e) {
        return e instanceof EntityPlayer ? e.getCommandSenderName() : e.getUniqueID().toString();
    }

    public static List<String> tags(Entity e) {
        List<String> out = new ArrayList<String>();
        NBTTagList l = e.getEntityData().getTagList("Tags", 8);
        for (int i = 0; i < l.tagCount(); i++) {
            out.add(l.getStringTagAt(i));
        }
        return out;
    }

    public static void setTags(Entity e, List<String> tags) {
        NBTTagList l = new NBTTagList();
        for (String t : tags) {
            l.appendTag(new NBTTagString(t));
        }
        e.getEntityData().setTag("Tags", l);
    }

    private static boolean typeKnown(String t) {
        String n = t.startsWith("minecraft:") ? t.substring(10) : t;
        if ("player".equals(n)) {
            return true;
        }
        if (Data112.entityClassName("minecraft:" + n) != null) {
            return true;
        }
        for (Object k : EntityList.stringToClassMapping.keySet()) {
            String s = String.valueOf(k);
            if (s.equalsIgnoreCase(n) || s.equalsIgnoreCase(t) || s.replaceAll("([a-z])([A-Z])", "$1_$2").equalsIgnoreCase(n)) {
                return true;
            }
        }
        return t.indexOf(':') > 0 || t.indexOf('.') > 0;
    }

    /** The entities a token stands for, of the given class (EntitySelector.matchEntities). */
    public static <T extends Entity> List<T> select(ICommandSender sender, String token, Class<T> cls) {
        List<T> out = new ArrayList<T>();
        Matcher mt = TOKEN.matcher(token);
        if (!mt.matches()) {
            return out;
        }
        final String kind = mt.group(1);
        Map<String, String> a = args(token, mt.group(2));
        // the place the selection is measured from
        ChunkCoordinates cc = sender.getPlayerCoordinates();
        double px = cc.posX + 0.5D;
        double py = cc.posY + 0.5D;
        double pz = cc.posZ + 0.5D;
        Entity self = senderEntity(sender);
        if (self != null && (sender == self || !(sender instanceof net.minecraft.tileentity.TileEntity))) {
            px = self.posX;
            py = self.posY;
            pz = self.posZ;
        }
        final double ox = coord(a, "x", px, true);
        final double oy = coord(a, "y", py, false);
        final double oz = coord(a, "z", pz, true);
        String type = a.get("type");
        boolean typeNot = type != null && type.startsWith("!");
        if (typeNot) {
            type = type.substring(1);
        }
        if (type != null && !typeKnown(type)) {
            throw new CommandException("commands.generic.entity.invalidType", type);
        }
        boolean typed = "e".equals(kind) || "r".equals(kind) || "s".equals(kind);
        boolean playersOnly = !typed || "r".equals(kind) && type == null || EntityPlayer.class.isAssignableFrom(cls)
            || a.containsKey("l") || a.containsKey("lm") || a.containsKey("m");
        List<Entity> pool = new ArrayList<Entity>();
        if ("s".equals(kind)) {
            if (self != null) {
                pool.add(self);
            }
        } else {
            boolean local = a.containsKey("x") || a.containsKey("y") || a.containsKey("z") || a.containsKey("dx") || a.containsKey("dy")
                || a.containsKey("dz") || a.containsKey("r") || a.containsKey("rm");
            List<World> worlds = new ArrayList<World>();
            if (local) {
                worlds.add(sender.getEntityWorld());
            } else {
                for (WorldServer w : MinecraftServer.getServer().worldServers) {
                    worlds.add(w);
                }
            }
            for (World w : worlds) {
                for (Object o : playersOnly ? w.playerEntities : w.loadedEntityList) {
                    pool.add((Entity) o);
                }
            }
        }
        String name = a.get("name");
        String team = a.get("team");
        String tag = a.get("tag");
        String mode = a.get("m");
        int r = intArg(a, "r", -1);
        int rm = intArg(a, "rm", -1);
        int l = intArg(a, "l", Integer.MAX_VALUE);
        int lm = intArg(a, "lm", Integer.MIN_VALUE);
        AxisAlignedBB box = null;
        if (a.containsKey("dx") || a.containsKey("dy") || a.containsKey("dz")) {
            int bx = MathHelper.floor_double(ox);
            int by = MathHelper.floor_double(oy);
            int bz = MathHelper.floor_double(oz);
            int dx = intArg(a, "dx", 0);
            int dy = intArg(a, "dy", 0);
            int dz = intArg(a, "dz", 0);
            box = AxisAlignedBB.getBoundingBox(bx + Math.min(0, dx), by + Math.min(0, dy), bz + Math.min(0, dz),
                bx + Math.max(0, dx) + 1, by + Math.max(0, dy) + 1, bz + Math.max(0, dz) + 1);
        }
        boolean yaw = a.containsKey("ry") || a.containsKey("rym");
        int yMin = clampAngle(intArg(a, "rym", 0));
        int yMax = clampAngle(intArg(a, "ry", 359));
        boolean pitch = a.containsKey("rx") || a.containsKey("rxm");
        int pMin = clampAngle(intArg(a, "rxm", 0));
        int pMax = clampAngle(intArg(a, "rx", 359));
        Scoreboard board = MinecraftServer.getServer().worldServerForDimension(0).getScoreboard();
        for (Entity e : pool) {
            if (!cls.isInstance(e) || e.isDead && !(e instanceof EntityPlayer)) {
                continue;
            }
            if (playersOnly && !(e instanceof EntityPlayer)) {
                continue;
            }
            if (type != null && Content120.typeMatches(e, type) == typeNot) {
                continue;
            }
            if (name != null) {
                boolean neg = name.startsWith("!");
                if (e.getCommandSenderName().equals(neg ? name.substring(1) : name) == neg) {
                    continue;
                }
            }
            if (team != null) {
                boolean neg = team.startsWith("!");
                String want = neg ? team.substring(1) : team;
                if (!(e instanceof EntityLivingBase)) {
                    continue;
                }
                ScorePlayerTeam t = board.getPlayersTeam(holder(e));
                String has = t == null ? "" : t.getRegisteredName();
                if (has.equals(want) == neg) {
                    continue;
                }
            }
            if (tag != null) {
                boolean neg = tag.startsWith("!");
                String want = neg ? tag.substring(1) : tag;
                List<String> ts = tags(e);
                boolean match = want.isEmpty() ? ts.isEmpty() : ts.contains(want);
                if (match == neg) {
                    continue;
                }
            }
            if (mode != null) {
                boolean neg = mode.startsWith("!");
                int want = gameMode(neg ? mode.substring(1) : mode);
                int has = gameModeOf((EntityPlayer) e);
                if (want == -1 || (has == want) == neg) {
                    continue;
                }
            }
            if (a.containsKey("l") || a.containsKey("lm")) {
                int xp = ((EntityPlayer) e).experienceLevel;
                if (xp > l || xp < lm) {
                    continue;
                }
            }
            if (!scores(a, board, e)) {
                continue;
            }
            double d = e.getDistanceSq(ox, oy, oz);
            if (r >= 0 && d > (double) r * r || rm >= 0 && d < (double) rm * rm) {
                continue;
            }
            if (box != null && !box.intersectsWith(e.boundingBox)) {
                continue;
            }
            if (yaw && !inAngle(clampAngle(MathHelper.floor_float(e.rotationYaw)), yMin, yMax)) {
                continue;
            }
            if (pitch && !inAngle(clampAngle(MathHelper.floor_float(e.rotationPitch)), pMin, pMax)) {
                continue;
            }
            out.add(cls.cast(e));
        }
        if ("s".equals(kind)) {
            return out;
        }
        int count = intArg(a, "c", "a".equals(kind) || "e".equals(kind) ? 0 : 1);
        if ("r".equals(kind)) {
            Collections.shuffle(out, sender.getEntityWorld().rand);
        } else {
            Collections.sort(out, new Comparator<Entity>() {
                @Override
                public int compare(Entity p, Entity q) {
                    return Double.compare(p.getDistanceSq(ox, oy, oz), q.getDistanceSq(ox, oy, oz));
                }
            });
        }
        if (count < 0) {
            Collections.reverse(out);
        }
        if (count != 0 && out.size() > Math.abs(count)) {
            out = new ArrayList<T>(out.subList(0, Math.abs(count)));
        }
        return out;
    }

    private static boolean scores(Map<String, String> a, Scoreboard board, Entity e) {
        for (Map.Entry<String, String> en : a.entrySet()) {
            String k = en.getKey();
            if (!k.startsWith("score_")) {
                continue;
            }
            boolean min = k.endsWith("_min") && k.length() > 10;
            String objective = k.substring(6, min ? k.length() - 4 : k.length());
            ScoreObjective o = board.getObjective(objective);
            if (o == null) {
                return false;
            }
            String h = holder(e);
            @SuppressWarnings("unchecked")
            Map<ScoreObjective, Score> held = board.func_96510_d(h);
            Score s = held == null ? null : held.get(o);
            if (s == null) {
                return false;
            }
            int v;
            try {
                v = Integer.parseInt(en.getValue());
            } catch (NumberFormatException ex) {
                return false;
            }
            if (min ? s.getScorePoints() < v : s.getScorePoints() > v) {
                return false;
            }
        }
        return true;
    }

    /** MathHelper.clampAngle of 1.12.2: degrees into -180..179. */
    private static int clampAngle(int a) {
        a %= 360;
        if (a >= 180) {
            a -= 360;
        }
        if (a < -180) {
            a += 360;
        }
        return a;
    }

    private static boolean inAngle(int v, int min, int max) {
        return min > max ? v >= min || v <= max : v >= min && v <= max;
    }

    private static int gameMode(String s) {
        if ("0".equals(s) || "s".equals(s) || "survival".equals(s)) {
            return 0;
        }
        if ("1".equals(s) || "c".equals(s) || "creative".equals(s)) {
            return 1;
        }
        if ("2".equals(s) || "a".equals(s) || "adventure".equals(s)) {
            return 2;
        }
        if ("3".equals(s) || "sp".equals(s) || "spectator".equals(s)) {
            return 3;
        }
        return -1;
    }

    private static int gameModeOf(EntityPlayer p) {
        if (Spectator112.is(p)) {
            return 3;
        }
        if (p instanceof EntityPlayerMP) {
            return ((EntityPlayerMP) p).theItemInWorldManager.getGameType().getID();
        }
        return p.capabilities.isCreativeMode ? 1 : 0;
    }

    /** A plain word: a player name, else an entity UUID in any world. */
    public static List<Entity> byName(String word) {
        List<Entity> out = new ArrayList<Entity>();
        EntityPlayerMP p = MinecraftServer.getServer().getConfigurationManager().func_152612_a(word);
        if (p != null) {
            out.add(p);
            return out;
        }
        for (WorldServer w : MinecraftServer.getServer().worldServers) {
            for (Object o : w.loadedEntityList) {
                if (((Entity) o).getUniqueID().toString().equalsIgnoreCase(word)) {
                    out.add((Entity) o);
                    return out;
                }
            }
        }
        return out;
    }

    /** Any entity token of a command: a selector or a plain name / UUID. */
    public static List<Entity> entities(ICommandSender sender, String token) {
        return isSelector(token) ? select(sender, token, Entity.class) : byName(token);
    }

    // ---- what the coremod puts in place of PlayerSelector and of the loop in CommandHandler ----------------

    public static EntityPlayerMP matchOnePlayer(ICommandSender sender, String token) {
        List<EntityPlayerMP> l = select(sender, token, EntityPlayerMP.class);
        return l.size() == 1 ? l.get(0) : null;
    }

    public static EntityPlayerMP[] matchPlayers(ICommandSender sender, String token) {
        List<EntityPlayerMP> l = select(sender, token, EntityPlayerMP.class);
        return l.toArray(new EntityPlayerMP[l.size()]);
    }

    /** EntitySelector.matchEntitiesToTextComponent: the names of every matched entity ("a, b and c"). */
    public static IChatComponent matchNames(ICommandSender sender, String token) {
        List<Entity> l = select(sender, token, Entity.class);
        if (l.isEmpty()) {
            return null;
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < l.size(); i++) {
            if (i > 0) {
                b.append(i == l.size() - 1 ? " and " : ", ");
            }
            b.append(l.get(i).getCommandSenderName());
        }
        return new ChatComponentText(b.toString());
    }

    /** EntitySelector.matchesMultiplePlayers: c is not 1 (@a and @e default to all). */
    public static boolean matchesMultiple(String token) {
        Matcher mt = TOKEN.matcher(token);
        if (!mt.matches()) {
            return false;
        }
        String kind = mt.group(1);
        return intArg(args(token, mt.group(2)), "c", "a".equals(kind) || "e".equals(kind) ? 0 : 1) != 1;
    }

    /** The targets the command loop runs a command for. */
    public static Entity[] commandTargets(ICommandSender sender, String token) {
        List<Entity> l = select(sender, token, Entity.class);
        Stats112.selected(sender, l.size());
        return l.toArray(new Entity[l.size()]);
    }

    public static String targetName(Entity e) {
        return holder(e);
    }

    /** NBTUtil.areNBTEquals(a, b, true): everything in a is in b; a list in a matches when each element is in b's. */
    public static boolean nbtContains(net.minecraft.nbt.NBTBase a, net.minecraft.nbt.NBTBase b) {
        if (a == b) {
            return true;
        }
        if (a == null) {
            return true;
        }
        if (b == null || a.getId() != b.getId()) {
            return false;
        }
        if (a instanceof NBTTagCompound) {
            NBTTagCompound ca = (NBTTagCompound) a;
            NBTTagCompound cb = (NBTTagCompound) b;
            for (Object k : ca.func_150296_c()) {
                if (!nbtContains(ca.getTag((String) k), cb.getTag((String) k))) {
                    return false;
                }
            }
            return true;
        }
        if (a instanceof NBTTagList) {
            NBTTagList la = (NBTTagList) a;
            NBTTagList lb = (NBTTagList) b;
            if (la.tagCount() == 0) {
                return lb.tagCount() == 0;
            }
            for (int i = 0; i < la.tagCount(); i++) {
                boolean found = false;
                for (int j = 0; j < lb.tagCount() && !found; j++) {
                    found = nbtContains(element(la, i), element(lb, j));
                }
                if (!found) {
                    return false;
                }
            }
            return true;
        }
        return a.equals(b);
    }

    private static net.minecraft.nbt.NBTBase element(NBTTagList l, int i) {
        switch (l.func_150303_d()) {
            case 10:
                return l.getCompoundTagAt(i);
            case 8:
                return new NBTTagString(l.getStringTagAt(i));
            default:
                NBTTagList copy = (NBTTagList) l.copy();
                return copy.removeTag(i);
        }
    }

    /** CommandBase.entityToNBT: the entity's save data, with the selected item for players. */
    public static NBTTagCompound entityNBT(Entity e) {
        NBTTagCompound t = new NBTTagCompound();
        e.writeToNBT(t);
        if (e instanceof EntityPlayer) {
            net.minecraft.item.ItemStack held = ((EntityPlayer) e).inventory.getCurrentItem();
            if (held != null) {
                t.setTag("SelectedItem", held.writeToNBT(new NBTTagCompound()));
            }
        }
        // 1.12 keeps the tags at the top of the entity data
        if (e.getEntityData().hasKey("Tags")) {
            t.setTag("Tags", e.getEntityData().getTag("Tags"));
        }
        return t;
    }
}
