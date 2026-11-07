package ru.codex.offhand1710.client;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.event.RenderHandEvent;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.util.glu.Project;

/**
 * The offhand item in first person, drawn like a real second hand.
 *
 * Forge fires RenderHandEvent in EntityRenderer.renderWorld right before vanilla clears the depth buffer and
 * calls renderHand. Here the same setup as renderHand is rebuilt (projection with the vanilla hand FOV,
 * camera hurt shake, view bobbing, lightmap), then the vanilla right-hand pose of
 * ItemRenderer.renderItemInFirstPerson is drawn mirrored to the left: arm lag behind the camera, swing,
 * raise/lower when the item changes, world brightness. Like the left hand of 1.12.2 the whole pose including
 * the item is mirrored; the front face winding is flipped meanwhile so models are not drawn inside out.
 * Vanilla then draws the main hand as usual.
 */
public final class OffhandFirstPersonRenderer {
    private static final float PI = (float) Math.PI;

    private static boolean broken;
    private static Method getFOVModifier;
    private static Method hurtCameraEffect;
    private static Method setupViewBobbing;
    private static Field farPlaneDistance;
    private static Field cameraZoom;

    private static void lookUp() {
        if (getFOVModifier != null) {
            return;
        }
        getFOVModifier = ReflectionHelper.findMethod(EntityRenderer.class, null,
                new String[] {"func_78481_a", "getFOVModifier"}, float.class, boolean.class);
        hurtCameraEffect = ReflectionHelper.findMethod(EntityRenderer.class, null,
                new String[] {"func_78482_e", "hurtCameraEffect"}, float.class);
        setupViewBobbing = ReflectionHelper.findMethod(EntityRenderer.class, null,
                new String[] {"func_78475_f", "setupViewBobbing"}, float.class);
        farPlaneDistance = ReflectionHelper.findField(EntityRenderer.class, "field_78530_s", "farPlaneDistance");
        cameraZoom = ReflectionHelper.findField(EntityRenderer.class, "field_78503_V", "cameraZoom");
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRenderHand(RenderHandEvent event) {
        if (event.isCanceled() || broken) {
            return; // another mod replaced the hands
        }
        Minecraft mc = Minecraft.getMinecraft();
        EntityClientPlayerMP player = mc.thePlayer;
        ItemStack stack = OffhandClientState.renderedStack();
        if (player == null || stack == null || stack.getItem() == null || mc.renderViewEntity != player
                || mc.gameSettings.thirdPersonView != 0 || mc.gameSettings.hideGUI || player.isPlayerSleeping()
                || mc.playerController.enableEverythingIsScrewedUpMode()) {
            return;
        }
        EntityRenderer renderer = mc.entityRenderer;
        float partialTicks = event.partialTicks;
        float fov;
        float far;
        try {
            lookUp();
            if (cameraZoom.getDouble(renderer) != 1.0D) {
                return; // vanilla draws no hand while zoomed for a large screenshot
            }
            fov = ((Float) getFOVModifier.invoke(renderer, partialTicks, false)).floatValue();
            far = farPlaneDistance.getFloat(renderer);
        } catch (Exception e) {
            broken = true;
            FMLLog.severe("[mc112bridge] first-person offhand disabled: %s", e);
            return;
        }

        // EntityRenderer.renderHand, projection part.
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        if (mc.gameSettings.anaglyph) {
            GL11.glTranslatef((float) (-(event.renderPass * 2 - 1)) * 0.07F, 0.0F, 0.0F);
        }
        Project.gluPerspective(fov, (float) mc.displayWidth / (float) mc.displayHeight, 0.05F, far * 2.0F);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        try {
            GL11.glLoadIdentity();
            if (mc.gameSettings.anaglyph) {
                GL11.glTranslatef((float) (event.renderPass * 2 - 1) * 0.1F, 0.0F, 0.0F);
            }
            // Vanilla clears depth before its hand too; the offhand must not be hidden by nearby blocks.
            GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
            hurtCameraEffect.invoke(renderer, partialTicks);
            if (mc.gameSettings.viewBobbing) {
                setupViewBobbing.invoke(renderer, partialTicks);
            }
            renderer.enableLightmap((double) partialTicks);
            try {
                renderOffhand(mc, player, stack, partialTicks);
            } finally {
                renderer.disableLightmap((double) partialTicks);
            }
        } catch (Exception e) {
            broken = true;
            FMLLog.severe("[mc112bridge] first-person offhand disabled: %s", e);
        } finally {
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
        }
    }

    /** ItemRenderer.renderItemInFirstPerson (1.7.10, item not in use), mirrored to the left hand. */
    private static void renderOffhand(Minecraft mc, EntityClientPlayerMP player, ItemStack stack, float partialTicks) {
        float equip = OffhandClientState.equipped(partialTicks);
        float swing = OffhandClientState.swing(partialTicks);

        float pitch = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * partialTicks;
        GL11.glPushMatrix();
        GL11.glRotatef(pitch, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * partialTicks,
                0.0F, 1.0F, 0.0F);
        RenderHelper.enableStandardItemLighting();
        GL11.glPopMatrix();

        GL11.glPushMatrix();
        try {
            // The arm lags slightly behind fast camera turns.
            float armPitch = player.prevRenderArmPitch + (player.renderArmPitch - player.prevRenderArmPitch) * partialTicks;
            float armYaw = player.prevRenderArmYaw + (player.renderArmYaw - player.prevRenderArmYaw) * partialTicks;
            GL11.glRotatef((player.rotationPitch - armPitch) * 0.1F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef((player.rotationYaw - armYaw) * 0.1F, 0.0F, 1.0F, 0.0F);

            int light = mc.theWorld.getLightBrightnessForSkyBlocks(MathHelper.floor_double(player.posX),
                    MathHelper.floor_double(player.posY), MathHelper.floor_double(player.posZ), 0);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,
                    (float) (light % 65536), (float) (light / 65536));
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

            GL11.glScalef(-1.0F, 1.0F, 1.0F);
            GL11.glFrontFace(GL11.GL_CW);

            float sqrtSwing = MathHelper.sqrt_float(swing);
            GL11.glTranslatef(-MathHelper.sin(sqrtSwing * PI) * 0.4F, MathHelper.sin(sqrtSwing * PI * 2.0F) * 0.2F,
                    -MathHelper.sin(swing * PI) * 0.2F);
            GL11.glTranslatef(0.7F * 0.8F, -0.65F * 0.8F - (1.0F - equip) * 0.6F, -0.9F * 0.8F);
            GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
            GL11.glEnable(GL12.GL_RESCALE_NORMAL);
            float swingSq = MathHelper.sin(swing * swing * PI);
            float swingRoot = MathHelper.sin(sqrtSwing * PI);
            GL11.glRotatef(-swingSq * 20.0F, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(-swingRoot * 20.0F, 0.0F, 0.0F, 1.0F);
            GL11.glRotatef(-swingRoot * 80.0F, 1.0F, 0.0F, 0.0F);
            GL11.glScalef(0.4F, 0.4F, 0.4F);
            Item item = stack.getItem();
            if (item.shouldRotateAroundWhenRendering()) {
                GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
            }

            if (item.requiresMultipleRenderPasses()) {
                for (int pass = 0; pass < item.getRenderPasses(stack.getItemDamage()); pass++) {
                    renderPass(mc, player, stack, pass);
                }
            } else {
                renderPass(mc, player, stack, 0);
            }
        } finally {
            GL11.glFrontFace(GL11.GL_CCW);
            GL11.glPopMatrix();
            GL11.glDisable(GL12.GL_RESCALE_NORMAL);
            RenderHelper.disableStandardItemLighting();
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private static void renderPass(Minecraft mc, EntityClientPlayerMP player, ItemStack stack, int pass) {
        int color = stack.getItem().getColorFromItemStack(stack, pass);
        GL11.glColor4f((float) (color >> 16 & 255) / 255.0F, (float) (color >> 8 & 255) / 255.0F,
                (float) (color & 255) / 255.0F, 1.0F);
        mc.entityRenderer.itemRenderer.renderItem(player, stack, pass, ItemRenderType.EQUIPPED_FIRST_PERSON);
    }
}
