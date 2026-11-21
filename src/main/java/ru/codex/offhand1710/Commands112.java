package ru.codex.offhand1710;

import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.DamageSource;

/**
 * /kill, /effect and /tp of 1.12.2, which take any entity (@e and the other selectors of Content120.selectEntities).
 * They replace the 1.7.10 commands of the same names, which only take players.
 */
public final class Commands112 {
    private Commands112() {
    }

    static List<Entity> targets(ICommandSender sender, String token) {
        List<Entity> list = Content120.selectEntities(sender, token);
        if (list.isEmpty()) {
            throw new CommandException("No entity was found");
        }
        return list;
    }

    public static final class Kill extends CommandBase {
        @Override
        public String getCommandName() {
            return "kill";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 2;
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "/kill [target]";
        }

        @Override
        public boolean isUsernameIndex(String[] args, int index) {
            return index == 0;
        }

        @Override
        public List addTabCompletionOptions(ICommandSender sender, String[] args) {
            return args.length == 1 ? getListOfStringsMatchingLastWord(args, net.minecraft.server.MinecraftServer.getServer().getAllUsernames()) : null;
        }

        /** Entity.onKillCommand of 1.12.2: living things take out-of-world damage, the rest is removed. */
        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            List<Entity> list;
            if (args.length == 0) {
                list = new java.util.ArrayList<Entity>();
                list.add(getCommandSenderAsPlayer(sender));
            } else {
                list = targets(sender, args[0]);
            }
            for (Entity e : list) {
                if (e instanceof EntityLivingBase) {
                    e.attackEntityFrom(DamageSource.outOfWorld, Float.MAX_VALUE);
                } else {
                    e.setDead();
                }
                func_152373_a(sender, this, "commands.kill.successful", e.func_145748_c_());
            }
        }
    }

    public static final class Effect extends CommandBase {
        private static final String[] NAMES = {null, "speed", "slowness", "haste", "mining_fatigue", "strength", "instant_health", "instant_damage",
            "jump_boost", "nausea", "regeneration", "resistance", "fire_resistance", "water_breathing", "invisibility", "blindness", "night_vision",
            "hunger", "weakness", "poison", "wither", "health_boost", "absorption", "saturation"};

        @Override
        public String getCommandName() {
            return "effect";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 2;
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "/effect <entity> <effect> [seconds] [amplifier] [hideParticles]  or  /effect <entity> clear";
        }

        @Override
        public boolean isUsernameIndex(String[] args, int index) {
            return index == 0;
        }

        static Potion potion(ICommandSender sender, String name) {
            String n = name.startsWith("minecraft:") ? name.substring(10) : name;
            try {
                int id = Integer.parseInt(n);
                if (id >= 0 && id < Potion.potionTypes.length && Potion.potionTypes[id] != null) {
                    return Potion.potionTypes[id];
                }
            } catch (NumberFormatException ignored) {
            }
            for (int i = 1; i < NAMES.length; i++) {
                if (NAMES[i].equals(n)) {
                    return Potion.potionTypes[i];
                }
            }
            if ("glowing".equals(n)) {
                return Content120.glowing;
            }
            if ("levitation".equals(n)) {
                return Content120.levitation;
            }
            if ("luck".equals(n)) {
                return Content120.luck;
            }
            if ("unluck".equals(n)) {
                return Content120.unluck;
            }
            throw new CommandException("Unknown effect " + name);
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length < 2) {
                throw new WrongUsageException(getCommandUsage(sender));
            }
            List<Entity> list = targets(sender, args[0]);
            if ("clear".equals(args[1])) {
                for (Entity e : list) {
                    if (e instanceof EntityLivingBase) {
                        ((EntityLivingBase) e).clearActivePotions();
                        func_152373_a(sender, this, "commands.effect.success.removed.all", e.getCommandSenderName());
                    }
                }
                return;
            }
            Potion potion = potion(sender, args[1]);
            int seconds = potion.isInstant() ? 1 : 30;
            if (args.length >= 3) {
                seconds = parseIntBounded(sender, args[2], 0, 1000000);
            }
            int amp = args.length >= 4 ? parseIntBounded(sender, args[3], 0, 255) : 0;
            boolean hide = args.length >= 5 && parseBoolean(sender, args[4]);
            int ticks = potion.isInstant() ? seconds : seconds * 20;
            for (Entity e : list) {
                if (!(e instanceof EntityLivingBase)) {
                    continue;
                }
                EntityLivingBase living = (EntityLivingBase) e;
                if (seconds == 0) {
                    if (living.isPotionActive(potion)) {
                        living.removePotionEffect(potion.id);
                        func_152373_a(sender, this, "commands.effect.success.removed", new net.minecraft.util.ChatComponentTranslation(potion.getName()), e.getCommandSenderName());
                    }
                    continue;
                }
                PotionEffect effect = new PotionEffect(potion.id, ticks, amp, hide);
                living.addPotionEffect(effect);
                func_152373_a(sender, this, "commands.effect.success", new net.minecraft.util.ChatComponentTranslation(effect.getEffectName()),
                    potion.id, amp, e.getCommandSenderName(), seconds);
            }
        }
    }

    /** CommandTP of 1.12.2: /tp [target] <x y z [yaw pitch]> or /tp [target] <destination>, any entity. */
    public static final class Tp extends CommandBase {
        private final String name;

        public Tp(String name) {
            this.name = name;
        }

        @Override
        public String getCommandName() {
            return name;
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 2;
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "/" + name + " [target] <x> <y> <z> [yaw] [pitch]  or  /" + name + " [target] <destination>";
        }

        @Override
        public boolean isUsernameIndex(String[] args, int index) {
            return index == 0 || index == 1 && args.length == 2;
        }

        @Override
        public List addTabCompletionOptions(ICommandSender sender, String[] args) {
            return args.length <= 2 ? getListOfStringsMatchingLastWord(args, net.minecraft.server.MinecraftServer.getServer().getAllUsernames()) : null;
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length < 1) {
                throw new WrongUsageException(getCommandUsage(sender));
            }
            List<Entity> movers;
            int shift;
            if (args.length == 1 || args.length == 3 || args.length == 5) {
                movers = new java.util.ArrayList<Entity>();
                Entity self = sender instanceof Entity ? (Entity) sender : Content120.executor;
                movers.add(self != null ? self : getCommandSenderAsPlayer(sender));
                shift = 0;
            } else {
                movers = targets(sender, args[0]);
                shift = 1;
            }
            if (args.length - shift == 1) {
                List<Entity> dest = targets(sender, args[shift]);
                Entity to = dest.get(0);
                for (Entity e : movers) {
                    move(e, to.posX, to.posY, to.posZ, to.rotationYaw, to.rotationPitch, to.dimension);
                    func_152373_a(sender, this, "commands.tp.success", e.getCommandSenderName(), to.getCommandSenderName());
                }
                return;
            }
            if (args.length - shift < 3) {
                throw new WrongUsageException(getCommandUsage(sender));
            }
            for (Entity e : movers) {
                double x = func_110666_a(sender, e.posX, args[shift]);
                double y = func_110666_a(sender, e.posY, args[shift + 1]);
                double z = func_110666_a(sender, e.posZ, args[shift + 2]);
                float yaw = args.length > shift + 3 ? (float) func_110666_a(sender, e.rotationYaw, args[shift + 3]) : e.rotationYaw;
                float pitch = args.length > shift + 4 ? (float) func_110666_a(sender, e.rotationPitch, args[shift + 4]) : e.rotationPitch;
                move(e, x, y, z, yaw, pitch, e.dimension);
                func_152373_a(sender, this, "commands.tp.success.coordinates", e.getCommandSenderName(), x, y, z);
            }
        }

        private static void move(Entity e, double x, double y, double z, float yaw, float pitch, int dimension) {
            if (e.dimension != dimension) {
                throw new CommandException("Unable to teleport because players are not in the same dimension");
            }
            e.mountEntity(null);
            if (e instanceof EntityPlayerMP) {
                ((EntityPlayerMP) e).playerNetServerHandler.setPlayerLocation(x, y, z, yaw, pitch);
            } else {
                e.setLocationAndAngles(x, y, z, yaw, pitch);
                e.setRotationYawHead(yaw);
            }
        }
    }

    private static net.minecraft.nbt.NBTTagCompound dataTag(String[] args, int from) {
        if (args.length <= from) {
            return null;
        }
        try {
            net.minecraft.nbt.NBTBase t = net.minecraft.nbt.JsonToNBT.func_150315_a(buildString(args, from));
            if (!(t instanceof net.minecraft.nbt.NBTTagCompound)) {
                throw new CommandException("commands.testfor.tagError", "Not a valid tag");
            }
            return (net.minecraft.nbt.NBTTagCompound) t;
        } catch (net.minecraft.nbt.NBTException e) {
            throw new CommandException("commands.testfor.tagError", e.getMessage());
        }
    }

    private static String buildString(String[] args, int from) {
        StringBuilder b = new StringBuilder();
        for (int i = from; i < args.length; i++) {
            if (i > from) {
                b.append(' ');
            }
            b.append(args[i]);
        }
        return b.toString();
    }

    /** The entities of a token, or the error 1.12.2 gives when there are none. */
    static List<Entity> found(ICommandSender sender, String token) {
        List<Entity> l = Selector112.entities(sender, token);
        if (l.isEmpty()) {
            throw new CommandException(Selector112.isSelector(token) ? "commands.generic.selector.notFound" : "commands.generic.entity.notFound", token);
        }
        return l;
    }

    /** /testfor of 1.12.2: any entity, optionally with a data tag it must contain. */
    public static final class TestFor extends CommandBase {
        @Override
        public String getCommandName() {
            return "testfor";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 2;
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "commands.testfor.usage";
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length < 1) {
                throw new WrongUsageException("commands.testfor.usage");
            }
            net.minecraft.nbt.NBTTagCompound want = dataTag(args, 1);
            int matched = 0;
            for (Entity e : found(sender, args[0])) {
                if (want != null && !Selector112.nbtContains(want, Selector112.entityNBT(e))) {
                    throw new CommandException("commands.testfor.failure", e.getCommandSenderName());
                }
                func_152373_a(sender, this, "commands.testfor.success", e.getCommandSenderName());
                matched++;
            }
            last = matched;
        }

        @Override
        public List addTabCompletionOptions(ICommandSender sender, String[] args) {
            return args.length == 1 ? getListOfStringsMatchingLastWord(args, net.minecraft.server.MinecraftServer.getServer().getAllUsernames()) : null;
        }
    }

    /** For the autotest: how many entities the last /testfor found. */
    public static volatile int last;

    private static final String[][] ENCHANTMENTS = {{"protection", "0"}, {"fire_protection", "1"}, {"feather_falling", "2"}, {"blast_protection", "3"},
        {"projectile_protection", "4"}, {"respiration", "5"}, {"aqua_affinity", "6"}, {"thorns", "7"}, {"depth_strider", "8"}, {"frost_walker", "9"},
        {"binding_curse", "10"}, {"sharpness", "16"}, {"smite", "17"}, {"bane_of_arthropods", "18"}, {"knockback", "19"}, {"fire_aspect", "20"},
        {"looting", "21"}, {"sweeping", "22"}, {"efficiency", "32"}, {"silk_touch", "33"}, {"unbreaking", "34"}, {"fortune", "35"}, {"power", "48"},
        {"punch", "49"}, {"flame", "50"}, {"infinity", "51"}, {"luck_of_the_sea", "61"}, {"lure", "62"}, {"mending", "70"}, {"vanishing_curse", "71"}};

    static net.minecraft.enchantment.Enchantment enchantment(String s) {
        String n = s.startsWith("minecraft:") ? s.substring(10) : s;
        for (String[] e : ENCHANTMENTS) {
            if (e[0].equals(n)) {
                n = e[1];
            }
        }
        try {
            int id = Integer.parseInt(n);
            if (id >= 0 && id < net.minecraft.enchantment.Enchantment.enchantmentsList.length && net.minecraft.enchantment.Enchantment.enchantmentsList[id] != null) {
                return net.minecraft.enchantment.Enchantment.enchantmentsList[id];
            }
        } catch (NumberFormatException ignored) {
        }
        throw new CommandException("commands.enchant.notFound", s);
    }

    /** /enchant of 1.12.2: the held item of any living entity, by id or by name. */
    public static final class Enchant extends CommandBase {
        @Override
        public String getCommandName() {
            return "enchant";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 2;
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "commands.enchant.usage";
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length < 2) {
                throw new WrongUsageException("commands.enchant.usage");
            }
            net.minecraft.enchantment.Enchantment en = enchantment(args[1]);
            int level = args.length >= 3 ? parseIntBounded(sender, args[2], en.getMinLevel(), en.getMaxLevel()) : 1;
            for (Entity e : found(sender, args[0])) {
                if (!(e instanceof EntityLivingBase)) {
                    throw new CommandException("commands.generic.entity.notFound", args[0]);
                }
                net.minecraft.item.ItemStack held = ((EntityLivingBase) e).getHeldItem();
                if (held == null) {
                    throw new CommandException("commands.enchant.noItem");
                }
                if (!en.canApply(held)) {
                    throw new CommandException("commands.enchant.cantEnchant");
                }
                if (held.hasTagCompound() && held.getEnchantmentTagList() != null) {
                    net.minecraft.nbt.NBTTagList list = held.getEnchantmentTagList();
                    for (int i = 0; i < list.tagCount(); i++) {
                        int id = list.getCompoundTagAt(i).getShort("id");
                        net.minecraft.enchantment.Enchantment other = net.minecraft.enchantment.Enchantment.enchantmentsList[id];
                        if (other != null && !other.canApplyTogether(en)) {
                            throw new CommandException("commands.enchant.cantCombine", en.getTranslatedName(level), other.getTranslatedName(list.getCompoundTagAt(i).getShort("lvl")));
                        }
                    }
                }
                held.addEnchantment(en, level);
                func_152373_a(sender, this, "commands.enchant.success");
            }
        }

        @Override
        public List addTabCompletionOptions(ICommandSender sender, String[] args) {
            return args.length == 1 ? getListOfStringsMatchingLastWord(args, net.minecraft.server.MinecraftServer.getServer().getAllUsernames()) : null;
        }
    }

    /** /scoreboard of 1.7.10 plus "players tag" of 1.12.2 (entity tags, the tag= selector argument). */
    public static final class Scoreboard extends net.minecraft.command.server.CommandScoreboard {
        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length >= 2 && "players".equalsIgnoreCase(args[0]) && "tag".equalsIgnoreCase(args[1])) {
                tag(sender, args);
                return;
            }
            if (args.length >= 2 && "players".equalsIgnoreCase(args[0]) && "enable".equalsIgnoreCase(args[1])) {
                enable(sender, args);
                return;
            }
            super.processCommand(sender, args);
        }

        /** "scoreboard players enable <player> <trigger>" of 1.12.2: the player may use /trigger once. */
        private void enable(ICommandSender sender, String[] args) {
            if (args.length < 4) {
                throw new WrongUsageException("commands.scoreboard.players.enable.usage");
            }
            String name = func_96332_d(sender, args[2]);
            if (name.length() > 40) {
                throw new net.minecraft.command.SyntaxErrorException("commands.scoreboard.players.name.tooLong", name, 40);
            }
            net.minecraft.scoreboard.Scoreboard board = net.minecraft.server.MinecraftServer.getServer().worldServers[0].getScoreboard();
            net.minecraft.scoreboard.ScoreObjective obj = board.getObjective(args[3]);
            if (obj == null) {
                throw new CommandException("commands.scoreboard.objectiveNotFound", args[3]);
            }
            if (obj.getCriteria() != Stats112.TRIGGER) {
                throw new CommandException("commands.scoreboard.players.enable.noTrigger", obj.getName());
            }
            board.func_96529_a(name, obj);
            Stats112.setLocked(name, obj, false);
            func_152373_a(sender, this, "commands.scoreboard.players.enable.success", obj.getName(), name);
        }

        private void tag(ICommandSender sender, String[] args) {
            if (args.length < 4) {
                throw new WrongUsageException("commands.scoreboard.players.tag.usage");
            }
            List<Entity> targets = found(sender, args[2]);
            Entity e = targets.get(0);
            String op = args[3];
            if ("list".equals(op)) {
                List<String> tags = Selector112.tags(e);
                sender.addChatMessage(new net.minecraft.util.ChatComponentTranslation("commands.scoreboard.players.tag.list", Selector112.holder(e)));
                sender.addChatMessage(new ChatComponentText(joinNiceString(tags.toArray())));
                return;
            }
            if (args.length < 5) {
                throw new WrongUsageException("commands.scoreboard.players.tag.usage");
            }
            String name = args[4];
            net.minecraft.nbt.NBTTagCompound want = dataTag(args, 5);
            if (want != null && !Selector112.nbtContains(want, Selector112.entityNBT(e))) {
                throw new CommandException("commands.scoreboard.players.tag.tagMismatch", Selector112.holder(e));
            }
            List<String> tags = Selector112.tags(e);
            if ("add".equals(op)) {
                if (tags.contains(name) || tags.size() >= Selector112.MAX_TAGS) {
                    throw new CommandException("commands.scoreboard.players.tag.tooMany", Selector112.MAX_TAGS);
                }
                tags.add(name);
                Selector112.setTags(e, tags);
                func_152373_a(sender, this, "commands.scoreboard.players.tag.success.add", name);
            } else if ("remove".equals(op)) {
                if (!tags.remove(name)) {
                    throw new CommandException("commands.scoreboard.players.tag.notFound", name);
                }
                Selector112.setTags(e, tags);
                func_152373_a(sender, this, "commands.scoreboard.players.tag.success.remove", name);
            } else {
                throw new WrongUsageException("commands.scoreboard.players.tag.usage");
            }
        }
    }

    /** CommandGameRule of 1.12.2: "name = value", no such rule, booleans checked, the value in the success line. */
    public static final class GameRule extends net.minecraft.command.CommandGameRule {
        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (Rules112.OFF) {
                super.processCommand(sender, args);
                return;
            }
            net.minecraft.world.GameRules rules = net.minecraft.server.MinecraftServer.getServer().worldServers[0].getGameRules();
            String name = args.length > 0 ? args[0] : "";
            String value = args.length > 1 ? func_82360_a(sender, args, 1) : "";
            if (args.length == 0) {
                sender.addChatMessage(new ChatComponentText(joinNiceString(rules.getRules())));
            } else if (args.length == 1) {
                if (!rules.hasRule(name)) {
                    throw new CommandException("commands.gamerule.norule", name);
                }
                sender.addChatMessage(new ChatComponentText(name).appendText(" = ").appendText(rules.getGameRuleStringValue(name)));
                String v = rules.getGameRuleStringValue(name);
                // GameRules.Value of 1.12.2: true is 1, a number is itself, anything else 0
                Stats112.set(sender, Stats112.QUERY_RESULT, "true".equals(v) ? 1 : Names112.isNumber(v) ? Integer.parseInt(v) : 0);
            } else {
                if (Rules112.isBoolean(rules, name) && !"true".equals(value) && !"false".equals(value)) {
                    throw new CommandException("commands.generic.boolean.invalid", value);
                }
                rules.setOrCreateGameRule(name, value);
                func_152373_a(sender, this, "commands.gamerule.success", name, value);
            }
        }

        @Override
        @SuppressWarnings("rawtypes")
        public List addTabCompletionOptions(ICommandSender sender, String[] args) {
            net.minecraft.world.GameRules rules = net.minecraft.server.MinecraftServer.getServer().worldServers[0].getGameRules();
            if (args.length == 1) {
                return getListOfStringsMatchingLastWord(args, rules.getRules());
            }
            if (args.length == 2) {
                if (Rules112.isBoolean(rules, args[0])) {
                    return getListOfStringsMatchingLastWord(args, "true", "false");
                }
                if ("gameLoopFunction".equals(args[0])) {
                    return getListOfStringsFromIterableMatchingLastWord(args, Data112.functionNames());
                }
            }
            return null;
        }
    }

    // ---- the names of 1.12.2 in the vanilla commands (Names112) ----

    private static String[] put(String[] args, int index, String value) {
        String[] out = args;
        if (index >= args.length) {
            out = new String[index + 1];
            System.arraycopy(args, 0, out, 0, args.length);
        } else {
            out = args.clone();
        }
        out[index] = value;
        return out;
    }

    /** /give with item ids and data of 1.12.2 (concrete 5 is the lime concrete of this mod). */
    public static final class Give extends net.minecraft.command.CommandGive {
        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (Names112.OFF || args.length < 2) {
                super.processCommand(sender, args);
                return;
            }
            int data = args.length >= 4 ? parseInt(sender, args[3]) : 0;
            Object[] it = Names112.item(args[1], data);
            if (it == null) {
                throw new CommandException("commands.give.item.notFound", args[1]);
            }
            String[] out = put(args, 1, (String) it[0]);
            if (args.length >= 4 || ((Integer) it[1]).intValue() != 0) {
                if (out.length < 3) {
                    out = put(out, 2, "1");
                }
                out = put(out, 3, String.valueOf(it[1]));
            }
            Stats112.set(sender, Stats112.AFFECTED_ITEMS, 0);
            super.processCommand(sender, out);
            Stats112.set(sender, Stats112.AFFECTED_ITEMS, out.length >= 3 ? parseIntBounded(sender, out[2], 1, 64) : 1);
        }

        @Override
        @SuppressWarnings("rawtypes")
        public List addTabCompletionOptions(ICommandSender sender, String[] args) {
            if (args.length == 2 && !Names112.OFF) {
                return getListOfStringsFromIterableMatchingLastWord(args, Names112.itemCompletions());
            }
            return super.addTabCompletionOptions(sender, args);
        }
    }

    /** /setblock with block ids and states of 1.12.2 ("stone variant=granite"). */
    public static final class SetBlock extends net.minecraft.command.server.CommandSetBlock {
        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            Stats112.set(sender, Stats112.AFFECTED_BLOCKS, 0);
            super.processCommand(sender, Names112.OFF || args.length < 4 ? args : blockArgs(args, 3, false));
            Stats112.set(sender, Stats112.AFFECTED_BLOCKS, 1);
        }

        @Override
        @SuppressWarnings("rawtypes")
        public List addTabCompletionOptions(ICommandSender sender, String[] args) {
            if (args.length == 4 && !Names112.OFF) {
                return getListOfStringsFromIterableMatchingLastWord(args, Names112.blockCompletions());
            }
            return super.addTabCompletionOptions(sender, args);
        }
    }

    /** args[at] is a 1.12.2 block and args[at + 1] its data or state: both become the 1.7.10 ones. */
    static String[] blockArgs(String[] args, int at, boolean anyMeta) {
        String data = args.length > at + 1 ? args[at + 1] : null;
        boolean any = anyMeta && (data == null || "-1".equals(data) || "*".equals(data));
        Object[] b = Names112.block(args[at], any ? null : data);
        if (b == null) {
            throw new CommandException("commands.give.block.notFound", args[at]);
        }
        String[] out = put(args, at, (String) b[0]);
        if (any) {
            return data == null ? out : put(out, at + 1, "-1");
        }
        if (data != null || ((Integer) b[1]).intValue() != 0) {
            out = put(out, at + 1, String.valueOf(b[1]));
        }
        return out;
    }

    /** /summon with entity ids of 1.12.2 ("zombie", "minecraft:lightning_bolt"). */
    public static final class Summon extends net.minecraft.command.server.CommandSummon {
        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (Names112.OFF || args.length < 1) {
                super.processCommand(sender, args);
                return;
            }
            super.processCommand(sender, put(args, 0, Names112.entity(args[0])));
        }

        @Override
        @SuppressWarnings("rawtypes")
        public List addTabCompletionOptions(ICommandSender sender, String[] args) {
            if (args.length == 1 && !Names112.OFF) {
                return getListOfStringsFromIterableMatchingLastWord(args, Names112.entityCompletions());
            }
            return super.addTabCompletionOptions(sender, args);
        }
    }

    /**
     * CommandClearInventory of 1.12.2: /clear [player] [item] [data] [maxCount] [dataTag]. maxCount 0 only counts,
     * a positive one stops there; the offhand and the item on the cursor are cleared too.
     */
    public static final class Clear extends net.minecraft.command.CommandClearInventory {
        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (Names112.OFF) {
                super.processCommand(sender, args);
                return;
            }
            EntityPlayerMP player = args.length == 0 ? getCommandSenderAsPlayer(sender) : getPlayer(sender, args[0]);
            net.minecraft.item.Item item = null;
            int data = args.length >= 3 ? parseIntWithMin(sender, args[2], -1) : -1;
            if (args.length >= 2) {
                Object[] it = Names112.item(args[1], Math.max(0, data));
                if (it == null) {
                    throw new CommandException("commands.give.item.notFound", args[1]);
                }
                item = (net.minecraft.item.Item) net.minecraft.item.Item.itemRegistry.getObject(it[0]);
                if (data >= 0) {
                    data = ((Integer) it[1]).intValue();
                }
            }
            int max = args.length >= 4 ? parseIntWithMin(sender, args[3], -1) : -1;
            net.minecraft.nbt.NBTTagCompound tag = null;
            if (args.length >= 5) {
                try {
                    net.minecraft.nbt.NBTBase t = net.minecraft.nbt.JsonToNBT.func_150315_a(func_82360_a(sender, args, 4));
                    if (!(t instanceof net.minecraft.nbt.NBTTagCompound)) {
                        throw new CommandException("commands.clear.tagError", "Not a valid tag");
                    }
                    tag = (net.minecraft.nbt.NBTTagCompound) t;
                } catch (net.minecraft.nbt.NBTException e) {
                    throw new CommandException("commands.clear.tagError", e.getMessage());
                }
            }
            int[] counted = new int[1];
            net.minecraft.entity.player.InventoryPlayer inv = player.inventory;
            boolean done = clearIn(inv.mainInventory, item, data, max, tag, counted) || clearIn(inv.armorInventory, item, data, max, tag, counted);
            OffhandInventory off = OffhandInventory.get(player);
            if (!done) {
                net.minecraft.item.ItemStack[] one = {off.getStack()};
                done = clearIn(one, item, data, max, tag, counted);
                off.setStack(one[0]);
            }
            if (!done) {
                net.minecraft.item.ItemStack[] cursor = {inv.getItemStack()};
                clearIn(cursor, item, data, max, tag, counted);
                inv.setItemStack(cursor[0]);
            }
            player.inventoryContainer.detectAndSendChanges();
            if (!player.capabilities.isCreativeMode) {
                player.updateHeldItem();
            }
            OffhandMessages.synchronize(player);
            Stats112.set(sender, Stats112.AFFECTED_ITEMS, counted[0]);
            if (counted[0] == 0) {
                throw new CommandException("commands.clear.failure", player.getCommandSenderName());
            }
            if (max == 0) {
                sender.addChatMessage(new net.minecraft.util.ChatComponentTranslation("commands.clear.testing", player.getCommandSenderName(), counted[0]));
            } else {
                func_152373_a(sender, this, "commands.clear.success", player.getCommandSenderName(), counted[0]);
            }
        }

        /** InventoryPlayer.clearMatchingItems of 1.12.2 over one array; true once maxCount is reached. */
        private static boolean clearIn(net.minecraft.item.ItemStack[] stacks, net.minecraft.item.Item item, int data, int max,
            net.minecraft.nbt.NBTTagCompound tag, int[] counted) {
            for (int i = 0; i < stacks.length; i++) {
                net.minecraft.item.ItemStack s = stacks[i];
                if (s == null || item != null && s.getItem() != item || data > -1 && s.getItemDamage() != data
                    || tag != null && !Selector112.nbtContains(tag, s.getTagCompound() == null ? new net.minecraft.nbt.NBTTagCompound() : s.getTagCompound())) {
                    continue;
                }
                int take = max <= 0 ? s.stackSize : Math.min(max - counted[0], s.stackSize);
                counted[0] += take;
                if (max != 0) {
                    s.stackSize -= take;
                    if (s.stackSize <= 0) {
                        stacks[i] = null;
                    }
                    if (max > 0 && counted[0] >= max) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        @SuppressWarnings("rawtypes")
        public List addTabCompletionOptions(ICommandSender sender, String[] args) {
            if (args.length == 2 && !Names112.OFF) {
                return getListOfStringsFromIterableMatchingLastWord(args, Names112.itemCompletions());
            }
            return super.addTabCompletionOptions(sender, args);
        }
    }

    /** /time of 1.7.10 plus "time query daytime|gametime|day" of 1.12.2 (QueryResult). */
    public static final class Time extends net.minecraft.command.CommandTime {
        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length >= 2 && "query".equals(args[0])) {
                net.minecraft.world.World w = net.minecraft.server.MinecraftServer.getServer().worldServers[0];
                int value;
                if ("daytime".equals(args[1])) {
                    value = (int) (w.getWorldTime() % 24000L);
                } else if ("gametime".equals(args[1])) {
                    value = (int) (w.getTotalWorldTime() % 2147483647L);
                } else if ("day".equals(args[1])) {
                    value = (int) (w.getWorldTime() / 24000L % 2147483647L);
                } else {
                    throw new WrongUsageException("commands.time.usage");
                }
                Stats112.set(sender, Stats112.QUERY_RESULT, value);
                func_152373_a(sender, this, "commands.time.query", value);
                return;
            }
            super.processCommand(sender, args);
        }

        @Override
        @SuppressWarnings("rawtypes")
        public List addTabCompletionOptions(ICommandSender sender, String[] args) {
            if (args.length == 1) {
                return getListOfStringsMatchingLastWord(args, "set", "add", "query");
            }
            if (args.length == 2 && "query".equals(args[0])) {
                return getListOfStringsMatchingLastWord(args, "daytime", "gametime", "day");
            }
            return super.addTabCompletionOptions(sender, args);
        }
    }
}
