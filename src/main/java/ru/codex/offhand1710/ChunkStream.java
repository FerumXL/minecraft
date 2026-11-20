package ru.codex.offhand1710;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.logging.log4j.LogManager;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.S26PacketMapChunkBulk;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkWatchEvent;

/**
 * Chunk streaming for fast players, after PlayerChunkMap of 1.12.2.
 *
 * Generation: 1.7.10 generates every chunk a PlayerManager.PlayerInstance asks for in the same tick (a whole new row of
 * the view square each time the player crosses a chunk border: 65 chunks at view distance 32). The coremod sends
 * those loadChunk calls here; a chunk that has to be generated (not loaded, not on disk, not next to a player) waits in
 * a queue that is worked off each tick within a time budget, nearest to where the players will be first, as 1.12
 * PlayerChunkMap.tick does (49 chunks, 50 ms). Chunks on disk keep Forge's asynchronous loading.
 *
 * Sending: EntityPlayerMP sends at most 5 chunks a tick from loadedChunks in the order they were queued. For a player
 * moving faster than walking, the queue is sorted by distance to where the player will be in a second and up to
 * EXTRA more chunks a tick are sent the way EntityPlayerMP.onUpdate sends them.
 */
public final class ChunkStream {
    /** Chunks generated per tick at most / time budget per tick; chunks this close to a player are never deferred. */
    private static final int MAX_PER_TICK = 49;
    private static final long BUDGET_NANOS = 35000000L;
    private static final int NEAR = 2;
    private static final int EXTRA = 10;
    private static final double FAST = 0.5D;

    private static final class Pending {
        final ChunkProviderServer provider;
        final int x;
        final int z;
        final Runnable done;
        double priority;

        Pending(ChunkProviderServer provider, int x, int z, Runnable done) {
            this.provider = provider;
            this.x = x;
            this.z = z;
            this.done = done;
        }
    }

    private static final List<Pending> QUEUE = new ArrayList<Pending>();
    private static final Map<EntityPlayerMP, double[]> LAST = new WeakHashMap<EntityPlayerMP, double[]>();
    private static Field loaderField;
    private static Method watcher;
    private static boolean broken;
    public static int generatedLastTick;
    public static int deferredTotal;

    public ChunkStream() {
    }

    /** Stands in for ChunkProviderServer.loadChunk(x, z, runnable) in PlayerManager.PlayerInstance. */
    public static final int[] REASONS = new int[6];
    private static int immediateThisTick;

    /**
     * View distance above 16 (OptiFine allows 32). Up to 16 the vanilla generation keeps the server at 20 TPS and gives
     * fewer holes ahead (measured: 15 of 80 chunks with the send order alone, 34-50 with the queue), so the queue and the
     * active-set filter only work above 16, where vanilla dropped to 3 TPS.
     */
    private static boolean wide() {
        MinecraftServer server = MinecraftServer.getServer();
        return server != null && server.getConfigurationManager() != null && server.getConfigurationManager().getViewDistance() > 16;
    }

