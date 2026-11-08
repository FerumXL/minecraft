package ru.codex.offhand1710.client;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.client.registry.RenderingRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;
import org.lwjgl.opengl.GL11;
import ru.codex.offhand1710.BlockShulkerBox;
import ru.codex.offhand1710.EntityArmorStand112;
import ru.codex.offhand1710.Items112;

/** Renderers and the screen of shulker boxes and armor stands. */
final class Items112Client {
    private static final String DOMAIN = "mc112bridge";

    private Items112Client() {
    }

    static void register() {
        BoxRenderer box = new BoxRenderer();
        ClientRegistry.bindTileEntitySpecialRenderer(BlockShulkerBox.Tile.class, box);
        for (BlockShulkerBox block : Items112.BOXES) {
            MinecraftForgeClient.registerItemRenderer(Item.getItemFromBlock(block), box);
        }
        RenderingRegistry.registerEntityRenderingHandler(EntityArmorStand112.class, new StandRenderer());
        RenderingRegistry.registerEntityRenderingHandler(ru.codex.offhand1710.Content119.EntityEndermite.class, new MiteRenderer());
    }

    static Object boxScreen(InventoryPlayer player, IInventory box) {
        return new BoxScreen(player, box);
    }

    private static final class BoxScreen extends GuiContainer {
        private static final ResourceLocation TEXTURE = new ResourceLocation(DOMAIN, "textures/gui/container/shulker_box.png");
        private final IInventory box;
        private final InventoryPlayer player;

        BoxScreen(InventoryPlayer player, IInventory box) {
            super(new BlockShulkerBox.BoxContainer(player, box));
            this.box = box;
            this.player = player;
        }

        @Override
        protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
            fontRendererObj.drawString(box.getInventoryName(), 8, 6, 4210752);
            fontRendererObj.drawString(StatCollector.translateToLocal(player.getInventoryName()), 8, 72, 4210752);
        }

