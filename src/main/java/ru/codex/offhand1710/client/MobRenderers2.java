package ru.codex.offhand1710.client;

import cpw.mods.fml.client.registry.RenderingRegistry;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import ru.codex.offhand1710.EntityHusk;
import ru.codex.offhand1710.EntityPolarBear;
import ru.codex.offhand1710.EntityRabbit;
import ru.codex.offhand1710.EntityStray;
import ru.codex.offhand1710.Illagers;

/** Renderers of the new mobs. */
final class MobRenderers2 {
    private static final String DOMAIN = "mc112bridge";
    private static final String ROOT = "textures/entity/";

    private MobRenderers2() {
    }

    static void register() {
        RenderingRegistry.registerEntityRenderingHandler(EntityHusk.class, new MobRenderers.Husk());
        RenderingRegistry.registerEntityRenderingHandler(EntityStray.class, new MobRenderers.Stray());
        RenderingRegistry.registerEntityRenderingHandler(EntityPolarBear.class, new PolarBear());
        RenderingRegistry.registerEntityRenderingHandler(EntityRabbit.class, new Rabbit());
        RenderingRegistry.registerEntityRenderingHandler(Illagers.Vindicator.class, new Illager("vindicator", 0.9375F));
        RenderingRegistry.registerEntityRenderingHandler(Illagers.Evoker.class, new Illager("evoker", 0.9375F));
        RenderingRegistry.registerEntityRenderingHandler(Illagers.Illusioner.class, new Illager("illusionist", 0.9375F));
        RenderingRegistry.registerEntityRenderingHandler(Illagers.Vex.class, new Vex());
        RenderingRegistry.registerEntityRenderingHandler(Illagers.Fangs.class, new Fangs());
    }

    static final class PolarBear extends RenderLiving {
        private static final ResourceLocation TEXTURE = new ResourceLocation(DOMAIN, ROOT + "bear/polarbear.png");

        PolarBear() {
            super(new MobModels.PolarBear(), 0.7F);
        }

        @Override
        protected ResourceLocation getEntityTexture(Entity entity) {
            return TEXTURE;
        }

        @Override
        protected void preRenderCallback(EntityLivingBase entity, float partial) {
            GL11.glScalef(1.2F, 1.2F, 1.2F);
        }
    }

    static final class Rabbit extends RenderLiving {
        private static final ResourceLocation[] TEXTURES = new ResourceLocation[6];

        static {
            String[] names = {"brown", "white", "black", "white_splotched", "gold", "salt"};
            for (int i = 0; i < names.length; i++) {
                TEXTURES[i] = new ResourceLocation(DOMAIN, ROOT + "rabbit/" + names[i] + ".png");
            }
        }

        Rabbit() {
            super(new MobModels.Rabbit(), 0.3F);
        }

        @Override
        protected ResourceLocation getEntityTexture(Entity entity) {
            int type = ((EntityRabbit) entity).getRabbitType();
            return TEXTURES[type >= 0 && type < TEXTURES.length ? type : 0];
        }
    }

    static final class Illager extends RenderLiving {
        private final ResourceLocation texture;
        private final float scale;

        Illager(String name, float scale) {
            super(new MobModels.Illager(), 0.5F);
            texture = new ResourceLocation(DOMAIN, ROOT + "illager/" + name + ".png");
            this.scale = scale;
        }

        @Override
        protected ResourceLocation getEntityTexture(Entity entity) {
            return texture;
        }

        @Override
        protected void preRenderCallback(EntityLivingBase entity, float partial) {
            GL11.glScalef(scale, scale, scale);
        }

        /** LayerHeldItem of the illagers: the weapon is only out while the arms are. */
        @Override
        protected void renderEquippedItems(EntityLivingBase entity, float partial) {
            super.renderEquippedItems(entity, partial);
            ItemStack held = entity.getHeldItem();
            int pose = ((Illagers.Base) entity).pose();
            if (held == null || (pose != Illagers.POSE_ATTACKING && pose != Illagers.POSE_BOW)) {
                return;
            }
            ModelRenderer arm = ((MobModels.Illager) mainModel).rightArm;
            GL11.glPushMatrix();
            arm.postRender(0.0625F);
            GL11.glTranslatef(-0.0625F, 0.4375F, 0.0625F);
            float size = 0.625F;
            if (held.getItem() == Items.bow) {
                GL11.glTranslatef(0.0F, 0.125F, 0.3125F);
                GL11.glRotatef(-20.0F, 0.0F, 1.0F, 0.0F);
            } else {
                GL11.glTranslatef(0.0F, 0.1875F, 0.0F);
            }
            GL11.glScalef(size, -size, size);
            GL11.glRotatef(-100.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
            renderManager.itemRenderer.renderItem(entity, held, 0);
            GL11.glPopMatrix();
        }
    }

