package ru.codex.offhand1710;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

/** The small part of the 1.12.2 structure API that MonumentPieces uses, on top of 1.7.10. Names as in the original. */
final class MonumentShim {
    private MonumentShim() {
    }

    /** EnumFacing: a down, b up, c north, d south, e west, f east. */
    enum Dir {
        a, b, c, d, e, f;

        int a() {
            return ordinal();
        }

        static Dir a(int index) {
            return values()[index];
        }

        Dir d() {
            return values()[ordinal() ^ 1];
        }

        int g() {
            return this == e ? -1 : (this == f ? 1 : 0);
        }

        int h() {
            return this == a ? -1 : (this == b ? 1 : 0);
        }

        int i() {
            return this == c ? -1 : (this == d ? 1 : 0);
        }

        static Dir randomHorizontal(Random random) {
            return values()[2 + random.nextInt(4)];
        }
    }

    /** StructureBoundingBox: a, b, c are the minimum x, y, z and d, e, f the maximum. */
    static final class Bbg {
        int a;
        int b;
        int c;
        int d;
        int e;
        int f;

        Bbg(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
            a = minX;
            b = minY;
            c = minZ;
            d = maxX;
            e = maxY;
            f = maxZ;
        }

        static Bbg a(int x1, int y1, int z1, int x2, int y2, int z2) {
            return new Bbg(Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2), Math.max(x1, x2), Math.max(y1, y2),
                    Math.max(z1, z2));
        }

        void a(int x, int y, int z) {
            a += x;
            b += y;
            c += z;
            d += x;
            e += y;
            f += z;
        }

        boolean a(Bbg o) {
            return d >= o.a && a <= o.d && f >= o.c && c <= o.f && e >= o.b && b <= o.e;
        }

        boolean a(int minX, int minZ, int maxX, int maxZ) {
            return d >= minX && a <= maxX && f >= minZ && c <= maxZ;
        }

        boolean b(int x, int y, int z) {
            return x >= a && x <= d && z >= c && z <= f && y >= b && y <= e;
        }
    }

    /** A block with metadata (IBlockState). */
    static final class Bs {
        static final Bs AIR = new Bs(Blocks.air, 0);
        static final Bs WATER = new Bs(Blocks.water, 0);
        static final Bs GOLD = new Bs(Blocks.gold_block, 0);
        static final Bs WET_SPONGE = new Bs(Content120.wetSponge, 0);
        static final Bs LANTERN = new Bs(PortedBlocks.seaLantern, 0);

        final Block block;
        final int meta;

        Bs(Block block, int meta) {
            this.block = block;
            this.meta = meta;
        }

        static Bs prismarine(int variant) {
            return new Bs(variant == 0 ? PortedBlocks.prismarine
                    : (variant == 1 ? PortedBlocks.prismarineBricks : PortedBlocks.darkPrismarine), 0);
        }

        Material a() {
            return block.getMaterial();
        }
    }

    /** StructureComponent. */
    abstract static class Bbx {
        protected Bbg l;
        private Dir facing;

        Bbx(int type) {
        }

        protected void a(Dir direction) {
            facing = direction;
        }

        protected Dir f() {
            return facing;
        }

        Bbg d() {
            return l;
        }

        protected int a(int x, int z) {
            if (facing == Dir.e) {
                return l.d - z;
            }
            return facing == Dir.f ? l.a + z : l.a + x;
        }

        protected int b(int x, int z) {
            if (facing == Dir.c) {
                return l.f - z;
            }
            return facing == Dir.d ? l.c + z : l.c + x;
        }

        protected int d(int y) {
            return y + l.b;
        }

        protected static int swKey(Dir direction) {
            return direction == Dir.c ? 1 : (direction == Dir.d ? 2 : (direction == Dir.e ? 3 : 4));
        }

        protected void a(World world, Bs state, int x, int y, int z, Bbg box) {
            int worldX = a(x, z);
            int worldY = d(y);
            int worldZ = b(x, z);
            if (box.b(worldX, worldY, worldZ)) {
                world.setBlock(worldX, worldY, worldZ, state.block, state.meta, 2);
            }
        }

        protected Bs a(World world, int x, int y, int z, Bbg box) {
            int worldX = a(x, z);
            int worldY = d(y);
            int worldZ = b(x, z);
            if (!box.b(worldX, worldY, worldZ)) {
                return Bs.AIR;
            }
            Block block = world.getBlock(worldX, worldY, worldZ);
            return block == Blocks.water ? Bs.WATER : new Bs(block, world.getBlockMetadata(worldX, worldY, worldZ));
        }

        protected void a(World world, Bbg box, int x1, int y1, int z1, int x2, int y2, int z2, Bs edge, Bs inside,
                boolean existingOnly) {
            for (int y = y1; y <= y2; y++) {
                for (int x = x1; x <= x2; x++) {
                    for (int z = z1; z <= z2; z++) {
                        if (existingOnly && a(world, x, y, z, box).a() == Material.air) {
                            continue;
                        }
                        boolean border = y == y1 || y == y2 || x == x1 || x == x2 || z == z1 || z == z2;
                        a(world, border ? edge : inside, x, y, z, box);
                    }
                }
            }
        }

        /** replaceAirAndLiquidDownwards: the legs that carry the building down to the sea floor. */
        protected void b(World world, Bs state, int x, int y, int z, Bbg box) {
            int worldX = a(x, z);
            int worldY = d(y);
            int worldZ = b(x, z);
            if (!box.b(worldX, worldY, worldZ)) {
                return;
            }
            while (worldY > 1) {
                Block block = world.getBlock(worldX, worldY, worldZ);
                if (!block.isAir(world, worldX, worldY, worldZ) && !block.getMaterial().isLiquid()) {
                    return;
                }
                world.setBlock(worldX, worldY, worldZ, state.block, state.meta, 2);
                worldY--;
            }
        }

        abstract boolean a(World world, Random random, Bbg box);
    }
}
