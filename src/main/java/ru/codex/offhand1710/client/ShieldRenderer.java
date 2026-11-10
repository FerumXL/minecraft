package ru.codex.offhand1710.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/**
 * Draws the shield with the 1.12.2 pipeline: the hand transform of LayerHeldItem or
 * ItemRenderer.renderItemInFirstPerson, the display transforms of models/item/shield.json and
 * shield_blocking.json (ItemTransformVec3f.apply, mirrored for the left hand), the -0.5 offset of builtin
 * models and TileEntityItemStackRenderer's scale(1, -1, -1) before ModelShield.
 *
 * For the main hand Forge has already applied the 1.7.10 held item transforms when it calls this renderer;
 * they are undone first so the shield sits where 1.12.2 puts it. The offhand renderers call the static
 * methods directly from the arm.
 */
public final class ShieldRenderer implements IItemRenderer {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("mc112bridge", "textures/entity/shield_base_nopattern.png");
    private static final ModelShield MODEL = new ModelShield();
    private static final float PI = (float) Math.PI;

    // Display transforms: translation x, y, z (pixels), rotation x, y, z (degrees), scale x, y, z.
    private static final float[] THIRD_RIGHT = {10.51F, 6, -4, 0, 90, 0, 1, 1, 1};
    private static final float[] THIRD_LEFT = {10.51F, 6, 12, 0, 90, 0, 1, 1, 1};
    private static final float[] FIRST_RIGHT = {-10, 2, -10, 0, 180, 5, 1.25F, 1.25F, 1.25F};
    private static final float[] FIRST_LEFT = {10, 0, -10, 0, 180, 5, 1.25F, 1.25F, 1.25F};
    private static final float[] BLOCKING_THIRD_RIGHT = {3.51F, 11, -2, 45, 135, 0, 1, 1, 1};
    private static final float[] BLOCKING_THIRD_LEFT = {13.51F, 3, 5, 45, 135, 0, 1, 1, 1};
    private static final float[] BLOCKING_FIRST_RIGHT = {-15, 5, -11, 0, 180, -5, 1.25F, 1.25F, 1.25F};
    private static final float[] BLOCKING_FIRST_LEFT = {5, 5, -11, 0, 180, -5, 1.25F, 1.25F, 1.25F};
    private static final float[] GUI = {2, 3, 0, 15, -25, -5, 0.65F, 0.65F, 0.65F};

