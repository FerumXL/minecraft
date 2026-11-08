package ru.codex.offhand1710.client;

import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;

/**
 * The survival inventory screen of 1.12.2: the texture (assets/minecraft/.../inventory.png) has the narrower player box
 * and the offhand frame, OffhandContainer has moved the crafting slots, and the title goes above them.
 */
public final class GuiInventoryOffhand extends GuiInventory {
    public GuiInventoryOffhand(EntityPlayer player) {
        super(player);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRendererObj.drawString(I18n.format("container.crafting"), 97, 8, 4210752);
    }
}
