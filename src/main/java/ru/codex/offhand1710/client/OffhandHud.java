package ru.codex.offhand1710.client;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import ru.codex.offhand1710.OffhandInventory;

/**
 * The offhand slot next to the hotbar, as in 1.12.2: shown only while the offhand holds an item, left of the
 * hotbar, in the same frame as the hotbar slots (taken from the hotbar texture).
 */
public final class OffhandHud {
    private static final ResourceLocation WIDGETS = new ResourceLocation("textures/gui/widgets.png");

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.HOTBAR) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) {
            return;
        }
        int center = event.resolution.getScaledWidth() / 2;
        int bottom = event.resolution.getScaledHeight();
        OffhandInventory offhand = OffhandInventory.get(mc.thePlayer);
        ItemStack stack = offhand.getStack();
        if (stack != null) {
            drawOffhandSlot(mc, stack, center, bottom);
        }
        drawShieldCooldown(mc, offhand, stack, center, bottom, event.partialTicks);
    }

    /**
     * The white cooldown sweep of 1.12.2 (RenderItem.renderItemOverlayIntoGUI) over every shield on the
     * hotbar and in the offhand slot while an axe hit keeps the shield down.
     */
    private static void drawShieldCooldown(Minecraft mc, OffhandInventory offhand, ItemStack offhandStack, int center,
            int bottom, float partialTicks) {
        float left = ((float) offhand.shieldCooldown - partialTicks) / (float) ru.codex.offhand1710.ShieldMechanics.AXE_COOLDOWN;
        if (left <= 0.0F) {
            return;
        }
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        for (int i = 0; i < 9; i++) {
            ItemStack hotbar = mc.thePlayer.inventory.mainInventory[i];
            if (hotbar != null && hotbar.getItem() instanceof ru.codex.offhand1710.ItemShield) {
                cooldownRect(center - 90 + i * 20 + 2, bottom - 16 - 3, left);
            }
        }
        if (offhandStack != null && offhandStack.getItem() instanceof ru.codex.offhand1710.ItemShield) {
            cooldownRect(center - 91 - 26, bottom - 19, left);
        }
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void cooldownRect(int x, int y, float left) {
        int top = y + MathHelper.floor_float(16.0F * (1.0F - left));
        Gui.drawRect(x, top, x + 16, top + MathHelper.ceiling_float_int(16.0F * left), 0x7FFFFFFF);
    }

    private static void drawOffhandSlot(Minecraft mc, ItemStack stack, int center, int bottom) {
        // 1.12.2: item at (center - 91 - 26, height - 19); the frame is 3 pixels left and up of the item.
        int x = center - 91 - 29;
        int y = bottom - 22;

        Gui gui = mc.ingameGUI;
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(770, 771, 1, 0);
        mc.getTextureManager().bindTexture(WIDGETS);
        gui.drawTexturedModalRect(x, y, 0, 0, 21, 22);       // left end and first slot of the hotbar
        gui.drawTexturedModalRect(x + 21, y, 181, 0, 1, 22); // right end of the hotbar
        GL11.glDisable(GL11.GL_BLEND);

        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        RenderHelper.enableGUIStandardItemLighting();
        RenderItem renderer = RenderItem.getInstance();
        renderer.renderItemAndEffectIntoGUI(mc.fontRenderer, mc.getTextureManager(), stack, x + 3, y + 3);
        renderer.renderItemOverlayIntoGUI(mc.fontRenderer, mc.getTextureManager(), stack, x + 3, y + 3);
        RenderHelper.disableStandardItemLighting();
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
