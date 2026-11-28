package ru.codex.offhand1710;

import org.apache.logging.log4j.LogManager;

import net.minecraft.world.World;
import net.minecraft.world.gen.ChunkProviderServer;

/** Small guards the coremod calls from vanilla code. */
public final class Hooks112 {
    private static int nullChunks;

    private Hooks112() {
    }

    /** World.func_147467_a got a null chunk (see asm.Transformer.nullChunkGuard): skipped, logged the first 10 times. */
    public static void nullChunk(World world, int blockX, int blockZ) {
        if (nullChunks >= 10) {
            return;
        }
        nullChunks++;
        int cx = blockX >> 4;
        int cz = blockZ >> 4;
        String loaded = world.getChunkProvider() instanceof ChunkProviderServer
                ? String.valueOf(((ChunkProviderServer) world.getChunkProvider()).chunkExists(cx, cz)) : "?";
        LogManager.getLogger("mc112bridge").warn("Ticked chunk " + cx + "," + cz + " is null (loaded=" + loaded + ", thread "
                + Thread.currentThread().getName() + "); mood sound and relight skipped for this tick");
    }

    private static int nullProvided;

    /** End of ChunkProviderServer.provideChunk: a null chunk becomes the empty chunk for this read, logged 10 times. */
    public static net.minecraft.world.chunk.Chunk notNullChunk(net.minecraft.world.chunk.Chunk chunk, ChunkProviderServer provider, int x, int z) {
        if (chunk != null) {
            return chunk;
        }
        if (nullProvided < 10) {
            nullProvided++;
            LogManager.getLogger("mc112bridge").warn("provideChunk " + x + "," + z + " gave null (thread " + Thread.currentThread().getName()
                + "); the empty chunk stands in for this read");
        }
        return new net.minecraft.world.chunk.EmptyChunk(provider.worldObj, x, z);
    }
}
