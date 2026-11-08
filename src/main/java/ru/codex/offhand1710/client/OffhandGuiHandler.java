package ru.codex.offhand1710.client;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import ru.codex.offhand1710.InventoryOffhand;
import ru.codex.offhand1710.MC112Bridge;
import ru.codex.offhand1710.SlotOffhand;

/**
 * Inventory screens: the survival inventory gets the offhand slot frame; the creative inventory tab gets the
 * slot moved to a free place (its own layout would put it on top of the first hotbar slot) and creative edits
 * of the slot sent to the server. The frame of the creative slot is part of the tab texture
 * (assets/minecraft/textures/gui/container/creative_inventory/tab_inventory.png, the 1.7.10 texture with the
 * offhand frame of the 1.12.2 one), so it is drawn with the background, under the items and tooltips.
 */
public final class OffhandGuiHandler {
    /** Free area right of the armor slots and the player in the creative inventory tab. */
    private static final int CREATIVE_X = 88;
    private static final int CREATIVE_Y = 20;

    private static Container syncedContainer;

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.gui != null && event.gui.getClass() == GuiInventory.class && mc.thePlayer != null) {
            event.gui = new GuiInventoryOffhand(mc.thePlayer);
        }
        if (event.gui instanceof GuiContainerCreative && mc.thePlayer != null
                && mc.thePlayer.inventoryContainer != syncedContainer) {
            syncedContainer = mc.thePlayer.inventoryContainer;
            syncedContainer.addCraftingToCrafters(new OffhandCreativeSync());
        }
    }

    @SubscribeEvent
    public void onTextureStitch(TextureStitchEvent.Pre event) {
        if (event.map.getTextureType() == 1) {
            SlotOffhand.emptyIcon = event.map.registerIcon(MC112Bridge.MOD_ID + ":empty_armor_slot_shield");
        }
    }

    private static Slot creativeSlot(GuiContainerCreative gui) {
        for (Object object : gui.inventorySlots.inventorySlots) {
            Slot slot = (Slot) object;
            if (slot.inventory instanceof InventoryOffhand) {
                return slot;
            }
        }
        return null;
    }

    /** Runs before the screen draws and before the next click is handled, after any tab switch. */
    @SubscribeEvent
    public void onDrawPre(GuiScreenEvent.DrawScreenEvent.Pre event) {
        if (event.gui instanceof GuiContainerCreative) {
            Slot slot = creativeSlot((GuiContainerCreative) event.gui);
            if (slot != null) {
                slot.xDisplayPosition = CREATIVE_X;
                slot.yDisplayPosition = CREATIVE_Y;
            }
        }
    }
}
