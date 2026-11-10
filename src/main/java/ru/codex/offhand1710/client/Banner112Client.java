package ru.codex.offhand1710.client;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.client.registry.RenderingRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import ru.codex.offhand1710.Banner112;

/** ModelBoat of 1.9 for every boat, ModelBanner and the layered banner textures. */
final class Banner112Client {
    private static final String DOMAIN = "mc112bridge";

    private Banner112Client() {
    }

    static void register() {
        BoatRenderer boats = new BoatRenderer();
        RenderingRegistry.registerEntityRenderingHandler(EntityBoat.class, boats);
        RenderingRegistry.registerEntityRenderingHandler(Banner112.EntityBoat112.class, boats);
        RenderingRegistry.registerEntityRenderingHandler(ru.codex.offhand1710.Content120.EntitySpectralArrow.class, new net.minecraft.client.renderer.entity.RenderArrow() {
            private final ResourceLocation texture = new ResourceLocation(DOMAIN, "textures/entity/projectiles/spectral_arrow.png");

            @Override
            protected ResourceLocation getEntityTexture(net.minecraft.entity.projectile.EntityArrow arrow) {
                return texture;
            }
        });
        RenderingRegistry.registerEntityRenderingHandler(ru.codex.offhand1710.Brew112.TippedArrow.class, new net.minecraft.client.renderer.entity.RenderArrow() {
            private final ResourceLocation texture = new ResourceLocation(DOMAIN, "textures/entity/projectiles/tipped_arrow.png");

            @Override
            protected ResourceLocation getEntityTexture(net.minecraft.entity.projectile.EntityArrow arrow) {
                return texture;
            }
        });
        RenderingRegistry.registerEntityRenderingHandler(ru.codex.offhand1710.Brew112.ThrownLingering.class, new net.minecraft.client.renderer.entity.RenderSnowball(ru.codex.offhand1710.Brew112.lingering, 16384));
        RenderingRegistry.registerEntityRenderingHandler(ru.codex.offhand1710.Brew112.DragonFireball.class, new net.minecraft.client.renderer.entity.RenderSnowball(ru.codex.offhand1710.Brew112.dragonBreath));
        RenderingRegistry.registerEntityRenderingHandler(ru.codex.offhand1710.Brew112.Cloud.class, new Render() {
            @Override
            public void doRender(Entity entity, double x, double y, double z, float yaw, float partial) {
            }

            @Override
            protected ResourceLocation getEntityTexture(Entity entity) {
                return null;
            }
        });
        BannerRenderer banners = new BannerRenderer();
        ClientRegistry.bindTileEntitySpecialRenderer(Banner112.BannerTile.class, banners);
        MinecraftForgeClient.registerItemRenderer(Banner112.banner, banners);
    }

    private static final int MAX_CACHED = 256;
    private static final ResourceLocation FALLBACK = new ResourceLocation(DOMAIN, "textures/entity/banner/base.png");
    private static final Map<String, ResourceLocation> CACHE = new HashMap<String, ResourceLocation>();

    private static BufferedImage read(String path) throws IOException {
        InputStream in = Minecraft.getMinecraft().getResourceManager().getResource(new ResourceLocation(DOMAIN, path)).getInputStream();
        try {
            return ImageIO.read(in);
        } finally {
            in.close();
        }
    }

    private static void layer(BufferedImage result, BufferedImage base, String mask, int color) throws IOException {
        BufferedImage image = read(mask);
        BufferedImage painted = new BufferedImage(result.getWidth(), result.getHeight(), BufferedImage.TYPE_INT_ARGB);
        int cr = color >> 16 & 255;
        int cg = color >> 8 & 255;
        int cb = color & 255;
        for (int y = 0; y < result.getHeight(); y++) {
            for (int x = 0; x < result.getWidth(); x++) {
                int m = image.getRGB(x, y);
                if ((m & 0xFF000000) != 0) {
                    int b = base.getRGB(x, y);
                    painted.setRGB(x, y, (m & 0xFF0000) << 8 | ((b >> 16 & 255) * cr / 255) << 16 | ((b >> 8 & 255) * cg / 255) << 8 | (b & 255) * cb / 255);
                }
            }
        }
        Graphics g = result.getGraphics();
        g.drawImage(painted, 0, 0, null);
        g.dispose();
    }

