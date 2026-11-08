package ru.codex.offhand1710.client;

import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import ru.codex.offhand1710.MoreMobs;
import ru.codex.offhand1710.OffhandMessages;

/** Client side of the parrot, llama and shulker: models, renderers and the Levitation effect of shulker bullets. */
public final class MoreMobsClient {
    private static final String DOMAIN = "mc112bridge";
    private static final String ROOT = "textures/entity/";
    private static final float PI = (float) Math.PI;
    private static int levitation;

    private MoreMobsClient() {
    }

    static void register() {
        RenderingRegistry.registerEntityRenderingHandler(MoreMobs.Parrot.class, new ParrotRender());
        RenderingRegistry.registerEntityRenderingHandler(MoreMobs.Llama.class, new LlamaRender());
        RenderingRegistry.registerEntityRenderingHandler(MoreMobs.Spit.class, new Small(ROOT + "llama/spit.png", 0.1F));
        RenderingRegistry.registerEntityRenderingHandler(MoreMobs.Shulker.class, new ShulkerRender());
        RenderingRegistry.registerEntityRenderingHandler(MoreMobs.Bullet.class, new Small(ROOT + "shulker/spark.png", 0.2F));
    }

    public static final class LevitateHandler implements IMessageHandler<OffhandMessages.Levitate, IMessage> {
        @Override
        public IMessage onMessage(final OffhandMessages.Levitate message, MessageContext context) {
            Minecraft.getMinecraft().func_152344_a(new Runnable() {
                @Override
                public void run() {
                    levitation = message.ticks;
                }
            });
            return null;
        }
    }

