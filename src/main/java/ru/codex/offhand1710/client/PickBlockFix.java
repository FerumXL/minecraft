package ru.codex.offhand1710.client;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.common.ForgeHooks;
import ru.codex.offhand1710.OffhandInventory;

/**
 * Middle click (pick block). Vanilla Minecraft.func_147112_ai tells the server in creative mode which slot
 * of the player container got the block as "number of slots - 9 + selected hotbar slot", assuming the
 * hotbar is the last 9 slots. The offhand slot is appended after them, so that lands one slot to the right
 * and the block appears twice. The presses are handled here first with the same logic and the real hotbar
 * position (slots 36-44 of ContainerPlayer); vanilla then finds no press left.
 */
public final class PickBlockFix {
    private static final int HOTBAR_START = 36;

    /**
     * A mouse press is read and acted on by vanilla later in the same tick, after the tick event below has already
     * run, so it has to be taken here, where it can still be cancelled.
     */
    @SubscribeEvent
    public void onMouse(MouseEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityClientPlayerMP player = mc.thePlayer;
        if (!event.buttonstate || player == null || mc.theWorld == null || mc.playerController == null
                || mc.currentScreen != null || event.button - 100 != mc.gameSettings.keyBindPickBlock.getKeyCode()) {
            return;
        }
        event.setCanceled(true);
        if (!player.isUsingItem() && !OffhandInventory.get(player).isUsing()) {
            pickBlock(mc, player);
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        EntityClientPlayerMP player = mc.thePlayer;
        if (player == null || mc.theWorld == null || mc.playerController == null || mc.currentScreen != null) {
            return;
        }
        KeyBinding pick = mc.gameSettings.keyBindPickBlock;
        boolean busy = player.isUsingItem() || OffhandInventory.get(player).isUsing();
        while (pick.isPressed()) {
            if (!busy) {
                pickBlock(mc, player); // while an item is in use vanilla swallows the press, so do we
            }
        }
    }

    private static void pickBlock(Minecraft mc, EntityClientPlayerMP player) {
        if (mc.objectMouseOver == null) {
            return;
        }
        boolean creative = player.capabilities.isCreativeMode;
        if (!ForgeHooks.onPickBlock(mc.objectMouseOver, player, mc.theWorld)) {
            return;
        }
        if (creative) {
            int current = player.inventory.currentItem;
            mc.playerController.sendSlotPacket(player.inventory.getStackInSlot(current), HOTBAR_START + current);
        }
    }
}
