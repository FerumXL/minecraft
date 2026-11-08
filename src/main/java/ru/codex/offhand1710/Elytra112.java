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
        if (impact > 0.0F) {
            player.playSound(impact > 4.0F ? "game.player.hurt.fall.big" : "game.player.hurt.fall.small", 1.0F, 1.0F);
            player.attackEntityFrom(FLY_INTO_WALL, impact);
        }
        state.elytraLastSpeed = speed;
        // Gliding counts as floating for the "flying is not enabled" kick of a dedicated server.
        ReflectionHelper.setPrivateValue(NetHandlerPlayServer.class, player.playerNetServerHandler, Integer.valueOf(0),
                "field_147365_f", "floatingTickCount");
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
        if (!player.capabilities.isCreativeMode) {
            stack.stackSize--;
        }
        broadcast(player, life);
        return true;
    }
}
