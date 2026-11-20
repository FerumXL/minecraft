package ru.codex.offhand1710;

import java.util.List;
import java.util.Map;

import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.BiomeGenPlains;
import net.minecraft.world.biome.BiomeGenSavanna;

/**
 * Own spawn entries of 1.12.2 for mobs that 1.7.10 spawns as a random variant of one entity:
 * EntityDonkey (plains 1 x 1-3, savanna 1 x 1) next to EntityHorse (always a horse), and EntityZombieVillager
 * (5 of every 100 zombie weight, 1 next to the 19 desert zombies, groups of 1) next to EntityZombie (never a villager).
 * The spawn classes are subclasses mapped in EntityList to the vanilla name and network id, so they are saved
 * and sent to clients as plain EntityHorse / Zombie and load back as such.
 */
public final class Variants112 {
    private Variants112() {
    }

    public static class Horse extends EntityHorse {
        public Horse(World world) {
            super(world);
        }

        @Override
        public IEntityLivingData onSpawnWithEgg(IEntityLivingData data) {
            // GroupData(type, variant): 1.12 EntityHorse.onInitialSpawn, the herd shares a colour, markings per horse
            return super.onSpawnWithEgg(data instanceof EntityHorse.GroupData ? data : new EntityHorse.GroupData(0, rand.nextInt(7)));
        }
    }

    public static class Donkey extends EntityHorse {
        public Donkey(World world) {
            super(world);
        }

        @Override
        public IEntityLivingData onSpawnWithEgg(IEntityLivingData data) {
            return super.onSpawnWithEgg(data instanceof EntityHorse.GroupData ? data : new EntityHorse.GroupData(1, 0));
        }
    }

    public static class Zombie extends EntityZombie {
        public Zombie(World world) {
            super(world);
        }

        @Override
        public IEntityLivingData onSpawnWithEgg(IEntityLivingData data) {
            IEntityLivingData r = super.onSpawnWithEgg(data);
            setVillager(false);
            return r;
        }
    }

    public static class ZombieVillager extends EntityZombie {
        public ZombieVillager(World world) {
            super(world);
        }

        @Override
        public IEntityLivingData onSpawnWithEgg(IEntityLivingData data) {
            IEntityLivingData r = super.onSpawnWithEgg(data);
            setVillager(true);
            return r;
        }
    }

    @SuppressWarnings("unchecked")
    public static void register() {
        if ("1".equals(System.getProperty("mc112.variants17"))) {
            return;
        }
        Map<Class<?>, String> classToString = ReflectionHelper.getPrivateValue(EntityList.class, null, "classToStringMapping", "field_75626_c");
        Map<Class<?>, Integer> classToId = ReflectionHelper.getPrivateValue(EntityList.class, null, "classToIDMapping", "field_75624_e");
        for (Class<?>[] pair : new Class<?>[][] {{Horse.class, EntityHorse.class}, {Donkey.class, EntityHorse.class},
            {Zombie.class, EntityZombie.class}, {ZombieVillager.class, EntityZombie.class}}) {
            classToString.put(pair[0], classToString.get(pair[1]));
            classToId.put(pair[0], classToId.get(pair[1]));
        }
        for (BiomeGenBase b : BiomeGenBase.getBiomeGenArray()) {
            if (b == null) {
                continue;
            }
            List<BiomeGenBase.SpawnListEntry> creatures = b.getSpawnableList(EnumCreatureType.creature);
            for (int i = 0; i < creatures.size(); i++) {
                BiomeGenBase.SpawnListEntry e = creatures.get(i);
                if (e.entityClass == EntityHorse.class) {
                    creatures.set(i, new BiomeGenBase.SpawnListEntry(Horse.class, e.itemWeight, e.minGroupCount, e.maxGroupCount));
                    if (b instanceof BiomeGenSavanna) {
                        creatures.add(new BiomeGenBase.SpawnListEntry(Donkey.class, 1, 1, 1));
                    } else if (b instanceof BiomeGenPlains) {
                        creatures.add(new BiomeGenBase.SpawnListEntry(Donkey.class, 1, 1, 3));
                    }
                    break;
                }
            }
            List<BiomeGenBase.SpawnListEntry> monsters = b.getSpawnableList(EnumCreatureType.monster);
            for (int i = 0; i < monsters.size(); i++) {
                BiomeGenBase.SpawnListEntry e = monsters.get(i);
                if (e.entityClass == EntityZombie.class) {
                    int w = e.itemWeight;
                    int zombie = w == 19 ? 19 : w * 95 / 100, villager = w == 19 ? 1 : Math.max(1, w * 5 / 100);
                    monsters.set(i, new BiomeGenBase.SpawnListEntry(Zombie.class, zombie, e.minGroupCount, e.maxGroupCount));
                    monsters.add(new BiomeGenBase.SpawnListEntry(ZombieVillager.class, villager, 1, 1));
                    break;
                }
            }
        }
    }
}
