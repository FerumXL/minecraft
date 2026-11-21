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
            super.processCommand(sender, args);
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
}
