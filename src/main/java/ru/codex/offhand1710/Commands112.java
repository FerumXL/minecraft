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
}
