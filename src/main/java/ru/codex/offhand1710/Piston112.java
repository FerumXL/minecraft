package ru.codex.offhand1710;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.BlockPistonMoving;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityPiston;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Facing;
import net.minecraft.world.World;

/**
 * Pistons with slime blocks (1.8): the rules of BlockPistonStructureHelper. Called from the coremod; anything that
 * does not touch a slime block is left to the vanilla code.
 */
public final class Piston112 {
    private static final int LIMIT = 12;
    private static boolean result;
    private static Method powered;

    private Piston112() {
    }

    /** The value tryExtend must return after {@link #extend} handled it. */
    public static boolean result() {
        return result;
    }

    private static boolean air(World w, int x, int y, int z) {
        return w.getBlock(x, y, z).getMaterial() == Material.air;
    }

    private static boolean extended(World w, int x, int y, int z) {
        return BlockPistonBase.isExtended(w.getBlockMetadata(x, y, z));
    }

    /** BlockPistonBase.canPush. */
    private static boolean canPush(World w, int x, int y, int z, int dir, boolean destroyable) {
        Block b = w.getBlock(x, y, z);
        if (b == Blocks.obsidian || y < 0 || y > 255 || (dir == 0 && y == 0) || (dir == 1 && y == 255)) {
            return false;
        }
        if (b == Blocks.piston || b == Blocks.sticky_piston) {
            if (extended(w, x, y, z)) {
                return false;
            }
        } else {
            if (b.getBlockHardness(w, x, y, z) == -1.0F || b.getMobilityFlag() == 2) {
                return false;
            }
            if (b.getMobilityFlag() == 1) {
                return destroyable;
            }
        }
        return !b.hasTileEntity(w.getBlockMetadata(x, y, z));
    }

    private static final class Structure {
        final World w;
        final int[] base;
        final int[] start;
        final int dir;
        final List<int[]> move = new ArrayList<int[]>();
        final List<int[]> destroy = new ArrayList<int[]>();

        Structure(World w, int x, int y, int z, int facing, boolean extending) {
            this.w = w;
            base = new int[] {x, y, z};
            if (extending) {
                dir = facing;
                start = at(base, facing, 1);
            } else {
                dir = Facing.oppositeSide[facing];
                start = at(base, facing, 2);
            }
        }

        static int[] at(int[] p, int side, int n) {
            return new int[] {p[0] + Facing.offsetsXForSide[side] * n, p[1] + Facing.offsetsYForSide[side] * n, p[2] + Facing.offsetsZForSide[side] * n};
        }

        static boolean same(int[] a, int[] b) {
            return a[0] == b[0] && a[1] == b[1] && a[2] == b[2];
        }

        Block block(int[] p) {
            return w.getBlock(p[0], p[1], p[2]);
        }

        boolean isAir(int[] p) {
            return air(w, p[0], p[1], p[2]);
        }

        boolean push(int[] p, boolean destroyable) {
            return canPush(w, p[0], p[1], p[2], dir, destroyable);
        }

        int index(int[] p) {
            for (int i = 0; i < move.size(); i++) {
                if (same(move.get(i), p)) {
                    return i;
                }
            }
            return -1;
        }

        boolean compute() {
            move.clear();
            destroy.clear();
            if (!push(start, false)) {
                if (block(start).getMobilityFlag() == 1) {
                    destroy.add(start);
                    return true;
                }
                return false;
            }
            if (!line(start)) {
                return false;
            }
            for (int i = 0; i < move.size(); i++) {
                int[] p = move.get(i);
                if (block(p) == Content119.slime && !sides(p)) {
                    return false;
                }
            }
            return true;
        }

        boolean sides(int[] p) {
            for (int side = 0; side < 6; side++) {
                if ((side >> 1) != (dir >> 1) && !line(at(p, side, 1))) {
                    return false;
                }
            }
            return true;
        }

