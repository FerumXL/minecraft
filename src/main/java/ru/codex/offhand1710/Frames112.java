package ru.codex.offhand1710;

import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.material.Material;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.item.ItemMap;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Direction;
import net.minecraft.world.World;

/**
 * Item frames of 1.8 - 1.12.2: eight rotations of 45 degrees (maps still four of 90), and a comparator reading a frame
 * behind a solid block: rotation % 8 + 1, 0 when empty (BlockRedstoneComparator.calculateInputStrength,
 * EntityItemFrame.getAnalogOutput). -Dmc112.frames17 leaves the frames of 1.7.10 (the coremod then patches nothing).
 */
public final class Frames112 {
    public static final boolean OFF = System.getProperty("mc112.frames17") != null;

    private Frames112() {
    }

    /**
     * RenderItemFrame of 1.7.10 turns the item by -90 * getRotation(); called instead of getRotation() there. A map turns
     * by 90 per rotation % 4 as in 1.12.2; another item by the even half, the odd 45 being added in the render event.
     */
    public static int renderRotation(EntityItemFrame frame) {
        int r = frame.getRotation();
        if (frame.getDisplayedItem() != null && frame.getDisplayedItem().getItem() instanceof ItemMap) {
            return r % 4;
        }
        return r / 2;
    }

    /** True when the item needs the extra 45 degrees. */
    public static boolean halfStep(EntityItemFrame frame) {
        return frame.getDisplayedItem() != null && !(frame.getDisplayedItem().getItem() instanceof ItemMap) && frame.getRotation() % 2 == 1;
    }

    /** BlockRedstoneComparator.getInputStrength returning: a frame two blocks behind, past a solid block. */
    public static int comparator(int strength, World world, int x, int y, int z, int meta) {
        int d = BlockDirectional.getDirection(meta);
        int bx = x + Direction.offsetX[d];
        int bz = z + Direction.offsetZ[d];
        Block b = world.getBlock(bx, y, bz);
        if (b.hasComparatorInputOverride() || strength >= 15 || !b.isNormalCube()) {
            return strength;
        }
        bx += Direction.offsetX[d];
        bz += Direction.offsetZ[d];
        Block c = world.getBlock(bx, y, bz);
        if (c.hasComparatorInputOverride() || c.getMaterial() != Material.air) {
            return strength;
        }
        List<?> frames = world.getEntitiesWithinAABB(EntityItemFrame.class, AxisAlignedBB.getBoundingBox(bx, y, bz, bx + 1, y + 1, bz + 1));
        for (Object o : frames) {
            EntityItemFrame f = (EntityItemFrame) o;
            if (f.hangingDirection == d) {
                return f.getDisplayedItem() == null ? 0 : f.getRotation() % 8 + 1;
            }
        }
        return strength;
    }
}
