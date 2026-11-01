package ru.codex.offhand1710.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import ru.codex.offhand1710.OffhandInventory;

public final class OffhandHud {
    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.HOTBAR) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.thePlayer == null || !Minecraft.isGuiEnabled()) {
            return;
        }
        ItemStack stack = OffhandInventory.get(minecraft.thePlayer).getStack();
        int x = event.resolution.getScaledWidth() / 2 - 114;
        int y = event.resolution.getScaledHeight() - 23;
        Gui.drawRect(x, y, x + 20, y + 20, 0xFF202020);
        Gui.drawRect(x + 1, y + 1, x + 19, y + 19, 0xFF777777);
        Gui.drawRect(x + 2, y + 2, x + 18, y + 18, 0xFF303030);
        if (stack != null) {
            RenderItem renderer = RenderItem.getInstance();
            renderer.renderItemAndEffectIntoGUI(minecraft.fontRenderer, minecraft.renderEngine, stack, x + 2, y + 2);
            renderer.renderItemOverlayIntoGUI(minecraft.fontRenderer, minecraft.renderEngine, stack, x + 2, y + 2);
        }
    }
}

