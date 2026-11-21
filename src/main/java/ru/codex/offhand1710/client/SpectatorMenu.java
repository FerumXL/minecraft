package ru.codex.offhand1710.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import org.lwjgl.opengl.GL11;
import ru.codex.offhand1710.MC112Bridge;
import ru.codex.offhand1710.Spectator112;

/**
 * GuiSpectator and SpectatorMenu of 1.12.2: in spectator mode the mouse wheel or a number key brings up a hotbar of
 * choices. The root has "Teleport to Player" and "Teleport to Team Member"; a page shows up to 7 entries, slot 8 pages
 * on (and slot 1 back once paged), slot 9 closes. Pressing the selected slot again (number key, or a click) picks it.
 * The bar fades out 5 seconds after the last use; picking a player sends a teleport request to the server.
 */
public final class SpectatorMenu {
    private static final ResourceLocation WIDGETS = new ResourceLocation("textures/gui/widgets.png");
    private static final ResourceLocation SPECTATOR = new ResourceLocation(MC112Bridge.MOD_ID, "textures/gui/spectator_widgets.png");

    /** One entry: kind 0 player (name), 1 team (name), 2 "players", 3 "teams", 4 previous, 5 next, 6 close. */
    static final class Item {
        final int kind;
        final String name;

        Item(int kind, String name) {
            this.kind = kind;
            this.name = name;
        }
    }

    /** null root, "" all players, otherwise the team whose members are listed; "#teams" the list of teams. */
    static String category;
    static boolean open;
    static int page;
    static int selected = -1;
    static long lastUsed;

    static List<Item> categoryItems() {
        Minecraft mc = Minecraft.getMinecraft();
        List<Item> out = new ArrayList<Item>();
        if (category == null) {
            out.add(new Item(2, StatCollector.translateToLocal("spectatorMenu.teleport")));
            out.add(new Item(3, StatCollector.translateToLocal("spectatorMenu.team_teleport")));
            return out;
        }
        if ("#teams".equals(category)) {
            for (Object o : mc.theWorld.getScoreboard().getTeams()) {
                ScorePlayerTeam t = (ScorePlayerTeam) o;
                if (!t.getMembershipCollection().isEmpty()) {
                    out.add(new Item(1, t.getRegisteredName()));
                }
            }
            return out;
        }
        List<String> names = new ArrayList<String>();
        for (Object o : mc.getNetHandler().playerInfoList) {
            String n = ((net.minecraft.client.gui.GuiPlayerInfo) o).name;
            if (n.equals(mc.thePlayer.getCommandSenderName())) {
                continue;
            }
            if (category.isEmpty()) {
                names.add(n);
            } else {
                ScorePlayerTeam t = mc.theWorld.getScoreboard().getPlayersTeam(n);
                if (t != null && t.getRegisteredName().equals(category)) {
                    names.add(n);
                }
            }
        }
        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
        for (String n : names) {
            out.add(new Item(0, n));
        }
        return out;
    }

    /** SpectatorMenu.getItem of 1.12.2. */
    static Item slot(int index) {
        List<Item> items = categoryItems();
        int i = index + page * 6;
        if (page > 0 && index == 0) {
            return new Item(4, StatCollector.translateToLocal("spectatorMenu.previous_page"));
        }
        if (index == 7) {
            return i < items.size() ? new Item(5, StatCollector.translateToLocal("spectatorMenu.next_page")) : null;
        }
        if (index == 8) {
            return new Item(6, StatCollector.translateToLocal("spectatorMenu.close"));
        }
        return i >= 0 && i < items.size() ? items.get(i) : null;
    }

    static boolean active() {
        Minecraft mc = Minecraft.getMinecraft();
        return mc.thePlayer != null && Spectator112.is(mc.thePlayer);
    }

    static void touch() {
        lastUsed = System.currentTimeMillis();
        if (!open) {
            open = true;
            category = null;
            page = 0;
            selected = -1;
        }
    }

    /** SpectatorMenu.selectSlot: the first press selects, a press on the selected slot picks it. */
    public static void press(int index) {
        touch();
        Item it = slot(index);
        if (it == null) {
            return;
        }
        if (selected != index) {
            selected = index;
            return;
        }
        pick(it);
    }

