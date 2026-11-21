package ru.codex.offhand1710;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.command.server.CommandBlockLogic;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.scoreboard.IScoreObjectiveCriteria;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreDummyCriteria;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityCommandBlock;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;

/**
 * CommandResultStats of 1.12.2 (/stats), the trigger criterion with /trigger and "scoreboard players enable".
 * A command's results (success count, affected blocks, entities and items, query result) go into the score that the
 * sender's CommandStats name, as 1.12.2 does: only into a score that already exists. The stats of an entity live in
 * its ForgeData, those of a command block in the CommandBlockLogic NBT (patched in Transformer). -Dmc112.stats17
 * turns the hooks off.
 */
public final class Stats112 {
    public static final boolean OFF = System.getProperty("mc112.stats17") != null;

    public static final int SUCCESS_COUNT = 0;
    public static final int AFFECTED_BLOCKS = 1;
    public static final int AFFECTED_ENTITIES = 2;
    public static final int AFFECTED_ITEMS = 3;
    public static final int QUERY_RESULT = 4;
    public static final String[] TYPES = {"SuccessCount", "AffectedBlocks", "AffectedEntities", "AffectedItems", "QueryResult"};

    /** The trigger criterion of 1.12.2 (IScoreCriteria.TRIGGER); the constructor files it under its name. */
    public static final IScoreObjectiveCriteria TRIGGER = new ScoreDummyCriteria("trigger");

    private static final Map<CommandBlockLogic, NBTTagCompound> LOGIC = new WeakHashMap<CommandBlockLogic, NBTTagCompound>();
    private static final ThreadLocal<int[]> DEPTH = new ThreadLocal<int[]>() {
        @Override
        protected int[] initialValue() {
            return new int[] {0, -1};
        }
    };

    private Stats112() {
    }

    public static int type(String name) {
        for (int i = 0; i < TYPES.length; i++) {
            if (TYPES[i].equals(name)) {
                return i;
            }
        }
        return -1;
    }

    // ---- where a sender keeps its stats ----

    /** The CommandStats compound of the sender (created on demand), or null for a sender without stats. */
    static NBTTagCompound statsOf(Object sender, boolean create) {
        if (sender != null && sender.getClass().getEnclosingClass() == Content120.CommandExecute.class && Content120.executor() != null) {
            sender = Content120.executor();
        }
        if (sender instanceof Entity) {
            NBTTagCompound data = ((Entity) sender).getEntityData();
            if (!data.hasKey("CommandStats") && create) {
                data.setTag("CommandStats", new NBTTagCompound());
            }
            return data.hasKey("CommandStats") ? data.getCompoundTag("CommandStats") : null;
        }
        if (sender instanceof CommandBlockLogic) {
            NBTTagCompound t = LOGIC.get(sender);
            if (t == null && create) {
                t = new NBTTagCompound();
                LOGIC.put((CommandBlockLogic) sender, t);
            }
            return t;
        }
        return null;
    }

    /** CommandBlockLogic.func_145758_a (write): the stats go with the block, as "CommandStats" of 1.12.2. */
    public static void writeLogic(CommandBlockLogic logic, NBTTagCompound nbt) {
        NBTTagCompound t = LOGIC.get(logic);
        if (t != null && !t.hasNoTags()) {
            nbt.setTag("CommandStats", t.copy());
        }
    }

    /** CommandBlockLogic.func_145759_b (read). */
    public static void readLogic(CommandBlockLogic logic, NBTTagCompound nbt) {
        if (nbt.hasKey("CommandStats")) {
            LOGIC.put(logic, (NBTTagCompound) nbt.getCompoundTag("CommandStats").copy());
        } else {
            LOGIC.remove(logic);
        }
    }

    /** CommandResultStats.setCommandStatForSender of 1.12.2. */
    public static void set(ICommandSender sender, int type, int amount) {
        if (OFF) {
            return;
        }
        NBTTagCompound stats = statsOf(sender, false);
        if (stats == null || !stats.hasKey(TYPES[type] + "Name") || !stats.hasKey(TYPES[type] + "Objective")) {
            return;
        }
        String name = stats.getString(TYPES[type] + "Name");
        if (name.startsWith("@")) {
            // a selector is looked up from the sender, each time
            try {
                List<Entity> found = Selector112.entities(sender, name);
                if (found == null || found.size() != 1) {
                    return;
                }
                name = Selector112.holder(found.get(0));
            } catch (CommandException e) {
                return;
            }
        }
        World w = sender.getEntityWorld();
        Scoreboard board = w == null ? null : w.getScoreboard();
        ScoreObjective obj = board == null ? null : board.getObjective(stats.getString(TYPES[type] + "Objective"));
        if (obj == null || !board.func_96510_d(name).containsKey(obj)) {
            return;
        }
        board.func_96529_a(name, obj).setScorePoints(amount);
    }

