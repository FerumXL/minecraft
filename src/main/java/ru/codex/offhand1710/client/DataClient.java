package ru.codex.offhand1710.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiCrafting;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.InventoryEffectRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import ru.codex.offhand1710.Data112;
import ru.codex.offhand1710.MC112Bridge;

/**
 * The advancement screen, toasts and the recipe book panel. Sizes, texture coordinates and the order of drawing are
 * those of 1.12.2 (GuiScreenAdvancements and its tab and widget classes, GuiRecipeBook and its page and buttons).
 */
public final class DataClient {
    private static final String DOMAIN = "mc112bridge";
    private static final ResourceLocation WIDGETS = new ResourceLocation(DOMAIN, "textures/gui/advancements/widgets.png");
    private static final ResourceLocation WINDOW = new ResourceLocation(DOMAIN, "textures/gui/advancements/window.png");
    private static final ResourceLocation TABS = new ResourceLocation(DOMAIN, "textures/gui/advancements/tabs.png");
    private static final ResourceLocation TOASTS = new ResourceLocation(DOMAIN, "textures/gui/toasts.png");
    private static final ResourceLocation BOOK = new ResourceLocation(DOMAIN, "textures/gui/recipe_book.png");
    /** The 1.12 sheets: only the picture of the book button is taken from them. */
    private static final ResourceLocation INVENTORY = new ResourceLocation(DOMAIN, "textures/gui/container/inventory.png");
    private static final ResourceLocation CRAFTING = new ResourceLocation(DOMAIN, "textures/gui/container/crafting_table.png");
    private static final String BACKGROUNDS = "textures/gui/advancements/backgrounds/";
    private static final RenderItem ITEMS = new RenderItem();
    private static final Gui GUI = new Gui();
    private static final KeyBinding KEY = new KeyBinding("key.advancements", Keyboard.KEY_L, "key.categories.misc");
    // written by the network thread (SyncHandler), read by the client thread
    private static final Set<String> DONE = Collections.synchronizedSet(new HashSet<String>());
    private static final Set<String> CRIT = Collections.synchronizedSet(new HashSet<String>());
    private static final Set<String> KNOWN = Collections.synchronizedSet(new HashSet<String>());
    /** Filled from the network thread. */
    private static final List<String> TOAST_QUEUE = new ArrayList<String>();
    /** The five places of GuiToast (bkc), one under the other. */
    private static final ToastSlot[] TOAST_SLOTS = new ToastSlot[5];
    /** Grows with every sync from the server: an open advancement screen rebuilds itself. */
    private static int syncCount;
    private static String lastTab;
    private static boolean bookOpen;
    private static int bookPage;
    private static int bookTab;
    private static boolean bookCraftable;
    /** The slot whose alternatives are open (right click), where its button was, and the recipe shown as a ghost in the grid. */
    private static Group altGroup;
    private static int altX;
    private static int altY;
    private static Data112.Recipe ghost;
    private static Events events;

    private DataClient() {
    }

    public static int keyCode() {
        return KEY.getKeyCode();
    }

    public static int knownCount() {
        return KNOWN.size();
    }

    public static void openAdvancements() {
        Minecraft.getMinecraft().displayGuiScreen(new Screen());
    }

    public static void openBook() {
        bookOpen = true;
    }

    public static boolean isBookOpen() {
        return bookOpen;
    }

    /**
     * Coremod hook in GuiContainer.mouseClicked and mouseMovedOrUp, on the "outside the window" test, as
     * hasClickedOutside of 1.12: over the book a click with an item on the cursor drops nothing.
     */
    public static boolean outsideWindow(boolean outside, GuiContainer gui, int x, int y) {
        return outside && !(events != null && events.overBook(gui, x, y));
    }

    /** Set when a click has rebuilt the book's buttons: the rest of that click's button loop must not press the new ones. */
    private static volatile boolean swallowClick;

    /** Coremod hook at the start of GuiContainer.mouseClicked; true means the book took the click. */
    public static boolean bookClick(GuiContainer gui, int x, int y, int button) {
        swallowClick = false;
        return events != null && events.click(gui, x, y, button);
    }

    /** Coremod hook at the start of GuiContainer.keyTyped; true means the book took the key. */
    public static boolean bookKey(GuiContainer gui, char c, int key) {
        return events != null && events.key(gui, c, key);
    }

    static void register() {
        ClientRegistry.registerKeyBinding(KEY);
        events = new Events();
        MinecraftForge.EVENT_BUS.register(events);
        FMLCommonHandler.instance().bus().register(events);
    }

    private static void split(String text, Set<String> into) {
        synchronized (into) {
            into.clear();
            if (text.length() > 0) {
                into.addAll(Arrays.asList(text.split(";")));
            }
        }
    }

    public static final class SyncHandler implements IMessageHandler<Data112.Sync, IMessage> {
        @Override
        public IMessage onMessage(Data112.Sync m, MessageContext context) {
            split(m.done, DONE);
            split(m.crit, CRIT);
            split(m.recipes, KNOWN);
            syncCount++;
            return null;
        }
    }

    public static final class ToastHandler implements IMessageHandler<Data112.Toast, IMessage> {
        @Override
        public IMessage onMessage(Data112.Toast m, MessageContext context) {
            synchronized (TOAST_QUEUE) {
                TOAST_QUEUE.add(m.id);
            }
            return null;
        }
    }

