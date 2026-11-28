package ru.codex.offhand1710.client;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.model.ModelBook;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnchantmentNameParts;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.util.glu.Project;

import ru.codex.offhand1710.Enchant112;

/**
 * GuiEnchantment of 1.12.2: the lapis slot, the level icons 1 - 3, the buttons greyed out without lapis or levels, and
 * the tooltip with the clue, the lapis and the levels it costs. The book is drawn as in 1.7.10 (the same code).
 */
public final class EnchantGui112 extends GuiContainer {
    private static final ResourceLocation TEXTURE = new ResourceLocation("mc112bridge", "textures/gui/container/enchanting_table.png");
    private static final ResourceLocation BOOK_TEXTURE = new ResourceLocation("textures/entity/enchanting_table_book.png");
    private static final ModelBook BOOK = new ModelBook();
    /** Set by the server just before the window opens (Enchant112.Name). */
    static volatile String pendingName = "";

    private final Enchant112.Table container;
    private final String name;
    private final Random random = new Random();
    public int ticks;
    public float flip;
    public float oFlip;
    public float flipT;
    public float flipA;
    public float open;
    public float oOpen;
    private ItemStack last;

    public EnchantGui112(InventoryPlayer inventory, World world, int x, int y, int z) {
        super(new Enchant112.Table(inventory, world, x, y, z));
        this.container = (Enchant112.Table) inventorySlots;
        this.name = pendingName;
        pendingName = "";
    }

    public Enchant112.Table table() {
        return container;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRendererObj.drawString(name.isEmpty() ? I18n.format("container.enchant") : name, 12, 5, 4210752);
        fontRendererObj.drawString(I18n.format("container.inventory"), 8, ySize - 96 + 2, 4210752);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        tickBook();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);
        int i = (width - xSize) / 2;
        int j = (height - ySize) / 2;
        for (int k = 0; k < 3; ++k) {
            int l = mouseX - (i + 60);
            int m = mouseY - (j + 14 + 19 * k);
            if (l >= 0 && m >= 0 && l < 108 && m < 19 && container.enchantItem(mc.thePlayer, k)) {
                mc.playerController.sendEnchantPacket(container.windowId, k);
            }
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(TEXTURE);
        int i = (width - xSize) / 2;
        int j = (height - ySize) / 2;
        drawTexturedModalRect(i, j, 0, 0, xSize, ySize);
        drawBook(partialTicks);
        EnchantmentNameParts.instance.reseedRandomGenerator((long) container.xpSeed);
        int lapis = container.lapisAmount();
        for (int l = 0; l < 3; ++l) {
            int left = i + 60;
            int text = left + 20;
            zLevel = 0.0F;
            mc.getTextureManager().bindTexture(TEXTURE);
            int level = container.enchantLevels[l];
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            if (level == 0) {
                drawTexturedModalRect(left, j + 14 + 19 * l, 0, 185, 108, 19);
            } else {
                String s = "" + level;
                int width = 86 - fontRendererObj.getStringWidth(s);
                String galactic = galactic(mc.standardGalacticFontRenderer, width);
                FontRenderer font = mc.standardGalacticFontRenderer;
                int color = 6839882;
                if ((lapis < l + 1 || mc.thePlayer.experienceLevel < level) && !mc.thePlayer.capabilities.isCreativeMode || container.enchantClue[l] == -1) {
                    drawTexturedModalRect(left, j + 14 + 19 * l, 0, 185, 108, 19);
                    drawTexturedModalRect(left + 1, j + 15 + 19 * l, 16 * l, 239, 16, 16);
                    font.drawSplitString(galactic, text, j + 16 + 19 * l, width, (color & 16711422) >> 1);
                    color = 4226832;
                } else {
                    int dx = mouseX - (i + 60);
                    int dy = mouseY - (j + 14 + 19 * l);
                    if (dx >= 0 && dy >= 0 && dx < 108 && dy < 19) {
                        drawTexturedModalRect(left, j + 14 + 19 * l, 0, 204, 108, 19);
                        color = 16777088;
                    } else {
                        drawTexturedModalRect(left, j + 14 + 19 * l, 0, 166, 108, 19);
                    }
                    drawTexturedModalRect(left + 1, j + 15 + 19 * l, 16 * l, 223, 16, 16);
                    font.drawSplitString(galactic, text, j + 16 + 19 * l, width, color);
                    color = 8453920;
                }
                fontRendererObj.drawStringWithShadow(s, text + 86 - fontRendererObj.getStringWidth(s), j + 16 + 19 * l + 7, color);
            }
        }
    }

