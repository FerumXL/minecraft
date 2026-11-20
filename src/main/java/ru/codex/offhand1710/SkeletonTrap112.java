package ru.codex.offhand1710;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;

/**
 * Skeleton traps of 1.12.2 (WorldServer.updateBlocks, EntitySkeletonHorse, EntityAISkeletonRiders): a thunderstorm bolt
 * becomes, with chance additionalDifficulty x 1 %, a harmless bolt and a trap skeleton horse. When a player comes within
 * 10 blocks the trap fires: a harmless bolt, the horse turns tame and gets a rider, and three more skeleton horsemen
 * appear; riders have an enchanted bow and iron helmet (level 5 + clampedDifficulty x nextInt(18)). An untriggered trap
 * vanishes after 18000 ticks. 1.7.10 has the skeleton horse as horse type 4; the trap state lives in the entity data.
 */
public final class SkeletonTrap112 {
    static final String TRAP = "mc112SkeletonTrap", TIME = "mc112SkeletonTrapTime";

    /** Replaces World.addWeatherEffect for the bolt of a thunderstorm in WorldServer.func_147456_g. */
    public static boolean strike(World world, Entity bolt) {
        if (bolt instanceof EntityLightningBolt && world.getGameRules().getGameRuleBooleanValue("doMobSpawning")
            && world.rand.nextDouble() < (double) additionalDifficulty(world, (int) bolt.posX, (int) bolt.posZ) * 0.01D) {
            spawnTrap(world, bolt.posX, bolt.posY, bolt.posZ);
            return true;
        }
        return world.addWeatherEffect(bolt);
    }

    public static EntityHorse spawnTrap(World world, double x, double y, double z) {
        EntityHorse horse = new EntityHorse(world);
        horse.setHorseType(4);
        horse.setGrowingAge(0);
        horse.setPosition(x, y, z);
        horse.getEntityData().setBoolean(TRAP, true);
        horse.func_110163_bv();
        world.spawnEntityInWorld(horse);
        world.addWeatherEffect(new HarmlessBolt(world, x, y, z));
        return horse;
    }

    /** DifficultyInstance.calculateAdditionalDifficulty of 1.12.2. */
    public static float additionalDifficulty(World world, int x, int z) {
        EnumDifficulty d = world.difficultySetting;
        if (d == EnumDifficulty.PEACEFUL) {
            return 0.0F;
        }
        boolean hard = d == EnumDifficulty.HARD;
        float f1 = MathHelper.clamp_float(((float) world.getWorldTime() - 72000.0F) / 1440000.0F, 0.0F, 1.0F) * 0.25F;
        float f = 0.75F + f1;
        Chunk c = world.blockExists(x, 64, z) ? world.getChunkFromBlockCoords(x, z) : null;
        float f2 = MathHelper.clamp_float((float) (c == null ? 0L : c.inhabitedTime) / 3600000.0F, 0.0F, 1.0F) * (hard ? 1.0F : 0.75F);
        f2 += MathHelper.clamp_float(world.getCurrentMoonPhaseFactor() * 0.25F, 0.0F, f1);
        if (d == EnumDifficulty.EASY) {
            f2 *= 0.5F;
        }
        return (float) d.getDifficultyId() * (f + f2);
    }

    static float clamped(World world, int x, int z) {
        float f = additionalDifficulty(world, x, z);
        return f < 2.0F ? 0.0F : f > 4.0F ? 1.0F : (f - 2.0F) / 2.0F;
    }

    public static boolean isTrap(EntityHorse horse) {
        return horse.getEntityData().getBoolean(TRAP);
    }

