package ru.codex.offhand1710;

import java.util.Random;

import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.BiomeGenPlains;

/**
 * Tree count rules of the 1.12.2 BiomeDecorator (class ank): one extra tree when
 * {@code rand.nextFloat() < extraTreeChance} (0.1 by default, 0.05 in plains), and plains have
 * treesPerChunk 0 instead of -999, so they get a lone tree now and then.
 * 1.7.10 uses {@code rand.nextInt(10) == 0} for every biome; both draw one value, so the
 * random sequence of the rest of the decoration stays the same.
 */
public final class Trees112 {
    /** -Dmc112.trees17=1: the 1.7.10 rules (for comparison runs). */
    private static final boolean OLD = "1".equals(System.getProperty("mc112.trees17"));

    private Trees112() {}

    /** Replaces {@code rand.nextInt(10)} before IFNE: returns true when there is NO extra tree. */
    public static boolean noExtraTree(Random rand, BiomeGenBase biome) {
        if (OLD) {
            return rand.nextInt(10) != 0;
        }
        float chance = biome instanceof BiomeGenPlains || biome != null && biome.biomeID == 129 ? 0.05F : 0.1F;
        return !(rand.nextFloat() < chance);
    }

    /** Plains (and sunflower plains) as in 1.12.2 BiomePlains: treesPerChunk = 0. */
    public static void applyBiomes() {
        if (OLD) {
            return;
        }
        for (BiomeGenBase b : BiomeGenBase.getBiomeGenArray()) {
            if (b != null && (b instanceof BiomeGenPlains || b.biomeID == 129) && b.theBiomeDecorator != null
                && b.theBiomeDecorator.treesPerChunk == -999) {
                b.theBiomeDecorator.treesPerChunk = 0;
            }
        }
    }
}
