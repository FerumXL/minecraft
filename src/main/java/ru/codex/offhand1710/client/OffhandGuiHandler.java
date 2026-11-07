package ru.codex.offhand1710.client;

import java.lang.reflect.Field;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import ru.codex.offhand1710.InventoryOffhand;
import ru.codex.offhand1710.MC112Bridge;
import ru.codex.offhand1710.SlotOffhand;

/**
 * Inventory screens: the survival inventory gets the offhand slot frame; the creative inventory tab gets the
 * slot moved to a free place (its own layout would put it on top of the first hotbar slot), a frame drawn
 * after the screen, and creative edits of the slot sent to the server.
 */
public final class OffhandGuiHandler {
    private static final ResourceLocation INVENTORY = new ResourceLocation("textures/gui/container/inventory.png");
    /** Free area right of the armor slots and the player in the creative inventory tab. */
    private static final int CREATIVE_X = 88;
    private static final int CREATIVE_Y = 20;

    private static Field guiLeft;
    private static Field guiTop;
    private static boolean creativeFrameBroken;
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

    /** The creative tab texture has no frame at that place: draw one, then the item again on top of it. */
    @SubscribeEvent
    public void onDrawPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.gui instanceof GuiContainerCreative) || creativeFrameBroken) {
            return;
        }
        GuiContainerCreative gui = (GuiContainerCreative) event.gui;
        Slot slot = creativeSlot(gui);
        if (slot == null) {
            return;
        }
        int left;
        int top;
        try {
            if (guiLeft == null) {
                guiLeft = ReflectionHelper.findField(GuiContainer.class, "field_147003_i", "guiLeft");
                guiTop = ReflectionHelper.findField(GuiContainer.class, "field_147009_r", "guiTop");
            }
            left = guiLeft.getInt(gui);
            top = guiTop.getInt(gui);
        } catch (Exception e) {
            creativeFrameBroken = true;
            FMLLog.warning("[mc112bridge] creative offhand slot frame disabled: %s", e);
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        int x = left + slot.xDisplayPosition;
        int y = top + slot.yDisplayPosition;

        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        RenderHelper.disableStandardItemLighting();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(INVENTORY);
        gui.drawTexturedModalRect(x - 1, y - 1, 7, 83, 18, 18);

        ItemStack stack = slot.getStack();
        if (stack == null) {
            IIcon icon = SlotOffhand.emptyIcon;
            if (icon != null) {
                mc.getTextureManager().bindTexture(TextureMap.locationItemsTexture);
                gui.drawTexturedModelRectFromIcon(x, y, icon, 16, 16);
            }
        } else {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL12.GL_RESCALE_NORMAL);
            RenderHelper.enableGUIStandardItemLighting();
            RenderItem renderer = RenderItem.getInstance();
            renderer.renderItemAndEffectIntoGUI(mc.fontRenderer, mc.getTextureManager(), stack, x, y);
            renderer.renderItemOverlayIntoGUI(mc.fontRenderer, mc.getTextureManager(), stack, x, y);
            RenderHelper.disableStandardItemLighting();
            GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        }
        // GuiContainer.drawScreen leaves lighting and depth test on.
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        RenderHelper.enableStandardItemLighting();
    }
}
