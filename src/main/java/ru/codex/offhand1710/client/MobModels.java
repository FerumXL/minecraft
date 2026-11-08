package ru.codex.offhand1710.client;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;
import ru.codex.offhand1710.EntityPolarBear;
import ru.codex.offhand1710.EntityRabbit;
import ru.codex.offhand1710.Illagers;

/** Models of the new mobs, with the boxes of the 1.12.2 models. */
final class MobModels {
    private static final float PI = (float) Math.PI;

    private MobModels() {
    }

    private static ModelRenderer box(ModelBase model, int u, int v, float x, float y, float z, int w, int h, int d, float px, float py, float pz) {
        ModelRenderer part = new ModelRenderer(model, u, v);
        part.addBox(x, y, z, w, h, d);
        part.setRotationPoint(px, py, pz);
        return part;
    }

    /** ModelPolarBear. */
    static final class PolarBear extends ModelBase {
        private final ModelRenderer head;
        private final ModelRenderer body;
        private final ModelRenderer leg1;
        private final ModelRenderer leg2;
        private final ModelRenderer leg3;
        private final ModelRenderer leg4;

        PolarBear() {
            textureWidth = 128;
            textureHeight = 64;
            head = new ModelRenderer(this, 0, 0);
            head.addBox(-3.5F, -3.0F, -3.0F, 7, 7, 7);
            head.setRotationPoint(0.0F, 10.0F, -16.0F);
            head.setTextureOffset(0, 44).addBox(-2.5F, 1.0F, -6.0F, 5, 3, 3);
            head.setTextureOffset(26, 0).addBox(-4.5F, -4.0F, -1.0F, 2, 2, 1);
            head.mirror = true;
            head.setTextureOffset(26, 0).addBox(2.5F, -4.0F, -1.0F, 2, 2, 1);
            head.mirror = false;
            body = new ModelRenderer(this);
            body.setTextureOffset(0, 19).addBox(-5.0F, -13.0F, -7.0F, 14, 14, 11);
            body.setTextureOffset(39, 0).addBox(-4.0F, -25.0F, -7.0F, 12, 12, 10);
            body.setRotationPoint(-2.0F, 9.0F, 12.0F);
            leg1 = box(this, 50, 22, -2.0F, 0.0F, -2.0F, 4, 10, 8, -4.5F, 14.0F, 6.0F);
            leg2 = box(this, 50, 22, -2.0F, 0.0F, -2.0F, 4, 10, 8, 4.5F, 14.0F, 6.0F);
            leg3 = box(this, 50, 40, -2.0F, 0.0F, -2.0F, 4, 10, 6, -3.5F, 14.0F, -8.0F);
            leg4 = box(this, 50, 40, -2.0F, 0.0F, -2.0F, 4, 10, 6, 3.5F, 14.0F, -8.0F);
        }

        @Override
        public void render(Entity entity, float limb, float amount, float age, float yaw, float pitch, float scale) {
            setRotationAngles(limb, amount, age, yaw, pitch, scale, entity);
            if (isChild) {
                GL11.glPushMatrix();
                GL11.glScalef(0.6666667F, 0.6666667F, 0.6666667F);
                GL11.glTranslatef(0.0F, 16.0F * scale, 4.0F * scale);
                head.render(scale);
                GL11.glPopMatrix();
                GL11.glPushMatrix();
                GL11.glScalef(0.5F, 0.5F, 0.5F);
                GL11.glTranslatef(0.0F, 24.0F * scale, 0.0F);
            } else {
                head.render(scale);
            }
            body.render(scale);
            leg1.render(scale);
            leg2.render(scale);
            leg3.render(scale);
            leg4.render(scale);
            if (isChild) {
                GL11.glPopMatrix();
            }
        }

