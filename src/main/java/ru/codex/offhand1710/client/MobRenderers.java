package ru.codex.offhand1710.client;

import net.minecraft.client.model.ModelSkeleton;
import net.minecraft.client.renderer.entity.RenderSkeleton;
import net.minecraft.client.renderer.entity.RenderZombie;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/** Renderers of the variants of vanilla mobs. */
final class MobRenderers {
    private static final String ROOT = "textures/entity/";

    private MobRenderers() {
    }

    /** RenderHusk of 1.12.2. */
    static final class Husk extends RenderZombie {
        private static final ResourceLocation TEXTURE = new ResourceLocation("mc112bridge", ROOT + "zombie/husk.png");

        @Override
        protected ResourceLocation getEntityTexture(Entity entity) {
            return TEXTURE;
        }

        @Override
        protected void preRenderCallback(EntityLivingBase entity, float partial) {
            GL11.glScalef(1.0625F, 1.0625F, 1.0625F);
            super.preRenderCallback(entity, partial);
        }
    }

    /** RenderStray and LayerStrayClothing of 1.12.2. */
    static final class Stray extends RenderSkeleton {
        private static final ResourceLocation TEXTURE = new ResourceLocation("mc112bridge", ROOT + "skeleton/stray.png");
        private static final ResourceLocation OVERLAY = new ResourceLocation("mc112bridge", ROOT + "skeleton/stray_overlay.png");
        private final ModelSkeleton clothes = new ModelSkeleton(0.25F);

        @Override
        protected ResourceLocation getEntityTexture(Entity entity) {
            return TEXTURE;
        }

        @Override
        protected void renderModel(EntityLivingBase entity, float limb, float amount, float age, float yaw, float pitch, float scale) {
            super.renderModel(entity, limb, amount, age, yaw, pitch, scale);
            ItemStack held = entity.getHeldItem();
            clothes.heldItemRight = held == null ? 0 : 1;
            clothes.aimedBow = held != null && held.getItem() == Items.bow;
            clothes.onGround = mainModel.onGround;
            bindTexture(OVERLAY);
            clothes.render(entity, limb, amount, age, yaw, pitch, scale);
        }
    }
}
