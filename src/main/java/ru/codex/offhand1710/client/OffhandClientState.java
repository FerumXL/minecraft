package ru.codex.offhand1710.client;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
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
    private static boolean swinging;
    private static int swingTicks;
    private static float swingProgress;
    private static float prevSwingProgress;

    /** The item drawn in first person; lags behind the real offhand item while it is lowered and raised. */
    public static ItemStack renderedStack() {
        return itemToRender;
    }

    public static float equipped(float partialTicks) {
        return prevEquippedProgress + (equippedProgress - prevEquippedProgress) * partialTicks;
    }

    public static float swing(float partialTicks) {
        float delta = swingProgress - prevSwingProgress;
        if (delta < 0.0F) {
            delta += 1.0F;
        }
        return prevSwingProgress + delta * partialTicks;
    }

    /** Same rule as EntityLivingBase.swingItem. */
    public static void startSwing(EntityPlayer player) {
        int end = swingEnd(player);
        if (!swinging || swingTicks >= end / 2 || swingTicks < 0) {
            swingTicks = -1;
            swinging = true;
        }
    }

    /** Same as ItemRenderer.resetEquippedProgress: the item dips and comes back up. */
    public static void resetEquip() {
        equippedProgress = 0.0F;
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
            swinging = false;
            swingTicks = 0;
            swingProgress = prevSwingProgress = 0.0F;
            return;
        }
        if (mc.isGamePaused()) {
            return;
        }

        prevSwingProgress = swingProgress;
        int end = swingEnd(player);
        if (swinging) {
            ++swingTicks;
            if (swingTicks >= end) {
                swingTicks = 0;
                swinging = false;
            }
        } else {
            swingTicks = 0;
        }
        swingProgress = (float) swingTicks / (float) end;

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
    }
}