        boolean line(int[] origin) {
            Block b = block(origin);
            if (isAir(origin) || !push(origin, false) || same(origin, base) || index(origin) > -1) {
                return true;
            }
            int count = 1;
            if (count + move.size() > LIMIT) {
                return false;
            }
            int back = Facing.oppositeSide[dir];
            while (b == Content119.slime) {
                int[] p = at(origin, back, count);
                b = block(p);
                if (isAir(p) || !push(p, false) || same(p, base)) {
                    break;
                }
                count++;
                if (count + move.size() > LIMIT) {
                    return false;
                }
            }
            int added = 0;
            for (int i = count - 1; i >= 0; i--) {
                move.add(at(origin, back, i));
                added++;
            }
            for (int i = 1;; i++) {
                int[] p = at(origin, dir, i);
                int found = index(p);
                if (found > -1) {
                    reorder(added, found);
                    for (int j = 0; j <= found + added; j++) {
                        int[] q = move.get(j);
                        if (block(q) == Content119.slime && !sides(q)) {
                            return false;
                        }
                    }
                    return true;
                }
                if (isAir(p)) {
                    return true;
                }
                if (!push(p, true) || same(p, base)) {
                    return false;
                }
                if (block(p).getMobilityFlag() == 1) {
                    destroy.add(p);
                    return true;
                }
                if (move.size() >= LIMIT) {
                    return false;
                }
                move.add(p);
                added++;
            }
        }

        void reorder(int added, int found) {
            List<int[]> head = new ArrayList<int[]>(move.subList(0, found));
            List<int[]> tail = new ArrayList<int[]>(move.subList(move.size() - added, move.size()));
            List<int[]> middle = new ArrayList<int[]>(move.subList(found, move.size() - added));
            move.clear();
            move.addAll(head);
            move.addAll(tail);
            move.addAll(middle);
        }

        boolean hasSlime() {
            for (int[] p : move) {
                if (block(p) == Content119.slime) {
                    return true;
                }
            }
            return false;
        }
    }

    /** BlockPistonBase.doMove. */
    private static void apply(World w, Structure s, int facing, boolean extending, BlockPistonBase piston) {
        Block[] blocks = new Block[s.move.size()];
        int[] metas = new int[s.move.size()];
        for (int i = 0; i < blocks.length; i++) {
            int[] p = s.move.get(i);
            blocks[i] = s.block(p);
            metas[i] = w.getBlockMetadata(p[0], p[1], p[2]);
        }
        for (int i = s.destroy.size() - 1; i >= 0; i--) {
            int[] p = s.destroy.get(i);
            s.block(p).dropBlockAsItem(w, p[0], p[1], p[2], w.getBlockMetadata(p[0], p[1], p[2]), 0);
            w.setBlockToAir(p[0], p[1], p[2]);
        }
        for (int[] p : s.move) {
            w.setBlock(p[0], p[1], p[2], Blocks.air, 0, 2);
        }
        for (int i = blocks.length - 1; i >= 0; i--) {
            int[] to = Structure.at(s.move.get(i), s.dir, 1);
            w.setBlock(to[0], to[1], to[2], Blocks.piston_extension, metas[i], 4);
            w.setTileEntity(to[0], to[1], to[2], BlockPistonMoving.getTileEntity(blocks[i], metas[i], s.dir, extending, false));
        }
        boolean sticky = piston == Blocks.sticky_piston;
        if (extending) {
            int[] head = Structure.at(s.base, facing, 1);
            int meta = facing | (sticky ? 8 : 0);
            w.setBlock(head[0], head[1], head[2], Blocks.piston_extension, meta, 4);
            w.setTileEntity(head[0], head[1], head[2], BlockPistonMoving.getTileEntity(Blocks.piston_head, meta, facing, true, false));
        }
        for (int i = s.destroy.size() - 1; i >= 0; i--) {
            int[] p = s.destroy.get(i);
            w.notifyBlocksOfNeighborChange(p[0], p[1], p[2], Blocks.air);
        }
        for (int i = blocks.length - 1; i >= 0; i--) {
            int[] p = s.move.get(i);
            w.notifyBlocksOfNeighborChange(p[0], p[1], p[2], blocks[i]);
        }
        if (extending) {
            int[] head = Structure.at(s.base, facing, 1);
            w.notifyBlocksOfNeighborChange(head[0], head[1], head[2], Blocks.piston_head);
        }
    }