    // ---- hooks of CommandHandler.executeCommand ----

    /** Start of executeCommand. */
    public static void enter() {
        int[] d = DEPTH.get();
        d[0]++;
    }

    /** The selector loop of executeCommand found n targets (Selector112.commandTargets). */
    public static void selected(ICommandSender sender, int n) {
        int[] d = DEPTH.get();
        d[1] = d[0];
        set(sender, AFFECTED_ENTITIES, n);
    }

    /** Just before ICommand.processCommand in executeCommand: one target unless the selector loop said otherwise. */
    public static void running(ICommandSender sender) {
        int[] d = DEPTH.get();
        if (d[1] != d[0]) {
            set(sender, AFFECTED_ENTITIES, 1);
        }
    }

    /** Every return of executeCommand: SuccessCount is the number of runs that worked. */
    public static void done(int successes, ICommandSender sender) {
        int[] d = DEPTH.get();
        if (d[1] == d[0]) {
            d[1] = -1;
        }
        d[0] = Math.max(0, d[0] - 1);
        set(sender, SUCCESS_COUNT, successes);
    }

    // ---- /stats ----

    public static final class CommandStats extends CommandBase {
        @Override
        public String getCommandName() {
            return "stats";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 2;
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "commands.stats.usage";
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length < 1) {
                throw new WrongUsageException("commands.stats.usage");
            }
            boolean block;
            if ("entity".equals(args[0])) {
                block = false;
            } else if ("block".equals(args[0])) {
                block = true;
            } else {
                throw new WrongUsageException("commands.stats.usage");
            }
            int i = block ? 4 : 2;
            if (args.length < i + 2) {
                throw new WrongUsageException(block ? "commands.stats.block.usage" : "commands.stats.entity.usage");
            }
            String mode = args[i++];
            if ("set".equals(mode)) {
                if (args.length < i + 3) {
                    throw new WrongUsageException(block ? "commands.stats.block.set.usage" : "commands.stats.entity.set.usage");
                }
            } else if ("clear".equals(mode)) {
                if (args.length < i + 1) {
                    throw new WrongUsageException(block ? "commands.stats.block.clear.usage" : "commands.stats.entity.clear.usage");
                }
            } else {
                throw new WrongUsageException("commands.stats.usage");
            }
            int type = type(args[i++]);
            if (type < 0) {
                throw new CommandException("commands.stats.failed");
            }
            NBTTagCompound stats;
            TileEntity te = null;
            if (block) {
                int[] p = BlockCommands112.pos(sender, args, 1);
                te = sender.getEntityWorld().getTileEntity(p[0], p[1], p[2]);
                if (!(te instanceof TileEntityCommandBlock)) {
                    throw new CommandException("commands.stats.noCompatibleBlock", p[0], p[1], p[2]);
                }
                stats = statsOf(((TileEntityCommandBlock) te).func_145993_a(), true);
            } else {
                List<Entity> found = Selector112.entities(sender, args[1]);
                if (found == null || found.isEmpty()) {
                    throw new net.minecraft.command.PlayerNotFoundException("commands.generic.entity.notFound", args[1]);
                }
                stats = statsOf(found.get(0), true);
            }
            if ("set".equals(mode)) {
                String who = args[i++];
                String objective = args[i];
                if (who.isEmpty() || objective.isEmpty()) {
                    throw new CommandException("commands.stats.failed");
                }
                stats.setString(TYPES[type] + "Name", who);
                stats.setString(TYPES[type] + "Objective", objective);
                func_152373_a(sender, this, "commands.stats.success", TYPES[type], objective, who);
            } else {
                stats.removeTag(TYPES[type] + "Name");
                stats.removeTag(TYPES[type] + "Objective");
                func_152373_a(sender, this, "commands.stats.cleared", TYPES[type]);
            }
            if (te != null) {
                te.markDirty();
            }
        }

