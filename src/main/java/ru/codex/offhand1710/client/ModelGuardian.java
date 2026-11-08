package ru.codex.offhand1710.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import ru.codex.offhand1710.EntityGuardian;

/** ModelGuardian of 1.12.2. */
public final class ModelGuardian extends ModelBase {
    private static final float PI = (float) Math.PI;
    private static final float[] ROT_X = {1.75F, 0.25F, 0.0F, 0.0F, 0.5F, 0.5F, 0.5F, 0.5F, 1.25F, 0.75F, 0.0F, 0.0F};
    private static final float[] ROT_Y = {0.0F, 0.0F, 0.0F, 0.0F, 0.25F, 1.75F, 1.25F, 0.75F, 0.0F, 0.0F, 0.0F, 0.0F};
    private static final float[] ROT_Z = {0.0F, 0.0F, 0.25F, 1.75F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.75F, 1.25F};
    private static final float[] POS_X = {0.0F, 0.0F, 8.0F, -8.0F, -8.0F, 8.0F, 8.0F, -8.0F, 0.0F, 0.0F, 8.0F, -8.0F};
    private static final float[] POS_Y = {-8.0F, -8.0F, -8.0F, -8.0F, 0.0F, 0.0F, 0.0F, 0.0F, 8.0F, 8.0F, 8.0F, 8.0F};
    private static final float[] POS_Z = {8.0F, -8.0F, 0.0F, 0.0F, -8.0F, -8.0F, 8.0F, 8.0F, 8.0F, -8.0F, 0.0F, 0.0F};

    private final ModelRenderer body;
    private final ModelRenderer eye;
    private final ModelRenderer[] spines = new ModelRenderer[12];
    private final ModelRenderer[] tail = new ModelRenderer[3];

    public ModelGuardian() {
        textureWidth = 64;
        textureHeight = 64;
        body = new ModelRenderer(this);
        body.setTextureOffset(0, 0).addBox(-6.0F, 10.0F, -8.0F, 12, 12, 16);
        body.setTextureOffset(0, 28).addBox(-8.0F, 10.0F, -6.0F, 2, 12, 12);
        body.mirror = true;
        body.setTextureOffset(0, 28).addBox(6.0F, 10.0F, -6.0F, 2, 12, 12);
        body.mirror = false;
        body.setTextureOffset(16, 40).addBox(-6.0F, 8.0F, -6.0F, 12, 2, 12);
        body.setTextureOffset(16, 40).addBox(-6.0F, 22.0F, -6.0F, 12, 2, 12);
        for (int i = 0; i < spines.length; i++) {
            spines[i] = new ModelRenderer(this, 0, 0);
            spines[i].addBox(-1.0F, -4.5F, -1.0F, 2, 9, 2);
            body.addChild(spines[i]);
        }
        eye = new ModelRenderer(this, 8, 0);
        eye.addBox(-1.0F, 15.0F, 0.0F, 2, 2, 1);
        body.addChild(eye);
        tail[0] = new ModelRenderer(this, 40, 0);
        tail[0].addBox(-2.0F, 14.0F, 7.0F, 4, 4, 8);
        tail[1] = new ModelRenderer(this, 0, 54);
        tail[1].addBox(0.0F, 14.0F, 0.0F, 3, 3, 7);
        tail[2] = new ModelRenderer(this);
        tail[2].setTextureOffset(41, 32).addBox(0.0F, 14.0F, 0.0F, 2, 2, 6);
        tail[2].setTextureOffset(25, 19).addBox(1.0F, 10.5F, 3.0F, 1, 9, 9);
        body.addChild(tail[0]);
        tail[0].addChild(tail[1]);
        tail[1].addChild(tail[2]);
    }

    @Override
    public void render(Entity entity, float limb, float amount, float age, float yaw, float pitch, float scale) {
        setRotationAngles(limb, amount, age, yaw, pitch, scale, entity);
        body.render(scale);
    }

    @Override
    public void setRotationAngles(float limb, float amount, float age, float yaw, float pitch, float scale, Entity entity) {
        EntityGuardian guardian = (EntityGuardian) entity;
        float partial = age - (float) guardian.ticksExisted;
        body.rotateAngleY = yaw * 0.017453292F;
        body.rotateAngleX = pitch * 0.017453292F;
        float retract = (1.0F - guardian.spikes(partial)) * 0.55F;
        for (int i = 0; i < 12; i++) {
            float wave = 1.0F + MathHelper.cos(age * 1.5F + (float) i) * 0.01F - retract;
            spines[i].rotateAngleX = PI * ROT_X[i];
            spines[i].rotateAngleY = PI * ROT_Y[i];
            spines[i].rotateAngleZ = PI * ROT_Z[i];
            spines[i].rotationPointX = POS_X[i] * wave;
            spines[i].rotationPointY = 16.0F + POS_Y[i] * wave;
            spines[i].rotationPointZ = POS_Z[i] * wave;
        }
        eye.rotationPointZ = -8.25F;
        Entity watched = Minecraft.getMinecraft().renderViewEntity;
        if (guardian.hasTargeted()) {
            watched = guardian.targeted();
        }
        if (watched != null) {
            double watchedY = watched.posY - (double) watched.yOffset + (double) ((EntityLivingBase) watched).getEyeHeight();
            if (watched == Minecraft.getMinecraft().renderViewEntity) {
                watchedY = watched.posY;
            }
            double ownY = guardian.posY + (double) guardian.getEyeHeight();
            eye.rotationPointY = watchedY - ownY > 0.0D ? 0.0F : 1.0F;
            Vec3 look = guardian.getLook(0.0F);
            Vec3 flat = Vec3.createVectorHelper(look.xCoord, 0.0D, look.zCoord);
            Vec3 toWatched = Vec3.createVectorHelper(guardian.posX - watched.posX, 0.0D, guardian.posZ - watched.posZ).normalize();
            toWatched.rotateAroundY(1.5707964F);
            double side = flat.dotProduct(toWatched);
            eye.rotationPointX = MathHelper.sqrt_float((float) Math.abs(side)) * 2.0F * (float) Math.signum(side);
        }
        eye.showModel = true;
        float swing = guardian.tail(partial);
        tail[0].rotateAngleY = MathHelper.sin(swing) * PI * 0.05F;
        tail[1].rotateAngleY = MathHelper.sin(swing) * PI * 0.1F;
        tail[1].rotationPointX = -1.5F;
        tail[1].rotationPointY = 0.5F;
        tail[1].rotationPointZ = 14.0F;
        tail[2].rotateAngleY = MathHelper.sin(swing) * PI * 0.15F;
        tail[2].rotationPointX = 0.5F;
        tail[2].rotationPointY = 0.5F;
        tail[2].rotationPointZ = 6.0F;
    }
}
