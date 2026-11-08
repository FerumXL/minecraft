package ru.codex.offhand1710.client;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderPlayerEvent;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import ru.codex.offhand1710.ItemElytra;
import ru.codex.offhand1710.OffhandInventory;

/** LayerElytra and ModelElytra of 1.12.2 and the body rotation of RenderPlayer.applyRotations while gliding. */
public final class ElytraRenderer {
    public static final ModelBiped EMPTY_ARMOR = new ModelBiped() {
        @Override
        public void render(Entity entity, float a, float b, float c, float d, float e, float f) {
        }
    };
    private static final ResourceLocation TEXTURE = new ResourceLocation("mc112bridge", "textures/entity/elytra.png");
    private static final Wings WINGS = new Wings();
    private boolean rotated;

    private static final class Wings extends ModelBase {
        final ModelRenderer left = new ModelRenderer(this, 22, 0);
        final ModelRenderer right = new ModelRenderer(this, 22, 0);

        Wings() {
            left.addBox(-10.0F, 0.0F, 0.0F, 10, 20, 2, 1.0F);
            right.mirror = true;
            right.addBox(0.0F, 0.0F, 0.0F, 10, 20, 2, 1.0F);
        }
    }

    private static boolean wearsElytra(EntityPlayer player) {
        ItemStack chest = player.inventory.armorInventory[2];
        return chest != null && chest.getItem() instanceof ItemElytra;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPre(RenderPlayerEvent.Pre event) {
        EntityPlayer player = event.entityPlayer;
        OffhandInventory state = OffhandInventory.get(player);
        rotated = false;
        if (!state.elytraFlying || RenderManager.instance.playerViewY == 180.0F) {
            return; // not in the inventory preview, it would turn the small figure over
        }
        float partial = event.partialRenderTick;
        double x = player.lastTickPosX + (player.posX - player.lastTickPosX) * (double) partial - RenderManager.renderPosX;
        double y = player.lastTickPosY + (player.posY - player.lastTickPosY) * (double) partial - RenderManager.renderPosY
                - (double) player.yOffset;
        double z = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double) partial - RenderManager.renderPosZ;
        float yaw = player.prevRenderYawOffset + (player.renderYawOffset - player.prevRenderYawOffset) * partial;
        float time = (float) state.elytraTicks + partial;
        float blend = MathHelper.clamp_float(time * time / 100.0F, 0.0F, 1.0F);

        GL11.glPushMatrix();
        rotated = true;
        GL11.glTranslated(x, y, z);
        GL11.glRotatef(180.0F - yaw, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(blend * (-90.0F - player.rotationPitch), 1.0F, 0.0F, 0.0F);
        Vec3 look = player.getLook(partial);
        double motionX = player.posX - player.prevPosX;
        double motionZ = player.posZ - player.prevPosZ;
        double motion = motionX * motionX + motionZ * motionZ;
        double view = look.xCoord * look.xCoord + look.zCoord * look.zCoord;
        if (motion > 0.0D && view > 0.0D) {
            double cosine = (motionX * look.xCoord + motionZ * look.zCoord) / (Math.sqrt(motion) * Math.sqrt(view));
            double side = motionX * look.zCoord - motionZ * look.xCoord;
            GL11.glRotatef((float) (Math.signum(side) * Math.acos(Math.min(1.0D, Math.max(-1.0D, cosine)))) * 180.0F / (float) Math.PI,
                    0.0F, 1.0F, 0.0F);
        }
        GL11.glRotatef(-(180.0F - yaw), 0.0F, 1.0F, 0.0F);
        GL11.glTranslated(-x, -y, -z);
    }

    @SubscribeEvent
    public void onPost(RenderPlayerEvent.Post event) {
        if (rotated) {
            rotated = false;
            GL11.glPopMatrix();
        }
    }

    /** 1.12.2 does not draw a cape over the elytra. */
    @SubscribeEvent
    public void onSpecialsPre(RenderPlayerEvent.Specials.Pre event) {
        if (wearsElytra(event.entityPlayer)) {
            event.renderCape = false;
        }
    }

    @SubscribeEvent
    public void onSpecialsPost(RenderPlayerEvent.Specials.Post event) {
        EntityPlayer player = event.entityPlayer;
        if (!wearsElytra(player)) {
            return;
        }
        pose(player, OffhandInventory.get(player));
        Minecraft.getMinecraft().getTextureManager().bindTexture(TEXTURE);
        GL11.glPushMatrix();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        GL11.glTranslatef(0.0F, 0.0F, 0.125F);
        WINGS.left.render(0.0625F);
        WINGS.right.render(0.0625F);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    /** ModelElytra.setRotationAngles of 1.12.2. */
    private static void pose(EntityPlayer player, OffhandInventory state) {
        float angleX = 0.2617994F;
        float angleZ = -0.2617994F;
        float pointY = 0.0F;
        float angleY = 0.0F;
        if (state.elytraFlying) {
            float spread = 1.0F;
            double dx = player.posX - player.prevPosX;
            double dy = player.posY - player.prevPosY;
            double dz = player.posZ - player.prevPosZ;
            if (dy < 0.0D) {
                double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
                spread = 1.0F - (float) Math.pow(-dy / length, 1.5D);
            }
            angleX = spread * 0.34906584F + (1.0F - spread) * 0.2617994F;
            angleZ = spread * -1.5707964F + (1.0F - spread) * -0.2617994F;
        } else if (player.isSneaking()) {
            angleX = 0.6981317F;
            angleZ = -0.7853982F;
            pointY = 3.0F;
            angleY = 0.08726646F;
        }
        state.wingX += (angleX - state.wingX) * 0.1F;
        state.wingY += (angleY - state.wingY) * 0.1F;
        state.wingZ += (angleZ - state.wingZ) * 0.1F;
        ModelRenderer left = WINGS.left;
        ModelRenderer right = WINGS.right;
        left.rotationPointX = 5.0F;
        left.rotationPointY = pointY;
        left.rotateAngleX = state.wingX;
        left.rotateAngleY = state.wingY;
        left.rotateAngleZ = state.wingZ;
        right.rotationPointX = -left.rotationPointX;
        right.rotationPointY = left.rotationPointY;
        right.rotateAngleX = left.rotateAngleX;
        right.rotateAngleY = -left.rotateAngleY;
        right.rotateAngleZ = -left.rotateAngleZ;
    }
}
