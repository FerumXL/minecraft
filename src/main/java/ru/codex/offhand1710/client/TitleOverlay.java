package ru.codex.offhand1710.client;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.opengl.GL11;
import ru.codex.offhand1710.Content121;

/** The title, subtitle and action bar text of GuiIngame in 1.8+. */
public final class TitleOverlay {
    private static String title = "";
    private static String subtitle = "";
    private static String actionbar = "";
    private static int fadeIn = 10;
    private static int stay = 70;
    private static int fadeOut = 20;
    private static int timer;
    private static int actionTimer;

    public static String currentTitle() {
        return timer > 0 ? title : "";
    }

    static void register() {
        TitleOverlay overlay = new TitleOverlay();
        MinecraftForge.EVENT_BUS.register(overlay);
        FMLCommonHandler.instance().bus().register(overlay);
    }

    public static final class Handler implements IMessageHandler<Content121.TitleMessage, IMessage> {
        @Override
        public IMessage onMessage(Content121.TitleMessage m, MessageContext context) {
            if ("title".equals(m.action)) {
                title = m.text;
                timer = fadeIn + stay + fadeOut;
            } else if ("subtitle".equals(m.action)) {
                subtitle = m.text;
            } else if ("actionbar".equals(m.action)) {
                actionbar = m.text;
                actionTimer = 60;
            } else if ("times".equals(m.action)) {
                fadeIn = m.fadeIn;
                stay = m.stay;
                fadeOut = m.fadeOut;
            } else if ("clear".equals(m.action)) {
                timer = 0;
            } else if ("reset".equals(m.action)) {
                timer = 0;
                title = "";
                subtitle = "";
                fadeIn = 10;
                stay = 70;
                fadeOut = 20;
            } else if ("stopsound".equals(m.action)) {
                Client122.stop(m.text);
            }
            return null;
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            if (timer > 0 && --timer == 0) {
                subtitle = "";
            }
            if (actionTimer > 0) {
                actionTimer--;
            }
        }
    }

    private static int alpha() {
        int elapsed = fadeIn + stay + fadeOut - timer;
        float value = 1.0F;
        if (fadeIn > 0 && elapsed < fadeIn) {
            value = (float) elapsed / (float) fadeIn;
        } else if (fadeOut > 0 && timer < fadeOut) {
            value = (float) timer / (float) fadeOut;
        }
        return Math.max(9, Math.min(255, (int) (value * 255.0F)));
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL || (timer <= 0 && actionTimer <= 0)) {
            return;
        }
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        int w = event.resolution.getScaledWidth();
        int h = event.resolution.getScaledHeight();
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        if (timer > 0) {
            int color = 0xFFFFFF | alpha() << 24;
            GL11.glPushMatrix();
            GL11.glTranslatef((float) w / 2.0F, (float) h / 2.0F, 0.0F);
            GL11.glPushMatrix();
            GL11.glScalef(4.0F, 4.0F, 4.0F);
            font.drawStringWithShadow(title, -font.getStringWidth(title) / 2, -10, color);
            GL11.glPopMatrix();
            GL11.glPushMatrix();
            GL11.glScalef(2.0F, 2.0F, 2.0F);
            font.drawStringWithShadow(subtitle, -font.getStringWidth(subtitle) / 2, 5, color);
            GL11.glPopMatrix();
            GL11.glPopMatrix();
        }
        if (actionTimer > 0) {
            font.drawStringWithShadow(actionbar, (w - font.getStringWidth(actionbar)) / 2, h - 68, 0xFFFFFF);
        }
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glPopMatrix();
    }
}