    /**
     * ItemCameraTransforms.applyTransformSide (1.12.2): translation with x mirrored for the left hand, then
     * the rotation as one quaternion built from the angles in X, Y, Z order (y and z negated for the left
     * hand), then the scale. The quaternion equals rotating about X, then Y, then Z.
     */
    static void applyDisplay(float[] t, boolean left) {
        float side = left ? -1.0F : 1.0F;
        GL11.glTranslatef(side * t[0] / 16.0F, t[1] / 16.0F, t[2] / 16.0F);
        GL11.glRotatef(t[3], 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(t[4] * side, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(t[5] * side, 0.0F, 0.0F, 1.0F);
        GL11.glScalef(t[6], t[7], t[8]);
    }

    private static ItemStack stack;

    /** The stack whose banner pattern the next draw uses; the offhand renderers set it before drawing. */
    static void setStack(ItemStack value) {
        stack = value;
    }

    private static ResourceLocation texture(ItemStack item) {
        if (item != null && item.hasTagCompound() && item.getTagCompound().hasKey("BlockEntityTag")) {
            net.minecraft.nbt.NBTTagCompound tag = item.getTagCompound().getCompoundTag("BlockEntityTag");
            return Banner112Client.layered("textures/entity/shield_base.png", "textures/entity/shield/", tag.getInteger("Base"), tag.getTagList("Patterns", 10));
        }
        return TEXTURE;
    }

    /** The builtin model offset and TileEntityItemStackRenderer for the shield. */
    private static void renderModel() {
        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture(stack));
        GL11.glPushMatrix();
        GL11.glScalef(1.0F, -1.0F, -1.0F);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        MODEL.render();
        GL11.glPopMatrix();
    }

    /** LayerHeldItem.renderHeldItem from the arm (after ModelRenderer.postRender of that arm). */
    public static void renderThirdPerson(boolean left, boolean blocking) {
        GL11.glPushMatrix();
        GL11.glRotatef(-90.0F, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
        GL11.glTranslatef((left ? -1.0F : 1.0F) / 16.0F, 0.125F, -0.625F);
        applyDisplay(blocking ? (left ? BLOCKING_THIRD_LEFT : BLOCKING_THIRD_RIGHT) : (left ? THIRD_LEFT : THIRD_RIGHT), left);
        renderModel();
        GL11.glPopMatrix();
    }

    /**
     * ItemRenderer.renderItemInFirstPerson (1.12.2) for the left hand, from the camera space after the arm
     * lag and lighting setup: swing offset, side position with the equip progress, swing rotations (none
     * while blocking), then the first person display transform.
     */
    public static void renderFirstPersonLeft(float equip, float swing, boolean blocking) {
        GL11.glPushMatrix();
        float side = -1.0F;
        float sqrtSwing = MathHelper.sqrt_float(swing);
        if (!blocking) {
            GL11.glTranslatef(side * -0.4F * MathHelper.sin(sqrtSwing * PI), 0.2F * MathHelper.sin(sqrtSwing * PI * 2.0F),
                    -0.2F * MathHelper.sin(swing * PI));
        }
        GL11.glTranslatef(side * 0.56F, -0.52F + (1.0F - equip) * -0.6F, -0.72F);
        if (!blocking) {
            float swingSq = MathHelper.sin(swing * swing * PI);
            float swingRoot = MathHelper.sin(sqrtSwing * PI);
            GL11.glRotatef(side * (45.0F + swingSq * -20.0F), 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(side * swingRoot * -20.0F, 0.0F, 0.0F, 1.0F);
            GL11.glRotatef(swingRoot * -80.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(side * -45.0F, 0.0F, 1.0F, 0.0F);
        }
        applyDisplay(blocking ? BLOCKING_FIRST_LEFT : FIRST_LEFT, true);
        renderModel();
        GL11.glPopMatrix();
    }

    private static boolean isBlocking(Object[] data, ItemStack stack) {
        if (data.length > 1 && data[1] instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) data[1];
            return player.isUsingItem() && player.getItemInUse() == stack;
        }
        return false;
    }

    @Override
    public boolean handleRenderType(ItemStack stack, ItemRenderType type) {
        return type == ItemRenderType.EQUIPPED || type == ItemRenderType.EQUIPPED_FIRST_PERSON
                || type == ItemRenderType.INVENTORY || type == ItemRenderType.ENTITY;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack stack, ItemRendererHelper helper) {
        switch (helper) {
            case EQUIPPED_BLOCK:
            case ENTITY_ROTATION:
            case ENTITY_BOBBING:
                return true;
            case BLOCK_3D:
                return type == ItemRenderType.ENTITY; // in RenderPlayer this would pick the block hand transform
            default:
                return false;
        }
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack stack, Object... data) {
        setStack(stack);
        switch (type) {
            case EQUIPPED:
                // Undo RenderPlayer.renderEquippedItems (plain item branch) and the EQUIPPED_BLOCK offset,
                // back to the right arm after postRender.
                GL11.glPushMatrix();
                GL11.glTranslatef(0.5F, 0.5F, 0.5F);
                GL11.glRotatef(-20.0F, 0.0F, 0.0F, 1.0F);
                GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
                GL11.glRotatef(-60.0F, 0.0F, 0.0F, 1.0F);
                GL11.glScalef(1.0F / 0.375F, 1.0F / 0.375F, 1.0F / 0.375F);
                GL11.glTranslatef(-0.25F, -0.1875F, 0.1875F);
                GL11.glTranslatef(0.0625F, -0.4375F, -0.0625F);
                renderThirdPerson(false, isBlocking(data, stack));
                GL11.glPopMatrix();
                break;
            case EQUIPPED_FIRST_PERSON:
                // Undo the 1.7.10 first person scale, the 45 degree turn and, while in use, the block action
                // transform; what stays matches the 1.12.2 side and swing transforms of the right hand.
                GL11.glPushMatrix();
                GL11.glTranslatef(0.5F, 0.5F, 0.5F);
                boolean blocking = isBlocking(data, stack);
                if (blocking) {
                    GL11.glRotatef(-60.0F, 0.0F, 1.0F, 0.0F);
                    GL11.glRotatef(80.0F, 1.0F, 0.0F, 0.0F);
                    GL11.glRotatef(-30.0F, 0.0F, 1.0F, 0.0F);
                    GL11.glTranslatef(0.5F, -0.2F, 0.0F);
                }
                GL11.glScalef(2.5F, 2.5F, 2.5F);
                GL11.glRotatef(-45.0F, 0.0F, 1.0F, 0.0F);
                applyDisplay(blocking ? BLOCKING_FIRST_RIGHT : FIRST_RIGHT, false);
                renderModel();
                GL11.glPopMatrix();
                break;
            case INVENTORY:
                // RenderItem.setupGuiTransform of 1.12.2 from the slot corner, then the gui display transform.
                renderInventory(GUI, new Runnable() {
                    @Override
                    public void run() {
                        renderModel();
                    }
                });
                break;
            case ENTITY:
                // Forge has rotated, bobbed and scaled by 0.25, which is also the scale of the ground transform.
                GL11.glPushMatrix();
                GL11.glTranslatef(0.0F, 0.25F, 0.0F);
                GL11.glTranslatef(4.0F / 16.0F / 0.25F, 4.0F / 16.0F / 0.25F, 2.0F / 16.0F / 0.25F);
                renderModel();
                GL11.glPopMatrix();
                break;
            default:
                break;
        }
    }

    /**
     * RenderItem.setupGuiTransform of 1.12.2 from the slot corner, then the gui display transform.
     *
     * Screens leave different OpenGL state behind (the hotbar and the container screens differ, and mods
     * such as OptiFine change more), so everything the model depends on is set here and restored afterwards:
     * the depth test (the shield is not convex, without it the handle shows through the plate), no face
     * culling (the GUI flip makes the matrix mirrored), item lighting, no lightmap, fog or blending.
     */
    static void renderInventory(float[] display, Runnable body) {
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_LIGHTING_BIT | GL11.GL_DEPTH_BUFFER_BIT
                | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT | GL11.GL_POLYGON_BIT);
        OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        boolean lightmap = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_FOG);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glDepthMask(true);
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_LIGHTING);
        RenderHelper.enableGUIStandardItemLighting();
        GL11.glTranslatef(8.0F, 8.0F, 0.0F);
        GL11.glScalef(1.0F, -1.0F, 1.0F);
        GL11.glScalef(16.0F, 16.0F, 16.0F);
        applyDisplay(display, false);
        body.run();
        GL11.glPopMatrix();
        GL11.glPopAttrib();
        if (lightmap) {
            OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        }
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** True while this player holds the shield raised in the offhand (for the offhand renderers). */
    static boolean isOffhandBlocking(EntityLivingBase entity, ItemStack stack) {
        if (!(entity instanceof EntityPlayer)) {
            return false;
        }
        ru.codex.offhand1710.OffhandInventory offhand = ru.codex.offhand1710.OffhandInventory.get((EntityPlayer) entity);
        return offhand.isUsing() && offhand.getStack() == stack;
    }
}