        @Override
        @SuppressWarnings("rawtypes")
        public List addTabCompletionOptions(ICommandSender sender, String[] args) {
            if (args.length == 1) {
                return getListOfStringsMatchingLastWord(args, "entity", "block");
            }
            boolean block = "block".equals(args[0]);
            int i = block ? 5 : 3;
            if (args.length == 2 && !block) {
                return getListOfStringsMatchingLastWord(args, MinecraftServer.getServer().getAllUsernames());
            }
            if (args.length == i) {
                return getListOfStringsMatchingLastWord(args, "set", "clear");
            }
            if (args.length == i + 1) {
                return getListOfStringsMatchingLastWord(args, TYPES);
            }
            if (args.length == i + 2 && "set".equals(args[i - 1])) {
                return getListOfStringsMatchingLastWord(args, MinecraftServer.getServer().getAllUsernames());
            }
            if (args.length == i + 3 && "set".equals(args[i - 1])) {
                return getListOfStringsFromIterableMatchingLastWord(args, objectiveNames(sender, false));
            }
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    static List<String> objectiveNames(ICommandSender sender, boolean triggersOnly) {
        List<String> out = new ArrayList<String>();
        for (Object o : sender.getEntityWorld().getScoreboard().getScoreObjectives()) {
            ScoreObjective obj = (ScoreObjective) o;
            if (!triggersOnly || obj.getCriteria() == TRIGGER) {
                out.add(obj.getName());
            }
        }
        return out;
    }

    // ---- /trigger: Score.isLocked of 1.12.2, kept beside the scoreboard ----

    /** Scores unlocked by "scoreboard players enable": "objective\tplayer". Saved with the overworld. */
    public static final class Unlocked extends WorldSavedData {
        public static final String KEY = "mc112triggers";
        public final java.util.Set<String> open = new java.util.HashSet<String>();

        public Unlocked(String key) {
            super(key);
        }

        @Override
        public void readFromNBT(NBTTagCompound nbt) {
            open.clear();
            NBTTagList l = nbt.getTagList("Open", 8);
            for (int i = 0; i < l.tagCount(); i++) {
                open.add(l.getStringTagAt(i));
            }
        }

        @Override
        public void writeToNBT(NBTTagCompound nbt) {
            NBTTagList l = new NBTTagList();
            for (String s : open) {
                l.appendTag(new net.minecraft.nbt.NBTTagString(s));
            }
            nbt.setTag("Open", l);
        }

        static Unlocked get() {
            World w = MinecraftServer.getServer().worldServers[0];
            Unlocked u = (Unlocked) w.mapStorage.loadData(Unlocked.class, KEY);
            if (u == null) {
                u = new Unlocked(KEY);
                w.mapStorage.setData(KEY, u);
            }
            return u;
        }
    }

    public static void setLocked(String player, ScoreObjective obj, boolean locked) {
        Unlocked u = Unlocked.get();
        String k = obj.getName() + "\t" + player;
        if (locked ? u.open.remove(k) : u.open.add(k)) {
            u.markDirty();
        }
    }

    public static boolean isLocked(String player, ScoreObjective obj) {
        return !Unlocked.get().open.contains(obj.getName() + "\t" + player);
    }

    /** CommandTrigger of 1.12.2. */
    public static final class CommandTrigger extends CommandBase {
        @Override
        public String getCommandName() {
            return "trigger";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 0;
        }

        @Override
        public boolean canCommandSenderUseCommand(ICommandSender sender) {
            return true;
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "commands.trigger.usage";
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length < 3) {
                throw new WrongUsageException("commands.trigger.usage");
            }
            EntityPlayerMP player;
            if (sender instanceof EntityPlayerMP) {
                player = (EntityPlayerMP) sender;
            } else if (sender.getClass().getEnclosingClass() == Content120.CommandExecute.class && Content120.executor() instanceof EntityPlayerMP) {
                player = (EntityPlayerMP) Content120.executor();
            } else {
                throw new CommandException("commands.trigger.invalidPlayer");
            }
            Scoreboard board = MinecraftServer.getServer().worldServers[0].getScoreboard();
            ScoreObjective obj = board.getObjective(args[0]);
            if (obj == null || obj.getCriteria() != TRIGGER) {
                throw new CommandException("commands.trigger.invalidObjective", args[0]);
            }
            int value = parseInt(sender, args[2]);
            String name = player.getCommandSenderName();
            if (!board.func_96510_d(name).containsKey(obj)) {
                throw new CommandException("commands.trigger.invalidObjective", args[0]);
            }
            Score score = board.func_96529_a(name, obj);
            if (isLocked(name, obj)) {
                throw new CommandException("commands.trigger.disabled", args[0]);
            }
            if ("set".equals(args[1])) {
                score.setScorePoints(value);
            } else if ("add".equals(args[1])) {
                score.increseScore(value);
            } else {
                throw new CommandException("commands.trigger.invalidMode", args[1]);
            }
            setLocked(name, obj, true);
            if (player.theItemInWorldManager.isCreative()) {
                func_152373_a(sender, this, "commands.trigger.success", args[0], args[1], args[2]);
            }
        }

        @Override
        @SuppressWarnings("rawtypes")
        public List addTabCompletionOptions(ICommandSender sender, String[] args) {
            if (args.length == 1) {
                return getListOfStringsFromIterableMatchingLastWord(args, objectiveNames(sender, true));
            }
            if (args.length == 2) {
                return getListOfStringsMatchingLastWord(args, "add", "set");
            }
            return null;
        }
    }
}
