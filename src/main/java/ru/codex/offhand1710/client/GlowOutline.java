package ru.codex.offhand1710.client;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import ru.codex.offhand1710.Maps112;

/**
 * The outline of Glowing. 1.12 runs the post chain shaders/post/entity_outline.json: entity_sobel (a two pixel edge,
 * colour = a fifth of the five samples of a cross), then a blur of radius 2 across and down whose alpha is the plain
 * sum of the samples, then a blit. The same arithmetic is done here with blending instead of shaders: the band is three
 * pixels outside the silhouette and three inside, white over the entity and fading to black outwards.
 */
public final class GlowOutline {
    private static final int[][] CROSS = {{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    private static final int[][] ROW = {{-2, 0}, {-1, 0}, {0, 0}, {1, 0}, {2, 0}};
    private static final int[][] COLUMN = {{0, -2}, {0, -1}, {0, 0}, {0, 1}, {0, 2}};

    /** The entities as they are drawn, only alpha matters; later reused as a scratch buffer. */
    private Framebuffer raw;
    /** The silhouette: white and opaque inside, nothing outside. */
    private Framebuffer mask;
    private Framebuffer a;
    private Framebuffer b;
    public static int lastDrawn;
    /** True while the silhouettes are drawn: name tags must not get into them. */
    private static boolean drawingMask;
    private static GlowOutline instance;
    /** Set by the first call of the coremod hook; without the hook the outline stays in RenderWorldLast. */
    private static boolean afterWorldSeen;
    /** The camera of the world pass: the hand pass replaces both matrices and does not put them back. */
    private final FloatBuffer projection = BufferUtils.createFloatBuffer(16);
    private final FloatBuffer modelview = BufferUtils.createFloatBuffer(16);
    private final List<Entity> glowing = new ArrayList<Entity>();
    private float partial;
    private boolean pending;

    private GlowOutline() {
    }

    static void register() {
        instance = new GlowOutline();
        MinecraftForge.EVENT_BUS.register(instance);
    }

    private static Framebuffer ensure(Framebuffer fb, int w, int h, boolean depth) {
        if (fb == null) {
            fb = new Framebuffer(w, h, depth);
            fb.setFramebufferFilter(GL11.GL_NEAREST);
            return fb;
        }
        if (fb.framebufferWidth != w || fb.framebufferHeight != h) {
            fb.createBindFramebuffer(w, h);
            fb.setFramebufferFilter(GL11.GL_NEAREST);
        }
        return fb;
    }

    private static void quad(int w, int h, double dx, double dy) {
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        t.addVertexWithUV(dx, h + dy, 0.0D, 0.0D, 0.0D);
        t.addVertexWithUV(w + dx, h + dy, 0.0D, 1.0D, 0.0D);
        t.addVertexWithUV(w + dx, dy, 0.0D, 1.0D, 1.0D);
        t.addVertexWithUV(dx, dy, 0.0D, 0.0D, 1.0D);
        t.draw();
    }

    /** Clears the target to the given colour, binds it and draws the source once per offset. */
    private static void pass(Framebuffer target, Framebuffer source, int[][] offsets, float r, float g, float bl, float al, int w, int h) {
        target.setFramebufferColor(r, g, bl, al);
        target.framebufferClear();
        target.bindFramebuffer(true);
        source.bindFramebufferTexture();
        for (int i = 0; i < offsets.length; i++) {
            quad(w, h, offsets[i][0], offsets[i][1]);
        }
    }

    @SubscribeEvent
    public void onSpecials(RenderLivingEvent.Specials.Pre event) {
        if (drawingMask && event.isCancelable()) {
            event.setCanceled(true);
        }
    }

    /**
     * Whether a renderer can be called now. Render.bindTexture goes through its RenderManager to the texture
     * manager, and in the first frame in a world, or for a renderer put into the map by hand, some of that is not
     * there yet (a NullPointerException seen once on entering a world).
     */
    private static boolean ready(Render render) {
        RenderManager manager = RenderManager.instance;
        if (manager.renderEngine == null || manager.worldObj == null || manager.livingPlayer == null) {
            return false;
        }
        Boolean known = READY.get(render);
        if (known == null) {
            Object own;
            try {
                own = ReflectionHelper.getPrivateValue(Render.class, render, "renderManager", "field_76990_c");
            } catch (RuntimeException ex) {
                own = manager;
            }
            known = Boolean.valueOf(own != null);
            READY.put(render, known);
        }
        return known.booleanValue();
    }

    private static final Map<Render, Boolean> READY = new WeakHashMap<Render, Boolean>();

    /**
     * Coremod hook right after EntityRenderer.renderWorld, hand included: where 1.12 calls
     * renderEntityOutlineFramebuffer, so the outline lies over the hand and under the screen shader.
     */
    public static void afterWorld() {
        afterWorldSeen = true;
        if (instance != null && instance.pending) {
            instance.pending = false;
            instance.draw();
        }
    }

    /** Only notes what is to be drawn and with which camera. */
    @SubscribeEvent
    public void onWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        lastDrawn = 0;
        pending = false;
        if (!OpenGlHelper.isFramebufferEnabled() || mc.theWorld == null) {
            return;
        }
        glowing.clear();
        for (Object o : mc.theWorld.loadedEntityList) {
            Entity e = (Entity) o;
            if (e instanceof EntityLivingBase && Maps112.isGlowing(e) && !(e == mc.renderViewEntity && mc.gameSettings.thirdPersonView == 0)) {
                glowing.add(e);
            }
        }
        if (glowing.isEmpty()) {
            return;
        }
        partial = event.partialTicks;
        projection.clear();
        modelview.clear();
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projection);
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelview);
        if (afterWorldSeen) {
            pending = true;
        } else {
            draw();
        }
    }

    private void draw() {
        Minecraft mc = Minecraft.getMinecraft();
        int w = mc.displayWidth;
        int h = mc.displayHeight;
        raw = ensure(raw, w, h, true);
        mask = ensure(mask, w, h, false);
        a = ensure(a, w, h, false);
        b = ensure(b, w, h, false);
        float pt = partial;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        projection.rewind();
        GL11.glLoadMatrix(projection);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        modelview.rewind();
        GL11.glLoadMatrix(modelview);
        try {
            // What the hand pass may have left behind.
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glFrontFace(GL11.GL_CCW);
            // 1. The entities themselves, textures on so that cut-out parts stay cut out. Only alpha is written.
            //    The renderer is called directly: RenderManager would add the shadow and the fire, and both would
            //    become part of the silhouette.
            raw.setFramebufferColor(1.0F, 1.0F, 1.0F, 0.0F);
            raw.framebufferClear();
            raw.bindFramebuffer(true);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(false);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_FOG);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
            GL11.glColorMask(false, false, false, true);
            drawingMask = true;
            try {
                for (Entity e : glowing) {
                    Render render = RenderManager.instance.getEntityRenderObject(e);
                    if (render == null || !ready(render) || e.worldObj == null || e.isDead) {
                        continue;
                    }
                    int depth = GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH);
                    double x = e.lastTickPosX + (e.posX - e.lastTickPosX) * pt;
                    double y = e.lastTickPosY + (e.posY - e.lastTickPosY) * pt;
                    double z = e.lastTickPosZ + (e.posZ - e.lastTickPosZ) * pt;
                    float yaw = e.prevRotationYaw + (e.rotationYaw - e.prevRotationYaw) * pt;
                    GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                    GL11.glPushMatrix();
                    try {
                        render.doRender(e, x - RenderManager.renderPosX, y - RenderManager.renderPosY, z - RenderManager.renderPosZ, yaw, pt);
                    } catch (RuntimeException ex) {
                        // a foreign renderer that cannot be drawn out of turn: no outline for it, and the matrices
                        // it pushed before failing come off again
                        while (GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH) > depth + 1) {
                            GL11.glPopMatrix();
                        }
                    }
                    GL11.glPopMatrix();
                    GL11.glDisable(GL11.GL_DEPTH_TEST);
                    GL11.glDepthMask(false);
                    GL11.glDisable(GL11.GL_LIGHTING);
                    GL11.glColorMask(false, false, false, true);
                    lastDrawn++;
                }
            } finally {
                drawingMask = false;
            }
            GL11.glColorMask(true, true, true, true);
            OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GL11.glEnable(GL11.GL_TEXTURE_2D);

            // 2. Screen space from here on.
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPushMatrix();
            GL11.glLoadIdentity();
            GL11.glOrtho(0.0D, w, h, 0.0D, -1.0D, 1.0D);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPushMatrix();
            GL11.glLoadIdentity();
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_BLEND);

            // 2a. A hard silhouette: whatever passed the alpha test becomes white and fully opaque (a hurt flash or
            //     a translucent layer leaves alpha between 0 and 1).
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.3F);
            GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            pass(mask, raw, new int[][] {{0, 0}, {0, 0}, {0, 0}, {0, 0}}, 0.0F, 0.0F, 0.0F, 0.0F, w, h);
            GL11.glDisable(GL11.GL_ALPHA_TEST);

            // 2b. entity_sobel: colour = 0.2 * the five samples of the cross; alpha here = "any of the five is
            //     inside", which is the outer edge of the sobel band.
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            GL11.glColor4f(0.2F, 0.2F, 0.2F, 1.0F);
            pass(a, mask, CROSS, 0.0F, 0.0F, 0.0F, 0.0F, w, h);
            // 2c. blur, radius 2, across and then down: colour is the mean of five, alpha is their sum.
            GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
            pass(b, a, ROW, 0.0F, 0.0F, 0.0F, 0.0F, w, h);
            pass(a, b, COLUMN, 0.0F, 0.0F, 0.0F, 0.0F, w, h);

            // 2d. The inner edge of the band: the silhouette shrunk by the same cross and the same two blurs
            //     ("all of the samples are inside"). Each quad multiplies what is there by the alpha of the source.
            GL11.glBlendFunc(GL11.GL_ZERO, GL11.GL_SRC_ALPHA);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            pass(b, mask, CROSS, 1.0F, 1.0F, 1.0F, 1.0F, w, h);
            pass(raw, b, ROW, 1.0F, 1.0F, 1.0F, 1.0F, w, h);
            pass(b, raw, COLUMN, 1.0F, 1.0F, 1.0F, 1.0F, w, h);

            // 2e. Cut the inside out of the widened silhouette: alpha *= 1 - inner.
            a.bindFramebuffer(true);
            b.bindFramebufferTexture();
            GL11.glColorMask(false, false, false, true);
            GL11.glBlendFunc(GL11.GL_ZERO, GL11.GL_ONE_MINUS_SRC_ALPHA);
            quad(w, h, 0.0D, 0.0D);
            GL11.glColorMask(true, true, true, true);

            // 3. Over the scene, as the final blit of 1.12 does (srcalpha, 1-srcalpha); the alpha of the picture
            //    itself is left alone.
            mc.getFramebuffer().bindFramebuffer(true);
            a.bindFramebufferTexture();
            OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ZERO, GL11.GL_ONE);
            quad(w, h, 0.0D, 0.0D);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();
        } finally {
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();
            mc.getFramebuffer().bindFramebuffer(true);
            GL11.glPopAttrib();
        }
    }
}
