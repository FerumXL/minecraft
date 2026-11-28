package ru.codex.offhand1710;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAnvil;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

/**
 * Particles of 1.9 - 1.10 that 1.7.10 has no kind for: dragon breath (ParticleDragonBreath) and falling dust under
 * sand, gravel, anvils and concrete powder (BlockFalling.randomDisplayTick, ParticleFallingDust). Common code calls
 * here; the client sets the factory. -Dmc112.particles17 keeps the stand-ins of 1.7.10 (portal particles, no dust).
 */
public final class Particles112 {
    public static final boolean OFF = System.getProperty("mc112.particles17") != null;

    /** Set by the client proxy. */
    public interface Factory {
        void dragonBreath(World world, double x, double y, double z, double mx, double my, double mz);

        void fallingDust(World world, double x, double y, double z, int rgb);
    }

    public static volatile Factory factory;

    private Particles112() {
    }

    /** EnumParticleTypes.DRAGON_BREATH; portal particles where there is no factory (a server, or turned off). */
    public static void dragonBreath(World world, double x, double y, double z, double mx, double my, double mz) {
        Factory f = factory;
        if (!OFF && f != null && world.isRemote) {
            f.dragonBreath(world, x, y, z, mx, my, mz);
        } else {
            world.spawnParticle("portal", x, y, z, mx, my, mz);
        }
    }

    /**
     * WorldClient.doVoidFogParticles calls Block.randomDisplayTick through here (coremod): BlockFalling of 1.12.2 adds
     * one dust particle in 16 ticks under a block with nothing to stand on.
     */
    public static void displayTick(Block block, World world, int x, int y, int z, Random rand) {
        block.randomDisplayTick(world, x, y, z, rand);
        Factory f = factory;
        if (OFF || f == null || !(block instanceof BlockFalling) || rand.nextInt(16) != 0) {
            return;
        }
        Block below = world.getBlock(x, y - 1, z);
        Material m = below.getMaterial();
        if (below == Blocks.fire || m == Material.air || m == Material.water || m == Material.lava) {
            f.fallingDust(world, x + rand.nextFloat(), y - 0.05D, z + rand.nextFloat(), dustColor(block, world.getBlockMetadata(x, y, z)));
        }
    }

    /** BlockFalling.getDustColor of 1.12.2: sand and red sand, gravel; everything else black. */
    public static int dustColor(Block block, int meta) {
        if (block == Blocks.sand) {
            return meta == 1 ? 11098145 : 14406560;
        }
        if (block == Blocks.gravel) {
            return -8356741;
        }
        if (block instanceof BlockAnvil) {
            return -16777216;
        }
        return -16777216;
    }
}
