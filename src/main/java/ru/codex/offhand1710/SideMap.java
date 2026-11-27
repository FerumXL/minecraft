package ru.codex.offhand1710;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.entity.Entity;

/**
 * A weak map keyed by entities that keeps the client and the server apart.
 *
 * Entity.equals of 1.7.10 compares entityId, and in single player the client and the server copy of an entity get the
 * same id, so one plain WeakHashMap would let both sides share (and overwrite) one value. -Dmc112.sharedEntityState
 * returns to one map for both sides.
 */
public final class SideMap<K extends Entity, V> {
    private static final boolean SHARED = System.getProperty("mc112.sharedEntityState") != null;

    private final Map<K, V> server = new WeakHashMap<K, V>();
    private final Map<K, V> client = new WeakHashMap<K, V>();

    private Map<K, V> side(Entity e) {
        return !SHARED && e.worldObj != null && e.worldObj.isRemote ? client : server;
    }

    public synchronized V get(K e) {
        return side(e).get(e);
    }

    public synchronized V put(K e, V v) {
        return side(e).put(e, v);
    }

    public synchronized V remove(K e) {
        return side(e).remove(e);
    }
}
