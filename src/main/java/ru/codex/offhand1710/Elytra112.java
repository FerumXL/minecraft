package ru.codex.offhand1710;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

/**
 * Server side of the elytra. The flag follows EntityLivingBase.updateElytra of 1.12.2. The flight itself is simulated
 * by the owner's client, as all player movement; what 1.12.2 reads from the server's motion is taken from the position
 * changes here, because the 1.7.10 server has no motion of its players.
 */
public final class Elytra112 {
    private static final DamageSource FLY_INTO_WALL = new DamageSource("flyIntoWall") {
        {
            setDamageBypassesArmor();
        }
    };

    public static ItemElytra elytra;

    /** EntityPlayer of 1.12.2 while gliding: size 0.6 x 0.6, eye 0.4 above the feet. */
    public static final float GLIDE_HEIGHT = 0.6F;
    public static final float GLIDE_EYE = 0.4F;
    /** -Dmc112.elytraSize17=1: keep the 1.7.10 1.8-high box and eye (comparison runs). */
    public static final boolean OLD_SIZE = "1".equals(System.getProperty("mc112.elytraSize17"));

    /**
     * Only height and the box change; yOffset stays 1.62, so the client still reports posY - minY = 1.62 and the
     * server stance check ("Illegal stance", 0.1..1.65) is not touched. Both sides apply the same box, so the
     * server's collision check of a move agrees with the client.
     */
    public static void applySize(net.minecraft.entity.player.EntityPlayer p, boolean gliding) {
        if (OLD_SIZE || p.isPlayerSleeping()) {
            return;
        }
        float want = gliding ? GLIDE_HEIGHT : 1.8F;
        if (p.height != want && (gliding || p.height == GLIDE_HEIGHT)) {
            p.height = want;
            p.boundingBox.maxY = p.boundingBox.minY + (double) want;
        }
    }

    /** DamageSource.FIREWORKS of 1.12.2. */
    private static final DamageSource FIREWORKS = new DamageSource("fireworks").setExplosion();

    /** Rockets boosting a gliding player (EntityFireworkRocket.boostedEntity of 1.12.2). */
    private static final java.util.Map<net.minecraft.entity.item.EntityFireworkRocket, EntityPlayerMP> ROCKETS =
            new java.util.HashMap<net.minecraft.entity.item.EntityFireworkRocket, EntityPlayerMP>();

    public static void register() {
        elytra = new ItemElytra();
        GameRegistry.registerItem(elytra, "elytra");
        // 1.7.10 leaves these out of the creative inventory; 1.12.2 has them under Miscellaneous.
        Items.fireworks.setCreativeTab(CreativeTabs.tabMisc);
        Items.firework_charge.setCreativeTab(CreativeTabs.tabMisc);
    }

    /** The worn elytra if it still works, otherwise null. */
    public static ItemStack usableElytra(EntityPlayer player) {
        ItemStack chest = player.inventory.armorInventory[2];
        return chest != null && chest.getItem() instanceof ItemElytra && ItemElytra.isUsable(chest) ? chest : null;
    }

    public static boolean canFly(EntityPlayer player) {
        return !player.onGround && !player.isRiding() && !player.capabilities.isFlying && !player.isInWater()
                && !player.handleLavaMovement() && usableElytra(player) != null;
    }

    /** Client asked: jump pressed in the air (NetHandlerPlayServer, START_FALL_FLYING). */
    public static void start(EntityPlayerMP player) {
        OffhandInventory state = OffhandInventory.get(player);
        if (!state.elytraFlying && canFly(player)) {
            state.elytraFlying = true;
            state.elytraTicks = 0;
            state.elytraLastSpeed = 0.0D;
            broadcast(player, 0);
        }
    }