    static void pick(Item it) {
        switch (it.kind) {
            case 0:
                MC112Bridge.NETWORK.channel.sendToServer(new Spectator112.TeleportMsg(it.name));
                open = false;
                break;
            case 1:
                category = it.name;
                page = 0;
                selected = -1;
                break;
            case 2:
                category = "";
                page = 0;
                selected = -1;
                break;
            case 3:
                category = "#teams";
                page = 0;
                selected = -1;
                break;
            case 4:
                page = Math.max(0, page - 1);
                selected = -1;
                break;
            case 5:
                page++;
                selected = -1;
                break;
            default:
                open = false;
                break;
        }
        lastUsed = System.currentTimeMillis();
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onMouse(MouseEvent event) {
        if (!active() || event.dwheel == 0) {
            if (active() && event.button == 2 && event.buttonstate && open && selected >= 0) {
                Item it = slot(selected);
                if (it != null) {
                    pick(it);
                }
                event.setCanceled(true);
            }
            return;
        }
        touch();
        int dir = event.dwheel > 0 ? -1 : 1;
        int s = selected;
        for (int k = 0; k < 9; k++) {
            s = (s + dir + 9) % 9;
            if (slot(s) != null) {
                break;
            }
        }
        selected = s;
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.phase != TickEvent.Phase.START || mc.thePlayer == null) {
            return;
        }
        if (!active()) {
            open = false;
            return;
        }
        if (mc.currentScreen == null) {
            for (int i = 0; i < 9; i++) {
                while (mc.gameSettings.keyBindsHotbar[i].isPressed()) {
                    press(i);
                }
            }
        }
        if (open && System.currentTimeMillis() - lastUsed > 5000L) {
            open = false;
        }
    }

    /** GuiSpectator.renderTooltip / renderPage: the bar, the icons or heads, the frame round the selected slot, its name. */
    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL || !active() || !open) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution res = event.resolution;
        float alpha = Math.max(0.0F, Math.min(1.0F, (float) (lastUsed + 5000L - System.currentTimeMillis()) / 2000.0F));
        int x = res.getScaledWidth() / 2 - 91, y = res.getScaledHeight() - 22;
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, alpha);
        mc.getTextureManager().bindTexture(WIDGETS);
        Gui g = mc.ingameGUI;
        g.drawTexturedModalRect(x, y, 0, 0, 182, 22);
        if (selected >= 0) {
            g.drawTexturedModalRect(x - 1 + selected * 20, y - 1, 0, 22, 24, 22);
        }
        for (int i = 0; i < 9; i++) {
            Item it = slot(i);
            if (it == null) {
                continue;
            }
            int ix = x + 3 + i * 20, iy = y + 3;
            if (it.kind == 0) {
                head(mc, it.name, ix, iy, alpha);
            } else {
                int u = it.kind == 2 ? 0 : it.kind == 3 || it.kind == 1 ? 16 : it.kind == 4 ? 144 : it.kind == 5 ? 160 : 128;
                mc.getTextureManager().bindTexture(SPECTATOR);
                GL11.glColor4f(1.0F, 1.0F, 1.0F, alpha);
                g.drawTexturedModalRect(ix, iy, u, 0, 16, 16);
            }
        }
        if (selected >= 0 && slot(selected) != null) {
            String name = slot(selected).name;
            int a = (int) (alpha * 255.0F);
            if (a > 8) {
                mc.fontRenderer.drawStringWithShadow(name, res.getScaledWidth() / 2 - mc.fontRenderer.getStringWidth(name) / 2, y - 12, 0xFFFFFF | a << 24);
            }
        }
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glDisable(GL11.GL_BLEND);
    }

    /** PlayerMenuObject.renderIcon: the face of the skin, 8x8 scaled to 16, with the hat layer. */
    private static void head(Minecraft mc, String name, int x, int y, float alpha) {
        ResourceLocation skin = AbstractClientPlayer.locationStevePng;
        EntityPlayer p = mc.theWorld.getPlayerEntityByName(name);
        if (p instanceof AbstractClientPlayer) {
            skin = ((AbstractClientPlayer) p).getLocationSkin();
        } else {
            skin = AbstractClientPlayer.getLocationSkin(name);
            AbstractClientPlayer.getDownloadImageSkin(skin, name);
        }
        mc.getTextureManager().bindTexture(skin);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, alpha);
        Gui.func_152125_a(x, y, 8.0F, 8.0F, 8, 8, 16, 16, 64.0F, 32.0F);
        Gui.func_152125_a(x, y, 40.0F, 8.0F, 8, 8, 16, 16, 64.0F, 32.0F);
    }
}
