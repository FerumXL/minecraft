package ru.codex.offhand1710.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import ru.codex.offhand1710.MC112Bridge;
import ru.codex.offhand1710.StructureBlock112;

/**
 * GuiEditStructure of 1.12.2: name, offset, size (save), integrity, seed, rotation and mirror (load), metadata (data),
 * the mode button, the toggles and the Save / Load / Detect actions; Done sends the fields, Cancel drops them.
 */
public final class StructureGui extends GuiScreen {
    private final StructureBlock112.Tile tile;
    private final net.minecraft.nbt.NBTTagCompound before = new net.minecraft.nbt.NBTTagCompound();
    private GuiTextField name, meta, px, py, pz, sx, sy, sz, integrity, seed;
    private final List<GuiTextField> fields = new ArrayList<GuiTextField>();

    public StructureGui(StructureBlock112.Tile tile) {
        this.tile = tile;
        tile.writeFields(before);
    }

    private static String t(String key) {
        return StatCollector.translateToLocal(key);
    }

    private GuiTextField field(int x, int y, int w, String text, int max) {
        GuiTextField f = new GuiTextField(fontRendererObj, x, y, w, 20);
        f.setMaxStringLength(max);
        f.setText(text);
        fields.add(f);
        return f;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        buttonList.clear();
        fields.clear();
        int c = width / 2;
        int m = tile.mode;
        buttonList.add(new GuiButton(0, c - 4 - 150, 210, 150, 20, t("gui.done")));
        buttonList.add(new GuiButton(1, c + 4, 210, 150, 20, t("gui.cancel")));
        buttonList.add(new GuiButton(2, c - 4 - 150, 185, 50, 20, t("structure_block.mode." + StructureBlock112.MODES[m].toLowerCase())));
        if (m == 3) {
            meta = field(c - 152, 40, 240, tile.metadata, 128);
            name = null;
        } else {
            name = field(c - 152, 40, m == 0 ? 250 : 300, tile.name, 64);
        }
        if (m == 0 || m == 1) {
            px = field(c - 152, 80, 70, String.valueOf(tile.posX), 15);
            py = field(c - 77, 80, 70, String.valueOf(tile.posY), 15);
            pz = field(c - 2, 80, 70, String.valueOf(tile.posZ), 15);
            buttonList.add(new GuiButton(10, c + 154 - 50, 160, 50, 20, onOff(!tile.ignoreEntities)));
            buttonList.add(new GuiButton(11, c + 154 - 50, m == 0 ? 120 : 80, 50, 20, onOff(tile.showBoundingBox)));
        }
        if (m == 0) {
            sx = field(c - 152, 120, 70, String.valueOf(tile.sizeX), 15);
            sy = field(c - 77, 120, 70, String.valueOf(tile.sizeY), 15);
            sz = field(c - 2, 120, 70, String.valueOf(tile.sizeZ), 15);
            buttonList.add(new GuiButton(12, c + 154 - 50, 80, 50, 20, onOff(tile.showAir)));
            buttonList.add(new GuiButton(20, c + 4 + 100, 185, 50, 20, t("structure_block.button.save")));
            buttonList.add(new GuiButton(22, c + 4 + 100, 40, 50, 20, t("structure_block.button.detect_size")));
        }
        if (m == 1) {
            integrity = field(c - 152, 120, 80, String.valueOf(tile.integrity), 15);
            seed = field(c - 72, 120, 80, String.valueOf(tile.seed), 31);
            String[] deg = {"0", "90", "180", "270"};
            for (int i = 0; i < 4; i++) {
                GuiButton b = new GuiButton(30 + i, c - 1 - 40 * (4 - i) + 2, 185, 40, 20, deg[i]);
                b.enabled = tile.rotation != i;
                buttonList.add(b);
            }
            buttonList.add(new GuiButton(13, c + 154 - 104, 185, 50, 20, mirror()));
            buttonList.add(new GuiButton(21, c + 4 + 100, 185, 50, 20, t("structure_block.button.load")));
        }
        if (name != null) {
            name.setFocused(true);
        } else {
            meta.setFocused(true);
        }
    }

    private static String onOff(boolean b) {
        return t(b ? "options.on" : "options.off");
    }

    private String mirror() {
        return new String[] {"|", "< >", "^ v"}[tile.mirror];
    }

    private static int num(GuiTextField f, int old) {
        try {
            return Integer.parseInt(f.getText().trim());
        } catch (NumberFormatException e) {
            return old;
        }
    }

