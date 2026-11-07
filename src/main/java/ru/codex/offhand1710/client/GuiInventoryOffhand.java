package ru.codex.offhand1710.client;

import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import ru.codex.offhand1710.OffhandContainer;

/**
 * The survival inventory screen with a frame for the offhand slot. The 1.7.10 inventory texture has no
 * offhand slot, so the frame of an ordinary inventory slot is drawn where 1.12.2 has it. The slot itself
 * (clicks, item, tooltip, empty shield icon) is handled by GuiContainer like any other slot.
 */
public final class GuiInventoryOffhand extends GuiInventory {
    private static final ResourceLocation INVENTORY = new ResourceLocation("textures/gui/container/inventory.png");

    public GuiInventoryOffhand(EntityPlayer player) {
        super(player);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        super.drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);
        Slot slot = OffhandContainer.findSlot(inventorySlots);
        if (slot == null) {
            return;
        }
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(INVENTORY);
        // Frame of the first main-inventory slot (texture 7,83), 18x18 around the 16x16 item position.
        drawTexturedModalRect(guiLeft + slot.xDisplayPosition - 1, guiTop + slot.yDisplayPosition - 1, 7, 83, 18, 18);
    }
}
