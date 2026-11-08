package ru.codex.offhand1710.client;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import ru.codex.offhand1710.ItemShield;
import ru.codex.offhand1710.OffhandInventory;

/**
 * The player model with the parts of the 1.12.2 ModelBiped pose that 1.7.10 lacks: the arm turned 30 degrees
 * inwards while blocking with a shield, and the swing of the left arm when the offhand acts.
 *
 * It is ModelBiped of 1.7.10 plus additions after its setRotationAngles. The additions only apply while the
 * player is drawn in the world or in an inventory preview (between RenderPlayerEvent.Pre and Post), not for
 * the first person arm, which 1.12.2 draws without any swing either.
 */
public class ModelBiped112 extends ModelBiped {
    private static final float PI = (float) Math.PI;
    private static final float BLOCK_TURN = 0.5235988F;

    private static EntityPlayer rendering;
    private static float partialTicks;

    public ModelBiped112(float size) {
        super(size);
    }

    static void begin(EntityPlayer player, float partial) {
        rendering = player;
        partialTicks = partial;
    }

    static void end() {
        rendering = null;
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float headYaw,
            float headPitch, float scale, Entity entity) {
        super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, headYaw, headPitch, scale, entity);
        if (entity == null || entity != rendering || aimedBow) {
            return; // the bow pose sets both arms absolutely, as in 1.12.2
        }
        EntityPlayer player = (EntityPlayer) entity;
        OffhandInventory offhand = OffhandInventory.get(player);

        // Gliding (ModelBiped of 1.12.2): the head looks ahead and the limbs hardly swing at speed.
        if (offhand.elytraFlying && offhand.elytraTicks > 4) {
            bipedHead.rotateAngleX = -0.7853982F;
            bipedHeadwear.rotateAngleX = bipedHead.rotateAngleX;
            double dx = player.posX - player.prevPosX;
            double dy = player.posY - player.prevPosY;
            double dz = player.posZ - player.prevPosZ;
            float slow = (float) (dx * dx + dy * dy + dz * dz) / 0.2F;
            slow = Math.max(1.0F, slow * slow * slow);
            bipedRightLeg.rotateAngleX /= slow;
            bipedLeftLeg.rotateAngleX /= slow;
            bipedRightArm.rotateAngleX /= slow;
            bipedLeftArm.rotateAngleX /= slow;
        }

        // ModelBiped.ArmPose.BLOCK (1.12.2): rotateAngleY of the left arm 0.5235988, of the right arm -0.5235988.
        if (heldItemLeft == 3 && isShield(offhand.getStack())) {
            bipedLeftArm.rotateAngleY += BLOCK_TURN;
        }
        if (heldItemRight == 3 && isShield(player.getItemInUse())) {
            bipedRightArm.rotateAngleY -= BLOCK_TURN;
        }

        // The swing of 1.12.2 for EnumHandSide.LEFT. 1.7.10 has just run the same code for the right arm with
        // onGround = 0 (no change), so it is only needed while the main hand is not swinging.
        float swing = offhand.swing(partialTicks);
        if (swing > 0.0F && onGround <= 0.0F) {
            swingLeftArm(swing);
        }
    }

    private static boolean isShield(ItemStack stack) {
        return stack != null && stack.getItem() instanceof ItemShield;
    }

    private void swingLeftArm(float progress) {
        float bodyYaw = -MathHelper.sin(MathHelper.sqrt_float(progress) * PI * 2.0F) * 0.2F;
        bipedBody.rotateAngleY = bodyYaw;
        bipedRightArm.rotationPointZ = MathHelper.sin(bodyYaw) * 5.0F;
        bipedRightArm.rotationPointX = -MathHelper.cos(bodyYaw) * 5.0F;
        bipedLeftArm.rotationPointZ = -MathHelper.sin(bodyYaw) * 5.0F;
        bipedLeftArm.rotationPointX = MathHelper.cos(bodyYaw) * 5.0F;
        bipedRightArm.rotateAngleY += bodyYaw;
        bipedLeftArm.rotateAngleY += bodyYaw;
        bipedLeftArm.rotateAngleX += bodyYaw;
        float eased = 1.0F - progress;
        eased *= eased;
        eased *= eased;
        eased = 1.0F - eased;
        float lift = MathHelper.sin(eased * PI);
        float pitch = MathHelper.sin(progress * PI) * -(bipedHead.rotateAngleX - 0.7F) * 0.75F;
        bipedLeftArm.rotateAngleX = (float) ((double) bipedLeftArm.rotateAngleX - ((double) lift * 1.2D + (double) pitch));
        bipedLeftArm.rotateAngleY += bodyYaw * 2.0F;
        bipedLeftArm.rotateAngleZ += MathHelper.sin(progress * PI) * -0.4F;
    }
}