        @Override
        public void setRotationAngles(float limb, float amount, float age, float yaw, float pitch, float scale, Entity entity) {
            head.rotateAngleX = pitch * 0.017453292F;
            head.rotateAngleY = yaw * 0.017453292F;
            body.rotateAngleX = PI / 2.0F;
            leg1.rotateAngleX = MathHelper.cos(limb * 0.6662F) * 1.4F * amount;
            leg2.rotateAngleX = MathHelper.cos(limb * 0.6662F + PI) * 1.4F * amount;
            leg3.rotateAngleX = MathHelper.cos(limb * 0.6662F + PI) * 1.4F * amount;
            leg4.rotateAngleX = MathHelper.cos(limb * 0.6662F) * 1.4F * amount;
            float up = ((EntityPolarBear) entity).standing(age - (float) entity.ticksExisted);
            up *= up;
            float down = 1.0F - up;
            body.rotateAngleX = (PI / 2.0F) - up * PI * 0.35F;
            body.rotationPointY = 9.0F * down + 11.0F * up;
            leg3.rotationPointY = 14.0F * down + -6.0F * up;
            leg3.rotationPointZ = -8.0F * down + -4.0F * up;
            leg3.rotateAngleX -= up * PI * 0.45F;
            leg4.rotationPointY = leg3.rotationPointY;
            leg4.rotationPointZ = leg3.rotationPointZ;
            leg4.rotateAngleX -= up * PI * 0.45F;
            head.rotationPointY = 10.0F * down + -12.0F * up;
            head.rotationPointZ = -16.0F * down + -3.0F * up;
            head.rotateAngleX += up * PI * 0.15F;
        }
    }

    /** ModelRabbit. */
    static final class Rabbit extends ModelBase {
        private final ModelRenderer leftFoot;
        private final ModelRenderer rightFoot;
        private final ModelRenderer leftThigh;
        private final ModelRenderer rightThigh;
        private final ModelRenderer body;
        private final ModelRenderer leftArm;
        private final ModelRenderer rightArm;
        private final ModelRenderer head;
        private final ModelRenderer rightEar;
        private final ModelRenderer leftEar;
        private final ModelRenderer tail;
        private final ModelRenderer nose;
        private final ModelRenderer[] all;

        private ModelRenderer part(int u, int v, float x, float y, float z, int w, int h, int d, float px, float py, float pz, float rx, float ry) {
            ModelRenderer p = box(this, u, v, x, y, z, w, h, d, px, py, pz);
            p.setTextureSize(64, 32);
            p.mirror = true;
            p.rotateAngleX = rx;
            p.rotateAngleY = ry;
            return p;
        }

        Rabbit() {
            textureWidth = 64;
            textureHeight = 32;
            leftFoot = part(26, 24, -1.0F, 5.5F, -3.7F, 2, 1, 7, 3.0F, 17.5F, 3.7F, 0.0F, 0.0F);
            rightFoot = part(8, 24, -1.0F, 5.5F, -3.7F, 2, 1, 7, -3.0F, 17.5F, 3.7F, 0.0F, 0.0F);
            leftThigh = part(30, 15, -1.0F, 0.0F, 0.0F, 2, 4, 5, 3.0F, 17.5F, 3.7F, -0.34906584F, 0.0F);
            rightThigh = part(16, 15, -1.0F, 0.0F, 0.0F, 2, 4, 5, -3.0F, 17.5F, 3.7F, -0.34906584F, 0.0F);
            body = part(0, 0, -3.0F, -2.0F, -10.0F, 6, 5, 10, 0.0F, 19.0F, 8.0F, -0.34906584F, 0.0F);
            leftArm = part(8, 15, -1.0F, 0.0F, -1.0F, 2, 7, 2, 3.0F, 17.0F, -1.0F, -0.17453292F, 0.0F);
            rightArm = part(0, 15, -1.0F, 0.0F, -1.0F, 2, 7, 2, -3.0F, 17.0F, -1.0F, -0.17453292F, 0.0F);
            head = part(32, 0, -2.5F, -4.0F, -5.0F, 5, 4, 5, 0.0F, 16.0F, -1.0F, 0.0F, 0.0F);
            rightEar = part(52, 0, -2.5F, -9.0F, -1.0F, 2, 5, 1, 0.0F, 16.0F, -1.0F, 0.0F, -0.2617994F);
            leftEar = part(58, 0, 0.5F, -9.0F, -1.0F, 2, 5, 1, 0.0F, 16.0F, -1.0F, 0.0F, 0.2617994F);
            tail = part(52, 6, -1.5F, -1.5F, 0.0F, 3, 3, 2, 0.0F, 20.0F, 7.0F, -0.3490659F, 0.0F);
            nose = part(32, 9, -0.5F, -2.5F, -5.5F, 1, 1, 1, 0.0F, 16.0F, -1.0F, 0.0F, 0.0F);
            all = new ModelRenderer[] {leftFoot, rightFoot, leftThigh, rightThigh, body, leftArm, rightArm, head, rightEar, leftEar, tail, nose};
        }

