package ru.codex.offhand1710.client;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.client.Minecraft;
import java.util.Random;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import ru.codex.offhand1710.OffhandInventory;

/**
 * First-person animation state of the local player's offhand: the equip (raise/lower) progress and the
 * arm swing. Both follow the vanilla main-hand code of 1.7.10 (ItemRenderer.updateEquippedItem and
 * EntityLivingBase.updateArmSwingProgress), ticked once per client tick.
 */
public final class OffhandClientState {
    private static ItemStack itemToRender;
    private static float equippedProgress;
    private static float prevEquippedProgress;

    /** The item drawn in first person; lags behind the real offhand item while it is lowered and raised. */
    public static ItemStack renderedStack() {
        return itemToRender;
    }

    public static float equipped(float partialTicks) {
        return prevEquippedProgress + (equippedProgress - prevEquippedProgress) * partialTicks;
    }

    /** Offhand swing of the local player (first person). */
    public static float swing(float partialTicks) {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        return player == null ? 0.0F : OffhandInventory.get(player).swing(partialTicks);
    }

    /** Same rule as EntityLivingBase.swingItem, for any player in view. */
    public static void startSwing(EntityPlayer player) {
        OffhandInventory offhand = OffhandInventory.get(player);
        int end = swingEnd(player);
        if (!offhand.swinging || offhand.swingTicks >= end / 2 || offhand.swingTicks < 0) {
            offhand.swingTicks = -1;
            offhand.swinging = true;
        }
    }

    /** Same as EntityLivingBase.updateArmSwingProgress. */
    private static void tickSwing(EntityPlayer player) {
        OffhandInventory offhand = OffhandInventory.get(player);
        offhand.prevSwingProgress = offhand.swingProgress;
        int end = swingEnd(player);
        if (offhand.swinging) {
            ++offhand.swingTicks;
            if (offhand.swingTicks >= end) {
                offhand.swingTicks = 0;
                offhand.swinging = false;
            }
        } else {
            offhand.swingTicks = 0;
        }
        offhand.swingProgress = (float) offhand.swingTicks / (float) end;
    }

    /** Same as ItemRenderer.resetEquippedProgress: the item dips and comes back up. */
    public static void resetEquip() {
        equippedProgress = 0.0F;
    }

    /** The item the local player started using, for the particles when it is finished (it may be gone by then). */
    private static ItemStack lastUsed;

    static void startUse(ItemStack stack) {
        lastUsed = stack == null ? null : stack.copy();
    }

    /** EntityPlayer.onItemUseFinish on the client: the final burst of eating particles. */
    static void finishUse(EntityPlayer player) {
        if (lastUsed != null) {
            useEffects(player, lastUsed, 16);
        }
    }

    /** EntityPlayer.updateItemUse (1.7.10): drinking sound, or eating particles from the mouth and sound. */
    static void useEffects(EntityPlayer player, ItemStack stack, int particles) {
        World world = player.worldObj;
        Random random = world.rand;
        EnumAction action = stack.getItemUseAction();
        if (action == EnumAction.drink) {
            world.playSound(player.posX, player.posY, player.posZ, "random.drink", 0.5F,
                    random.nextFloat() * 0.1F + 0.9F, false);
        }
        if (action != EnumAction.eat) {
            return;
        }
        String particle = "iconcrack_" + Item.getIdFromItem(stack.getItem());
        if (stack.getHasSubtypes()) {
            particle = particle + "_" + stack.getItemDamage();
        }
        float pitch = -player.rotationPitch * (float) Math.PI / 180.0F;
        float yaw = -player.rotationYaw * (float) Math.PI / 180.0F;
        for (int i = 0; i < particles; ++i) {
            Vec3 motion = Vec3.createVectorHelper(((double) random.nextFloat() - 0.5D) * 0.1D,
                    Math.random() * 0.1D + 0.1D, 0.0D);
            motion.rotateAroundX(pitch);
            motion.rotateAroundY(yaw);
            Vec3 position = Vec3.createVectorHelper(((double) random.nextFloat() - 0.5D) * 0.3D,
                    (double) (-random.nextFloat()) * 0.6D - 0.3D, 0.6D);
            position.rotateAroundX(pitch);
            position.rotateAroundY(yaw);
            position = position.addVector(player.posX, player.posY + (double) player.getEyeHeight(), player.posZ);
            world.spawnParticle(particle, position.xCoord, position.yCoord, position.zCoord,
                    motion.xCoord, motion.yCoord + 0.05D, motion.zCoord);
        }
        world.playSound(player.posX, player.posY, player.posZ, "random.eat", 0.5F + 0.5F * (float) random.nextInt(2),
                (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false);
    }

    /**
     * Offhand held use of every player in view: count down and play the eating/drinking effects every 4 ticks
     * during the last 25 (as EntityPlayer.onUpdate). The local player's count never reaches 0 on its own, the
     * server's stop message ends it, so the client never thinks the hand is free a tick too early.
     */
    private static void tickUses(Minecraft mc) {
        for (Object object : mc.theWorld.playerEntities) {
            EntityPlayer player = (EntityPlayer) object;
            OffhandInventory offhand = OffhandInventory.get(player);
            if (offhand.useCount <= 0) {
                continue;
            }
            ItemStack stack = offhand.getStack();
            if (stack == null) {
                offhand.useCount = 0;
                continue;
            }
            if (offhand.useCount <= 25 && offhand.useCount % 4 == 0) {
                useEffects(player, stack, 5);
            }
            if (offhand.useCount > 1 || player != mc.thePlayer) {
                offhand.useCount--;
            }
        }
    }

    private static int swingEnd(EntityPlayer player) {
        if (player.isPotionActive(Potion.digSpeed)) {
            return 6 - (1 + player.getActivePotionEffect(Potion.digSpeed).getAmplifier());
        }
        if (player.isPotionActive(Potion.digSlowdown)) {
            return 6 + (1 + player.getActivePotionEffect(Potion.digSlowdown).getAmplifier()) * 2;
        }
        return 6;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;
        if (player == null) {
            itemToRender = null;
            equippedProgress = prevEquippedProgress = 0.0F;
            return;
        }
        if (mc.isGamePaused()) {
            return;
        }

        if (mc.theWorld != null) {
            for (Object object : mc.theWorld.playerEntities) {
                tickSwing((EntityPlayer) object);
            }
        }

        prevEquippedProgress = equippedProgress;
        ItemStack current = OffhandInventory.get(player).getStack();
        boolean same = current == itemToRender;
        // A synced copy of the same item (same id and damage) is not a new item: no re-equip animation.
        if (current != null && itemToRender != null && current != itemToRender
                && current.getItem() == itemToRender.getItem()
                && current.getItemDamage() == itemToRender.getItemDamage()) {
            itemToRender = current;
            same = true;
        }
        float delta = (same ? 1.0F : 0.0F) - equippedProgress;
        if (delta < -0.4F) {
            delta = -0.4F;
        }
        if (delta > 0.4F) {
            delta = 0.4F;
        }
        equippedProgress += delta;
        if (equippedProgress < 0.1F) {
            itemToRender = current;
        }

        if (mc.thePlayer.movementInput != null && !(mc.thePlayer.movementInput instanceof OffhandMovementInput)) {
            mc.thePlayer.movementInput = new OffhandMovementInput(mc.thePlayer.movementInput, mc.thePlayer);
        }
        if (mc.theWorld != null) {
            tickUses(mc);
        }
        OffhandInventory own = OffhandInventory.get(player);
        if (own.shieldCooldown > 0) {
            own.shieldCooldown--;
        }
    }
}