    /** Recent server ticks were short: chunks are still generated at once, at most 16 a tick (only a loaded server queues). */
    private static boolean light() {
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) {
            return false;
        }
        long[] times = server.tickTimeArray;
        int now = server.getTickCounter();
        long sum = 0;
        for (int i = 1; i <= 20; i++) {
            sum += times[((now - i) % 100 + 100) % 100];
        }
        return sum / 20L < 30000000L;
    }

    public static Chunk load(ChunkProviderServer provider, int x, int z, Runnable done) {
        int why = broken ? 0 : provider.chunkExists(x, z) ? 1 : onDisk(provider, x, z) ? 2 : nearPlayer(provider, x, z) ? 3 : 4;
        if (why == 4 && (!wide() || immediateThisTick < 16 && light())) {
            why = 5; // the server has time: generated now, as 1.7.10 does
            immediateThisTick++;
        }
        REASONS[why]++;
        if (why != 4) {
            return provider.loadChunk(x, z, done);
        }
        synchronized (QUEUE) {
            QUEUE.add(new Pending(provider, x, z, done));
        }
        deferredTotal++;
        return null;
    }

    private static Field activeField;
    private static final List<ChunkCoordIntPair> SKIPPED = new ArrayList<ChunkCoordIntPair>();

    /** WorldServer.updateBlocks, last: the chunks left out go back into the active set. */
    @SuppressWarnings("unchecked")
    public static void restoreActive(WorldServer world) {
        if (SKIPPED.isEmpty()) {
            return;
        }
        try {
            ((java.util.Set<ChunkCoordIntPair>) activeField.get(world)).addAll(SKIPPED);
        } catch (Exception e) {
            broken = true;
        }
        SKIPPED.clear();
    }
    public static int skippedActive;

    /** WorldServer.updateBlocks, first: chunks of the active set that are not loaded are left out of this tick. */
    @SuppressWarnings("unchecked")
    public static void filterActive(WorldServer world) {
        // only while the server is behind: a server with time left generates them as 1.7.10 does (skipping them costs
        // the client chunks ahead, measured: holes in front of a fast flight at view distance 16 went from 15 to 50)
        if (broken || !wide() || light()) {
            return;
        }
        try {
            if (activeField == null) {
                activeField = ReflectionHelper.findField(net.minecraft.world.World.class, "activeChunkSet", "field_72993_I");
            }
            java.util.Set<ChunkCoordIntPair> set = (java.util.Set<ChunkCoordIntPair>) activeField.get(world);
            Iterator<ChunkCoordIntPair> it = set.iterator();
            while (it.hasNext()) {
                ChunkCoordIntPair c = it.next();
                if (!world.theChunkProviderServer.chunkExists(c.chunkXPos, c.chunkZPos)) {
                    it.remove();
                    SKIPPED.add(c);
                    skippedActive++;
                }
            }
        } catch (Exception e) {
            broken = true;
            LogManager.getLogger("mc112bridge").warn("Chunk streaming off: " + e);
        }
    }

    /** Stands in for the plain ChunkProviderServer.loadChunk(x, z) of PlayerInstance (its result is not used there). */
    public static Chunk loadSync(ChunkProviderServer provider, int x, int z) {
        return load(provider, x, z, null);
    }

    /**
     * World.getChunkFromChunkCoords in PlayerInstance: a chunk still waiting for generation is not generated just to be
     * looked at (removePlayer, processChunk); an empty chunk stands in, which nothing sends.
     */
    public static Chunk existing(net.minecraft.world.World world, int x, int z) {
        if (broken || world.getChunkProvider().chunkExists(x, z)) {
            return world.getChunkFromChunkCoords(x, z);
        }
        return new net.minecraft.world.chunk.EmptyChunk(world, x, z);
    }

    private static boolean onDisk(ChunkProviderServer provider, int x, int z) {
        try {
            if (loaderField == null) {
                loaderField = ReflectionHelper.findField(ChunkProviderServer.class, "currentChunkLoader", "field_73247_e");
            }
            Object loader = loaderField.get(provider);
            return !(loader instanceof AnvilChunkLoader) || ((AnvilChunkLoader) loader).chunkExists(provider.worldObj, x, z);
        } catch (Exception e) {
            broken = true;
            LogManager.getLogger("mc112bridge").warn("Chunk streaming off: " + e);
            return true;
        }
    }

    private static boolean nearPlayer(ChunkProviderServer provider, int x, int z) {
        for (Object o : provider.worldObj.playerEntities) {
            EntityPlayerMP p = (EntityPlayerMP) o;
            if (Math.abs(((int) Math.floor(p.posX) >> 4) - x) <= NEAR && Math.abs(((int) Math.floor(p.posZ) >> 4) - z) <= NEAR) {
                return true;
            }
        }
        return false;
    }

    /** Where the player will be in about a second (block x, z), from the movement of the last tick. */
    private static double[] ahead(EntityPlayerMP p) {
        double[] last = LAST.get(p);
        double vx = last == null ? 0.0D : p.posX - last[0];
        double vz = last == null ? 0.0D : p.posZ - last[1];
        return new double[] {p.posX + vx * 20.0D, p.posZ + vz * 20.0D, Math.sqrt(vx * vx + vz * vz)};
    }

    private static double distance(WorldServer world, int cx, int cz) {
        double best = Double.MAX_VALUE;
        for (Object o : world.playerEntities) {
            EntityPlayerMP p = (EntityPlayerMP) o;
            double[] a = ahead(p);
            double bx = cx * 16 + 8;
            double bz = cz * 16 + 8;
            double near = (bx - p.posX) * (bx - p.posX) + (bz - p.posZ) * (bz - p.posZ);
            double far = (bx - a[0]) * (bx - a[0]) + (bz - a[1]) * (bz - a[1]);
            best = Math.min(best, Math.min(near, far));
        }
        return best;
    }

    private static boolean watched(WorldServer world, int x, int z) {
        try {
            if (watcher == null) {
                watcher = ReflectionHelper.findMethod(PlayerManager.class, null, new String[] {"getOrCreateChunkWatcher", "func_72690_a"}, int.class, int.class, boolean.class);
            }
            return watcher.invoke(world.getPlayerManager(), Integer.valueOf(x), Integer.valueOf(z), Boolean.FALSE) != null;
        } catch (Exception e) {
            return true;
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        immediateThisTick = 0;
        List<Pending> work;
        synchronized (QUEUE) {
            if (QUEUE.isEmpty()) {
                generatedLastTick = 0;
                rememberPositions();
                return;
            }
            work = new ArrayList<Pending>(QUEUE);
            QUEUE.clear();
        }
        for (Pending p : work) {
            p.priority = distance((WorldServer) p.provider.worldObj, p.x, p.z);
        }
        Collections.sort(work, new Comparator<Pending>() {
            @Override
            public int compare(Pending a, Pending b) {
                return Double.compare(a.priority, b.priority);
            }
        });
        long end = System.nanoTime() + BUDGET_NANOS;
        int done = 0;
        Iterator<Pending> it = work.iterator();
        while (it.hasNext()) {
            Pending p = it.next();
            if (done > 0 && (done >= MAX_PER_TICK || System.nanoTime() > end)) {
                break;
            }
            it.remove();
            if (!p.provider.chunkExists(p.x, p.z)) {
                if (!watched((WorldServer) p.provider.worldObj, p.x, p.z)) {
                    continue; // nobody waits for it any more
                }
                done++;
            }
            p.provider.loadChunk(p.x, p.z, p.done);
        }
        generatedLastTick = done;
        synchronized (QUEUE) {
            QUEUE.addAll(work);
        }
        rememberPositions();
    }

    private static void rememberPositions() {
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) {
            return;
        }
        for (Object o : server.getConfigurationManager().playerEntityList) {
            EntityPlayerMP p = (EntityPlayerMP) o;
            LAST.put(p, new double[] {p.posX, p.posZ});
        }
    }

    public static int pending() {
        synchronized (QUEUE) {
            return QUEUE.size();
        }
    }

    /** After the player's own tick: the send queue of a fast player sorted toward where it goes, and EXTRA more chunks sent. */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof EntityPlayerMP)) {
            return;
        }
        final EntityPlayerMP player = (EntityPlayerMP) event.player;
        @SuppressWarnings("unchecked")
        List<ChunkCoordIntPair> queue = player.loadedChunks;
        if (queue.size() <= 5) {
            return;
        }
        final double[] a = ahead(player);
        if (a[2] < FAST || a[2] > 8.0D) {
            return; // walking, or a teleport
        }
        Collections.sort(queue, new Comparator<ChunkCoordIntPair>() {
            @Override
            public int compare(ChunkCoordIntPair p, ChunkCoordIntPair q) {
                return Double.compare(score(p), score(q));
            }

            private double score(ChunkCoordIntPair c) {
                double bx = c.chunkXPos * 16 + 8;
                double bz = c.chunkZPos * 16 + 8;
                double near = (bx - player.posX) * (bx - player.posX) + (bz - player.posZ) * (bz - player.posZ);
                double far = (bx - a[0]) * (bx - a[0]) + (bz - a[1]) * (bz - a[1]);
                return Math.min(near, far);
            }
        });
        WorldServer world = player.getServerForPlayer();
        List<Chunk> chunks = new ArrayList<Chunk>();
        List<TileEntity> tiles = new ArrayList<TileEntity>();
        Iterator<ChunkCoordIntPair> it = queue.iterator();
        while (it.hasNext() && chunks.size() < EXTRA) {
            ChunkCoordIntPair c = it.next();
            if (c == null) {
                it.remove();
                continue;
            }
            if (!world.blockExists(c.chunkXPos << 4, 0, c.chunkZPos << 4)) {
                continue;
            }
            Chunk chunk = world.getChunkFromChunkCoords(c.chunkXPos, c.chunkZPos);
            if (!chunk.func_150802_k()) {
                continue;
            }
            chunks.add(chunk);
            for (Object t : world.func_147486_a(c.chunkXPos * 16, 0, c.chunkZPos * 16, c.chunkXPos * 16 + 15, 256, c.chunkZPos * 16 + 15)) {
                tiles.add((TileEntity) t);
            }
            it.remove();
        }
        if (chunks.isEmpty()) {
            return;
        }
        for (int i = 0; i < chunks.size(); i += 5) {
            player.playerNetServerHandler.sendPacket(new S26PacketMapChunkBulk(chunks.subList(i, Math.min(chunks.size(), i + 5))));
        }
        for (TileEntity t : tiles) {
            net.minecraft.network.Packet packet = t.getDescriptionPacket();
            if (packet != null) {
                player.playerNetServerHandler.sendPacket(packet);
            }
        }
        for (Chunk chunk : chunks) {
            world.getEntityTracker().func_85172_a(player, chunk);
            MinecraftForge.EVENT_BUS.post(new ChunkWatchEvent.Watch(chunk.getChunkCoordIntPair(), player));
        }
    }
}