    @SubscribeEvent
    public void onUpdate(LivingUpdateEvent event) {
        if (!(event.entityLiving instanceof EntityHorse) || event.entityLiving.worldObj.isRemote) {
            return;
        }
        EntityHorse horse = (EntityHorse) event.entityLiving;
        NBTTagCompound data = horse.getEntityData();
        if (!data.getBoolean(TRAP)) {
            return;
        }
        int t = data.getInteger(TIME) + 1;
        data.setInteger(TIME, t);
        if (t >= 18000) {
            horse.setDead();
            return;
        }
        EntityPlayer near = horse.worldObj.getClosestPlayer(horse.posX, horse.posY, horse.posZ, 10.0D);
        if (near != null && !Spectator112.is(near)) {
            trigger(horse);
        }
    }

    /** EntityAISkeletonRiders.updateTask of 1.12.2. */
    public static void trigger(EntityHorse horse) {
        World world = horse.worldObj;
        horse.getEntityData().removeTag(TRAP);
        horse.getEntityData().removeTag(TIME);
        horse.setHorseTamed(true);
        horse.setGrowingAge(0);
        world.addWeatherEffect(new HarmlessBolt(world, horse.posX, horse.posY, horse.posZ));
        EntitySkeleton rider = rider(horse);
        rider.mountEntity(horse);
        for (int i = 0; i < 3; i++) {
            EntityHorse h = horse(horse);
            EntitySkeleton s = rider(h);
            s.mountEntity(h);
            h.addVelocity(horse.getRNG().nextGaussian() * 0.5D, 0.0D, horse.getRNG().nextGaussian() * 0.5D);
        }
    }

    private static EntityHorse horse(EntityHorse at) {
        EntityHorse h = new EntityHorse(at.worldObj);
        h.onSpawnWithEgg(null);
        h.setHorseType(4);
        h.setPosition(at.posX, at.posY, at.posZ);
        h.hurtResistantTime = 60;
        h.func_110163_bv();
        h.setHorseTamed(true);
        h.setGrowingAge(0);
        at.worldObj.spawnEntityInWorld(h);
        return h;
    }

    private static EntitySkeleton rider(EntityHorse horse) {
        World world = horse.worldObj;
        EntitySkeleton s = new EntitySkeleton(world);
        s.onSpawnWithEgg(null);
        s.setSkeletonType(0);
        s.setPosition(horse.posX, horse.posY, horse.posZ);
        s.hurtResistantTime = 60;
        s.func_110163_bv();
        if (s.getEquipmentInSlot(4) == null) {
            s.setCurrentItemOrArmor(4, new ItemStack(Items.iron_helmet));
        }
        float c = clamped(world, (int) horse.posX, (int) horse.posZ);
        ItemStack bow = s.getHeldItem() != null ? s.getHeldItem() : new ItemStack(Items.bow);
        s.setCurrentItemOrArmor(0, EnchantmentHelper.addRandomEnchantment(s.getRNG(), bow, (int) (5.0F + c * (float) s.getRNG().nextInt(18))));
        s.setCurrentItemOrArmor(4, EnchantmentHelper.addRandomEnchantment(s.getRNG(), s.getEquipmentInSlot(4), (int) (5.0F + c * (float) s.getRNG().nextInt(18))));
        world.spawnEntityInWorld(s);
        return s;
    }

    /** EntityLightningBolt with effectOnly of 1.12.2: thunder and the flash, no fire and no damage. */
    public static final class HarmlessBolt extends EntityLightningBolt {
        private int life = 2;

        /** The 1.7.10 constructor sets fire where it is made: made below the world, then moved. */
        public HarmlessBolt(World world, double x, double y, double z) {
            super(world, x, -10.0D, z);
            setLocationAndAngles(x, y, z, 0.0F, 0.0F);
        }

        @Override
        public void onUpdate() {
            if (life == 2) {
                worldObj.playSoundEffect(posX, posY, posZ, "ambient.weather.thunder", 10000.0F, 0.8F + rand.nextFloat() * 0.2F);
                worldObj.playSoundEffect(posX, posY, posZ, "random.explode", 2.0F, 0.5F + rand.nextFloat() * 0.2F);
            }
            if (--life < 0) {
                setDead();
            }
        }
    }
}