    /** EntityLivingBase.travel of 1.12.2 under Levitation I. */
    public static final class Ticker {
        @SubscribeEvent
        public void onPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase == TickEvent.Phase.END && event.player == Minecraft.getMinecraft().thePlayer) {
                ru.codex.offhand1710.OffhandInventory state = ru.codex.offhand1710.OffhandInventory.get(event.player);
                if (state.bedBounce > 0.0D) {
                    event.player.motionY = state.bedBounce;
                    state.bedBounce = 0.0D;
                }
            }
            if (event.phase == TickEvent.Phase.END && levitation > 0 && event.player == Minecraft.getMinecraft().thePlayer) {
                levitation--;
                event.player.motionY += (0.05D - event.player.motionY) * 0.2D;
                event.player.fallDistance = 0.0F;
            }
        }
    }

    private static ModelRenderer part(ModelBase model, int u, int v, float x, float y, float z, int w, int h, int d, float px, float py, float pz) {
        ModelRenderer p = new ModelRenderer(model, u, v);
        p.addBox(x, y, z, w, h, d);
        p.setRotationPoint(px, py, pz);
        return p;
    }

    /** ModelParrot. */
    private static final class ParrotModel extends ModelBase {
        final ModelRenderer body;
        final ModelRenderer tail;
        final ModelRenderer leftWing;
        final ModelRenderer rightWing;
        final ModelRenderer head;
        final ModelRenderer leftLeg;
        final ModelRenderer rightLeg;

        ParrotModel() {
            textureWidth = 32;
            textureHeight = 32;
            body = part(this, 2, 8, -1.5F, 0.0F, -1.5F, 3, 6, 3, 0.0F, 16.5F, -3.0F);
            body.rotateAngleX = 0.4937F;
            tail = part(this, 22, 1, -1.5F, -1.0F, -1.0F, 3, 4, 1, 0.0F, 21.07F, 1.16F);
            leftWing = part(this, 19, 8, -0.5F, 0.0F, -1.5F, 1, 5, 3, 1.5F, 16.94F, -2.76F);
            rightWing = part(this, 19, 8, -0.5F, 0.0F, -1.5F, 1, 5, 3, -1.5F, 16.94F, -2.76F);
            leftWing.rotateAngleX = -0.6981F;
            leftWing.rotateAngleY = -PI;
            rightWing.rotateAngleX = -0.6981F;
            rightWing.rotateAngleY = -PI;
            head = part(this, 2, 2, -1.0F, -1.5F, -1.0F, 2, 3, 2, 0.0F, 15.69F, -2.76F);
            head.addChild(part(this, 10, 0, -1.0F, -0.5F, -2.0F, 2, 1, 4, 0.0F, -2.0F, -1.0F));
            head.addChild(part(this, 11, 7, -0.5F, -1.0F, -0.5F, 1, 2, 1, 0.0F, -0.5F, -1.5F));
            head.addChild(part(this, 16, 7, -0.5F, 0.0F, -0.5F, 1, 2, 1, 0.0F, -1.75F, -2.45F));
            ModelRenderer feather = part(this, 2, 18, 0.0F, -4.0F, -2.0F, 0, 5, 4, 0.0F, -2.15F, 0.15F);
            feather.rotateAngleX = -0.2214F;
            head.addChild(feather);
            leftLeg = part(this, 14, 18, -0.5F, 0.0F, -0.5F, 1, 2, 1, 1.0F, 22.0F, -1.05F);
            rightLeg = part(this, 14, 18, -0.5F, 0.0F, -0.5F, 1, 2, 1, -1.0F, 22.0F, -1.05F);
        }

        @Override
        public void render(Entity entity, float limb, float amount, float age, float yaw, float pitch, float scale) {
            setRotationAngles(limb, amount, age, yaw, pitch, scale, entity);
            body.render(scale);
            leftWing.render(scale);
            rightWing.render(scale);
            tail.render(scale);
            head.render(scale);
            leftLeg.render(scale);
            rightLeg.render(scale);
        }

        @Override
        public void setRotationAngles(float limb, float amount, float age, float yaw, float pitch, float scale, Entity entity) {
            MoreMobs.Parrot parrot = (MoreMobs.Parrot) entity;
            float partial = age - (float) entity.ticksExisted;
            float flap = parrot.prevFlap + (parrot.flap - parrot.prevFlap) * partial;
            float speed = parrot.prevFlapSpeed + (parrot.flapSpeed - parrot.prevFlapSpeed) * partial;
            float wing = (MathHelper.sin(flap) + 1.0F) * speed;
            float bob = wing * 0.3F;
            head.rotateAngleX = pitch * 0.017453292F;
            head.rotateAngleY = yaw * 0.017453292F;
            head.rotateAngleZ = 0.0F;
            head.rotationPointX = 0.0F;
            body.rotationPointX = 0.0F;
            tail.rotationPointX = 0.0F;
            rightWing.rotationPointX = -1.5F;
            leftWing.rotationPointX = 1.5F;
            leftLeg.rotateAngleX = -0.0299F;
            rightLeg.rotateAngleX = -0.0299F;
            leftLeg.rotationPointY = 22.0F;
            rightLeg.rotationPointY = 22.0F;
            leftLeg.rotateAngleZ = 0.0F;
            rightLeg.rotateAngleZ = 0.0F;
            if (parrot.isDancing()) {
                float x = MathHelper.cos((float) entity.ticksExisted);
                float y = MathHelper.sin((float) entity.ticksExisted);
                leftLeg.rotateAngleZ = -0.34906584F;
                rightLeg.rotateAngleZ = 0.34906584F;
                head.rotationPointX = x;
                head.rotationPointY = 15.69F + y;
                head.rotateAngleX = 0.0F;
                head.rotateAngleY = 0.0F;
                head.rotateAngleZ = y * 0.4F;
                body.rotationPointX = x;
                body.rotationPointY = 16.5F + y;
                leftWing.rotateAngleZ = -0.0873F - wing;
                leftWing.rotationPointX = 1.5F + x;
                leftWing.rotationPointY = 16.94F + y;
                rightWing.rotateAngleZ = 0.0873F + wing;
                rightWing.rotationPointX = -1.5F + x;
                rightWing.rotationPointY = 16.94F + y;
                tail.rotationPointX = x;
                tail.rotationPointY = 21.07F + y;
                return;
            }
            if (parrot.isSitting()) {
                head.rotationPointY = 17.59F;
                tail.rotateAngleX = 1.5388988F;
                tail.rotationPointY = 22.97F;
                body.rotationPointY = 18.4F;
                leftWing.rotateAngleZ = -0.0873F;
                leftWing.rotationPointY = 18.84F;
                rightWing.rotateAngleZ = 0.0873F;
                rightWing.rotationPointY = 18.84F;
                leftLeg.rotationPointY = 23.0F;
                rightLeg.rotationPointY = 23.0F;
                leftLeg.rotateAngleX += 1.0F;
                rightLeg.rotateAngleX += 1.0F;
                return;
            }
            if (parrot.isFlying()) {
                leftLeg.rotateAngleX += 0.6981317F;
                rightLeg.rotateAngleX += 0.6981317F;
            } else {
                leftLeg.rotateAngleX += MathHelper.cos(limb * 0.6662F) * 1.4F * amount;
                rightLeg.rotateAngleX += MathHelper.cos(limb * 0.6662F + PI) * 1.4F * amount;
            }
            head.rotationPointY = 15.69F + bob;
            tail.rotateAngleX = 1.015F + MathHelper.cos(limb * 0.6662F) * 0.3F * amount;
            tail.rotationPointY = 21.07F + bob;
            body.rotationPointY = 16.5F + bob;
            leftWing.rotateAngleZ = -0.0873F - wing;
            leftWing.rotationPointY = 16.94F + bob;
            rightWing.rotateAngleZ = 0.0873F + wing;
            rightWing.rotationPointY = 16.94F + bob;
            leftLeg.rotationPointY = 22.0F + bob;
            rightLeg.rotationPointY = 22.0F + bob;
        }
    }

    private static final class ParrotRender extends RenderLiving {
        private static final ResourceLocation[] TEXTURES = new ResourceLocation[5];

        static {
            String[] names = {"red_blue", "blue", "green", "yellow_blue", "grey"};
            for (int i = 0; i < 5; i++) {
                TEXTURES[i] = new ResourceLocation(DOMAIN, ROOT + "parrot/parrot_" + names[i] + ".png");
            }
        }

        ParrotRender() {
            super(new ParrotModel(), 0.3F);
        }

        @Override
        protected ResourceLocation getEntityTexture(Entity entity) {
            int variant = ((MoreMobs.Parrot) entity).variant();
            return TEXTURES[variant >= 0 && variant < 5 ? variant : 0];
        }
    }

    /** ModelLlama. */
    private static final class LlamaModel extends ModelBase {
        final ModelRenderer head;
        final ModelRenderer body;
        final ModelRenderer leg1;
        final ModelRenderer leg2;
        final ModelRenderer leg3;
        final ModelRenderer leg4;

        LlamaModel() {
            textureWidth = 128;
            textureHeight = 64;
            head = new ModelRenderer(this, 0, 0);
            head.addBox(-2.0F, -14.0F, -10.0F, 4, 4, 9);
            head.setRotationPoint(0.0F, 7.0F, -6.0F);
            head.setTextureOffset(0, 14).addBox(-4.0F, -16.0F, -6.0F, 8, 18, 6);
            head.setTextureOffset(17, 0).addBox(-4.0F, -19.0F, -4.0F, 3, 3, 2);
            head.setTextureOffset(17, 0).addBox(1.0F, -19.0F, -4.0F, 3, 3, 2);
            body = part(this, 29, 0, -6.0F, -10.0F, -7.0F, 12, 18, 10, 0.0F, 5.0F, 2.0F);
            leg1 = part(this, 29, 29, -2.0F, 0.0F, -2.0F, 4, 14, 4, -3.5F, 10.0F, 6.0F);
            leg2 = part(this, 29, 29, -2.0F, 0.0F, -2.0F, 4, 14, 4, 3.5F, 10.0F, 6.0F);
            leg3 = part(this, 29, 29, -2.0F, 0.0F, -2.0F, 4, 14, 4, -3.5F, 10.0F, -5.0F);
            leg4 = part(this, 29, 29, -2.0F, 0.0F, -2.0F, 4, 14, 4, 3.5F, 10.0F, -5.0F);
        }

        @Override
        public void render(Entity entity, float limb, float amount, float age, float yaw, float pitch, float scale) {
            setRotationAngles(limb, amount, age, yaw, pitch, scale, entity);
            if (isChild) {
                GL11.glPushMatrix();
                GL11.glScalef(0.71428573F, 0.64935064F, 0.7936508F);
                GL11.glTranslatef(0.0F, 21.0F * scale, 0.22F);
                head.render(scale);
                GL11.glPopMatrix();
                GL11.glPushMatrix();
                GL11.glScalef(0.625F, 0.45454544F, 0.45454544F);
                GL11.glTranslatef(0.0F, 33.0F * scale, 0.0F);
                body.render(scale);
                GL11.glPopMatrix();
                GL11.glPushMatrix();
                GL11.glScalef(0.45454544F, 0.41322312F, 0.45454544F);
                GL11.glTranslatef(0.0F, 33.0F * scale, 0.0F);
                leg1.render(scale);
                leg2.render(scale);
                leg3.render(scale);
                leg4.render(scale);
                GL11.glPopMatrix();
            } else {
                head.render(scale);
                body.render(scale);
                leg1.render(scale);
                leg2.render(scale);
                leg3.render(scale);
                leg4.render(scale);
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
        }
    }

    private static final class LlamaRender extends RenderLiving {
        private static final ResourceLocation[] TEXTURES = new ResourceLocation[4];

        static {
            String[] names = {"creamy", "white", "brown", "gray"};
            for (int i = 0; i < 4; i++) {
                TEXTURES[i] = new ResourceLocation(DOMAIN, ROOT + "llama/llama_" + names[i] + ".png");
            }
        }

        LlamaRender() {
            super(new LlamaModel(), 0.7F);
        }

        @Override
        protected ResourceLocation getEntityTexture(Entity entity) {
            int variant = ((MoreMobs.Llama) entity).variant();
            return TEXTURES[variant >= 0 && variant < 4 ? variant : 0];
        }
    }

    /** ModelShulker. */
    private static final class ShulkerModel extends ModelBase {
        final ModelRenderer lid;
        final ModelRenderer base;
        final ModelRenderer head;

        ShulkerModel() {
            textureWidth = 64;
            textureHeight = 64;
            lid = new ModelRenderer(this);
            lid.setTextureOffset(0, 0).addBox(-8.0F, -16.0F, -8.0F, 16, 12, 16);
            lid.setRotationPoint(0.0F, 24.0F, 0.0F);
            base = new ModelRenderer(this);
            base.setTextureOffset(0, 28).addBox(-8.0F, -8.0F, -8.0F, 16, 8, 16);
            base.setRotationPoint(0.0F, 24.0F, 0.0F);
            head = new ModelRenderer(this);
            head.setTextureOffset(0, 52).addBox(-3.0F, 0.0F, -3.0F, 6, 6, 6);
            head.setRotationPoint(0.0F, 12.0F, 0.0F);
        }

        @Override
        public void render(Entity entity, float limb, float amount, float age, float yaw, float pitch, float scale) {
            setRotationAngles(limb, amount, age, yaw, pitch, scale, entity);
            base.render(scale);
            lid.render(scale);
            head.render(scale);
        }

        @Override
        public void setRotationAngles(float limb, float amount, float age, float yaw, float pitch, float scale, Entity entity) {
            float peek = ((MoreMobs.Shulker) entity).peek(age - (float) entity.ticksExisted);
            float angle = (0.5F + peek) * PI;
            float wave = -1.0F + MathHelper.sin(angle);
            float bob = angle > PI ? MathHelper.sin(age * 0.1F) * 0.7F : 0.0F;
            lid.setRotationPoint(0.0F, 16.0F + MathHelper.sin(angle) * 8.0F + bob, 0.0F);
            lid.rotateAngleY = peek > 0.3F ? wave * wave * wave * wave * PI * 0.125F : 0.0F;
            head.rotateAngleX = pitch * 0.017453292F;
            head.rotateAngleY = yaw * 0.017453292F;
        }
    }

    private static final class ShulkerRender extends RenderLiving {
        private static final ResourceLocation TEXTURE = new ResourceLocation(DOMAIN, ROOT + "shulker/shulker_purple.png");

        ShulkerRender() {
            super(new ShulkerModel(), 0.0F);
        }

        @Override
        protected ResourceLocation getEntityTexture(Entity entity) {
            return TEXTURE;
        }
    }

    /** Spit and shulker bullets: a small textured cube. */
    private static final class Small extends Render {
        private final ResourceLocation texture;
        private final float size;
        private final ModelBase model = new ModelBase() {
        };
        private final ModelRenderer cube;

        Small(String path, float size) {
            texture = new ResourceLocation(DOMAIN, path);
            this.size = size;
            model.textureWidth = 64;
            model.textureHeight = 32;
            cube = new ModelRenderer(model, 0, 0);
            cube.addBox(-4.0F, -4.0F, -4.0F, 8, 8, 8);
        }

        @Override
        public void doRender(Entity entity, double x, double y, double z, float yaw, float partial) {
            GL11.glPushMatrix();
            GL11.glTranslatef((float) x, (float) y + 0.15F, (float) z);
            GL11.glScalef(size * 16.0F, size * 16.0F, size * 16.0F);
            GL11.glRotatef(((float) entity.ticksExisted + partial) * 15.0F, 0.0F, 1.0F, 0.0F);
            bindTexture(texture);
            cube.render(0.0625F * 0.25F);
            GL11.glPopMatrix();
        }

        @Override
        protected ResourceLocation getEntityTexture(Entity entity) {
            return texture;
        }
    }

}