    /** BannerTextures with LayeredColorMaskTexture, for banners and shields. */
    static ResourceLocation layered(String basePath, String maskDir, int baseColor, NBTTagList patterns) {
        StringBuilder key = new StringBuilder(basePath).append(baseColor);
        int count = patterns == null ? 0 : Math.min(patterns.tagCount(), Banner112.MAX_PATTERNS);
        for (int i = 0; i < count; i++) {
            NBTTagCompound entry = patterns.getCompoundTagAt(i);
            key.append(entry.getString("Pattern")).append(entry.getInteger("Color") & 15).append(';');
        }
        String name = key.toString();
        ResourceLocation found = CACHE.get(name);
        if (found != null) {
            return found;
        }
        if (CACHE.size() >= MAX_CACHED) {
            return FALLBACK;
        }
        ResourceLocation location = FALLBACK;
        try {
            BufferedImage base = read(basePath);
            BufferedImage result = new BufferedImage(base.getWidth(), base.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics g = result.getGraphics();
            g.drawImage(base, 0, 0, null);
            g.dispose();
            layer(result, base, maskDir + "base.png", Banner112.COLOR_VALUES[baseColor & 15]);
            for (int i = 0; i < count; i++) {
                NBTTagCompound entry = patterns.getCompoundTagAt(i);
                Banner112.Pattern pattern = Banner112.byCode(entry.getString("Pattern"));
                if (pattern != null) {
                    layer(result, base, maskDir + pattern.name + ".png", Banner112.COLOR_VALUES[entry.getInteger("Color") & 15]);
                }
            }
            location = new ResourceLocation(DOMAIN, "layered/" + CACHE.size());
            Minecraft.getMinecraft().getTextureManager().loadTexture(location, new DynamicTexture(result));
        } catch (IOException e) {
            location = FALLBACK;
        }
        CACHE.put(name, location);
        return location;
    }

    /** bpj. */    private static final class BoatModel extends ModelBase {
        final ModelRenderer[] hull = new ModelRenderer[5];
        final ModelRenderer[] paddles = new ModelRenderer[2];
        final ModelRenderer mask;

        BoatModel() {
            textureWidth = 128;
            textureHeight = 64;
            hull[0] = box(0, 0, -14.0F, -9.0F, -3.0F, 28, 16, 3, 0.0F, 3.0F, 1.0F);
            hull[1] = box(0, 19, -13.0F, -7.0F, -1.0F, 18, 6, 2, -15.0F, 4.0F, 4.0F);
            hull[2] = box(0, 27, -8.0F, -7.0F, -1.0F, 16, 6, 2, 15.0F, 4.0F, 0.0F);
            hull[3] = box(0, 35, -14.0F, -7.0F, -1.0F, 28, 6, 2, 0.0F, 4.0F, -9.0F);
            hull[4] = box(0, 43, -14.0F, -7.0F, -1.0F, 28, 6, 2, 0.0F, 4.0F, 9.0F);
            hull[0].rotateAngleX = 1.5707964F;
            hull[1].rotateAngleY = 4.712389F;
            hull[2].rotateAngleY = 1.5707964F;
            hull[3].rotateAngleY = 3.1415927F;
            paddles[0] = paddle(true);
            paddles[0].setRotationPoint(3.0F, -5.0F, 9.0F);
            paddles[1] = paddle(false);
            paddles[1].setRotationPoint(3.0F, -5.0F, -9.0F);
            paddles[1].rotateAngleY = 3.1415927F;
            paddles[0].rotateAngleZ = 0.19634955F;
            paddles[1].rotateAngleZ = 0.19634955F;
            mask = box(0, 0, -14.0F, -9.0F, -3.0F, 28, 16, 3, 0.0F, -3.0F, 1.0F);
            mask.rotateAngleX = 1.5707964F;
        }

        private ModelRenderer box(int u, int v, float x, float y, float z, int w, int h, int d, float px, float py, float pz) {
            ModelRenderer part = new ModelRenderer(this, u, v).setTextureSize(128, 64);
            part.addBox(x, y, z, w, h, d);
            part.setRotationPoint(px, py, pz);
            return part;
        }

        private ModelRenderer paddle(boolean left) {
            ModelRenderer part = new ModelRenderer(this, 62, left ? 0 : 20).setTextureSize(128, 64);
            part.addBox(-1.0F, 0.0F, -5.0F, 2, 2, 18);
            part.addBox(left ? -1.001F : 0.001F, -3.0F, 8.0F, 1, 6, 7);
            return part;
        }

        void renderPaddle(int side, float phase, float scale) {
            ModelRenderer part = paddles[side];
            part.rotateAngleX = lerp(-1.0471975803375244D, -0.2617993950843811D, (MathHelper.sin(-phase) + 1.0F) / 2.0F);
            part.rotateAngleY = lerp(-0.7853981852531433D, 0.7853981852531433D, (MathHelper.sin(-phase + 1.0F) + 1.0F) / 2.0F);
            if (side == 1) {
                part.rotateAngleY = 3.1415927F - part.rotateAngleY;
            }
            part.render(scale);
        }

        private static float lerp(double a, double b, float t) {
            return (float) (a + (b - a) * (double) t);
        }
    }

    private static final class BoatRenderer extends Render {
        private static final String[] NAMES = {"oak", "spruce", "birch", "jungle", "acacia", "darkoak"};
        private final BoatModel model = new BoatModel();

        BoatRenderer() {
            shadowSize = 0.5F;
        }

        @Override
        public void doRender(Entity entity, double x, double y, double z, float yaw, float partial) {
            EntityBoat boat = (EntityBoat) entity;
            GL11.glPushMatrix();
            // In 1.7.10 the position of a boat is the centre of its box, 0.3 above its bottom.
            GL11.glTranslatef((float) x, (float) y + 0.075F, (float) z);
            GL11.glRotatef(180.0F - yaw, 0.0F, 1.0F, 0.0F);
            float hit = (float) boat.getTimeSinceHit() - partial;
            float damage = Math.max(0.0F, boat.getDamageTaken() - partial);
            if (hit > 0.0F) {
                GL11.glRotatef(MathHelper.sin(hit) * hit * damage / 10.0F * (float) boat.getForwardDirection(), 1.0F, 0.0F, 0.0F);
            }
            GL11.glScalef(-1.0F, -1.0F, 1.0F);
            bindEntityTexture(entity);
            for (ModelRenderer part : model.hull) {
                part.render(0.0625F);
            }
            double dx = boat.posX - boat.prevPosX;
            double dz = boat.posZ - boat.prevPosZ;
            float phase = dx * dx + dz * dz > 1.0E-4D ? ((float) boat.ticksExisted + partial) * 0.3926991F : 0.0F;
            model.renderPaddle(0, phase, 0.0625F);
            model.renderPaddle(1, phase, 0.0625F);
            // Keeps the water out of the hull: writes depth only, as ModelBoat.renderMultipass does.
            GL11.glColorMask(false, false, false, false);
            model.mask.render(0.0625F);
            GL11.glColorMask(true, true, true, true);
            GL11.glPopMatrix();
        }

        @Override
        protected ResourceLocation getEntityTexture(Entity entity) {
            int wood = entity instanceof Banner112.EntityBoat112 ? ((Banner112.EntityBoat112) entity).wood() : 0;
            return new ResourceLocation(DOMAIN, "textures/entity/boat/boat_" + NAMES[wood] + ".png");
        }
    }

    /** bpf. */
    private static final class BannerModel extends ModelBase {
        final ModelRenderer slate;
        final ModelRenderer stand;
        final ModelRenderer top;

        BannerModel() {
            textureWidth = 64;
            textureHeight = 64;
            slate = new ModelRenderer(this, 0, 0);
            slate.addBox(-10.0F, 0.0F, -2.0F, 20, 40, 1);
            stand = new ModelRenderer(this, 44, 0);
            stand.addBox(-1.0F, -30.0F, -1.0F, 2, 42, 2);
            top = new ModelRenderer(this, 0, 42);
            top.addBox(-10.0F, -32.0F, -1.0F, 20, 2, 2);
        }

        void render() {
            slate.rotationPointY = -32.0F;
            slate.render(0.0625F);
            stand.render(0.0625F);
            top.render(0.0625F);
        }
    }

    /** TileEntityBannerRenderer with BannerTextures and LayeredColorMaskTexture. */
    private static final class BannerRenderer extends TileEntitySpecialRenderer implements IItemRenderer {
        private final BannerModel model = new BannerModel();
                private final float scale = 0.6666667F;

        private void drawModel(ResourceLocation texture) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
            GL11.glPushMatrix();
            GL11.glEnable(GL12.GL_RESCALE_NORMAL);
            GL11.glScalef(scale, -scale, -scale);
            GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
            GL11.glDisable(GL11.GL_CULL_FACE);
            model.render();
            GL11.glPopAttrib();
            GL11.glPopMatrix();
        }

        @Override
        public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partial) {
            Banner112.BannerTile banner = (Banner112.BannerTile) tile;
            boolean standing = !(tile.getBlockType() == Banner112.wall);
            int meta = tile.getBlockMetadata();
            GL11.glPushMatrix();
            if (standing) {
                GL11.glTranslatef((float) x + 0.5F, (float) y + 0.5F, (float) z + 0.5F);
                GL11.glRotatef(-((float) (meta * 360) / 16.0F), 0.0F, 1.0F, 0.0F);
                model.stand.showModel = true;
            } else {
                float yaw = 0.0F;
                if (meta == 2) {
                    yaw = 180.0F;
                } else if (meta == 4) {
                    yaw = 90.0F;
                } else if (meta == 5) {
                    yaw = -90.0F;
                }
                GL11.glTranslatef((float) x + 0.5F, (float) y - 0.16666667F, (float) z + 0.5F);
                GL11.glRotatef(-yaw, 0.0F, 1.0F, 0.0F);
                GL11.glTranslatef(0.0F, -0.3125F, -0.4375F);
                model.stand.showModel = false;
            }
            float time = (float) (tile.xCoord * 7 + tile.yCoord * 9 + tile.zCoord * 13) + (float) tile.getWorldObj().getTotalWorldTime() + partial;
            model.slate.rotateAngleX = (-0.0125F + 0.01F * MathHelper.cos(time * 3.1415927F * 0.02F)) * 3.1415927F;
            drawModel(layered("textures/entity/banner_base.png", "textures/entity/banner/", banner.base, banner.patterns));
            GL11.glPopMatrix();
        }

