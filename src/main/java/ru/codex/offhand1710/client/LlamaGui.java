package ru.codex.offhand1710.client;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import org.lwjgl.opengl.GL11;
import ru.codex.offhand1710.LlamaContainer;
import ru.codex.offhand1710.MoreMobs;

/**
 * GuiScreenHorseInventory of 1.12.2 for a llama: the window of horse.png, the chest area (strength columns x 3 rows)
 * from (0, 166), the carpet slot background at (36, 220) and the llama drawn at (51, 60), scale 17.
 */
public final class LlamaGui extends GuiContainer {
    private static final ResourceLocation TEXTURE = new ResourceLocation("mc112bridge", "textures/gui/container/horse.png");
    private final MoreMobs.Llama llama;
    private final InventoryPlayer player;
    private float mouseX;
    private float mouseY;

    private final int cols;

    public LlamaGui(InventoryPlayer player, MoreMobs.Llama llama, int cols) {
        super(new LlamaContainer(player, llama, cols));
        this.cols = cols;
        this.llama = llama;
        this.player = player;
        allowUserInput = false;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int x, int y) {
        fontRendererObj.drawString(llama.hasCustomNameTag() ? llama.getCustomNameTag() : StatCollector.translateToLocal("entity.Llama.name"), 8, 6, 4210752);
        fontRendererObj.drawString(StatCollector.translateToLocal(player.getInventoryName()), 8, ySize - 96 + 2, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partial, int x, int y) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(TEXTURE);
        int i = (width - xSize) / 2;
        int j = (height - ySize) / 2;
        drawTexturedModalRect(i, j, 0, 0, xSize, ySize);
        if (cols > 0) {
            drawTexturedModalRect(i + 79, j + 17, 0, ySize, cols * 18, 54);
        }
        drawTexturedModalRect(i + 7, j + 35, 36, ySize + 54, 18, 18);
        GuiInventory.func_147046_a(i + 51, j + 60, 17, (float) (i + 51) - mouseX, (float) (j + 75 - 50) - mouseY, llama);
    }

    @Override
    public void drawScreen(int x, int y, float partial) {
        mouseX = (float) x;
        mouseY = (float) y;
        super.drawScreen(x, y, partial);
    }
}
