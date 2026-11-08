package ru.codex.offhand1710.client;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import cpw.mods.fml.client.registry.RenderingRegistry;
import net.minecraft.block.Block;
import org.lwjgl.opengl.GL11;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.init.Blocks;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import ru.codex.offhand1710.End112;

/** Block shapes of chorus plants and end rods, which 1.7.10 cannot express without a renderer. */
final class EndRenderers {
    private EndRenderers() {
    }

    static void register() {
        End112.chorusRenderId = RenderingRegistry.getNextAvailableRenderId();
        End112.rodRenderId = RenderingRegistry.getNextAvailableRenderId();
        RenderingRegistry.registerBlockHandler(End112.chorusRenderId, new Chorus());
        RenderingRegistry.registerBlockHandler(End112.rodRenderId, new Rod());
    }

    private static final class Chorus implements ISimpleBlockRenderingHandler {
        private static final double MIN = 0.1875D;
        private static final double MAX = 0.8125D;

        private static boolean link(IBlockAccess world, int x, int y, int z) {
            Block block = world.getBlock(x, y, z);
            return block == End112.chorusPlant || block == End112.chorusFlower;
        }

        @Override
        public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId, RenderBlocks renderer) {
            if (block == End112.chorusFlower) {
                renderer.setRenderBounds(0.0625D, 0.0D, 0.0625D, 0.9375D, 0.875D, 0.9375D);
                renderer.renderStandardBlock(block, x, y, z);
                return true;
            }
            renderer.setRenderBounds(MIN, MIN, MIN, MAX, MAX, MAX);
            renderer.renderStandardBlock(block, x, y, z);
            if (link(world, x, y + 1, z)) {
                renderer.setRenderBounds(MIN, MAX, MIN, MAX, 1.0D, MAX);
                renderer.renderStandardBlock(block, x, y, z);
            }
            if (link(world, x, y - 1, z) || world.getBlock(x, y - 1, z) == Blocks.end_stone) {
                renderer.setRenderBounds(MIN, 0.0D, MIN, MAX, MIN, MAX);
                renderer.renderStandardBlock(block, x, y, z);
            }
            if (link(world, x - 1, y, z)) {
                renderer.setRenderBounds(0.0D, MIN, MIN, MIN, MAX, MAX);
                renderer.renderStandardBlock(block, x, y, z);
            }
            if (link(world, x + 1, y, z)) {
                renderer.setRenderBounds(MAX, MIN, MIN, 1.0D, MAX, MAX);
                renderer.renderStandardBlock(block, x, y, z);
            }
            if (link(world, x, y, z - 1)) {
                renderer.setRenderBounds(MIN, MIN, 0.0D, MAX, MAX, MIN);
                renderer.renderStandardBlock(block, x, y, z);
            }
            if (link(world, x, y, z + 1)) {
                renderer.setRenderBounds(MIN, MIN, MAX, MAX, MAX, 1.0D);
                renderer.renderStandardBlock(block, x, y, z);
            }
            return true;
        }

        @Override
        public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {
        }

        @Override
        public boolean shouldRender3DInInventory(int modelId) {
            return false;
        }

