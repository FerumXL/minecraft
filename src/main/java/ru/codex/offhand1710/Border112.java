package ru.codex.offhand1710;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderHell;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

/**
 * The world border of 1.8 - 1.12.2 (WorldBorder, CommandWorldBorder): centre, diameter with a timed change in real
 * time, damage outside past the buffer, warning distance and time, a wall that entities inside cannot cross, no
 * building or digging outside. One border for the server, kept in level.dat under the keys of 1.12.2 (so a 1.12.2
 * world keeps its border); the Nether has the centre / 8 as in WorldProviderHell.createWorldBorder. The client gets
 * it by packet. -Dmc112.border17 turns it off.
 */
public final class Border112 {
    public static final boolean OFF = System.getProperty("mc112.border17") != null;
    public static final double WORLD_SIZE = 29999984.0D;

    public double centerX;
    public double centerZ;
    public double startDiameter = 6.0E7D;
    public double endDiameter = 6.0E7D;
    public long startTime = System.currentTimeMillis();
    public long endTime = startTime;
    public double damageAmount = 0.2D;
    public double damageBuffer = 5.0D;
    public int warningTime = 15;
    public int warningDistance = 5;

    /** The server's border (the overworld's level.dat) and the one this client was sent. */
    private static final Map<WorldInfo, Border112> SAVED = new WeakHashMap<WorldInfo, Border112>();
    public static Border112 client = new Border112();
    private static final Map<Entity, Boolean> OUTSIDE = new WeakHashMap<Entity, Boolean>();

    // ---- WorldBorder of 1.12.2 ----

    /** 0 stationary, 1 growing, 2 shrinking (EnumBorderStatus). */
    public int status() {
        if (diameterNow() == endDiameter) {
            return 0;
        }
        return endDiameter > startDiameter ? 1 : 2;
    }

    /** The colour of the wall: 0x20A0FF, 0x40FF80 growing, 0xFF3030 shrinking. */
    public int color() {
        int s = status();
        return s == 1 ? 4259712 : s == 2 ? 16724016 : 2138367;
    }

    private double diameterNow() {
        if (endTime == startTime) {
            return startDiameter;
        }
        double f = (double) (float) (System.currentTimeMillis() - startTime) / (double) (float) (endTime - startTime);
        return f < 1.0D ? startDiameter + (endDiameter - startDiameter) * f : endDiameter;
    }

    public double diameter() {
        double d = diameterNow();
        if (d == endDiameter && endTime != startTime) {
            setTransition(endDiameter);
        }
        return d;
    }

    public long timeUntilTarget() {
        return status() == 0 ? 0L : endTime - System.currentTimeMillis();
    }

    public double resizeSpeed() {
        return endTime == startTime ? 0.0D : Math.abs(startDiameter - endDiameter) / (double) (endTime - startTime);
    }

    public void setTransition(double size) {
        startDiameter = size;
        endDiameter = size;
        endTime = System.currentTimeMillis();
        startTime = endTime;
    }

    public void setTransition(double from, double to, long millis) {
        startDiameter = from;
        endDiameter = to;
        startTime = System.currentTimeMillis();
        endTime = startTime + millis;
    }

    public double minX(double cx) {
        return Math.max(cx - diameter() / 2.0D, -WORLD_SIZE);
    }

    public double maxX(double cx) {
        return Math.min(cx + diameter() / 2.0D, WORLD_SIZE);
    }

    /** The border as one world sees it: {minX, minZ, maxX, maxZ}. */
    public double[] box(World w) {
        double cx = centerX;
        double cz = centerZ;
        if (w != null && w.provider instanceof WorldProviderHell) {
            cx /= 8.0D;
            cz /= 8.0D;
        }
        double r = diameter() / 2.0D;
        return new double[] {Math.max(cx - r, -WORLD_SIZE), Math.max(cz - r, -WORLD_SIZE), Math.min(cx + r, WORLD_SIZE), Math.min(cz + r, WORLD_SIZE)};
    }