        @Override
        public void render(Entity entity, float limb, float amount, float age, float yaw, float pitch, float scale) {
            setRotationAngles(limb, amount, age, yaw, pitch, scale, entity);
            GL11.glPushMatrix();
            if (isChild) {
                GL11.glPushMatrix();
                GL11.glScalef(0.56666666F, 0.56666666F, 0.56666666F);
                GL11.glTranslatef(0.0F, 22.0F * scale, 2.0F * scale);
                head.render(scale);
                leftEar.render(scale);
                rightEar.render(scale);
                nose.render(scale);
                GL11.glPopMatrix();
                GL11.glScalef(0.4F, 0.4F, 0.4F);
                GL11.glTranslatef(0.0F, 36.0F * scale, 0.0F);
                for (ModelRenderer p : all) {
                    if (p != head && p != leftEar && p != rightEar && p != nose) {
                        p.render(scale);
                    }
                }
            } else {
                GL11.glScalef(0.6F, 0.6F, 0.6F);
                GL11.glTranslatef(0.0F, 16.0F * scale, 0.0F);
                for (ModelRenderer p : all) {
                    p.render(scale);
                }
            }
            GL11.glPopMatrix();
        }

        @Override
        public void setRotationAngles(float limb, float amount, float age, float yaw, float pitch, float scale, Entity entity) {
            float partial = age - (float) entity.ticksExisted;
            nose.rotateAngleX = pitch * 0.017453292F;
            head.rotateAngleX = pitch * 0.017453292F;
            rightEar.rotateAngleX = pitch * 0.017453292F;
            leftEar.rotateAngleX = pitch * 0.017453292F;
            nose.rotateAngleY = yaw * 0.017453292F;
            head.rotateAngleY = yaw * 0.017453292F;
            rightEar.rotateAngleY = nose.rotateAngleY - 0.2617994F;
            leftEar.rotateAngleY = nose.rotateAngleY + 0.2617994F;
            float jump = MathHelper.sin(((EntityRabbit) entity).jump(partial) * PI);
            leftThigh.rotateAngleX = (jump * 50.0F - 21.0F) * 0.017453292F;
            rightThigh.rotateAngleX = (jump * 50.0F - 21.0F) * 0.017453292F;
            leftFoot.rotateAngleX = jump * 50.0F * 0.017453292F;
            rightFoot.rotateAngleX = jump * 50.0F * 0.017453292F;
            leftArm.rotateAngleX = (jump * -40.0F - 11.0F) * 0.017453292F;
            rightArm.rotateAngleX = (jump * -40.0F - 11.0F) * 0.017453292F;
        }
    }

    /** ModelIllager. */
    static final class Illager extends ModelBase {
        final ModelRenderer head;
        final ModelRenderer body;
        final ModelRenderer arms;
        final ModelRenderer rightLeg;
        final ModelRenderer leftLeg;
        final ModelRenderer rightArm;
        final ModelRenderer leftArm;

