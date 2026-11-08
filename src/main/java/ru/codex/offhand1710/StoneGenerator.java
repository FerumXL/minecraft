package ru.codex.offhand1710;

import java.util.Random;

import cpw.mods.fml.common.IWorldGenerator;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.feature.WorldGenMinable;

/**
 * Granite, diorite and andesite in the overworld with the 1.12.2 default settings: veins of up to 33 blocks,
 * 10 attempts per chunk each, at heights 0-79, replacing stone. Not in superflat worlds.
 *
 * Positions are NOT offset by 8 here: WorldGenMinable adds the 8 itself. A second offset pushes the veins past the
 * 2x2 chunk area that exists while a chunk is populated, each vein then generates new chunks, and the chain of
 * generations ends in a StackOverflowError.
 */
public final class StoneGenerator implements IWorldGenerator {
    private static final int VEIN_SIZE = 33;
    private static final int VEINS_PER_CHUNK = 10;
    private static final int MAX_HEIGHT = 80;

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkProvider chunkGenerator,
            IChunkProvider chunkProvider) {
        if (world.provider.dimensionId != 0 || world.getWorldInfo().getTerrainType() == WorldType.FLAT) {
            return;
        }
        long started = GenProbe.ENABLED ? System.nanoTime() : 0L;
        int baseX = chunkX * 16;
        int baseZ = chunkZ * 16;
        generateVeins(world, random, baseX, baseZ, new WorldGenMinable(PortedBlocks.granite, 0, VEIN_SIZE, Blocks.stone));
        generateVeins(world, random, baseX, baseZ, new WorldGenMinable(PortedBlocks.diorite, 0, VEIN_SIZE, Blocks.stone));
        generateVeins(world, random, baseX, baseZ, new WorldGenMinable(PortedBlocks.andesite, 0, VEIN_SIZE, Blocks.stone));
        if (GenProbe.ENABLED) {
            GenProbe.stone(System.nanoTime() - started);
        }
    }

    private static void generateVeins(World world, Random random, int baseX, int baseZ, WorldGenMinable vein) {
        for (int i = 0; i < VEINS_PER_CHUNK; i++) {
            vein.generate(world, random, baseX + random.nextInt(16), random.nextInt(MAX_HEIGHT), baseZ + random.nextInt(16));
        }
    }
}