        @Override
        public int getRenderId() {
            return End112.chorusRenderId;
        }
    }

    /** The end rod model of 1.12.2: a 2x15x2 shaft on a 4x1x4 base, along the direction in the metadata. */
    private static final class Rod implements ISimpleBlockRenderingHandler {
        private int meta;
        private double ox;
        private double oy;
        private double oz;

        /** A point of the upward model, turned to face the direction of the block. */
        private void vertex(Tessellator t, double x, double y, double z, double u, double v) {
            double wx;
            double wy;
            double wz;
            switch (meta) {
                case 0:
                    wx = x;
                    wy = 1.0D - y;
                    wz = z;
                    break;
                case 2:
                    wx = x;
                    wy = z;
                    wz = 1.0D - y;
                    break;
                case 3:
                    wx = x;
                    wy = z;
                    wz = y;
                    break;
                case 4:
                    wx = 1.0D - y;
                    wy = x;
                    wz = z;
                    break;
                case 5:
                    wx = y;
                    wy = x;
                    wz = z;
                    break;
                default:
                    wx = x;
                    wy = y;
                    wz = z;
                    break;
            }
            t.addVertexWithUV(ox + wx, oy + wy, oz + wz, u, v);
        }

        private void box(Tessellator t, IIcon icon, double x0, double y0, double z0, double x1, double y1, double z1, int u, int v, int width, int height, int topU, int topV, int topSize) {
            double us = icon.getInterpolatedU((double) u);
            double ue = icon.getInterpolatedU((double) (u + width));
            double vs = icon.getInterpolatedV((double) v);
            double ve = icon.getInterpolatedV((double) (v + height));
            vertex(t, x0, y1, z0, us, vs);
            vertex(t, x1, y1, z0, ue, vs);
            vertex(t, x1, y0, z0, ue, ve);
            vertex(t, x0, y0, z0, us, ve);
            vertex(t, x1, y1, z1, us, vs);
            vertex(t, x0, y1, z1, ue, vs);
            vertex(t, x0, y0, z1, ue, ve);
            vertex(t, x1, y0, z1, us, ve);
            vertex(t, x0, y1, z1, us, vs);
            vertex(t, x0, y1, z0, ue, vs);
            vertex(t, x0, y0, z0, ue, ve);
            vertex(t, x0, y0, z1, us, ve);
            vertex(t, x1, y1, z0, us, vs);
            vertex(t, x1, y1, z1, ue, vs);
            vertex(t, x1, y0, z1, ue, ve);
            vertex(t, x1, y0, z0, us, ve);
            double tu0 = icon.getInterpolatedU((double) topU);
            double tu1 = icon.getInterpolatedU((double) (topU + topSize));
            double tv0 = icon.getInterpolatedV((double) topV);
            double tv1 = icon.getInterpolatedV((double) (topV + topSize));
            vertex(t, x0, y1, z1, tu0, tv1);
            vertex(t, x1, y1, z1, tu1, tv1);
            vertex(t, x1, y1, z0, tu1, tv0);
            vertex(t, x0, y1, z0, tu0, tv0);
            vertex(t, x0, y0, z0, tu0, tv0);
            vertex(t, x1, y0, z0, tu1, tv0);
            vertex(t, x1, y0, z1, tu1, tv1);
            vertex(t, x0, y0, z1, tu0, tv1);
        }

        @Override
        public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId, RenderBlocks renderer) {
            Tessellator t = Tessellator.instance;
            t.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));
            t.setColorOpaque_F(1.0F, 1.0F, 1.0F);
            meta = world.getBlockMetadata(x, y, z);
            ox = (double) x;
            oy = (double) y;
            oz = (double) z;
            IIcon icon = block.getIcon(0, 0);
            box(t, icon, 0.4375D, 0.0625D, 0.4375D, 0.5625D, 1.0D, 0.5625D, 0, 0, 2, 15, 2, 0, 2);
            box(t, icon, 0.375D, 0.0D, 0.375D, 0.625D, 0.0625D, 0.625D, 2, 6, 4, 1, 2, 2, 4);
            return true;
        }

        @Override
        public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {
            Tessellator t = Tessellator.instance;
            meta = 1;
            ox = -0.5D;
            oy = -0.5D;
            oz = -0.5D;
            IIcon icon = block.getIcon(0, 0);
            GL11.glDisable(GL11.GL_LIGHTING);
            t.startDrawingQuads();
            t.setColorOpaque_F(1.0F, 1.0F, 1.0F);
            box(t, icon, 0.4375D, 0.0625D, 0.4375D, 0.5625D, 1.0D, 0.5625D, 0, 0, 2, 15, 2, 0, 2);
            box(t, icon, 0.375D, 0.0D, 0.375D, 0.625D, 0.0625D, 0.625D, 2, 6, 4, 1, 2, 2, 4);
            t.draw();
            GL11.glEnable(GL11.GL_LIGHTING);
        }

        @Override
        public boolean shouldRender3DInInventory(int modelId) {
            return true;
        }

        @Override
        public int getRenderId() {
            return End112.rodRenderId;
        }
    }
}
