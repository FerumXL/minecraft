package ru.codex.offhand1710.client;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import org.lwjgl.opengl.GL11;
import ru.codex.offhand1710.Border112;

/**
 * The world border on the client: the animated wall of RenderGlobal.renderWorldBorder and the red tint of
 * GuiIngame.renderVignette of 1.12.2.
 */
public final class Border112Client {
    private static final ResourceLocation FORCEFIELD = new ResourceLocation("mc112bridge", "textures/misc/forcefield.png");

    public static final class Handler implements IMessageHandler<Border112.Msg, IMessage> {
        @Override
        public IMessage onMessage(Border112.Msg message, MessageContext ctx) {
            message.apply();
            return null;
        }
    }

    @SubscribeEvent
    public void onConnect(cpw.mods.fml.common.network.FMLNetworkEvent.ClientConnectedToServerEvent event) {
        Border112.client = new Border112();
    }

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent event) {
        if (Border112.OFF) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        EntityLivingBase e = mc.renderViewEntity;
        if (e == null || mc.theWorld == null) {
            return;
        }
        render(mc, e, event.partialTicks);
    }

    /** RenderGlobal.renderWorldBorder of 1.12.2. */
    static void render(Minecraft mc, EntityLivingBase e, float partial) {
        Border112 border = Border112.client;
        double[] b = border.box(mc.theWorld);
        double reach = mc.gameSettings.renderDistanceChunks * 16;
        if (e.posX < b[2] - reach && e.posX > b[0] + reach && e.posZ < b[3] - reach && e.posZ > b[1] + reach) {
            return;
        }
        double alpha = Math.pow(1.0D - Border112.distance(b, e.posX, e.posZ) / reach, 4.0D);
        double px = e.lastTickPosX + (e.posX - e.lastTickPosX) * partial;
        double py = e.lastTickPosY + (e.posY - e.lastTickPosY) * partial;
        double pz = e.lastTickPosZ + (e.posZ - e.lastTickPosZ) * partial;
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ZERO);
        mc.getTextureManager().bindTexture(FORCEFIELD);
        GL11.glDepthMask(false);
        GL11.glPushMatrix();
        int c = border.color();
        GL11.glColor4f((c >> 16 & 255) / 255.0F, (c >> 8 & 255) / 255.0F, (c & 255) / 255.0F, (float) alpha);
        GL11.glPolygonOffset(-3.0F, -3.0F);
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_LIGHTING);
        float shift = (float) (Minecraft.getSystemTime() % 3000L) / 3000.0F;
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        t.setTranslation(-px, -py, -pz);
        double from = Math.max(MathHelper.floor_double(pz - reach), b[1]);
        double to = Math.min(MathHelper.ceiling_double_int(pz + reach), b[3]);
        if (px > b[2] - reach) {
            float u = 0.0F;
            for (double z = from; z < to; u += 0.5F) {
                double len = Math.min(1.0D, to - z);
                float w = (float) len * 0.5F;
                t.addVertexWithUV(b[2], 256.0D, z, shift + u, shift);
                t.addVertexWithUV(b[2], 256.0D, z + len, shift + w + u, shift);
                t.addVertexWithUV(b[2], 0.0D, z + len, shift + w + u, shift + 128.0F);
                t.addVertexWithUV(b[2], 0.0D, z, shift + u, shift + 128.0F);
                z++;
            }
        }
        if (px < b[0] + reach) {
            float u = 0.0F;
            for (double z = from; z < to; u += 0.5F) {
                double len = Math.min(1.0D, to - z);
                float w = (float) len * 0.5F;
                t.addVertexWithUV(b[0], 256.0D, z, shift + w + u, shift);
                t.addVertexWithUV(b[0], 256.0D, z + len, shift + u, shift);
                t.addVertexWithUV(b[0], 0.0D, z + len, shift + u, shift + 128.0F);
                t.addVertexWithUV(b[0], 0.0D, z, shift + w + u, shift + 128.0F);
                z++;
            }
        }
        from = Math.max(MathHelper.floor_double(px - reach), b[0]);
        to = Math.min(MathHelper.ceiling_double_int(px + reach), b[2]);
        if (pz > b[3] - reach) {
            float u = 0.0F;
            for (double x = from; x < to; u += 0.5F) {
                double len = Math.min(1.0D, to - x);
                float w = (float) len * 0.5F;
                t.addVertexWithUV(x, 256.0D, b[3], shift + w + u, shift);
                t.addVertexWithUV(x + len, 256.0D, b[3], shift + u, shift);
                t.addVertexWithUV(x + len, 0.0D, b[3], shift + u, shift + 128.0F);
                t.addVertexWithUV(x, 0.0D, b[3], shift + w + u, shift + 128.0F);
                x++;
            }
        }
        if (pz < b[1] + reach) {
            float u = 0.0F;
            for (double x = from; x < to; u += 0.5F) {
                double len = Math.min(1.0D, to - x);
                float w = (float) len * 0.5F;
                t.addVertexWithUV(x, 256.0D, b[1], shift + u, shift);
                t.addVertexWithUV(x + len, 256.0D, b[1], shift + w + u, shift);
                t.addVertexWithUV(x + len, 0.0D, b[1], shift + w + u, shift + 128.0F);
                t.addVertexWithUV(x, 0.0D, b[1], shift + u, shift + 128.0F);
                x++;
            }
        }
        t.draw();
        t.setTranslation(0.0D, 0.0D, 0.0D);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glPolygonOffset(0.0F, 0.0F);
        GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glPopMatrix();
        GL11.glDepthMask(true);
    }

    /**
     * GuiIngame.renderVignette: the colour call goes through here. Near the border (warning distance, or the distance
     * a shrinking border covers in the warning time) the vignette turns red, as in 1.12.2.
     */
    public static void vignetteColor(float r, float g, float b, float a) {
        float tint = Border112.OFF ? 0.0F : tint();
        if (tint > 0.0F) {
            GL11.glColor4f(0.0F, tint, tint, 1.0F);
        } else {
            GL11.glColor4f(r, g, b, a);
        }
    }

    /** The share of red: 1 - distance / max(warning distance, the warning time's worth of shrinking). */
    public static float tint() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.renderViewEntity == null || mc.theWorld == null) {
            return 0.0F;
        }
        Border112 border = Border112.client;
        float f = (float) Border112.distance(border.box(mc.theWorld), mc.renderViewEntity.posX, mc.renderViewEntity.posZ);
        double shrink = Math.min(border.resizeSpeed() * border.warningTime * 1000.0D, Math.abs(border.endDiameter - border.diameter()));
        double warn = Math.max(border.warningDistance, shrink);
        return f < warn ? (float) (1.0D - f / warn) : 0.0F;
    }
}