        @Override
        public boolean handleRenderType(ItemStack stack, ItemRenderType type) {
            return true;
        }

        @Override
        public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack stack, ItemRendererHelper helper) {
            return helper == ItemRendererHelper.EQUIPPED_BLOCK || helper == ItemRendererHelper.ENTITY_ROTATION || helper == ItemRendererHelper.ENTITY_BOBBING
                    || (helper == ItemRendererHelper.BLOCK_3D && type == ItemRenderType.ENTITY);
        }

        // Display transforms of models/item/banner.json: translation in pixels, rotation, scale.
        private static final float[] FIRST = {0, 0, 0, 0, 90, 0, 0.375F, 0.375F, 0.375F};
        private static final float[] THIRD = {0, 2, 0.5F, 0, 90, 0, 0.375F, 0.375F, 0.375F};
        private static final float[] GUI = {0, -3.25F, 0, 30, 20, 0, 0.5325F, 0.5325F, 0.5325F};

        @Override
        public void renderItem(ItemRenderType type, final ItemStack stack, Object... data) {
            NBTTagList list = null;
            if (stack.hasTagCompound() && stack.getTagCompound().hasKey("BlockEntityTag")) {
                list = stack.getTagCompound().getCompoundTag("BlockEntityTag").getTagList("Patterns", 10);
            }
            final ResourceLocation texture = layered("textures/entity/banner_base.png", "textures/entity/banner/", stack.getItemDamage(), list);
            model.stand.showModel = true;
            model.slate.rotateAngleX = 0.0F;
            Runnable body = new Runnable() {
                @Override
                public void run() {
                    drawModel(texture);
                }
            };
            GL11.glPushMatrix();
            switch (type) {
                case INVENTORY:
                    ShieldRenderer.renderInventory(GUI, body);
                    break;
                case EQUIPPED_FIRST_PERSON:
                    // Undoes the 1.7.10 first person scale and turn, as the shield does.
                    GL11.glTranslatef(0.5F, 0.5F, 0.5F);
                    GL11.glScalef(2.5F, 2.5F, 2.5F);
                    GL11.glRotatef(-45.0F, 0.0F, 1.0F, 0.0F);
                    ShieldRenderer.applyDisplay(FIRST, false);
                    body.run();
                    break;
                case EQUIPPED:
                    // Back to the right arm, then LayerHeldItem and the display transform.
                    GL11.glTranslatef(0.5F, 0.5F, 0.5F);
                    GL11.glRotatef(-20.0F, 0.0F, 0.0F, 1.0F);
                    GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
                    GL11.glRotatef(-60.0F, 0.0F, 0.0F, 1.0F);
                    GL11.glScalef(1.0F / 0.375F, 1.0F / 0.375F, 1.0F / 0.375F);
                    GL11.glTranslatef(-0.25F, -0.1875F, 0.1875F);
                    GL11.glTranslatef(0.0625F, -0.4375F, -0.0625F);
                    GL11.glRotatef(-90.0F, 1.0F, 0.0F, 0.0F);
                    GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
                    GL11.glTranslatef(0.0625F, 0.125F, -0.625F);
                    ShieldRenderer.applyDisplay(THIRD, false);
                    body.run();
                    break;
                default:
                    if (RenderItem.renderInFrame) {
                        GL11.glTranslatef(0.0F, 0.25F + 0.25F, 0.0F);
                    } else {
                        // Ground transform of banner.json: scale 0.25, translation 1/16. Forge has scaled by 0.5
                        // (an item drawn as a 3D block), the other half is here. The foot of the pole is half a
                        // unit under the origin, that is 0.125 of a block: exactly the height of the item entity's
                        // position over its bottom in 1.7.10, so the pole stands on the ground as it does in 1.12
                        // and no translation is left.
                        GL11.glScalef(0.5F, 0.5F, 0.5F);
                    }
                    body.run();
                    break;
            }
            GL11.glPopMatrix();
        }    }
}
