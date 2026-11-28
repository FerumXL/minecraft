package ru.codex.offhand1710;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.EntityInteractEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;

/**
 * Spectator mode of 1.12.2 (GameType.SPECTATOR) on top of the 1.7.10 creative mode, which has no fourth game type:
 * the player flies through blocks (noClip, which NetHandlerPlayServer and EntityPlayerSP of 1.7.10 already respect),
 * cannot touch blocks, entities or items, is not targeted by mobs and is not drawn for other players; the hotbar and
 * the hand are hidden, a left click on a living entity looks through its eyes until sneaking.
 * The flag is kept in the persisted player data, so it survives death and relog.
 */
public final class Spectator112 {
    static final String TAG = "mc112Spectator";
    /** Client: ids of the players in spectator mode. */
    public static final Set<Integer> CLIENT = new HashSet<Integer>();
    /** Server: what each spectator is looking through. */
    static final Map<EntityPlayerMP, Entity> SPECTATING = new HashMap<EntityPlayerMP, Entity>();

    private static final ThreadLocal<java.util.List<Object>> HIDDEN = new ThreadLocal<java.util.List<Object>>();

    /**
     * Start of SpawnerAnimals.findChunksForSpawning: WorldEntitySpawner of 1.12.2 skips spectators (no chunks around
     * them count for spawning, and nothing is generated for it), so they are out of the world's player list meanwhile.
     */
    public static void hideForSpawning(net.minecraft.world.WorldServer w) {
        showAfterSpawning(w);
        if (System.getProperty("mc112.spectatorSpawn17") != null) {
            return;
        }
        java.util.List<Object> out = null;
        for (java.util.Iterator<?> it = w.playerEntities.iterator(); it.hasNext();) {
            Object o = it.next();
            if (o instanceof EntityPlayer && is((EntityPlayer) o)) {
                if (out == null) {
                    out = new java.util.ArrayList<Object>();
                }
                out.add(o);
                it.remove();
            }
        }
        if (out != null) {
            HIDDEN.set(out);
        }
    }

    /** Every return of findChunksForSpawning (and the end of the world tick, in case it threw). */
    @SuppressWarnings("unchecked")
    public static void showAfterSpawning(net.minecraft.world.WorldServer w) {
        java.util.List<Object> out = HIDDEN.get();
        if (out != null) {
            HIDDEN.remove();
            ((java.util.List<Object>) w.playerEntities).addAll(out);
        }
    }

    @SubscribeEvent
    public void onWorldTickEnd(cpw.mods.fml.common.gameevent.TickEvent.WorldTickEvent event) {
        if (event.phase == cpw.mods.fml.common.gameevent.TickEvent.Phase.END && event.world instanceof net.minecraft.world.WorldServer) {
            showAfterSpawning((net.minecraft.world.WorldServer) event.world);
        }
    }