    /** EnchantmentNameParts.generateNewRandomName of 1.12.2: the words cut to two lines of the button. */
    @SuppressWarnings("unchecked")
    private static String galactic(FontRenderer font, int width) {
        List<String> lines = font.listFormattedStringToWidth(EnchantmentNameParts.instance.generateNewRandomName(), width);
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < lines.size() && k < 2; k++) {
            if (k > 0) {
                sb.append(' ');
            }
            sb.append(lines.get(k));
        }
        return sb.toString();
    }

    private void drawBook(float partialTicks) {
        GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        ScaledResolution sr = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        GL11.glViewport((sr.getScaledWidth() - 320) / 2 * sr.getScaleFactor(), (sr.getScaledHeight() - 240) / 2 * sr.getScaleFactor(), 320 * sr.getScaleFactor(),
            240 * sr.getScaleFactor());
        GL11.glTranslatef(-0.34F, 0.23F, 0.0F);
        Project.gluPerspective(90.0F, 1.3333334F, 9.0F, 80.0F);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        RenderHelper.enableStandardItemLighting();
        GL11.glTranslatef(0.0F, 3.3F, -16.0F);
        GL11.glScalef(5.0F, 5.0F, 5.0F);
        GL11.glRotatef(180.0F, 0.0F, 0.0F, 1.0F);
        mc.getTextureManager().bindTexture(BOOK_TEXTURE);
        GL11.glRotatef(20.0F, 1.0F, 0.0F, 0.0F);
        float f = oOpen + (open - oOpen) * partialTicks;
        GL11.glTranslatef((1.0F - f) * 0.2F, (1.0F - f) * 0.1F, (1.0F - f) * 0.25F);
        GL11.glRotatef(-(1.0F - f) * 90.0F - 90.0F, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(180.0F, 1.0F, 0.0F, 0.0F);
        float f1 = oFlip + (flip - oFlip) * partialTicks + 0.25F;
        float f2 = oFlip + (flip - oFlip) * partialTicks + 0.75F;
        f1 = (f1 - (float) MathHelper.floor_float(f1)) * 1.6F - 0.3F;
        f2 = (f2 - (float) MathHelper.floor_float(f2)) * 1.6F - 0.3F;
        f1 = MathHelper.clamp_float(f1, 0.0F, 1.0F);
        f2 = MathHelper.clamp_float(f2, 0.0F, 1.0F);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        BOOK.render(null, 0.0F, f1, f2, f, 0.0F, 0.0625F);
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        RenderHelper.disableStandardItemLighting();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glViewport(0, 0, mc.displayWidth, mc.displayHeight);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        RenderHelper.disableStandardItemLighting();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        List<String> list = tooltip(mouseX, mouseY);
        if (list != null) {
            drawHoveringText(list, mouseX, mouseY, fontRendererObj);
        }
    }

    /** GuiEnchantment.drawScreen of 1.12.2: the tooltip of the offer under the mouse, or null. */
    public List<String> tooltip(int mouseX, int mouseY) {
        boolean creative = mc.thePlayer.capabilities.isCreativeMode;
        int lapis = container.lapisAmount();
        int i = (width - xSize) / 2;
        int j = (height - ySize) / 2;
        for (int k = 0; k < 3; ++k) {
            int level = container.enchantLevels[k];
            int clue = container.enchantClue[k];
            Enchantment e = clue >= 0 && clue < Enchantment.enchantmentsList.length ? Enchantment.enchantmentsList[clue] : null;
            int clueLevel = container.worldClue[k];
            int cost = k + 1;
            int dx = mouseX - (i + 60);
            int dy = mouseY - (j + 14 + 19 * k);
            if (dx >= 0 && dy >= 0 && dx < 108 && dy < 17 && level > 0) {
                List<String> list = new ArrayList<String>();
                list.add("" + EnumChatFormatting.WHITE + EnumChatFormatting.ITALIC + I18n.format("container.enchant.clue", e == null ? "" : e.getTranslatedName(clueLevel)));
                if (e == null) {
                    list.add("");
                    list.add(EnumChatFormatting.RED + I18n.format("forge.container.enchant.limitedEnchantability"));
                } else if (!creative) {
                    list.add("");
                    if (mc.thePlayer.experienceLevel < level) {
                        list.add(EnumChatFormatting.RED + I18n.format("container.enchant.level.requirement", level));
                    } else {
                        String s = cost == 1 ? I18n.format("container.enchant.lapis.one") : I18n.format("container.enchant.lapis.many", cost);
                        list.add((lapis >= cost ? EnumChatFormatting.GRAY : EnumChatFormatting.RED) + s);
                        s = cost == 1 ? I18n.format("container.enchant.level.one") : I18n.format("container.enchant.level.many", cost);
                        list.add(EnumChatFormatting.GRAY + s);
                    }
                }
                return list;
            }
        }
        return null;
    }

    /** GuiEnchantment.tickBook. */
    private void tickBook() {
        ItemStack stack = container.getSlot(0).getStack();
        if (!ItemStack.areItemStacksEqual(stack, last)) {
            last = stack;
            do {
                flipT += (float) (random.nextInt(4) - random.nextInt(4));
            } while (flip <= flipT + 1.0F && flip >= flipT - 1.0F);
        }
        ++ticks;
        oFlip = flip;
        oOpen = open;
        boolean any = false;
        for (int i = 0; i < 3; ++i) {
            if (container.enchantLevels[i] != 0) {
                any = true;
            }
        }
        open = MathHelper.clamp_float(open + (any ? 0.2F : -0.2F), 0.0F, 1.0F);
        float f1 = MathHelper.clamp_float((flipT - flip) * 0.4F, -0.2F, 0.2F);
        flipA += (f1 - flipA) * 0.9F;
        flip += flipA;
    }

    public static final class NameHandler implements IMessageHandler<Enchant112.Name, IMessage> {
        @Override
        public IMessage onMessage(Enchant112.Name message, MessageContext ctx) {
            pendingName = message.name;
            return null;
        }
    }

    /** For the autotest: the screen open now, if it is this one. */
    static EnchantGui112 current() {
        return Minecraft.getMinecraft().currentScreen instanceof EnchantGui112 ? (EnchantGui112) Minecraft.getMinecraft().currentScreen : null;
    }
}