        Illager() {
            textureWidth = 64;
            textureHeight = 64;
            head = new ModelRenderer(this);
            head.setTextureOffset(0, 0).addBox(-4.0F, -10.0F, -4.0F, 8, 10, 8);
            ModelRenderer nose = new ModelRenderer(this);
            nose.setRotationPoint(0.0F, -2.0F, 0.0F);
            nose.setTextureOffset(24, 0).addBox(-1.0F, -1.0F, -6.0F, 2, 4, 2);
            head.addChild(nose);
            body = new ModelRenderer(this);
            body.setTextureOffset(16, 20).addBox(-4.0F, 0.0F, -3.0F, 8, 12, 6);
            body.setTextureOffset(0, 38).addBox(-4.0F, 0.0F, -3.0F, 8, 18, 6, 0.5F);
            arms = new ModelRenderer(this);
            arms.setRotationPoint(0.0F, 2.0F, 0.0F);
            arms.setTextureOffset(44, 22).addBox(-8.0F, -2.0F, -2.0F, 4, 8, 4);
            ModelRenderer other = new ModelRenderer(this, 44, 22);
            other.mirror = true;
            other.addBox(4.0F, -2.0F, -2.0F, 4, 8, 4);
            arms.addChild(other);
            arms.setTextureOffset(40, 38).addBox(-4.0F, 2.0F, -2.0F, 8, 4, 4);
            rightLeg = box(this, 0, 22, -2.0F, 0.0F, -2.0F, 4, 12, 4, -2.0F, 12.0F, 0.0F);
            leftLeg = new ModelRenderer(this, 0, 22);
            leftLeg.mirror = true;
            leftLeg.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4);
            leftLeg.setRotationPoint(2.0F, 12.0F, 0.0F);
            rightArm = box(this, 40, 46, -3.0F, -2.0F, -2.0F, 4, 12, 4, -5.0F, 2.0F, 0.0F);
            leftArm = new ModelRenderer(this, 40, 46);
            leftArm.mirror = true;
            leftArm.addBox(-1.0F, -2.0F, -2.0F, 4, 12, 4);
            leftArm.setRotationPoint(5.0F, 2.0F, 0.0F);
        }

        @Override
        public void render(Entity entity, float limb, float amount, float age, float yaw, float pitch, float scale) {
            setRotationAngles(limb, amount, age, yaw, pitch, scale, entity);
            head.render(scale);
            body.render(scale);
            rightLeg.render(scale);
            leftLeg.render(scale);
            if (((Illagers.Base) entity).pose() == Illagers.POSE_CROSSED) {
                arms.render(scale);
            } else {
                rightArm.render(scale);
                leftArm.render(scale);
            }
        }