        @Override
        protected void drawGuiContainerBackgroundLayer(float partial, int mouseX, int mouseY) {
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            mc.getTextureManager().bindTexture(TEXTURE);
            drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
        }
    }

    private static final class BoxModel extends ModelBase {
        final ModelRenderer base;
        final ModelRenderer lid;

        BoxModel() {
            textureWidth = 64;
            textureHeight = 64;
            lid = new ModelRenderer(this);
            lid.setTextureOffset(0, 0).addBox(-8.0F, -16.0F, -8.0F, 16, 12, 16);
            lid.setRotationPoint(0.0F, 24.0F, 0.0F);
            base = new ModelRenderer(this);
            base.setTextureOffset(0, 28).addBox(-8.0F, -8.0F, -8.0F, 16, 8, 16);
            base.setRotationPoint(0.0F, 24.0F, 0.0F);
        }
    }

    /** TileEntityShulkerBoxRenderer: the shulker model with the lid sliding and turning open. */
    private static final class BoxRenderer extends TileEntitySpecialRenderer implements IItemRenderer {
        private final BoxModel model = new BoxModel();

        private void draw(int color, float progress) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(new ResourceLocation(DOMAIN, "textures/entity/shulker/shulker_" + Items112.textureColor(color) + ".png"));
            model.lid.setRotationPoint(0.0F, 24.0F - progress * 0.5F * 16.0F, 0.0F);
            model.lid.rotateAngleY = 270.0F * progress * 0.017453292F;
            // The lid has a cut-out in its sides: without the alpha test it shows as a black slit.
            GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
            model.base.render(0.0625F);
            model.lid.render(0.0625F);
            GL11.glPopAttrib();
        }

        @Override
        public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partial) {
            int meta = tile.getWorldObj() == null ? 1 : tile.getBlockMetadata();
            int color = tile.getBlockType() instanceof BlockShulkerBox ? ((BlockShulkerBox) tile.getBlockType()).color : 10;
            GL11.glPushMatrix();
            GL11.glTranslatef((float) x + 0.5F, (float) y + 0.5F, (float) z + 0.5F);
            switch (meta) {
                case 0:
                    GL11.glRotatef(180.0F, 1.0F, 0.0F, 0.0F);
                    break;
                case 2:
                    GL11.glRotatef(-90.0F, 1.0F, 0.0F, 0.0F);
                    break;
                case 3:
                    GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
                    break;
                case 4:
                    GL11.glRotatef(90.0F, 0.0F, 0.0F, 1.0F);
                    break;
                case 5:
                    GL11.glRotatef(-90.0F, 0.0F, 0.0F, 1.0F);
                    break;
                default:
                    break;
            }
            GL11.glTranslatef(0.0F, 1.0F, 0.0F);
            GL11.glScalef(1.0F, -1.0F, -1.0F);
            GL11.glDisable(GL11.GL_CULL_FACE);
            draw(color, ((BlockShulkerBox.Tile) tile).progress(partial));
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
            int color = ((BlockShulkerBox) net.minecraft.block.Block.getBlockFromItem(stack.getItem())).color;
            GL11.glPushMatrix();
            if (type == ItemRenderType.EQUIPPED || type == ItemRenderType.EQUIPPED_FIRST_PERSON) {
                GL11.glTranslatef(0.5F, 0.5F, 0.5F);
            }
            GL11.glTranslatef(0.0F, 1.0F, 0.0F);
            GL11.glScalef(1.0F, -1.0F, -1.0F);
            GL11.glDisable(GL11.GL_CULL_FACE);
            draw(color, 0.0F);
            GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glPopMatrix();
        }
    }

    /** ModelEnderMite, reconstructed from memory of 1.8. */
    private static final class MiteModel extends ModelBase {
        private static final int[][] SIZE = {{4, 3, 2}, {6, 4, 5}, {3, 3, 1}, {1, 2, 1}};
        private static final int[][] UV = {{0, 0}, {0, 5}, {0, 14}, {0, 18}};
        private final ModelRenderer[] parts = new ModelRenderer[4];

        MiteModel() {
            float z = -3.5F;
            for (int i = 0; i < 4; i++) {
                parts[i] = new ModelRenderer(this, UV[i][0], UV[i][1]);
                parts[i].addBox((float) SIZE[i][0] * -0.5F, 0.0F, (float) SIZE[i][2] * -0.5F, SIZE[i][0], SIZE[i][1], SIZE[i][2]);
                parts[i].setRotationPoint(0.0F, (float) (24 - SIZE[i][1]), z);
                if (i < 3) {
                    z += (float) (SIZE[i][2] + SIZE[i + 1][2]) * 0.5F;
                }
            }
        }

        @Override
        public void render(Entity entity, float limb, float amount, float age, float yaw, float pitch, float scale) {
            setRotationAngles(limb, amount, age, yaw, pitch, scale, entity);
            for (ModelRenderer part : parts) {
                part.render(scale);
            }
        }

        @Override
        public void setRotationAngles(float limb, float amount, float age, float yaw, float pitch, float scale, Entity entity) {
            for (int i = 0; i < 4; i++) {
                float wave = age * 0.9F + (float) i * 0.15F * (float) Math.PI;
                parts[i].rotateAngleY = (float) Math.cos(wave) * (float) Math.PI * 0.01F * (float) (1 + Math.abs(i - 2));
                parts[i].rotationPointX = (float) Math.sin(wave) * (float) Math.PI * 0.1F * (float) Math.abs(i - 2);
            }
        }
    }

    private static final class MiteRenderer extends net.minecraft.client.renderer.entity.RenderLiving {
        private static final ResourceLocation TEXTURE = new ResourceLocation(DOMAIN, "textures/entity/endermite.png");

        MiteRenderer() {
            super(new MiteModel(), 0.3F);
        }

        @Override
        protected ResourceLocation getEntityTexture(Entity entity) {
            return TEXTURE;
        }

        @Override
        protected float getDeathMaxRotation(net.minecraft.entity.EntityLivingBase entity) {
            return 180.0F;
        }
    }

    /** ModelArmorStand. */
    private static final class StandModel extends ModelBiped {
        private final ModelRenderer sideRight;
        private final ModelRenderer sideLeft;
        private final ModelRenderer hips;
        private final ModelRenderer plate;

        StandModel() {
            super(0.0F, 0.0F, 64, 64);
            bipedHead = part(0, 0, -1.0F, -7.0F, -1.0F, 2, 7, 2, 0.0F, 0.0F, 0.0F, false);
            bipedBody = part(0, 26, -6.0F, 0.0F, -1.5F, 12, 3, 3, 0.0F, 0.0F, 0.0F, false);
            bipedRightArm = part(24, 0, -2.0F, -2.0F, -1.0F, 2, 12, 2, -5.0F, 2.0F, 0.0F, false);
            bipedLeftArm = part(32, 16, 0.0F, -2.0F, -1.0F, 2, 12, 2, 5.0F, 2.0F, 0.0F, true);
            bipedRightLeg = part(8, 0, -1.0F, 0.0F, -1.0F, 2, 11, 2, -1.9F, 12.0F, 0.0F, false);
            bipedLeftLeg = part(40, 16, -1.0F, 0.0F, -1.0F, 2, 11, 2, 1.9F, 12.0F, 0.0F, true);
            sideRight = part(16, 0, -3.0F, 3.0F, -1.0F, 2, 7, 2, 0.0F, 0.0F, 0.0F, false);
            sideLeft = part(48, 16, 1.0F, 3.0F, -1.0F, 2, 7, 2, 0.0F, 0.0F, 0.0F, false);
            hips = part(0, 48, -4.0F, 10.0F, -1.0F, 8, 2, 2, 0.0F, 0.0F, 0.0F, false);
            plate = part(0, 32, -6.0F, 11.0F, -6.0F, 12, 1, 12, 0.0F, 12.0F, 0.0F, false);
            bipedHeadwear.showModel = false;
        }

        private ModelRenderer part(int u, int v, float x, float y, float z, int w, int h, int d, float px, float py, float pz, boolean mirror) {
            ModelRenderer part = new ModelRenderer(this, u, v);
            part.mirror = mirror;
            part.addBox(x, y, z, w, h, d);
            part.setRotationPoint(px, py, pz);
            return part;
        }

        private static void pose(ModelRenderer part, EntityArmorStand112 stand, int index) {
            part.rotateAngleX = 0.017453292F * stand.pose(index, 0);
            part.rotateAngleY = 0.017453292F * stand.pose(index, 1);
            part.rotateAngleZ = 0.017453292F * stand.pose(index, 2);
        }

        @Override
        public void setRotationAngles(float limb, float amount, float age, float yaw, float pitch, float scale, Entity entity) {
            EntityArmorStand112 stand = (EntityArmorStand112) entity;
            pose(bipedHead, stand, 0);
            pose(bipedBody, stand, 1);
            pose(bipedLeftArm, stand, 2);
            pose(bipedRightArm, stand, 3);
            pose(bipedLeftLeg, stand, 4);
            pose(bipedRightLeg, stand, 5);
            pose(sideRight, stand, 1);
            pose(sideLeft, stand, 1);
            pose(hips, stand, 1);
            bipedLeftArm.showModel = stand.showArms();
            bipedRightArm.showModel = stand.showArms();
            plate.showModel = stand.showPlate();
            plate.rotateAngleY = -0.017453292F * entity.rotationYaw;
        }

        @Override
        public void render(Entity entity, float limb, float amount, float age, float yaw, float pitch, float scale) {
            super.render(entity, limb, amount, age, yaw, pitch, scale);
            sideRight.render(scale);
            sideLeft.render(scale);
            hips.render(scale);
            plate.render(scale);
        }
    }

    private static final class StandRenderer extends RenderBiped {
        private static final ResourceLocation TEXTURE = new ResourceLocation(DOMAIN, "textures/entity/armorstand/wood.png");

        StandRenderer() {
            super(new StandModel(), 0.0F);
        }

        @Override
        protected ResourceLocation getEntityTexture(Entity entity) {
            return TEXTURE;
        }

        @Override
        protected boolean func_110813_b(net.minecraft.entity.EntityLivingBase entity) {
            return false;
        }
    }
}