    private static void broadcast(EntityPlayerMP player, int boost) {
        MC112Bridge.NETWORK.channel.sendToAllAround(
                new OffhandMessages.ElytraState(player.getEntityId(), OffhandInventory.get(player).elytraFlying, boost),
                new NetworkRegistry.TargetPoint(player.dimension, player.posX, player.posY, player.posZ, 128.0D));
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !(event.player instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        OffhandInventory state = OffhandInventory.get(player);
        if (state.elytraStartRequested) {
            state.elytraStartRequested = false;
            start(player);
        }
        applySize(player, state.elytraFlying && canFly(player));
        if (!state.elytraFlying) {
            return;
        }
        if (!canFly(player)) {
            state.elytraFlying = false;
            state.elytraTicks = 0;
            broadcast(player, 0);
            return;
        }
        state.elytraTicks++;
        if (state.elytraTicks % 20 == 0) {
            usableElytra(player).damageItem(1, player);
            broadcast(player, 0); // also tells players who came into view during the flight
        }
        double deltaX = player.posX - player.prevPosX;
        double deltaZ = player.posZ - player.prevPosZ;
        if (player.posY - player.prevPosY > -0.5D) {
            player.fallDistance = 1.0F;
        }
        double speed = (double) MathHelper.sqrt_double(deltaX * deltaX + deltaZ * deltaZ);
        float impact = (float) ((state.elytraLastSpeed - speed) * 10.0D - 3.0D);
        // 1.12.2 counts a speed drop only when the glide hit a wall (collidedHorizontally), not when it was stopped
        // at the edge of the loaded chunks or slowed by a rocket
        if (impact > 0.0F && player.isCollidedHorizontally) {
            player.playSound(impact > 4.0F ? "game.player.hurt.fall.big" : "game.player.hurt.fall.small", 1.0F, 1.0F);
            player.attackEntityFrom(FLY_INTO_WALL, impact);
        }
        state.elytraLastSpeed = speed;
        // Gliding counts as floating for the "flying is not enabled" kick of a dedicated server.
        ReflectionHelper.setPrivateValue(NetHandlerPlayServer.class, player.playerNetServerHandler, Integer.valueOf(0),
                "field_147365_f", "floatingTickCount");
    }

    /** EntityFireworkRocket.onUpdate of 1.12.2 for a rocket with a boosted entity: it stays at the player. */
    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote || ROCKETS.isEmpty()) {
            return;
        }
        java.util.Iterator<java.util.Map.Entry<net.minecraft.entity.item.EntityFireworkRocket, EntityPlayerMP>> it = ROCKETS.entrySet().iterator();
        while (it.hasNext()) {
            java.util.Map.Entry<net.minecraft.entity.item.EntityFireworkRocket, EntityPlayerMP> e = it.next();
            net.minecraft.entity.item.EntityFireworkRocket rocket = e.getKey();
            EntityPlayerMP player = e.getValue();
            if (rocket.worldObj != event.world) {
                continue;
            }
            if (rocket.isDead) {
                it.remove();
                explode(rocket, player);
                continue;
            }
            if (player.isDead || player.worldObj != rocket.worldObj) {
                continue;
            }
            rocket.setPosition(player.posX, player.posY, player.posZ);
            rocket.motionX = player.posX - player.prevPosX;
            rocket.motionY = player.posY - player.prevPosY;
            rocket.motionZ = player.posZ - player.prevPosZ;
        }
    }

    /** EntityFireworkRocket.dealExplosionDamage of 1.12.2: 5 + 2 per explosion to the boosted player. */
    private static void explode(net.minecraft.entity.item.EntityFireworkRocket rocket, EntityPlayerMP player) {
        ItemStack stack = rocket.getDataWatcher().getWatchableObjectItemStack(8);
        NBTTagCompound tag = stack == null ? null : stack.getTagCompound();
        if (tag == null || !tag.hasKey("Fireworks")) {
            return;
        }
        int explosions = tag.getCompoundTag("Fireworks").getTagList("Explosions", 10).tagCount();
        if (explosions > 0 && !player.isDead) {
            player.attackEntityFrom(FIREWORKS, 5.0F + (float) (explosions * 2));
        }
    }

    @SubscribeEvent
    public void onInteract(PlayerInteractEvent event) {
        if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_AIR || !(event.entityPlayer instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.entityPlayer;
        ItemStack held = player.getCurrentEquippedItem();
        if (useFirework(player, held) && held.stackSize <= 0) {
            player.inventory.mainInventory[player.inventory.currentItem] = null;
        }
    }

    /** ItemFirework.onItemRightClick of 1.12.2. The caller clears the slot when the stack runs out. */
    public static boolean useFirework(EntityPlayerMP player, ItemStack stack) {
        OffhandInventory state = OffhandInventory.get(player);
        if (stack == null || stack.getItem() != Items.fireworks || !state.elytraFlying) {
            return false;
        }
        int flight = 1;
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null && tag.hasKey("Fireworks")) {
            flight += tag.getCompoundTag("Fireworks").getByte("Flight");
        }
        int life = 10 * flight + player.getRNG().nextInt(6) + player.getRNG().nextInt(7);
        player.worldObj.playSoundAtEntity(player, "fireworks.launch", 3.0F, 1.0F);
        // the rocket itself rides with the player and goes off at the end (spark trail, explosion, damage)
        net.minecraft.entity.item.EntityFireworkRocket rocket = new net.minecraft.entity.item.EntityFireworkRocket(player.worldObj, player.posX, player.posY, player.posZ, stack.copy());
        ReflectionHelper.setPrivateValue(net.minecraft.entity.item.EntityFireworkRocket.class, rocket, Integer.valueOf(life), "lifetime", "field_92055_b");
        rocket.motionX = 0.0D;
        rocket.motionY = 0.0D;
        rocket.motionZ = 0.0D;
        if (player.worldObj.spawnEntityInWorld(rocket)) {
            ROCKETS.put(rocket, player);
        }
        if (!player.capabilities.isCreativeMode) {
            stack.stackSize--;
        }
        broadcast(player, life);
        return true;
    }
}