    public static boolean is(EntityPlayer p) {
        if (p == null) {
            return false;
        }
        if (p.worldObj.isRemote) {
            return CLIENT.contains(p.getEntityId());
        }
        return p.getEntityData().getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG).getBoolean(TAG);
    }

    public static void set(EntityPlayerMP p, boolean on) {
        NBTTagCompound data = p.getEntityData();
        NBTTagCompound persisted = data.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        persisted.setBoolean(TAG, on);
        data.setTag(EntityPlayer.PERSISTED_NBT_TAG, persisted);
        if (on) {
            p.setGameType(WorldSettings.GameType.CREATIVE);
            p.capabilities.isFlying = true;
            p.mountEntity(null);
            Shoulder112.dropAll(p);
        } else {
            p.noClip = false;
            SPECTATING.remove(p);
        }
        p.sendPlayerAbilities();
        sync(p);
    }

    static void sync(EntityPlayerMP p) {
        Msg m = new Msg(p.getEntityId(), is(p));
        MC112Bridge.NETWORK.channel.sendToAllAround(m, new NetworkRegistry.TargetPoint(p.dimension, p.posX, p.posY, p.posZ, 256.0D));
        MC112Bridge.NETWORK.channel.sendTo(m, p);
    }

    @SubscribeEvent
    public void onTick(TickEvent.PlayerTickEvent event) {
        EntityPlayer p = event.player;
        if (is(p)) {
            p.noClip = true;
            p.capabilities.allowFlying = true;
            p.capabilities.isFlying = true;
            p.fallDistance = 0.0F;
            p.setFire(0);
            p.extinguish();
            if (!p.worldObj.isRemote && event.phase == TickEvent.Phase.END) {
                Entity target = SPECTATING.get(p);
                if (target != null) {
                    if (target.isDead || target.worldObj != p.worldObj || p.isSneaking()) {
                        SPECTATING.remove(p);
                    } else {
                        ((EntityPlayerMP) p).playerNetServerHandler.setPlayerLocation(target.posX, target.posY, target.posZ, p.rotationYaw, p.rotationPitch);
                    }
                }
            }
        } else if (p.noClip) {
            // no player of 1.7.10 has noClip otherwise
            p.noClip = false;
        }
    }

    @SubscribeEvent
    public void onTrack(PlayerEvent.StartTracking event) {
        if (event.target instanceof EntityPlayerMP && event.entityPlayer instanceof EntityPlayerMP && is((EntityPlayer) event.target)) {
            MC112Bridge.NETWORK.channel.sendTo(new Msg(event.target.getEntityId(), true), (EntityPlayerMP) event.entityPlayer);
        }
    }

    @SubscribeEvent
    public void onLogin(PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            sync((EntityPlayerMP) event.player);
        }
    }

    @SubscribeEvent
    public void onRespawn(PlayerRespawnEvent event) {
        if (event.player instanceof EntityPlayerMP && is(event.player)) {
            set((EntityPlayerMP) event.player, true);
        }
    }

    // ---- what a spectator may not do (both sides, so the client does not even try) ----

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (is(event.entityPlayer)) {
            event.setCanceled(true);
            if (event.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK && !event.entityPlayer.worldObj.isRemote && !CONTAINERS_OFF) {
                openContainer(event.entityPlayer, event.x, event.y, event.z);
            }
        }
    }

    /** -Dmc112.spectatorChests17: a spectator opens nothing, as before. */
    static final boolean CONTAINERS_OFF = System.getProperty("mc112.spectatorChests17") != null;

    /**
     * PlayerInteractionManager.processRightClickBlock of 1.12.2 for a spectator: a container opens to look into
     * (Container.slotClick refuses its clicks, see noClick); nothing else is touched.
     */
    static void openContainer(EntityPlayer player, int x, int y, int z) {
        net.minecraft.world.World w = player.worldObj;
        net.minecraft.tileentity.TileEntity te = w.getTileEntity(x, y, z);
        if (te instanceof net.minecraft.tileentity.TileEntityChest && w.getBlock(x, y, z) instanceof net.minecraft.block.BlockChest) {
            net.minecraft.inventory.IInventory inv = ((net.minecraft.block.BlockChest) w.getBlock(x, y, z)).func_149951_m(w, x, y, z);
            if (inv != null) {
                player.displayGUIChest(inv);
            }
        } else if (te instanceof net.minecraft.tileentity.TileEntityFurnace) {
            player.func_146101_a((net.minecraft.tileentity.TileEntityFurnace) te);
        } else if (te instanceof net.minecraft.tileentity.TileEntityDispenser) {
            player.func_146102_a((net.minecraft.tileentity.TileEntityDispenser) te);
        } else if (te instanceof net.minecraft.tileentity.TileEntityBrewingStand) {
            player.func_146098_a((net.minecraft.tileentity.TileEntityBrewingStand) te);
        } else if (te instanceof net.minecraft.tileentity.TileEntityHopper) {
            player.func_146093_a((net.minecraft.tileentity.TileEntityHopper) te);
        } else if (te instanceof net.minecraft.inventory.IInventory) {
            player.displayGUIChest((net.minecraft.inventory.IInventory) te);
        }
    }

    /** Start of Container.slotClick (coremod): a spectator may look, not take. */
    public static boolean noClick(EntityPlayer player) {
        return !CONTAINERS_OFF && player != null && is(player);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onAttack(AttackEntityEvent event) {
        if (is(event.entityPlayer)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityInteract(EntityInteractEvent event) {
        if (is(event.entityPlayer)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPickup(EntityItemPickupEvent event) {
        if (is(event.entityPlayer)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onBreak(BlockEvent.BreakEvent event) {
        if (is(event.getPlayer())) {
            event.setCanceled(true);
        }
    }

    /** Cancelling a toss would lose the item in 1.7.10: it goes back into the inventory instead. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onToss(ItemTossEvent event) {
        if (is(event.player)) {
            event.player.inventory.addItemStackToInventory(event.entityItem.getEntityItem());
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onTarget(LivingSetAttackTargetEvent event) {
        if (event.target instanceof EntityPlayer && is((EntityPlayer) event.target) && event.entityLiving instanceof EntityLiving) {
            ((EntityLiving) event.entityLiving).setAttackTarget(null);
        }
    }

    /** Client -> server: look through this entity (-1 = back to the own eyes). */
    public static final class SpectateMsg implements IMessage {
        public int entityId;

        public SpectateMsg() {
        }

        public SpectateMsg(int entityId) {
            this.entityId = entityId;
        }

        @Override
        public void fromBytes(ByteBuf b) {
            entityId = b.readInt();
        }

        @Override
        public void toBytes(ByteBuf b) {
            b.writeInt(entityId);
        }
    }

    public static final class SpectateHandler implements IMessageHandler<SpectateMsg, IMessage> {
        @Override
        public IMessage onMessage(SpectateMsg message, MessageContext context) {
            EntityPlayerMP p = context.getServerHandler().playerEntity;
            if (!is(p)) {
                return null;
            }
            Entity e = message.entityId < 0 ? null : p.worldObj.getEntityByID(message.entityId);
            if (e == null || e == p) {
                SPECTATING.remove(p);
            } else if (e.getDistanceSqToEntity(p) < 64.0D * 64.0D) {
                SPECTATING.put(p, e);
            }
            return null;
        }
    }

    /** Client -> server: CPacketSpectate of 1.12.2, teleport this spectator to the named player (any dimension). */
    public static final class TeleportMsg implements IMessage {
        public String name;

        public TeleportMsg() {
        }

        public TeleportMsg(String name) {
            this.name = name;
        }

        @Override
        public void fromBytes(ByteBuf b) {
            name = cpw.mods.fml.common.network.ByteBufUtils.readUTF8String(b);
        }

        @Override
        public void toBytes(ByteBuf b) {
            cpw.mods.fml.common.network.ByteBufUtils.writeUTF8String(b, name);
        }
    }

    public static final class TeleportHandler implements IMessageHandler<TeleportMsg, IMessage> {
        @Override
        public IMessage onMessage(TeleportMsg m, MessageContext context) {
            EntityPlayerMP p = context.getServerHandler().playerEntity;
            EntityPlayerMP target = net.minecraft.server.MinecraftServer.getServer().getConfigurationManager().func_152612_a(m.name);
            if (!is(p) || target == null || target == p) {
                return null;
            }
            SPECTATING.remove(p);
            if (target.dimension != p.dimension) {
                net.minecraft.server.MinecraftServer.getServer().getConfigurationManager().transferPlayerToDimension(p, target.dimension);
            }
            p.playerNetServerHandler.setPlayerLocation(target.posX, target.posY, target.posZ, target.rotationYaw, target.rotationPitch);
            return null;
        }
    }

    /** Server -> clients: a player entered or left spectator mode. */
    public static final class Msg implements IMessage {
        public int entityId;
        public boolean on;

        public Msg() {
        }

        public Msg(int entityId, boolean on) {
            this.entityId = entityId;
            this.on = on;
        }

        @Override
        public void fromBytes(ByteBuf b) {
            entityId = b.readInt();
            on = b.readBoolean();
        }

        @Override
        public void toBytes(ByteBuf b) {
            b.writeInt(entityId);
            b.writeBoolean(on);
        }
    }

    /** /gamemode of 1.12.2: survival, creative, adventure, spectator (names, letters s c a sp, or 0-3), any player selector. */
    public static final class CommandGameMode extends CommandBase {
        @Override
        public String getCommandName() {
            return "gamemode";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 2;
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "/gamemode <survival|creative|adventure|spectator> [player]";
        }

        @Override
        public boolean isUsernameIndex(String[] args, int index) {
            return index == 1;
        }

        @Override
        public List addTabCompletionOptions(ICommandSender sender, String[] args) {
            return args.length == 1 ? getListOfStringsMatchingLastWord(args, "survival", "creative", "adventure", "spectator")
                : args.length == 2 ? getListOfStringsMatchingLastWord(args, net.minecraft.server.MinecraftServer.getServer().getAllUsernames()) : null;
        }

        static int mode(String s) {
            String m = s.toLowerCase();
            if (m.equals("survival") || m.equals("s") || m.equals("0")) {
                return 0;
            }
            if (m.equals("creative") || m.equals("c") || m.equals("1")) {
                return 1;
            }
            if (m.equals("adventure") || m.equals("a") || m.equals("2")) {
                return 2;
            }
            if (m.equals("spectator") || m.equals("sp") || m.equals("3")) {
                return 3;
            }
            throw new WrongUsageException("/gamemode <survival|creative|adventure|spectator> [player]");
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length < 1) {
                throw new WrongUsageException(getCommandUsage(sender));
            }
            int mode = mode(args[0]);
            java.util.List<EntityPlayerMP> players = new java.util.ArrayList<EntityPlayerMP>();
            if (args.length >= 2) {
                for (Entity e : Commands112.targets(sender, args[1])) {
                    if (e instanceof EntityPlayerMP) {
                        players.add((EntityPlayerMP) e);
                    }
                }
            } else {
                players.add(getCommandSenderAsPlayer(sender));
            }
            String[] names = {"survival", "creative", "adventure", "spectator"};
            for (EntityPlayerMP p : players) {
                if (mode == 3) {
                    set(p, true);
                } else {
                    if (is(p)) {
                        set(p, false);
                    }
                    p.setGameType(WorldSettings.GameType.getByID(mode));
                    p.fallDistance = 0.0F;
                }
                ChatComponentTranslation name = new ChatComponentTranslation("gameMode." + names[mode]);
                if (p != sender) {
                    func_152374_a(sender, this, 1, "commands.gamemode.success.other", p.getCommandSenderName(), name);
                } else {
                    func_152374_a(sender, this, 1, "commands.gamemode.success.self", name);
                }
            }
        }
    }
}