    /** Fields of the window into the tile (sent to the server, which clamps them). */
    private void apply() {
        if (name != null) {
            tile.name = name.getText();
        }
        if (meta != null) {
            tile.metadata = meta.getText();
        }
        if (px != null) {
            tile.posX = num(px, tile.posX);
            tile.posY = num(py, tile.posY);
            tile.posZ = num(pz, tile.posZ);
        }
        if (sx != null) {
            tile.sizeX = num(sx, tile.sizeX);
            tile.sizeY = num(sy, tile.sizeY);
            tile.sizeZ = num(sz, tile.sizeZ);
        }
        if (integrity != null) {
            try {
                tile.integrity = Math.max(0.0F, Math.min(1.0F, Float.parseFloat(integrity.getText().trim())));
            } catch (NumberFormatException ignored) {
            }
            try {
                tile.seed = Long.parseLong(seed.getText().trim());
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private void send(int action) {
        apply();
        MC112Bridge.NETWORK.channel.sendToServer(new StructureBlock112.Msg(tile, action));
    }

    @Override
    protected void actionPerformed(GuiButton b) {
        switch (b.id) {
            case 0:
                send(0);
                mc.displayGuiScreen(null);
                return;
            case 1:
                tile.readFields(before);
                mc.displayGuiScreen(null);
                return;
            case 2:
                apply();
                tile.mode = (tile.mode + 1) % 4;
                break;
            case 10:
                tile.ignoreEntities = !tile.ignoreEntities;
                break;
            case 11:
                tile.showBoundingBox = !tile.showBoundingBox;
                break;
            case 12:
                tile.showAir = !tile.showAir;
                break;
            case 13:
                tile.mirror = (tile.mirror + 1) % 3;
                break;
            case 20:
                send(1);
                mc.displayGuiScreen(null);
                return;
            case 21:
                send(2);
                mc.displayGuiScreen(null);
                return;
            case 22:
                send(3);
                mc.displayGuiScreen(null);
                return;
            default:
                if (b.id >= 30 && b.id < 34) {
                    tile.rotation = b.id - 30;
                }
                break;
        }
        apply();
        initGui();
    }

    @Override
    protected void keyTyped(char c, int key) {
        for (GuiTextField f : fields) {
            if (f.isFocused()) {
                f.textboxKeyTyped(c, key);
            }
        }
        if (key == Keyboard.KEY_TAB) {
            int i = 0;
            for (; i < fields.size(); i++) {
                if (fields.get(i).isFocused()) {
                    break;
                }
            }
            for (GuiTextField f : fields) {
                f.setFocused(false);
            }
            fields.get((i + 1) % fields.size()).setFocused(true);
        } else if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
            actionPerformed((GuiButton) buttonList.get(0));
        } else if (key == Keyboard.KEY_ESCAPE) {
            actionPerformed((GuiButton) buttonList.get(1));
        }
    }

    @Override
    protected void mouseClicked(int x, int y, int button) {
        super.mouseClicked(x, y, button);
        for (GuiTextField f : fields) {
            f.mouseClicked(x, y, button);
        }
    }

    @Override
    public void updateScreen() {
        for (GuiTextField f : fields) {
            f.updateCursorCounter();
        }
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void drawScreen(int mx, int my, float partial) {
        drawDefaultBackground();
        int c = width / 2;
        drawCenteredString(fontRendererObj, t("tile.mc112bridge.structure_block.name"), c, 10, 16777215);
        int m = tile.mode;
        if (m == 3) {
            drawString(fontRendererObj, t("structure_block.custom_data"), c - 153, 30, 10526880);
        } else {
            drawString(fontRendererObj, t("structure_block.structure_name"), c - 153, 30, 10526880);
        }
        if (m == 0 || m == 1) {
            drawString(fontRendererObj, t("structure_block.position"), c - 153, 70, 10526880);
            // GuiEditStructure: each toggle has its label above it, right-aligned to the button
            label("structure_block.include_entities", 150);
            label("structure_block.show_boundingbox", m == 0 ? 110 : 70);
        }
        if (m == 0) {
            drawString(fontRendererObj, t("structure_block.size"), c - 153, 110, 10526880);
            label("structure_block.show_air", 70);
        }
        if (m == 1) {
            drawString(fontRendererObj, t("structure_block.integrity"), c - 153, 110, 10526880);
        }
        drawString(fontRendererObj, t("structure_block.mode_info." + StructureBlock112.MODES[m].toLowerCase()), c - 153, 174, 10526880);
        for (GuiTextField f : fields) {
            f.drawTextBox();
        }
        super.drawScreen(mx, my, partial);
    }

    private void label(String key, int y) {
        String s = t(key);
        drawString(fontRendererObj, s, width / 2 + 154 - fontRendererObj.getStringWidth(s), y, 10526880);
    }

    /** TileEntityStructureRenderer of 1.12.2: the white box of the structure in save and load mode. */
    public static final class Render extends TileEntitySpecialRenderer {
        @Override
        public void renderTileEntityAt(TileEntity te, double x, double y, double z, float partial) {
            if (!(te instanceof StructureBlock112.Tile)) {
                return;
            }
            StructureBlock112.Tile t = (StructureBlock112.Tile) te;
            if (!t.showBoundingBox || t.mode > 1 || t.sizeX < 1 || t.sizeY < 1 || t.sizeZ < 1) {
                return;
            }
            double x0 = x + t.posX, y0 = y + t.posY, z0 = z + t.posZ;
            double x1 = x0 + t.sizeX, y1 = y0 + t.sizeY, z1 = z0 + t.sizeZ;
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_FOG);
            GL11.glLineWidth(2.0F);
            Tessellator tes = Tessellator.instance;
            tes.startDrawing(GL11.GL_LINES);
            tes.setColorRGBA(229, 229, 229, 255);
            double[][] e = {{x0, y0, z0, x1, y0, z0}, {x0, y0, z1, x1, y0, z1}, {x0, y1, z0, x1, y1, z0}, {x0, y1, z1, x1, y1, z1},
                {x0, y0, z0, x0, y1, z0}, {x1, y0, z0, x1, y1, z0}, {x0, y0, z1, x0, y1, z1}, {x1, y0, z1, x1, y1, z1},
                {x0, y0, z0, x0, y0, z1}, {x1, y0, z0, x1, y0, z1}, {x0, y1, z0, x0, y1, z1}, {x1, y1, z0, x1, y1, z1}};
            for (double[] l : e) {
                tes.addVertex(l[0], l[1], l[2]);
                tes.addVertex(l[3], l[4], l[5]);
            }
            tes.draw();
            GL11.glLineWidth(1.0F);
            GL11.glEnable(GL11.GL_FOG);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_LIGHTING);
        }
    }
}