    /** getClosestDistance: to the nearest side, positive inside. */
    public static double distance(double[] b, double x, double z) {
        return Math.min(Math.min(x - b[0], b[2] - x), Math.min(z - b[1], b[3] - z));
    }

    public static boolean containsBlock(double[] b, int x, int z) {
        return x + 1 > b[0] && x < b[2] && z + 1 > b[1] && z < b[3];
    }

    public static boolean containsBox(double[] b, AxisAlignedBB a) {
        return a.maxX > b[0] && a.minX < b[2] && a.maxZ > b[1] && a.minZ < b[3];
    }

    public static Border112 of(World w) {
        if (w != null && w.isRemote) {
            return client;
        }
        MinecraftServer s = MinecraftServer.getServer();
        if (s == null || s.worldServers == null || s.worldServers.length == 0 || s.worldServers[0] == null) {
            return client;
        }
        return saved(s.worldServers[0].getWorldInfo());
    }

    private static synchronized Border112 saved(WorldInfo info) {
        Border112 b = SAVED.get(info);
        if (b == null) {
            b = new Border112();
            SAVED.put(info, b);
        }
        return b;
    }

    // ---- level.dat (WorldInfo hooks) ----

    /** WorldInfo.updateTagCompound: the keys of WorldInfo of 1.12.2. */
    public static void write(WorldInfo info, NBTTagCompound nbt) {
        Border112 b = saved(info);
        nbt.setDouble("BorderCenterX", b.centerX);
        nbt.setDouble("BorderCenterZ", b.centerZ);
        nbt.setDouble("BorderSize", b.diameter());
        nbt.setLong("BorderSizeLerpTime", b.timeUntilTarget());
        nbt.setDouble("BorderSizeLerpTarget", b.endDiameter);
        nbt.setDouble("BorderSafeZone", b.damageBuffer);
        nbt.setDouble("BorderDamagePerBlock", b.damageAmount);
        nbt.setDouble("BorderWarningBlocks", b.warningDistance);
        nbt.setDouble("BorderWarningTime", b.warningTime);
    }

    /** WorldInfo(NBTTagCompound). */
    public static void read(WorldInfo info, NBTTagCompound nbt) {
        if (!nbt.hasKey("BorderCenterX")) {
            return;
        }
        Border112 b = saved(info);
        b.centerX = nbt.getDouble("BorderCenterX");
        b.centerZ = nbt.getDouble("BorderCenterZ");
        double size = nbt.hasKey("BorderSize") ? nbt.getDouble("BorderSize") : 6.0E7D;
        if (nbt.hasKey("BorderSizeLerpTime") && nbt.getLong("BorderSizeLerpTime") > 0L) {
            b.setTransition(size, nbt.getDouble("BorderSizeLerpTarget"), nbt.getLong("BorderSizeLerpTime"));
        } else {
            b.setTransition(size);
        }
        if (nbt.hasKey("BorderSafeZone")) {
            b.damageBuffer = nbt.getDouble("BorderSafeZone");
        }
        if (nbt.hasKey("BorderDamagePerBlock")) {
            b.damageAmount = nbt.getDouble("BorderDamagePerBlock");
        }
        if (nbt.hasKey("BorderWarningBlocks")) {
            b.warningDistance = (int) nbt.getDouble("BorderWarningBlocks");
        }
        if (nbt.hasKey("BorderWarningTime")) {
            b.warningTime = (int) nbt.getDouble("BorderWarningTime");
        }
    }

    // ---- collisions (World.getCollidingBoundingBoxes hook) ----