    static final class Vex extends RenderBiped {
        private static final ResourceLocation TEXTURE = new ResourceLocation(DOMAIN, ROOT + "illager/vex.png");
        private static final ResourceLocation CHARGING = new ResourceLocation(DOMAIN, ROOT + "illager/vex_charging.png");

        Vex() {
            super(new MobModels.Vex(), 0.3F);
        }

        @Override
        protected ResourceLocation getEntityTexture(Entity entity) {
            return ((Illagers.Vex) entity).isCharging() ? CHARGING : TEXTURE;
        }

        @Override
        protected void preRenderCallback(EntityLivingBase entity, float partial) {
            GL11.glScalef(0.4F, 0.4F, 0.4F);
        }
    }

    /** RenderEvokerFangs and ModelEvokerFangs. */
    static final class Fangs extends Render {
        private static final ResourceLocation TEXTURE = new ResourceLocation(DOMAIN, ROOT + "illager/fangs.png");

        private static final class Model extends ModelBase {
            final ModelRenderer base;
            final ModelRenderer upper;
            final ModelRenderer lower;

            Model() {
                textureWidth = 64;
                textureHeight = 32;
                base = new ModelRenderer(this, 0, 0);
                base.setRotationPoint(-5.0F, 22.0F, -5.0F);
                base.addBox(0.0F, 0.0F, 0.0F, 10, 12, 10);
                upper = new ModelRenderer(this, 40, 0);
                upper.setRotationPoint(1.5F, 22.0F, -4.0F);
                upper.addBox(0.0F, 0.0F, 0.0F, 4, 14, 8);
                lower = new ModelRenderer(this, 40, 0);
                lower.setRotationPoint(-1.5F, 22.0F, 4.0F);
                lower.addBox(0.0F, 0.0F, 0.0F, 4, 14, 8);
            }
        }

        private final Model model = new Model();

        @Override
        public void doRender(Entity entity, double x, double y, double z, float yaw, float partial) {
            float bite = ((Illagers.Fangs) entity).bite(partial);
            if (bite <= 0.0F) {
                return;
            }
            float scale = 1.0F;
            if (bite > 0.9F) {
                scale = (float) ((double) scale * ((1.0D - (double) bite) / 0.10000000149011612D));
            }
            float open = 1.0F - bite * bite * bite * bite * 0.0F;
            GL11.glPushMatrix();
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glTranslatef((float) x, (float) y, (float) z);
            GL11.glRotatef(90.0F - entity.rotationYaw, 0.0F, 1.0F, 0.0F);
            GL11.glScalef(-scale, -scale, scale);
            GL11.glTranslatef(0.0F, -1.501F, 0.0F);
            bindTexture(TEXTURE);
            float jaw = Math.min(bite * 2.0F, 1.0F);
            float t = 1.0F - jaw * jaw;
            model.upper.rotateAngleZ = 3.1415927F - open * 0.35F * t * 3.1415927F;
            model.lower.rotateAngleZ = 3.1415927F + open * 0.35F * t * 3.1415927F;
            float rise = (bite + (float) Math.sin((double) bite * 2.7D)) * 7.2F;
            model.upper.rotationPointY = 24.0F - rise;
            model.lower.rotationPointY = model.upper.rotationPointY;
            model.base.rotationPointY = 24.0F - rise * 0.0F;
            model.base.render(0.0625F);
            model.upper.render(0.0625F);
            model.lower.render(0.0625F);
            GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glPopMatrix();
        }

        @Override
        protected ResourceLocation getEntityTexture(Entity entity) {
            return TEXTURE;
        }
    }

}
