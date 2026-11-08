package ru.codex.offhand1710.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer;
import org.lwjgl.opengl.GL11;

/** ModelDragonHead of 1.9 with the dragon's own texture, for the block and the item. */
final class DragonHeadRenderer extends TileEntitySpecialRenderer implements IItemRenderer {
    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/entity/enderdragon/dragon.png");
    private static final float[] YAW = {0.0F, 0.0F, 0.0F, 180.0F, 90.0F, 270.0F};

    private static final class Model extends ModelBase {
        final ModelRenderer head;
        final ModelRenderer jaw;

        Model() {
            textureWidth = 256;
            textureHeight = 256;
            setTextureOffset("head.upperlip", 176, 44);
            setTextureOffset("head.upperhead", 112, 30);
            setTextureOffset("head.scale", 0, 0);
            setTextureOffset("head.nostril", 112, 0);
            setTextureOffset("jaw.jaw", 176, 65);
            float offset = -16.0F;
            head = new ModelRenderer(this, "head");
            head.addBox("upperlip", -6.0F, -1.0F, -8.0F + offset, 12, 5, 16);
            head.addBox("upperhead", -8.0F, -8.0F, 6.0F + offset, 16, 16, 16);
            head.mirror = true;
            head.addBox("scale", -5.0F, -12.0F, 12.0F + offset, 2, 4, 6);
            head.addBox("nostril", -5.0F, -3.0F, -6.0F + offset, 2, 2, 4);
            head.mirror = false;
            head.addBox("scale", 3.0F, -12.0F, 12.0F + offset, 2, 4, 6);
            head.addBox("nostril", 3.0F, -3.0F, -6.0F + offset, 2, 2, 4);
            jaw = new ModelRenderer(this, "jaw");
            jaw.setRotationPoint(0.0F, 4.0F, 8.0F + offset);
            jaw.addBox("jaw", -6.0F, 0.0F, -16.0F, 12, 4, 16);
            jaw.rotateAngleX = 0.2F;
            head.addChild(jaw);
        }
    }

    private final Model model = new Model();

    private void draw() {
        Minecraft.getMinecraft().getTextureManager().bindTexture(TEXTURE);
        GL11.glScalef(-0.75F, -0.75F, 0.75F);
        GL11.glTranslatef(0.0F, -0.375F, 0.0F);
        model.head.render(0.0625F);
    }

    @Override
    public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partial) {
        int meta = tile.getWorldObj() == null ? 2 : tile.getBlockMetadata();
        GL11.glPushMatrix();
        GL11.glTranslatef((float) x + 0.5F, (float) y, (float) z + 0.5F);
        GL11.glRotatef(YAW[meta > 5 ? 2 : meta], 0.0F, 1.0F, 0.0F);
        GL11.glDisable(GL11.GL_CULL_FACE);
        draw();
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glPopMatrix();
    }

    @Override
    public boolean handleRenderType(ItemStack stack, ItemRenderType type) {
        return true;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack stack, ItemRendererHelper helper) {
        return true;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack stack, Object... data) {
        GL11.glPushMatrix();
        if (type == ItemRenderType.INVENTORY) {
            GL11.glTranslatef(0.0F, -0.3F, 0.0F);
        } else if (type != ItemRenderType.ENTITY) {
            GL11.glTranslatef(0.5F, 0.5F, 0.5F);
        }
        GL11.glScalef(0.8F, 0.8F, 0.8F);
        GL11.glDisable(GL11.GL_CULL_FACE);
        draw();
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glPopMatrix();
    }
}