    /** Head of BlockPistonBase.tryExtend. Returns true when the slime rules took over. */
    public static boolean extend(BlockPistonBase piston, World w, int x, int y, int z, int facing) {
        Structure s = new Structure(w, x, y, z, facing, true);
        boolean ok = s.compute();
        if (!touchesSlime(s)) {
            return false;
        }
        result = ok;
        if (ok) {
            apply(w, s, facing, true, piston);
        }
        return true;
    }

    private static boolean touchesSlime(Structure s) {
        return s.hasSlime() || s.block(s.start) == Content119.slime;
    }

    private static boolean powered(BlockPistonBase piston, World w, int x, int y, int z, int facing) {
        try {
            if (powered == null) {
                powered = ReflectionHelper.findMethod(BlockPistonBase.class, piston, new String[] {"isIndirectlyPowered", "func_150072_a"}, World.class, int.class, int.class, int.class, int.class);
            }
            return (Boolean) powered.invoke(piston, w, x, y, z, facing);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Head of BlockPistonBase.onBlockEventReceived: a sticky piston pulling a slime structure back. */
    public static boolean retract(BlockPistonBase piston, World w, int x, int y, int z, int event, int facing) {
        if (event != 1 || piston != Blocks.sticky_piston) {
            return false;
        }
        Structure s = new Structure(w, x, y, z, facing, false);
        s.compute();
        if (!touchesSlime(s)) {
            return false;
        }
        if (!w.isRemote && powered(piston, w, x, y, z, facing)) {
            return false;
        }
        int[] head = Structure.at(s.base, facing, 1);
        TileEntity moving = w.getTileEntity(head[0], head[1], head[2]);
        if (moving instanceof TileEntityPiston) {
            ((TileEntityPiston) moving).clearPistonTileEntity();
        }
        w.setBlock(x, y, z, Blocks.piston_extension, facing, 3);
        w.setTileEntity(x, y, z, BlockPistonMoving.getTileEntity(piston, facing, facing, false, true));
        w.setBlockToAir(head[0], head[1], head[2]);
        boolean ok = s.compute();
        if (ok) {
            apply(w, s, facing, false, piston);
        }
        w.playSoundEffect((double) x + 0.5D, (double) y + 0.5D, (double) z + 0.5D, "tile.piston.in", 0.5F, w.rand.nextFloat() * 0.15F + 0.6F);
        result = true;
        return true;
    }

    /** TileEntityPiston.moveCollidedEntities: a moving slime block throws what it touches along its direction. */
    public static void bounce(TileEntityPiston tile, float progress) {
        if (tile.getStoredBlockID() != Content119.slime || tile.getWorldObj() == null) {
            return;
        }
        int side = tile.getPistonOrientation();
        if (!tile.isExtending()) {
            side = Facing.oppositeSide[side];
        }
        int dx = Facing.offsetsXForSide[side];
        int dy = Facing.offsetsYForSide[side];
        int dz = Facing.offsetsZForSide[side];
        AxisAlignedBB box = AxisAlignedBB.getBoundingBox(tile.xCoord, tile.yCoord, tile.zCoord, tile.xCoord + 1, tile.yCoord + 1, tile.zCoord + 1)
                .addCoord((double) dx * 0.5D, (double) dy * 0.5D, (double) dz * 0.5D);
        for (Object o : tile.getWorldObj().getEntitiesWithinAABBExcludingEntity(null, box)) {
            Entity e = (Entity) o;
            if (dx != 0) {
                e.motionX = (double) dx;
            } else if (dy != 0) {
                e.motionY = (double) dy;
            } else {
                e.motionZ = (double) dz;
            }
        }
    }
}