    private static void icon(ItemStack stack, int x, int y) {
        if (stack == null) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        RenderHelper.enableGUIStandardItemLighting();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        ITEMS.zLevel = 100.0F;
        ITEMS.renderItemAndEffectIntoGUI(mc.fontRenderer, mc.getTextureManager(), stack, x, y);
        ITEMS.zLevel = 0.0F;
        RenderHelper.disableStandardItemLighting();
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** A piece of the bound 256x256 sheet. */
    private static void blit(int x, int y, int u, int v, int w, int h) {
        GUI.drawTexturedModalRect(x, y, u, v, w, h);
    }

    /** Fills an area with a piece of the sheet repeated, the last row and column cut. */
    private static void repeat(int x, int y, int w, int h, int u, int v, int pieceW, int pieceH) {
        for (int dx = 0; dx < w; dx += pieceW) {
            for (int dy = 0; dy < h; dy += pieceH) {
                blit(x + dx, y + dy, u, v, Math.min(pieceW, w - dx), Math.min(pieceH, h - dy));
            }
        }
    }

    private static void across(int x1, int x2, int y, int colour) {
        Gui.drawRect(Math.min(x1, x2), y, Math.max(x1, x2) + 1, y + 1, colour);
    }

    private static void down(int x, int y1, int y2, int colour) {
        Gui.drawRect(x, Math.min(y1, y2) + 1, x + 1, Math.max(y1, y2), colour);
    }

    private static void textured() {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Advancements
    // ------------------------------------------------------------------------------------------------------------

    /** The kinds of tabs of tabs.png: where the strip is on the sheet, the size of a tab, how many fit, icon offset. */
    private static final int[][] TAB_KINDS = {
            {0, 0, 28, 32, 8, 6, 9}, // above
            {84, 0, 28, 32, 8, 6, 6}, // below
            {0, 64, 32, 28, 5, 10, 5}, // left
            {96, 64, 32, 28, 5, 6, 5}, // right
    };
    private static final Pattern LAST_WORD = Pattern.compile("(.+) \\S+");

    private static final class Widget extends TreeLayout.Node {
        Data112.Advancement a;
        Tab tab;
        Widget above;
        int x;
        int y;
        boolean done;
        String title;
        List<String> lines;
        int width;
        /** "n/m" of the requirements done, or null when there is one only; and the share done. */
        String progress;
        float fraction;

        boolean drawn() {
            return !a.hidden || done;
        }

        int frameU() {
            return "goal".equals(a.frame) ? 52 : ("challenge".equals(a.frame) ? 26 : 0);
        }
    }

    private static final class Tab {
        Widget root;
        final List<Widget> all = new ArrayList<Widget>();
        int kind;
        int index;
        ResourceLocation background;
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        int scrollX;
        int scrollY;
        boolean centred;
        float fade;

        int x(int left) {
            int w = TAB_KINDS[kind][2];
            return kind < 2 ? left + (w + 4) * index : (kind == 2 ? left - w + 4 : left + 248);
        }

        int y(int top) {
            int h = TAB_KINDS[kind][3];
            return kind == 0 ? top - h + 4 : (kind == 1 ? top + 136 : top + h * index);
        }

        boolean over(int left, int top, int mx, int my) {
            int x = x(left);
            int y = y(top);
            return mx > x && mx < x + TAB_KINDS[kind][2] && my > y && my < y + TAB_KINDS[kind][3];
        }
    }

    private static boolean doneBelow(Data112.Advancement a, Map<String, List<Data112.Advancement>> children) {
        if (DONE.contains(a.id)) {
            return true;
        }
        List<Data112.Advancement> kids = children.get(a.id);
        if (kids != null) {
            for (Data112.Advancement k : kids) {
                if (doneBelow(k, children)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * What the 1.12 server lets the client see (PlayerAdvancements, method f of class np): an advancement that is
     * done or has something done under it, and otherwise one whose parent or grandparent is done, as long as nothing
     * hidden stands in between.
     */
    private static boolean visible(Data112.Advancement a, Map<String, List<Data112.Advancement>> children) {
        Data112.Advancement c = a;
        for (int i = 0; c != null && i <= 2; i++) {
            if (i == 0 && doneBelow(c, children)) {
                return true;
            }
            if (!c.shown) {
                return false;
            }
            if (DONE.contains(c.id)) {
                return true;
            }
            if (c.hidden) {
                return false;
            }
            c = c.parent == null ? null : Data112.ADVANCEMENTS.get(c.parent);
        }
        return false;
    }

    public static final class Screen extends GuiScreen {
        private final List<Tab> tabs = new ArrayList<Tab>();
        private Tab selected;
        private int built = -1;
        private boolean scrolling;
        private int scrollMouseX;
        private int scrollMouseY;

        @Override
        public void initGui() {
            rebuild();
        }

        private void rebuild() {
            Map<String, int[]> scrolls = new HashMap<String, int[]>();
            for (Tab t : tabs) {
                if (t.centred) {
                    scrolls.put(t.root.a.id, new int[] {t.scrollX, t.scrollY});
                }
            }
            tabs.clear();
            selected = null;
            built = syncCount;
            Map<String, List<Data112.Advancement>> children = new HashMap<String, List<Data112.Advancement>>();
            for (Data112.Advancement a : Data112.ADVANCEMENTS.values()) {
                if (a.parent != null) {
                    List<Data112.Advancement> l = children.get(a.parent);
                    if (l == null) {
                        l = new ArrayList<Data112.Advancement>();
                        children.put(a.parent, l);
                    }
                    l.add(a);
                }
            }
            List<Data112.Advancement> seen = new ArrayList<Data112.Advancement>();
            List<String> ids = new ArrayList<String>();
            List<String> parents = new ArrayList<String>();
            for (Data112.Advancement a : Data112.ADVANCEMENTS.values()) {
                if (visible(a, children)) {
                    seen.add(a);
                    ids.add(a.id);
                    parents.add(a.parent);
                }
            }
            Map<String, Widget> widgets = new HashMap<String, Widget>();
            for (Integer i : TreeLayout.order(ids, parents)) {
                Data112.Advancement a = seen.get(i.intValue());
                if (!a.shown) {
                    continue;
                }
                Widget w = new Widget();
                w.a = a;
                w.done = DONE.contains(a.id);
                if (a.parent == null) {
                    int index = tabs.size();
                    int kind = 0;
                    while (kind < TAB_KINDS.length && index >= TAB_KINDS[kind][4]) {
                        index -= TAB_KINDS[kind][4];
                        kind++;
                    }
                    if (kind >= TAB_KINDS.length) {
                        continue;
                    }
                    Tab t = new Tab();
                    t.root = w;
                    t.kind = kind;
                    t.index = index;
                    t.background = background(a.background);
                    w.tab = t;
                    tabs.add(t);
                } else {
                    // the nearest ancestor that is displayed
                    Data112.Advancement p = Data112.ADVANCEMENTS.get(a.parent);
                    while (p != null && !p.shown) {
                        p = p.parent == null ? null : Data112.ADVANCEMENTS.get(p.parent);
                    }
                    Widget above = p == null ? null : widgets.get(p.id);
                    if (above == null) {
                        continue;
                    }
                    w.above = above;
                    w.tab = above.tab;
                    above.kids.add(w);
                }
                w.tab.all.add(w);
                widgets.put(a.id, w);
            }
            for (Tab t : tabs) {
                TreeLayout.run(t.root);
                for (Widget w : t.all) {
                    w.x = (int) Math.floor(w.depth * 28.0F);
                    w.y = (int) Math.floor(w.row * 27.0F);
                    t.minX = Math.min(t.minX, w.x);
                    t.maxX = Math.max(t.maxX, w.x + 28);
                    t.minY = Math.min(t.minY, w.y);
                    t.maxY = Math.max(t.maxY, w.y + 27);
                    describe(w);
                }
                int[] old = scrolls.get(t.root.a.id);
                if (old != null) {
                    t.scrollX = old[0];
                    t.scrollY = old[1];
                    t.centred = true;
                }
                if (t.root.a.id.equals(lastTab)) {
                    selected = t;
                }
            }
            if (selected == null && !tabs.isEmpty()) {
                selected = tabs.get(0);
            }
        }

        private ResourceLocation background(String name) {
            if (name == null) {
                return null;
            }
            int colon = name.indexOf(':');
            String space = colon < 0 ? "minecraft" : name.substring(0, colon);
            String path = colon < 0 ? name : name.substring(colon + 1);
            if ("minecraft".equals(space) && path.startsWith(BACKGROUNDS)) {
                // these five are files of 1.12 and are kept with the mod
                space = DOMAIN;
            }
            return new ResourceLocation(space, path);
        }

        /** The text of the box that opens under the cursor and its width. */
        @SuppressWarnings("unchecked")
        private void describe(Widget w) {
            FontRenderer font = fontRendererObj;
            w.title = font.trimStringToWidth(StatCollector.translateToLocal(w.a.title), 163);
            int total = w.a.total();
            w.progress = null;
            w.fraction = w.done ? 1.0F : 0.0F;
            int progressWidth = 0;
            if (total > 1) {
                int n = w.a.completed(CRIT);
                w.progress = n + "/" + total;
                w.fraction = w.done ? 1.0F : (float) n / (float) total;
                progressWidth = font.getStringWidth(" ") + font.getStringWidth("0") * String.valueOf(total).length() * 2 + font.getStringWidth("/");
            } else if (total == 1 && !w.done) {
                w.fraction = w.a.completed(CRIT) > 0 ? 1.0F : 0.0F;
            }
            int width = 29 + font.getStringWidth(w.title) + progressWidth;
            String text = w.a.description == null ? "" : StatCollector.translateToLocal(w.a.description);
            w.lines = new ArrayList<String>();
            if (text.length() > 0) {
                List<String> lines = font.listFormattedStringToWidth(text, width);
                if (lines.size() >= 2) {
                    // 1.12 evens the lines out: a little wider if that pulls a word up, a little narrower if the
                    // first line ends with a lone word
                    String first = lines.get(0);
                    int wider = font.getStringWidth(first + ' ' + lines.get(1).split(" ")[0]);
                    if (wider - width <= 10) {
                        lines = font.listFormattedStringToWidth(text, wider);
                    } else {
                        Matcher m = LAST_WORD.matcher(first);
                        if (m.matches()) {
                            int narrower = font.getStringWidth(m.group(1));
                            if (width - narrower <= 10) {
                                lines = font.listFormattedStringToWidth(text, narrower);
                            }
                        }
                    }
                }
                w.lines.addAll(lines);
            }
            for (String line : w.lines) {
                width = Math.max(width, font.getStringWidth(line));
            }
            w.width = width + 3 + 5;
        }

        @Override
        protected void mouseClicked(int x, int y, int button) {
            super.mouseClicked(x, y, button);
            if (button == 0) {
                int left = (width - 252) / 2;
                int top = (height - 140) / 2;
                for (Tab t : tabs) {
                    if (t.over(left, top, x, y)) {
                        selected = t;
                        lastTab = t.root.a.id;
                        break;
                    }
                }
            }
        }

        @Override
        protected void keyTyped(char c, int key) {
            if (key == KEY.getKeyCode() || key == Keyboard.KEY_ESCAPE) {
                mc.displayGuiScreen(null);
            }
        }

        @Override
        public boolean doesGuiPauseGame() {
            return false;
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partial) {
            if (built != syncCount) {
                rebuild();
            }
            int left = (width - 252) / 2;
            int top = (height - 140) / 2;
            if (Mouse.isButtonDown(0)) {
                if (!scrolling) {
                    scrolling = true;
                } else if (selected != null) {
                    drag(selected, mouseX - scrollMouseX, mouseY - scrollMouseY);
                }
                scrollMouseX = mouseX;
                scrollMouseY = mouseY;
            } else {
                scrolling = false;
            }
            drawDefaultBackground();
            inside(left + 9, top + 18);
            window(left, top);
            tooltips(mouseX, mouseY, left, top);
            super.drawScreen(mouseX, mouseY, partial);
        }

        private void drag(Tab t, int dx, int dy) {
            if (t.maxX - t.minX > 234) {
                t.scrollX = Math.max(-(t.maxX - 234), Math.min(0, t.scrollX + dx));
            }
            if (t.maxY - t.minY > 113) {
                t.scrollY = Math.max(-(t.maxY - 113), Math.min(0, t.scrollY + dy));
            }
        }

        /** The tree of the selected tab over its tiled background, cut to the 234x113 opening of the window. */
        private void inside(int ix, int iy) {
            Tab t = selected;
            if (t == null) {
                Gui.drawRect(ix, iy, ix + 234, iy + 113, 0xFF000000);
                String empty = StatCollector.translateToLocal("advancements.empty");
                fontRendererObj.drawString(empty, ix + 117 - fontRendererObj.getStringWidth(empty) / 2, iy + 56 - fontRendererObj.FONT_HEIGHT / 2, -1);
                fontRendererObj.drawString(":(", ix + 117 - fontRendererObj.getStringWidth(":(") / 2, iy + 113 - fontRendererObj.FONT_HEIGHT, -1);
                return;
            }
            if (!t.centred) {
                t.scrollX = 117 - (t.maxX + t.minX) / 2;
                t.scrollY = 56 - (t.maxY + t.minY) / 2;
                t.centred = true;
            }
            int scale = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight).getScaleFactor();
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor(ix * scale, mc.displayHeight - (iy + 113) * scale, 234 * scale, 113 * scale);
            textured();
            if (t.background != null) {
                mc.getTextureManager().bindTexture(t.background);
                int bx = t.scrollX % 16;
                int by = t.scrollY % 16;
                Tessellator tess = Tessellator.instance;
                tess.startDrawingQuads();
                for (int i = -1; i <= 15; i++) {
                    for (int j = -1; j <= 8; j++) {
                        int x = ix + bx + 16 * i;
                        int y = iy + by + 16 * j;
                        tess.addVertexWithUV(x, y + 16, 0.0D, 0.0D, 1.0D);
                        tess.addVertexWithUV(x + 16, y + 16, 0.0D, 1.0D, 1.0D);
                        tess.addVertexWithUV(x + 16, y, 0.0D, 1.0D, 0.0D);
                        tess.addVertexWithUV(x, y, 0.0D, 0.0D, 0.0D);
                    }
                }
                tess.draw();
            } else {
                Gui.drawRect(ix, iy, ix + 234, iy + 113, 0xFF000000);
            }
            int ox = ix + t.scrollX;
            int oy = iy + t.scrollY;
            for (Widget w : t.all) {
                connect(w, ox, oy, true);
            }
            for (Widget w : t.all) {
                connect(w, ox, oy, false);
            }
            for (Widget w : t.all) {
                if (w.drawn()) {
                    textured();
                    mc.getTextureManager().bindTexture(WIDGETS);
                    blit(ox + w.x + 3, oy + w.y, w.frameU(), 128 + (w.done ? 0 : 26), 26, 26);
                    icon(w.a.icon, ox + w.x + 8, oy + w.y + 5);
                }
            }
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }

        /** The elbow from the parent: first all of them in black, three wide, then all in white over that. */
        private void connect(Widget w, int ox, int oy, boolean outline) {
            if (w.above == null) {
                return;
            }
            int fromX = ox + w.above.x + 13;
            int bendX = ox + w.above.x + 26 + 4;
            int fromY = oy + w.above.y + 13;
            int toX = ox + w.x + 13;
            int toY = oy + w.y + 13;
            if (outline) {
                int c = 0xFF000000;
                across(bendX, fromX, fromY - 1, c);
                across(bendX + 1, fromX, fromY, c);
                across(bendX, fromX, fromY + 1, c);
                across(toX, bendX - 1, toY - 1, c);
                across(toX, bendX - 1, toY, c);
                across(toX, bendX - 1, toY + 1, c);
                down(bendX - 1, toY, fromY, c);
                down(bendX + 1, toY, fromY, c);
            } else {
                int c = 0xFFFFFFFF;
                across(bendX, fromX, fromY, c);
                across(toX, bendX, toY, c);
                down(bendX, toY, fromY, c);
            }
        }

        private void window(int left, int top) {
            textured();
            mc.getTextureManager().bindTexture(WINDOW);
            blit(left, top, 0, 0, 252, 140);
            if (tabs.size() > 1) {
                mc.getTextureManager().bindTexture(TABS);
                for (Tab t : tabs) {
                    int[] k = TAB_KINDS[t.kind];
                    int u = k[0] + (t.index > 0 ? k[2] : 0) + (t.index == k[4] - 1 ? k[2] : 0);
                    int v = t == selected ? k[1] + k[3] : k[1];
                    blit(t.x(left), t.y(top), u, v, k[2], k[3]);
                }
                for (Tab t : tabs) {
                    int[] k = TAB_KINDS[t.kind];
                    icon(t.root.a.icon, t.x(left) + k[5], t.y(top) + k[6]);
                }
            }
            GL11.glDisable(GL11.GL_BLEND);
            fontRendererObj.drawString(StatCollector.translateToLocal("gui.advancements"), left + 8, top + 6, 4210752);
        }

        private void tooltips(int mouseX, int mouseY, int left, int top) {
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            Tab t = selected;
            if (t != null) {
                int ix = left + 9;
                int iy = top + 18;
                int mx = mouseX - ix;
                int my = mouseY - iy;
                Gui.drawRect(ix, iy, ix + 234, iy + 113, ((int) Math.floor(t.fade * 255.0F)) << 24);
                Widget over = null;
                if (mx > 0 && mx < 234 && my > 0 && my < 113) {
                    for (Widget w : t.all) {
                        int x = t.scrollX + w.x;
                        int y = t.scrollY + w.y;
                        if (w.drawn() && mx >= x && mx <= x + 26 && my >= y && my <= y + 26) {
                            over = w;
                            break;
                        }
                    }
                }
                if (over != null) {
                    box(over, ix + t.scrollX, iy + t.scrollY, left);
                    t.fade = Math.min(0.3F, t.fade + 0.02F);
                } else {
                    t.fade = Math.max(0.0F, t.fade - 0.04F);
                }
            }
            if (tabs.size() > 1) {
                for (Tab other : tabs) {
                    if (other.over(left, top, mouseX, mouseY)) {
                        List<String> lines = new ArrayList<String>();
                        lines.add(StatCollector.translateToLocal(other.root.a.title));
                        drawHoveringText(lines, mouseX, mouseY, fontRendererObj);
                        RenderHelper.disableStandardItemLighting();
                    }
                }
            }
        }

        /** The opened widget: a title bar 26 high across the icon and a box with the description under or over it. */
        private void box(Widget w, int ox, int oy, int left) {
            Tab t = w.tab;
            int fontH = fontRendererObj.FONT_HEIGHT;
            boolean toLeft = left + t.scrollX + w.x + w.width + 26 >= width;
            boolean upwards = 113 - t.scrollY - w.y - 26 <= 6 + w.lines.size() * fontH;
            int state = w.done ? 0 : 1;
            int half = w.width / 2;
            int rest = w.width - half;
            int y = oy + w.y;
            int x = toLeft ? ox + w.x - w.width + 26 + 6 : ox + w.x;
            int boxH = 32 + w.lines.size() * fontH;
            textured();
            mc.getTextureManager().bindTexture(WIDGETS);
            if (!w.lines.isEmpty()) {
                int by = upwards ? y + 26 - boxH : y;
                int bw = w.width;
                // nine pieces of the 200x26 plate at (0, 52), the border is 10
                blit(x, by, 0, 52, 10, 10);
                repeat(x + 10, by, bw - 20, 10, 10, 52, 180, 26);
                blit(x + bw - 10, by, 190, 52, 10, 10);
                blit(x, by + boxH - 10, 0, 68, 10, 10);
                repeat(x + 10, by + boxH - 10, bw - 20, 10, 10, 68, 180, 26);
                blit(x + bw - 10, by + boxH - 10, 190, 68, 10, 10);
                repeat(x, by + 10, 10, boxH - 20, 0, 62, 200, 6);
                repeat(x + 10, by + 10, bw - 20, boxH - 20, 10, 62, 180, 6);
                repeat(x + bw - 10, by + 10, 10, boxH - 20, 190, 62, 200, 6);
            }
            // the bar is filled as far as the progress goes: 0 = obtained, 1 = not yet (as the widget of 1.12 picks)
            int fill = Math.round(w.fraction * (float) w.width);
            int sLeft;
            int sRight;
            int sIcon;
            if (w.fraction >= 1.0F) {
                fill = w.width / 2;
                sLeft = 0;
                sRight = 0;
                sIcon = 0;
            } else if (fill < 2) {
                fill = w.width / 2;
                sLeft = 1;
                sRight = 1;
                sIcon = 1;
            } else if (fill > w.width - 2) {
                fill = w.width / 2;
                sLeft = 0;
                sRight = 0;
                sIcon = 1;
            } else {
                sLeft = 0;
                sRight = 1;
                sIcon = 1;
            }
            blit(x, y, 0, sLeft * 26, fill, 26);
            blit(x + fill, y, 200 - (w.width - fill), sRight * 26, w.width - fill, 26);
            blit(ox + w.x + 3, y, w.frameU(), 128 + sIcon * 26, 26, 26);
            fontRendererObj.drawString(w.title, toLeft ? x + 5 : ox + w.x + 32, y + 9, -1, true);
            if (w.progress != null) {
                int pw = fontRendererObj.getStringWidth(w.progress);
                fontRendererObj.drawString(w.progress, toLeft ? ox + w.x - pw : x + w.width - pw - 5, y + 9, -1, true);
            }
            for (int i = 0; i < w.lines.size(); i++) {
                int ly = upwards ? y + 26 - boxH + 7 + i * fontH : y + 9 + 17 + i * fontH;
                fontRendererObj.drawString(w.lines.get(i), x + 5, ly, -5592406, false);
            }
            icon(w.a.icon, ox + w.x + 8, y + 5);
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Recipe book
    // ------------------------------------------------------------------------------------------------------------

    /** Recipes shown as one slot of the book: those of one category that share a group, or a single one. */
    private static final class Group {
        int category;
        final List<Data112.Recipe> recipes = new ArrayList<Data112.Recipe>();
    }

    /**
     * The tab of the book a recipe goes to, by the creative tab of what it makes, as RecipeBookClient of 1.12 sorts
     * them: 1 tools (and combat), 2 building blocks, 3 everything else, 4 redstone; 0 is the tab with all of them.
     */
    private static int category(Data112.Recipe r) {
        CreativeTabs tab = r.result.getItem().getCreativeTab();
        if (tab == CreativeTabs.tabBlock) {
            return 2;
        }
        if (tab == CreativeTabs.tabTools || tab == CreativeTabs.tabCombat) {
            return 1;
        }
        if (tab == CreativeTabs.tabRedstone) {
            return 4;
        }
        return 3;
    }

    private static boolean canCraft(Minecraft mc, Data112.Recipe recipe) {
        int[] used = new int[mc.thePlayer.inventory.mainInventory.length];
        for (List<ItemStack> options : recipe.slots) {
            if (options.isEmpty()) {
                continue;
            }
            boolean found = false;
            for (int i = 0; i < used.length && !found; i++) {
                ItemStack have = mc.thePlayer.inventory.mainInventory[i];
                if (have != null && used[i] < have.stackSize) {
                    for (ItemStack o : options) {
                        if (o.getItem() == have.getItem() && o.getItemDamage() == have.getItemDamage()) {
                            used[i]++;
                            found = true;
                            break;
                        }
                    }
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    private static boolean anyCraftable(Minecraft mc, Group g) {
        for (Data112.Recipe r : g.recipes) {
            if (canCraft(mc, r)) {
                return true;
            }
        }
        return false;
    }

    /** A button that is a piece of a sheet; the hovered picture lies hoverV lower. */
    private static class ImageButton extends GuiButton {
        final ResourceLocation sheet;
        int u;
        final int v;
        final int hoverV;

        ImageButton(int id, int x, int y, int w, int h, ResourceLocation sheet, int u, int v, int hoverV) {
            super(id, x, y, w, h, "");
            this.sheet = sheet;
            this.u = u;
            this.v = v;
            this.hoverV = hoverV;
        }

        boolean over(int mouseX, int mouseY) {
            return mouseX >= xPosition && mouseY >= yPosition && mouseX < xPosition + width && mouseY < yPosition + height;
        }

        /** One click is one action: buttons made by a rebuild during this click are not pressed by the same click loop. */
        @Override
        public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
            return !swallowClick && super.mousePressed(mc, mouseX, mouseY);
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY) {
            if (!visible) {
                return;
            }
            field_146123_n = over(mouseX, mouseY);
            textured();
            mc.getTextureManager().bindTexture(sheet);
            blit(xPosition, yPosition, u, field_146123_n ? v + hoverV : v, width, height);
        }
    }

    /** Not a button at all: the body of the book, drawn before the buttons that lie on it. */
    private static final class PanelButton extends GuiButton {
        String page;
        GuiTextField field;

        PanelButton(int x, int y) {
            super(9399, x, y, 0, 0, "");
        }

        @Override
        public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
            return false;
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY) {
            textured();
            mc.getTextureManager().bindTexture(BOOK);
            blit(xPosition, yPosition, 1, 1, 147, 166);
            if (page != null) {
                mc.fontRenderer.drawString(page, xPosition + 73 - mc.fontRenderer.getStringWidth(page) / 2, yPosition + 141, -1);
                GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            }
            if (field != null) {
                field.drawTextBox();
                GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            }
        }
    }

    private static final class TabButton extends ImageButton {
        final int category;

        TabButton(int category, int x, int y) {
            super(9310 + category, x, y, 35, 27, BOOK, 153, 2, 0);
            this.category = category;
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY) {
            if (!visible) {
                return;
            }
            boolean chosen = category == bookTab;
            field_146123_n = over(mouseX, mouseY);
            textured();
            mc.getTextureManager().bindTexture(BOOK);
            blit(chosen ? xPosition - 2 : xPosition, yPosition, chosen ? u + 35 : u, v, width, height);
            int y = yPosition + 5;
            switch (category) {
                case 0:
                    icon(new ItemStack(Items.compass), xPosition + 9, y);
                    break;
                case 1:
                    icon(new ItemStack(Items.iron_axe), xPosition + 3, y);
                    icon(new ItemStack(Items.golden_sword), xPosition + 14, y);
                    break;
                case 2:
                    icon(new ItemStack(Blocks.brick_block), xPosition + 9, y);
                    break;
                case 3:
                    icon(new ItemStack(Items.lava_bucket), xPosition + 3, y);
                    icon(new ItemStack(Items.apple), xPosition + 14, y);
                    break;
                default:
                    icon(new ItemStack(Items.redstone), xPosition + 9, y);
                    break;
            }
        }
    }

    /** One slot of the book. */
    private static final class RecipeButton extends GuiButton {
        final Group group;
        final long born = System.currentTimeMillis();

        RecipeButton(int id, int x, int y, Group group) {
            super(id, x, y, 25, 25, "");
            this.group = group;
        }

        /** While the window of alternatives is open, clicks belong to it. */
        @Override
        public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
            return altGroup == null && !swallowClick && super.mousePressed(mc, mouseX, mouseY);
        }

        /** Craftable ones first, the rest after them unless only craftable ones are shown. */
        private List<Data112.Recipe> cycle(Minecraft mc) {
            List<Data112.Recipe> yes = new ArrayList<Data112.Recipe>();
            List<Data112.Recipe> no = new ArrayList<Data112.Recipe>();
            for (Data112.Recipe r : group.recipes) {
                (canCraft(mc, r) ? yes : no).add(r);
            }
            if (!bookCraftable || yes.isEmpty()) {
                yes.addAll(no);
            }
            return yes;
        }

        /** The recipe whose result is on the button now; it changes every 30 ticks. */
        Data112.Recipe recipe(Minecraft mc) {
            List<Data112.Recipe> list = cycle(mc);
            return list.get((int) ((System.currentTimeMillis() - born) / 1500L % list.size()));
        }

        private boolean sameResult() {
            ItemStack first = group.recipes.get(0).result;
            for (Data112.Recipe r : group.recipes) {
                if (r.result.getItem() != first.getItem() || r.result.getItemDamage() != first.getItemDamage()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY) {
            if (!visible) {
                return;
            }
            field_146123_n = mouseX >= xPosition && mouseY >= yPosition && mouseX < xPosition + width && mouseY < yPosition + height;
            textured();
            mc.getTextureManager().bindTexture(BOOK);
            // the red frame is 25 to the right, the frame of a slot with several recipes 25 lower
            blit(xPosition, yPosition, anyCraftable(mc, group) ? 29 : 54, group.recipes.size() > 1 ? 231 : 206, 25, 25);
            ItemStack stack = recipe(mc).result;
            int at = 4;
            if (group.recipes.size() > 1 && sameResult()) {
                icon(stack, xPosition + 5, yPosition + 5);
                at = 3;
            }
            icon(stack, xPosition + at, yPosition + at);
        }
    }

    /** Lends its protected tooltip drawing to screens that are not ours. */
    private static final class Tip extends GuiScreen {
        void show(GuiScreen on, List<String> lines, int x, int y) {
            mc = Minecraft.getMinecraft();
            width = on.width;
            height = on.height;
            fontRendererObj = mc.fontRenderer;
            drawHoveringText(lines, x, y, mc.fontRenderer);
            RenderHelper.disableStandardItemLighting();
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    public static final class Events {
        private final List<RecipeButton> shown = new ArrayList<RecipeButton>();
        private ImageButton filter;
        private ImageButton pageBack;
        private ImageButton pageNext;
        private TabButton currentTab;
        private GuiTextField search;
        private String lastQuery = "";
        private int panelX;
        private int panelY;
        /** While the buttons are remade by the book itself: the search field stays as it is. */
        private boolean rebuilding;
        private boolean keyRepeat;
        private final Tip tip = new Tip();
        private boolean leftWas;
        private boolean rightWas;

        @SubscribeEvent
        public void onKey(InputEvent.KeyInputEvent event) {
            if (KEY.isPressed() && Minecraft.getMinecraft().currentScreen == null) {
                openAdvancements();
            }
        }

        private boolean applies(GuiScreen gui) {
            return bookOpen && (gui instanceof GuiInventory || gui instanceof GuiCrafting);
        }

        private void repeat(boolean on) {
            if (on != keyRepeat) {
                Keyboard.enableRepeatEvents(on);
                keyRepeat = on;
            }
        }

        /** Remakes the buttons of an open book, for a change that does not reset the search field. */
        private void rebuild(GuiScreen gui) {
            rebuilding = true;
            try {
                gui.setWorldAndResolution(Minecraft.getMinecraft(), gui.width, gui.height);
            } finally {
                rebuilding = false;
            }
        }

        /**
         * The panel, and the tab that is selected at the moment of the test (after a click on another tab that one is
         * the selected one), as GuiRecipeBook.hasClickedOutside. The panel is measured from where it is drawn, so its
         * three leftmost columns count too.
         */
        boolean overBook(GuiContainer gui, int x, int y) {
            if (!applies(gui)) {
                return false;
            }
            if (altGroup != null) {
                return true;
            }
            int left;
            try {
                left = ((Integer) ReflectionHelper.getPrivateValue(GuiContainer.class, gui, "guiLeft", "field_147003_i")).intValue();
            } catch (RuntimeException e) {
                return false;
            }
            return x >= panelX && x < left && y >= panelY && y < panelY + 166 || currentTab != null && currentTab.over(x, y);
        }

        /** As GuiRecipeBook.mouseClicked: an open window of alternatives takes every click; the page keeps the focus. */
        boolean click(GuiContainer gui, int x, int y, int button) {
            if (!applies(gui)) {
                return false;
            }
            if (altGroup != null) {
                return true;
            }
            if (button != 0 || search == null || overPage(x, y)) {
                return false;
            }
            search.mouseClicked(x, y, button);
            return false;
        }

        private boolean overPage(int x, int y) {
            if (pageBack != null && pageBack.over(x, y) || pageNext != null && pageNext.over(x, y)) {
                return true;
            }
            for (RecipeButton b : shown) {
                if (x >= b.xPosition && y >= b.yPosition && x < b.xPosition + b.width && y < b.yPosition + b.height) {
                    return true;
                }
            }
            return false;
        }

        /** As GuiRecipeBook.keyPressed with the book beside the window; the keys it does not take go on to GuiContainer. */
        boolean key(GuiContainer gui, char c, int code) {
            if (!applies(gui) || search == null) {
                return false;
            }
            Minecraft mc = Minecraft.getMinecraft();
            if (GameSettings.isKeyDown(mc.gameSettings.keyBindChat) && !search.isFocused()) {
                search.setFocused(true);
                return false;
            }
            if (!search.textboxKeyTyped(c, code)) {
                return false;
            }
            String text = search.getText().toLowerCase(Locale.ROOT);
            if (!text.equals(lastQuery)) {
                lastQuery = text;
                bookPage = 0;
                rebuild(gui);
            }
            return true;
        }

        private boolean matches(Group g, String text) {
            for (Data112.Recipe r : g.recipes) {
                if (EnumChatFormatting.getTextWithoutFormattingCodes(r.result.getDisplayName()).toLowerCase(Locale.ROOT).contains(text)) {
                    return true;
                }
            }
            return false;
        }

        @SubscribeEvent
        public void onTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END && search != null && applies(Minecraft.getMinecraft().currentScreen)) {
                search.updateCursorCounter();
            }
        }

        @SubscribeEvent
        public void onOpen(GuiOpenEvent event) {
            repeat(false);
        }

        /** The slots of the book: known recipes that fit the grid, gathered as 1.12 does, in the order of the data. */
        private List<Group> groups(boolean small) {
            List<Group> out = new ArrayList<Group>();
            Map<String, Group> named = new LinkedHashMap<String, Group>();
            // 1.12.2 registers its recipes as Files.walk over the zip file system of the jar finds them, and that
            // walk gives the entries of a directory in the reverse of their order in the jar (alphabetical there),
            // so the book runs from yellow_wool back to acacia_planks. recipes.txt keeps the order of the jar.
            List<Data112.Recipe> recipes = new ArrayList<Data112.Recipe>(Data112.RECIPES.values());
            Collections.reverse(recipes);
            for (Data112.Recipe r : recipes) {
                if (r.result == null || r.result.getItem() == null || !KNOWN.contains(r.id)) {
                    continue;
                }
                if (small && !(r.width <= 2 && r.height <= 2 && r.slots.size() <= 4)) {
                    continue;
                }
                int category = category(r);
                Group g = null;
                if (r.group != null && r.group.length() > 0) {
                    g = named.get(category + "|" + r.group);
                }
                if (g == null) {
                    g = new Group();
                    g.category = category;
                    out.add(g);
                    if (r.group != null && r.group.length() > 0) {
                        named.put(category + "|" + r.group, g);
                    }
                }
                g.recipes.add(r);
            }
            return out;
        }

        @SubscribeEvent
        @SuppressWarnings("unchecked")
        public void onInit(GuiScreenEvent.InitGuiEvent.Post event) {
            if (!(event.gui instanceof GuiInventory || event.gui instanceof GuiCrafting)) {
                return;
            }
            Minecraft mc = Minecraft.getMinecraft();
            GuiContainer gui = (GuiContainer) event.gui;
            boolean small = gui instanceof GuiInventory;
            int left;
            shown.clear();
            filter = null;
            pageBack = null;
            pageNext = null;
            currentTab = null;
            altGroup = null;
            if (!bookOpen) {
                ghost = null;
            }
            if (bookOpen) {
                // Like the recipe book of 1.12: the window moves right to make room for the panel.
                left = 177 + (gui.width - 176 - 200) / 2;
                ReflectionHelper.setPrivateValue(GuiContainer.class, gui, Integer.valueOf(left), "guiLeft", "field_147003_i");
                if (gui instanceof InventoryEffectRenderer) {
                    // and the column of potion effects, which would lie under the book, is not drawn
                    try {
                        ReflectionHelper.setPrivateValue(InventoryEffectRenderer.class, (InventoryEffectRenderer) gui, Boolean.FALSE, "field_147045_u");
                    } catch (RuntimeException e) {
                        // the column stays
                    }
                }
            } else {
                left = (gui.width - 176) / 2;
                try {
                    left = ((Integer) ReflectionHelper.getPrivateValue(GuiContainer.class, gui, "guiLeft", "field_147003_i")).intValue();
                } catch (RuntimeException e) {
                    // the usual place
                }
            }
            if (small) {
                event.buttonList.add(new ImageButton(9300, left + 104, gui.height / 2 - 22, 20, 18, INVENTORY, 178, 0, 19));
            } else {
                event.buttonList.add(new ImageButton(9300, left + 5, gui.height / 2 - 49, 20, 18, CRAFTING, 0, 168, 19));
            }
            if (!bookOpen || mc.thePlayer == null) {
                search = null;
                repeat(false);
                return;
            }
            int x = (gui.width - 147) / 2 - 86;
            int y = (gui.height - 166) / 2;
            panelX = x;
            panelY = y;
            if (!rebuilding || search == null) {
                search = new GuiTextField(mc.fontRenderer, x + 25, y + 14, 80, mc.fontRenderer.FONT_HEIGHT + 5);
                search.setMaxStringLength(50);
                search.setEnableBackgroundDrawing(false);
                search.setVisible(true);
                search.setTextColor(0xFFFFFF);
                lastQuery = "";
            }
            repeat(true);
            String query = search.getText().toLowerCase(Locale.ROOT);
            List<Group> all = groups(small);
            boolean[] has = new boolean[5];
            has[0] = true;
            for (Group g : all) {
                has[g.category] = true;
            }
            if (bookTab < 0 || bookTab > 4 || !has[bookTab]) {
                bookTab = 0;
            }
            List<Group> list = new ArrayList<Group>();
            for (Group g : all) {
                if ((bookTab == 0 || g.category == bookTab) && (query.length() == 0 || matches(g, query)) && (!bookCraftable || anyCraftable(mc, g))) {
                    list.add(g);
                }
            }
            int pages = (list.size() + 19) / 20;
            bookPage = Math.max(0, Math.min(bookPage, pages - 1));
            PanelButton panel = new PanelButton(x, y);
            panel.page = pages > 1 ? (bookPage + 1) + "/" + pages : null;
            panel.field = search;
            event.buttonList.add(panel);
            filter = new ImageButton(9303, x + 110, y + 12, 26, 16, BOOK, bookCraftable ? 152 + 28 : 152, 41, 18);
            event.buttonList.add(filter);
            if (pages > 1 && bookPage > 0) {
                pageBack = new ImageButton(9301, x + 38, y + 137, 12, 17, BOOK, 1 + 13, 208, 18);
                event.buttonList.add(pageBack);
            }
            if (pages > 1 && bookPage < pages - 1) {
                pageNext = new ImageButton(9302, x + 93, y + 137, 12, 17, BOOK, 1, 208, 18);
                event.buttonList.add(pageNext);
            }
            int row = 0;
            for (int c = 0; c < 5; c++) {
                if (has[c]) {
                    TabButton tab = new TabButton(c, x - 30, y + 3 + 27 * row++);
                    if (c == bookTab) {
                        currentTab = tab;
                    }
                    event.buttonList.add(tab);
                }
            }
            for (int i = bookPage * 20, n = 0; i < list.size() && n < 20; i++, n++) {
                RecipeButton b = new RecipeButton(9400 + n, x + 11 + 25 * (n % 5), y + 31 + 25 * (n / 5), list.get(i));
                shown.add(b);
                event.buttonList.add(b);
            }
        }

        @SubscribeEvent
        public void onAction(GuiScreenEvent.ActionPerformedEvent.Post event) {
            if (!(event.gui instanceof GuiInventory || event.gui instanceof GuiCrafting)) {
                return;
            }
            Minecraft mc = Minecraft.getMinecraft();
            int id = event.button.id;
            boolean again = true;
            if (id == 9300) {
                bookOpen = !bookOpen;
            } else if (id == 9301) {
                bookPage--;
            } else if (id == 9302) {
                bookPage++;
            } else if (id == 9303) {
                bookCraftable = !bookCraftable;
                bookPage = 0;
            } else if (event.button instanceof TabButton) {
                bookTab = ((TabButton) event.button).category;
                bookPage = 0;
            } else {
                again = false;
                if (event.button instanceof RecipeButton) {
                    choose(mc, ((RecipeButton) event.button).recipe(mc));
                }
            }
            if (id == 9300) {
                swallowClick = true;
                event.gui.setWorldAndResolution(mc, event.gui.width, event.gui.height);
            } else if (again) {
                swallowClick = true;
                rebuild(event.gui);
            }
        }

        @SubscribeEvent
        @SuppressWarnings("unchecked")
        public void onDrawn(GuiScreenEvent.DrawScreenEvent.Post event) {
            swallowClick = false;
            if (!bookOpen || !(event.gui instanceof GuiInventory || event.gui instanceof GuiCrafting)) {
                return;
            }
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.thePlayer == null) {
                return;
            }
            drawGhost(mc, event.gui);
            boolean held = mc.thePlayer.inventory.getItemStack() != null;
            if (alternatives(mc, event.gui, event.mouseX, event.mouseY) || held) {
                return;
            }
            int mx = event.mouseX;
            int my = event.mouseY;
            for (RecipeButton b : shown) {
                if (mx >= b.xPosition && my >= b.yPosition && mx < b.xPosition + b.width && my < b.yPosition + b.height) {
                    ItemStack stack = b.recipe(mc).result;
                    List<String> lines = new ArrayList<String>(stack.getTooltip(mc.thePlayer, mc.gameSettings.advancedItemTooltips));
                    if (!lines.isEmpty()) {
                        lines.set(0, stack.getRarity().rarityColor + lines.get(0));
                    }
                    tip.show(event.gui, lines, mx, my);
                    return;
                }
            }
            if (filter != null && filter.over(mx, my)) {
                List<String> lines = new ArrayList<String>();
                lines.add(StatCollector.translateToLocal(bookCraftable ? "gui.recipebook.toggleRecipes.craftable" : "gui.recipebook.toggleRecipes.all"));
                tip.show(event.gui, lines, mx, my);
            }
        }

        /** A recipe chosen in the book: one that can be made is filled in, any other is shown as a ghost in the grid. */
        private void choose(Minecraft mc, Data112.Recipe r) {
            if (canCraft(mc, r)) {
                ghost = null;
                MC112Bridge.NETWORK.channel.sendToServer(new Data112.FillMessage(r.id));
            } else {
                ghost = r;
            }
        }

        /** The recipe as a see-through picture over the empty slots of the grid; the options of a slot take turns. */
        private void drawGhost(Minecraft mc, GuiScreen gui) {
            if (ghost == null || !(gui instanceof GuiContainer)) {
                return;
            }
            if (canCraft(mc, ghost)) {
                ghost = null;
                return;
            }
            GuiContainer c = (GuiContainer) gui;
            int gridW = gui instanceof GuiInventory ? 2 : 3;
            int left;
            int top;
            try {
                left = ((Integer) ReflectionHelper.getPrivateValue(GuiContainer.class, c, "guiLeft", "field_147003_i")).intValue();
                top = ((Integer) ReflectionHelper.getPrivateValue(GuiContainer.class, c, "guiTop", "field_147009_r")).intValue();
            } catch (RuntimeException e) {
                return;
            }
            if (ghost.shaped && (ghost.width > gridW || ghost.height > gridW)) {
                return;
            }
            int index = 0;
            for (List<ItemStack> options : ghost.slots) {
                int cell = index;
                if (ghost.shaped && ghost.width > 0) {
                    cell = index / ghost.width * gridW + index % ghost.width;
                }
                index++;
                if (options.isEmpty() || cell >= gridW * gridW || cell + 1 >= c.inventorySlots.inventorySlots.size()) {
                    continue;
                }
                net.minecraft.inventory.Slot slot = c.inventorySlots.getSlot(cell + 1);
                if (slot.getHasStack()) {
                    continue;
                }
                ItemStack stack = options.get((int) (System.currentTimeMillis() / 1000L % options.size()));
                int sx = left + slot.xDisplayPosition;
                int sy = top + slot.yDisplayPosition;
                icon(stack, sx, sy);
                Gui.drawRect(sx, sy, sx + 16, sy + 16, 0x30FFFFFF);
            }
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        }

        /**
         * The window of alternatives: a right click on a slot with several recipes opens it, a click in it picks one,
         * any other click closes it. There is no mouse event for screens in this Forge, so the buttons are polled.
         * Returns true if it took the tooltip.
         */
        private boolean alternatives(Minecraft mc, GuiScreen gui, int mx, int my) {
            boolean right = Mouse.isButtonDown(1);
            boolean left = Mouse.isButtonDown(0);
            boolean rightNew = right && !rightWas;
            boolean leftNew = left && !leftWas;
            rightWas = right;
            leftWas = left;
            if (rightNew) {
                RecipeButton hit = null;
                for (RecipeButton b : shown) {
                    if (mx >= b.xPosition && my >= b.yPosition && mx < b.xPosition + b.width && my < b.yPosition + b.height && b.group.recipes.size() > 1) {
                        hit = b;
                    }
                }
                if (hit != null) {
                    altGroup = hit.group;
                    altX = hit.xPosition;
                    altY = hit.yPosition;
                    return false;
                }
                altGroup = null;
            }
            if (altGroup == null) {
                return false;
            }
            List<Data112.Recipe> list = altGroup.recipes;
            int n = list.size();
            int cols = Math.min(4, n);
            int rows = (n + 3) / 4;
            int bw = cols * 25 + 8;
            int bh = rows * 25 + 8;
            int bx = Math.max(2, Math.min(gui.width - bw - 2, altX + 12 - bw / 2));
            int by = Math.max(2, Math.min(gui.height - bh - 2, altY + 27));
            int over = -1;
            for (int i = 0; i < n; i++) {
                int cx = bx + 4 + 25 * (i % 4);
                int cy = by + 4 + 25 * (i / 4);
                if (mx >= cx && my >= cy && mx < cx + 25 && my < cy + 25) {
                    over = i;
                }
            }
            if (leftNew || rightNew) {
                if (leftNew && over >= 0) {
                    choose(mc, list.get(over));
                }
                if (leftNew || !(mx >= bx && my >= by && mx < bx + bw && my < by + bh)) {
                    altGroup = null;
                    return false;
                }
            }
            GL11.glPushMatrix();
            GL11.glTranslatef(0.0F, 0.0F, 300.0F);
            Gui.drawRect(bx - 1, by - 1, bx + bw + 1, by + bh + 1, 0xFF000000);
            Gui.drawRect(bx, by, bx + bw, by + bh, 0xFFC6C6C6);
            for (int i = 0; i < n; i++) {
                int cx = bx + 4 + 25 * (i % 4);
                int cy = by + 4 + 25 * (i / 4);
                textured();
                mc.getTextureManager().bindTexture(BOOK);
                blit(cx, cy, canCraft(mc, list.get(i)) ? 29 : 54, 206, 25, 25);
                icon(list.get(i).result, cx + 4, cy + 4);
            }
            GL11.glPopMatrix();
            if (over >= 0) {
                ItemStack stack = list.get(over).result;
                List<String> lines = new ArrayList<String>(stack.getTooltip(mc.thePlayer, mc.gameSettings.advancedItemTooltips));
                if (!lines.isEmpty()) {
                    lines.set(0, stack.getRarity().rarityColor + lines.get(0));
                }
                tip.show(gui, lines, mx, my);
            }
            return true;
        }

        /** Without a screen open the toasts go over the HUD... */
        @SubscribeEvent
        public void onOverlay(RenderGameOverlayEvent.Post event) {
            if (event.type == RenderGameOverlayEvent.ElementType.ALL && Minecraft.getMinecraft().currentScreen == null) {
                toasts(event.resolution.getScaledWidth());
            }
        }

        /** ...and with one, over the screen, as 1.12 draws them after everything else. */
        @SubscribeEvent
        public void onScreenDrawn(GuiScreenEvent.DrawScreenEvent.Post event) {
            toasts(event.gui.width);
        }
    }

    private static void toasts(int screenWidth) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.gameSettings.hideGUI) {
            return;
        }
        RenderHelper.disableStandardItemLighting();
        for (int i = 0; i < TOAST_SLOTS.length; i++) {
            if (TOAST_SLOTS[i] != null && TOAST_SLOTS[i].render(mc, screenWidth, i)) {
                TOAST_SLOTS[i] = null;
            }
            if (TOAST_SLOTS[i] == null) {
                String next = null;
                synchronized (TOAST_QUEUE) {
                    while (next == null && !TOAST_QUEUE.isEmpty()) {
                        next = TOAST_QUEUE.remove(0);
                        if (!Data112.ADVANCEMENTS.containsKey(next)) {
                            next = null;
                        }
                    }
                }
                if (next != null) {
                    TOAST_SLOTS[i] = new ToastSlot(Data112.ADVANCEMENTS.get(next));
                }
            }
        }
    }

    /**
     * One place of GuiToast (bkc$a) with the advancement toast in it (bjy): it slides in from the right in 600 ms
     * (the square of the time, so it slows down), stays 5 seconds and slides out the same way.
     */
    private static final class ToastSlot {
        private final Data112.Advancement a;
        private long start = -1L;
        private long shown = -1L;
        private boolean hiding;
        private boolean played;

        ToastSlot(Data112.Advancement a) {
            this.a = a;
        }

        private float visibility(long now) {
            float f = Math.max(0.0F, Math.min(1.0F, (float) (now - start) / 600.0F));
            f *= f;
            return hiding ? 1.0F - f : f;
        }

        /** Returns true once it has gone. */
        boolean render(Minecraft mc, int screenWidth, int index) {
            long now = Minecraft.getSystemTime();
            if (start == -1L) {
                start = now;
            }
            if (!hiding && now - start <= 600L) {
                shown = now;
            }
            GL11.glPushMatrix();
            GL11.glTranslatef((float) screenWidth - 160.0F * visibility(now), (float) (index * 32), 500.0F);
            boolean hide = draw(mc, now - shown);
            GL11.glPopMatrix();
            if (hide && !hiding) {
                start = now - (long) (int) ((1.0F - visibility(now)) * 600.0F);
                hiding = true;
            }
            return hiding && now - start > 600L;
        }

        /** Returns true when its time is up. */
        @SuppressWarnings("unchecked")
        private boolean draw(Minecraft mc, long time) {
            FontRenderer font = mc.fontRenderer;
            if (!played) {
                played = true;
                if ("challenge".equals(a.frame)) {
                    mc.getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation(DOMAIN, "ui.toast.challenge_complete"), 1.0F));
                }
            }
            textured();
            mc.getTextureManager().bindTexture(TOASTS);
            blit(0, 0, 0, 0, 160, 32);
            int colour = "challenge".equals(a.frame) ? 0xFF88FF : 0xFFFF00;
            String header = StatCollector.translateToLocal("advancements.toast." + a.frame);
            List<String> title = font.listFormattedStringToWidth(StatCollector.translateToLocal(a.title), 125);
            if (title.size() == 1) {
                font.drawString(header, 30, 7, colour | 0xFF000000);
                font.drawString(title.get(0), 30, 18, -1);
            } else if (time < 1500L) {
                // a long title: first the header alone, fading out...
                int alpha = (int) Math.floor(Math.max(0.0F, Math.min(1.0F, (1500.0F - time) / 300.0F)) * 255.0F) << 24 | 0x04000000;
                font.drawString(header, 30, 11, colour | alpha);
            } else {
                // ...then the lines of the title, fading in
                int alpha = (int) Math.floor(Math.max(0.0F, Math.min(1.0F, (time - 1500.0F) / 300.0F)) * 252.0F) << 24 | 0x04000000;
                int y = 16 - title.size() * font.FONT_HEIGHT / 2;
                for (String line : title) {
                    font.drawString(line, 30, y, 0xFFFFFF | alpha);
                    y += font.FONT_HEIGHT;
                }
            }
            icon(a.icon, 8, 8);
            return time >= 5000L;
        }
    }

}