    /**
     * World.getCollisionBoxes of 1.12.2: for an entity inside the border, every block position outside it is solid.
     * Inside or outside has one block of give (Entity.isOutsideBorder), so an entity standing on the line keeps its side.
     */
    @SuppressWarnings("unchecked")
    public static void collide(List<AxisAlignedBB> list, World w, Entity e, AxisAlignedBB a) {
        if (OFF || e == null || a == null) {
            return;
        }
        double[] b = of(w).box(w);
        if (a.minX > b[0] + 2.0D && a.maxX < b[2] - 2.0D && a.minZ > b[1] + 2.0D && a.maxZ < b[3] - 2.0D && e.posX > b[0] + 2.0D
            && e.posX < b[2] - 2.0D && e.posZ > b[1] + 2.0D && e.posZ < b[3] - 2.0D) {
            synchronized (OUTSIDE) {
                OUTSIDE.remove(e);
            }
            return;
        }
        boolean outside;
        synchronized (OUTSIDE) {
            outside = Boolean.TRUE.equals(OUTSIDE.get(e));
        }
        double give = outside ? 1.0D : -1.0D;
        boolean inside = e.posX > b[0] + give && e.posX < b[2] - give && e.posZ > b[1] + give && e.posZ < b[3] - give;
        synchronized (OUTSIDE) {
            if (inside) {
                OUTSIDE.remove(e);
            } else {
                OUTSIDE.put(e, Boolean.TRUE);
            }
        }
        if (!inside) {
            return;
        }
        int x0 = MathHelper.floor_double(a.minX) - 1;
        int x1 = MathHelper.ceiling_double_int(a.maxX) + 1;
        int y0 = MathHelper.floor_double(a.minY) - 1;
        int y1 = MathHelper.ceiling_double_int(a.maxY) + 1;
        int z0 = MathHelper.floor_double(a.minZ) - 1;
        int z1 = MathHelper.ceiling_double_int(a.maxZ) + 1;
        for (int x = x0; x < x1; x++) {
            for (int z = z0; z < z1; z++) {
                if (containsBlock(b, x, z)) {
                    continue;
                }
                for (int y = y0; y < y1; y++) {
                    AxisAlignedBB block = AxisAlignedBB.getBoundingBox(x, y, z, x + 1, y + 1, z + 1);
                    if (block.intersectsWith(a)) {
                        list.add(block);
                    }
                }
            }
        }
    }

    // ---- spawning inside (EntityPlayerMP of 1.12.2 caps the spread by the distance to the border) ----

    public static int spawnSpread(World w, ChunkCoordinates c, int radius) {
        if (OFF) {
            return radius;
        }
        int j = MathHelper.floor_double(distance(of(w).box(w), c.posX, c.posZ));
        if (j < radius) {
            radius = j;
        }
        if (j <= 1) {
            radius = 1;
        }
        return radius;
    }

    // ---- events ----

    public static final class Events {
        /** EntityLivingBase.onEntityUpdate of 1.12.2: a player wholly outside, past the buffer, takes damage. */
        @SubscribeEvent
        public void onLiving(LivingEvent.LivingUpdateEvent event) {
            if (OFF || !(event.entityLiving instanceof EntityPlayer) || event.entityLiving.worldObj.isRemote || !event.entityLiving.isEntityAlive()) {
                return;
            }
            EntityPlayer p = (EntityPlayer) event.entityLiving;
            if (p.isEntityInsideOpaqueBlock()) {
                return;
            }
            Border112 border = of(p.worldObj);
            double[] b = border.box(p.worldObj);
            if (containsBox(b, p.boundingBox)) {
                return;
            }
            double d = distance(b, p.posX, p.posZ) + border.damageBuffer;
            if (d < 0.0D && border.damageAmount > 0.0D) {
                p.attackEntityFrom(DamageSource.inWall, (float) Math.max(1, MathHelper.floor_double(-d * border.damageAmount)));
            }
        }

        /** No digging and no using a block outside the border (WorldServer.isBlockModifiable, PlayerControllerMP of 1.12.2). */
        @SubscribeEvent
        public void onInteract(PlayerInteractEvent event) {
            if (OFF || event.action == PlayerInteractEvent.Action.RIGHT_CLICK_AIR) {
                return;
            }
            double[] b = of(event.world).box(event.world);
            if (!containsBlock(b, event.x, event.z)) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
            send(event.player);
        }

        @SubscribeEvent
        public void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
            send(event.player);
        }