        @Override
        public void setRotationAngles(float limb, float amount, float age, float yaw, float pitch, float scale, Entity entity) {
            head.rotateAngleY = yaw * 0.017453292F;
            head.rotateAngleX = pitch * 0.017453292F;
            arms.rotationPointY = 3.0F;
            arms.rotationPointZ = -1.0F;
            arms.rotateAngleX = -0.75F;
            rightLeg.rotateAngleX = MathHelper.cos(limb * 0.6662F) * 1.4F * amount * 0.5F;
            leftLeg.rotateAngleX = MathHelper.cos(limb * 0.6662F + PI) * 1.4F * amount * 0.5F;
            int pose = ((Illagers.Base) entity).pose();
            if (pose == Illagers.POSE_ATTACKING) {
                float swing = MathHelper.sin(onGround * PI);
                float lift = MathHelper.sin((1.0F - (1.0F - onGround) * (1.0F - onGround)) * PI);
                rightArm.rotateAngleZ = 0.0F;
                leftArm.rotateAngleZ = 0.0F;
                rightArm.rotateAngleY = 0.15707964F;
                leftArm.rotateAngleY = -0.15707964F;
                rightArm.rotateAngleX = -1.8849558F + MathHelper.cos(age * 0.09F) * 0.15F + swing * 2.2F - lift * 0.4F;
                leftArm.rotateAngleX = MathHelper.cos(age * 0.19F) * 0.5F + swing * 1.2F - lift * 0.4F;
                rightArm.rotateAngleZ += MathHelper.cos(age * 0.09F) * 0.05F + 0.05F;
                leftArm.rotateAngleZ -= MathHelper.cos(age * 0.09F) * 0.05F + 0.05F;
                rightArm.rotateAngleX += MathHelper.sin(age * 0.067F) * 0.05F;
                leftArm.rotateAngleX -= MathHelper.sin(age * 0.067F) * 0.05F;
            } else if (pose == Illagers.POSE_SPELL) {
                rightArm.rotationPointZ = 0.0F;
                rightArm.rotationPointX = -5.0F;
                leftArm.rotationPointZ = 0.0F;
                leftArm.rotationPointX = 5.0F;
                rightArm.rotateAngleX = MathHelper.cos(age * 0.6662F) * 0.25F;
                leftArm.rotateAngleX = MathHelper.cos(age * 0.6662F) * 0.25F;
                rightArm.rotateAngleZ = 2.3561945F;
                leftArm.rotateAngleZ = -2.3561945F;
                rightArm.rotateAngleY = 0.0F;
                leftArm.rotateAngleY = 0.0F;
            } else if (pose == Illagers.POSE_BOW) {
                rightArm.rotateAngleY = -0.1F + head.rotateAngleY;
                rightArm.rotateAngleX = -1.5707964F + head.rotateAngleX;
                leftArm.rotateAngleX = -0.9424779F + head.rotateAngleX;
                leftArm.rotateAngleY = head.rotateAngleY - 0.4F;
                leftArm.rotateAngleZ = 1.5707964F;
            }
        }
    }

    /** A small flying biped with wings, the vex. */
    static final class Vex extends ModelBiped {
        private final ModelRenderer leftWing;
        private final ModelRenderer rightWing;

        Vex() {
            super(0.0F, 0.0F, 64, 64);
            bipedLeftLeg.showModel = false;
            bipedHeadwear.showModel = false;
            bipedRightLeg = new ModelRenderer(this, 32, 0);
            bipedRightLeg.addBox(-1.0F, -1.0F, -2.0F, 6, 10, 4, 0.0F);
            bipedRightLeg.setRotationPoint(-1.9F, 12.0F, 0.0F);
            rightWing = new ModelRenderer(this, 0, 32);
            rightWing.addBox(-20.0F, 0.0F, 0.0F, 20, 12, 1);
            leftWing = new ModelRenderer(this, 0, 32);
            leftWing.mirror = true;
            leftWing.addBox(0.0F, 0.0F, 0.0F, 20, 12, 1);
        }

        @Override
        public void render(Entity entity, float limb, float amount, float age, float yaw, float pitch, float scale) {
            super.render(entity, limb, amount, age, yaw, pitch, scale);
            float flap = 0.3926991F + MathHelper.cos(age * 0.8F) * PI * 0.05F;
            rightWing.rotationPointZ = 2.0F;
            leftWing.rotationPointZ = 2.0F;
            rightWing.rotationPointY = 1.0F;
            leftWing.rotationPointY = 1.0F;
            rightWing.rotateAngleY = 0.47123894F + MathHelper.cos(age * 0.8F) * PI * 0.05F;
            leftWing.rotateAngleY = -rightWing.rotateAngleY;
            leftWing.rotateAngleZ = -0.47123894F;
            leftWing.rotateAngleX = flap;
            rightWing.rotateAngleX = flap;
            rightWing.rotateAngleZ = 0.47123894F;
            rightWing.render(scale);
            leftWing.render(scale);
        }

        @Override
        public void setRotationAngles(float limb, float amount, float age, float yaw, float pitch, float scale, Entity entity) {
            super.setRotationAngles(limb, amount, age, yaw, pitch, scale, entity);
            bipedRightLeg.rotateAngleX += 0.62831855F;
        }
    }
}
