package ru.codex.offhand1710.client;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.event.RenderItemInFrameEvent;
import org.lwjgl.opengl.GL11;

import ru.codex.offhand1710.Cooldowns112;
import ru.codex.offhand1710.Frames112;

/** The client half of Cooldowns112 (the white bar) and of Frames112 (the 45-degree steps). */
public final class Cooldowns112Client {
    /** Partial ticks of the frame being drawn, for a smooth bar. */
    private static java.lang.reflect.Field timer;

    private static float partial() {
        try {
            if (timer == null) {
                timer = cpw.mods.fml.relauncher.ReflectionHelper.findField(Minecraft.class, "timer", "field_71428_T");
            }
            return ((net.minecraft.util.Timer) timer.get(Minecraft.getMinecraft())).renderPartialTicks;
        } catch (Exception e) {
            return 0.0F;
        }
    }

    /** RenderItem.renderItemOverlayIntoGUI of 1.12.2, the end: the cooldown drawn over the slot from the bottom up. */
    public static void overlay(ItemStack stack, int x, int y) {
        Minecraft mc = Minecraft.getMinecraft();
        if (stack == null || mc.thePlayer == null || Cooldowns112.OFF) {
            return;
        }
        float f = Cooldowns112.get(mc.thePlayer, stack.getItem(), partial());
        if (f <= 0.0F) {
            return;
        }
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        int top = y + MathHelper.floor_float(16.0F * (1.0F - f));
        net.minecraft.client.gui.Gui.drawRect(x, top, x + 16, top + MathHelper.ceiling_float_int(16.0F * f), 0x7FFFFFFF);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    /** RenderItemFrame of 1.12.2 turns an item by 45 per rotation: the odd step about the centre 1.7.10 keeps the item on. */
    @SubscribeEvent
    public void onFrame(RenderItemInFrameEvent event) {
        EntityItemFrame frame = event.entityItemFrame;
        if (!Frames112.OFF && Frames112.halfStep(frame)) {
            GL11.glTranslatef(0.0F, 0.16F, 0.0F);
            GL11.glRotatef(-45.0F, 0.0F, 0.0F, 1.0F);
            GL11.glTranslatef(0.0F, -0.16F, 0.0F);
        }
    }

    public static final class Handler implements IMessageHandler<Cooldowns112.Msg, IMessage> {
        @Override
        public IMessage onMessage(Cooldowns112.Msg message, MessageContext ctx) {
            Minecraft mc = Minecraft.getMinecraft();
            Item item = Item.getItemById(message.item);
            if (mc.thePlayer != null && item != null) {
                Cooldowns112.set(mc.thePlayer, item, message.ticks);
            }
            return null;
        }
    }
}