        @SubscribeEvent
        public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
            send(event.player);
        }
    }

    // ---- the packet ----

    public static final class Msg implements IMessage {
        public double centerX, centerZ, from, to;
        public long millis;
        public double damage, buffer;
        public int warnTime, warnDistance;

        public Msg() {
        }

        Msg(Border112 b) {
            centerX = b.centerX;
            centerZ = b.centerZ;
            from = b.diameter();
            to = b.endDiameter;
            millis = b.timeUntilTarget();
            damage = b.damageAmount;
            buffer = b.damageBuffer;
            warnTime = b.warningTime;
            warnDistance = b.warningDistance;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            centerX = buf.readDouble();
            centerZ = buf.readDouble();
            from = buf.readDouble();
            to = buf.readDouble();
            millis = buf.readLong();
            damage = buf.readDouble();
            buffer = buf.readDouble();
            warnTime = buf.readInt();
            warnDistance = buf.readInt();
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeDouble(centerX);
            buf.writeDouble(centerZ);
            buf.writeDouble(from);
            buf.writeDouble(to);
            buf.writeLong(millis);
            buf.writeDouble(damage);
            buf.writeDouble(buffer);
            buf.writeInt(warnTime);
            buf.writeInt(warnDistance);
        }

        /** On the client. */
        public void apply() {
            Border112 b = new Border112();
            b.centerX = centerX;
            b.centerZ = centerZ;
            if (millis > 0L) {
                b.setTransition(from, to, millis);
            } else {
                b.setTransition(to);
            }
            b.damageAmount = damage;
            b.damageBuffer = buffer;
            b.warningTime = warnTime;
            b.warningDistance = warnDistance;
            client = b;
        }
    }

    static void send(EntityPlayer p) {
        if (!OFF && p instanceof EntityPlayerMP) {
            MC112Bridge.NETWORK.channel.sendTo(new Msg(of(p.worldObj)), (EntityPlayerMP) p);
        }
    }

    static void sendAll() {
        if (!OFF) {
            MC112Bridge.NETWORK.channel.sendToAll(new Msg(of(null)));
        }
    }

    // ---- /worldborder ----

    public static final class Command extends CommandBase {
        @Override
        public String getCommandName() {
            return "worldborder";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 2;
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "commands.worldborder.usage";
        }

        private static String f1(double d) {
            return String.format("%.1f", d);
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length < 1) {
                throw new WrongUsageException("commands.worldborder.usage");
            }
            Border112 b = of(null);
            if ("set".equals(args[0]) || "add".equals(args[0])) {
                boolean add = "add".equals(args[0]);
                if (args.length < 2) {
                    throw new WrongUsageException(add ? "commands.worldborder.add.usage" : "commands.worldborder.set.usage");
                }
                double old = add ? b.endDiameter : b.diameter();
                double size = add ? old + bounded(sender, args[1], -old, 6.0E7D - old) : bounded(sender, args[1], 1.0D, 6.0E7D);
                long time = (add ? b.timeUntilTarget() : 0L) + (args.length > 2 ? longBounded(sender, args[2]) * 1000L : 0L);
                if (time > 0L) {
                    b.setTransition(old, size, time);
                    func_152373_a(sender, this, old > size ? "commands.worldborder.setSlowly.shrink.success" : "commands.worldborder.setSlowly.grow.success",
                        f1(size), f1(old), Long.toString(time / 1000L));
                } else {
                    b.setTransition(size);
                    func_152373_a(sender, this, "commands.worldborder.set.success", f1(size), f1(old));
                }
            } else if ("center".equals(args[0])) {
                if (args.length < 3) {
                    throw new WrongUsageException("commands.worldborder.center.usage");
                }
                ChunkCoordinates c = sender.getPlayerCoordinates();
                double x = func_110666_a(sender, c.posX + 0.5D, args[1]);
                double z = func_110666_a(sender, c.posZ + 0.5D, args[2]);
                b.centerX = x;
                b.centerZ = z;
                func_152373_a(sender, this, "commands.worldborder.center.success", x, z);
            } else if ("damage".equals(args[0])) {
                if (args.length < 2) {
                    throw new WrongUsageException("commands.worldborder.damage.usage");
                }
                if ("buffer".equals(args[1])) {
                    if (args.length < 3) {
                        throw new WrongUsageException("commands.worldborder.damage.buffer.usage");
                    }
                    double v = parseDoubleWithMin(sender, args[2], 0.0D);
                    double old = b.damageBuffer;
                    b.damageBuffer = v;
                    func_152373_a(sender, this, "commands.worldborder.damage.buffer.success", f1(v), f1(old));
                } else if ("amount".equals(args[1])) {
                    if (args.length < 3) {
                        throw new WrongUsageException("commands.worldborder.damage.amount.usage");
                    }
                    double v = parseDoubleWithMin(sender, args[2], 0.0D);
                    double old = b.damageAmount;
                    b.damageAmount = v;
                    func_152373_a(sender, this, "commands.worldborder.damage.amount.success", String.format("%.2f", v), String.format("%.2f", old));
                } else {
                    throw new WrongUsageException("commands.worldborder.damage.usage");
                }
            } else if ("warning".equals(args[0])) {
                if (args.length < 2) {
                    throw new WrongUsageException("commands.worldborder.warning.usage");
                }
                if ("time".equals(args[1])) {
                    if (args.length < 3) {
                        throw new WrongUsageException("commands.worldborder.warning.time.usage");
                    }
                    int v = parseIntWithMin(sender, args[2], 0);
                    int old = b.warningTime;
                    b.warningTime = v;
                    func_152373_a(sender, this, "commands.worldborder.warning.time.success", v, old);
                } else if ("distance".equals(args[1])) {
                    if (args.length < 3) {
                        throw new WrongUsageException("commands.worldborder.warning.distance.usage");
                    }
                    int v = parseIntWithMin(sender, args[2], 0);
                    int old = b.warningDistance;
                    b.warningDistance = v;
                    func_152373_a(sender, this, "commands.worldborder.warning.distance.success", v, old);
                } else {
                    throw new WrongUsageException("commands.worldborder.warning.usage");
                }
            } else if ("get".equals(args[0])) {
                double d = b.diameter();
                Stats112.set(sender, Stats112.QUERY_RESULT, MathHelper.floor_double(d + 0.5D));
                sender.addChatMessage(new ChatComponentTranslation("commands.worldborder.get.success", String.format("%.0f", d)));
                return;
            } else {
                throw new WrongUsageException("commands.worldborder.usage");
            }
            sendAll();
        }

        private static double bounded(ICommandSender sender, String s, double min, double max) {
            double d = parseDouble(sender, s);
            if (d < min) {
                throw new net.minecraft.command.NumberInvalidException("commands.generic.double.tooSmall", d, min);
            }
            if (d > max) {
                throw new net.minecraft.command.NumberInvalidException("commands.generic.double.tooBig", d, max);
            }
            return d;
        }

        private static long longBounded(ICommandSender sender, String s) {
            try {
                long l = Long.parseLong(s);
                if (l < 0L) {
                    throw new net.minecraft.command.NumberInvalidException("commands.generic.num.tooSmall", l, 0);
                }
                if (l > 9223372036854775L) {
                    throw new net.minecraft.command.NumberInvalidException("commands.generic.num.tooBig", l, 9223372036854775L);
                }
                return l;
            } catch (NumberFormatException e) {
                throw new net.minecraft.command.NumberInvalidException("commands.generic.num.invalid", s);
            }
        }

        @Override
        @SuppressWarnings("rawtypes")
        public List addTabCompletionOptions(ICommandSender sender, String[] args) {
            if (args.length == 1) {
                return getListOfStringsMatchingLastWord(args, "set", "center", "damage", "warning", "add", "get");
            }
            if (args.length == 2 && "damage".equals(args[0])) {
                return getListOfStringsMatchingLastWord(args, "buffer", "amount");
            }
            if (args.length == 2 && "warning".equals(args[0])) {
                return getListOfStringsMatchingLastWord(args, "time", "distance");
            }
            return null;
        }
    }
}
