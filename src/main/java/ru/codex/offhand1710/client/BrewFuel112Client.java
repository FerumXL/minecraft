package ru.codex.offhand1710.client;

import cpw.mods.fml.relauncher.ReflectionHelper;
import java.lang.reflect.Field;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiBrewingStand;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.tileentity.TileEntityBrewingStand;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import ru.codex.offhand1710.BrewFuel112;

/**
 * GuiBrewingStand.drawGuiContainerBackgroundLayer of 1.12.2: the window of 1.9 with the fuel slot, the fuel bar
 * (18 pixels for 20 brews), the arrow and the bubbles.
 */
public final class BrewFuel112Client {
    private static final ResourceLocation TEXTURE = new ResourceLocation("mc112bridge", "textures/gui/container/brewing_stand.png");
    private static final int[] BUBBLES = {29, 24, 20, 16, 11, 6, 0};
    private static Field stand, left, top, xSize, ySize;

    private BrewFuel112Client() {
    }

    public static boolean background(GuiBrewingStand gui) {
        if (BrewFuel112.OFF) {
            return false;
        }
        try {
            if (stand == null) {
                stand = ReflectionHelper.findField(GuiBrewingStand.class, "field_147013_v", "tileBrewingStand");
                left = ReflectionHelper.findField(GuiContainer.class, "field_147003_i", "guiLeft");
                top = ReflectionHelper.findField(GuiContainer.class, "field_147009_r", "guiTop");
                xSize = ReflectionHelper.findField(GuiContainer.class, "field_146999_f", "xSize");
                ySize = ReflectionHelper.findField(GuiContainer.class, "field_147000_g", "ySize");
            }
            TileEntityBrewingStand tile = (TileEntityBrewingStand) stand.get(gui);
            int x = left.getInt(gui);
            int y = top.getInt(gui);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            Minecraft.getMinecraft().getTextureManager().bindTexture(TEXTURE);
            gui.drawTexturedModalRect(x, y, 0, 0, xSize.getInt(gui), ySize.getInt(gui));
            int fuel = BrewFuel112.fuel(tile);
            int bar = MathHelper.clamp_int((18 * fuel + 20 - 1) / 20, 0, 18);
            if (bar > 0) {
                gui.drawTexturedModalRect(x + 60, y + 44, 176, 29, bar, 4);
            }
            int time = tile.getBrewTime();
            if (time > 0) {
                int arrow = (int) (28.0F * (1.0F - (float) time / 400.0F));
                if (arrow > 0) {
                    gui.drawTexturedModalRect(x + 97, y + 16, 176, 0, 9, arrow);
                }
                int bubbles = BUBBLES[time / 2 % 7];
                if (bubbles > 0) {
                    gui.drawTexturedModalRect(x + 63, y + 14 + 29 - bubbles, 185, 29 - bubbles, 12, bubbles);
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
