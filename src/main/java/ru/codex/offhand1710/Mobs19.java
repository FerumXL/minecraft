package ru.codex.offhand1710;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import java.util.Random;
import java.util.UUID;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSilverfish;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILeapAtTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIMoveTowardsRestriction;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.EntitySilverfish;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.pathfinding.PathEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 * Spiders, cave spiders, silverfish and zombie pigmen with the task AI of 1.9 - 1.12.2 instead of the old AI 1.7.10
 * still gave them: the coremod makes isAIEnabled true for them (Mobs19.aiEnabled), here they get the tasks and the
 * movement speeds of 1.12.2 (Speeds112 leaves them alone then).
 * Spider: leaps at the target, gives up in daylight, hunts only in the dark, climbs as before.
 * Silverfish: calls its kin out of nearby infested stone when hurt, hides in stone when idle.
 * Pigman: the zombie tasks of 1.12.2, attacks only when angry, the anger and its speed boost of 1.12.2.
 * -Dmc112.mobAI17 keeps the old AI.
 */
public final class Mobs19 {
    public static final boolean OFF = System.getProperty("mc112.mobAI17") != null;
    private static final UUID BOOST = UUID.fromString("49455A49-7EC5-45BA-B886-3B90B23A1718");
    private static final UUID BOOST_112 = UUID.fromString("5B6E1C7A-3D1F-4C1A-9E44-0A9D6F2B1121");
    private static final AttributeModifier ANGRY = new AttributeModifier(BOOST_112, "Attacking speed boost", 0.05D, 0).setSaved(false);

    public Mobs19() {
    }

    /** The coremod calls this from isAIEnabled of EntitySpider, EntitySilverfish and EntityPigZombie. */
    public static boolean aiEnabled() {
        return !OFF;
    }

    public static boolean handles(Object e) {
        return !OFF && (e instanceof EntitySpider || e instanceof EntitySilverfish || e instanceof EntityPigZombie);
    }

    private static void clear(EntityAITasks tasks) {
        for (Object o : tasks.taskEntries.toArray()) {
            tasks.removeTask(((EntityAITasks.EntityAITaskEntry) o).action);
        }
    }

    private static boolean has(EntityAITasks tasks, Class<?> type) {
        for (Object o : tasks.taskEntries) {
            if (type.isInstance(((EntityAITasks.EntityAITaskEntry) o).action)) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent event) {
        if (OFF || event.world.isRemote || !handles(event.entity)) {
            return;
        }
        EntityCreature e = (EntityCreature) event.entity;
        if (has(e.tasks, Melee.class)) {
            return;
        }
        clear(e.tasks);
        clear(e.targetTasks);
        IAttributeInstance speed = e.getEntityAttribute(SharedMonsterAttributes.movementSpeed);
        if (e instanceof EntitySpider) {
            speed.setBaseValue(0.30000001192092896D);
            e.tasks.addTask(1, new EntityAISwimming(e));
            e.tasks.addTask(3, new EntityAILeapAtTarget(e, 0.4F));
            e.tasks.addTask(4, new Melee(e, 1.0D, true, 4.0D, true));
            e.tasks.addTask(5, new EntityAIWander(e, 0.8D));
            e.tasks.addTask(6, new EntityAIWatchClosest(e, EntityPlayer.class, 8.0F));
            e.tasks.addTask(6, new EntityAILookIdle(e));
            e.targetTasks.addTask(1, new EntityAIHurtByTarget(e, false));
            e.targetTasks.addTask(2, new DarkTarget(e, EntityPlayer.class));
            e.targetTasks.addTask(3, new DarkTarget(e, EntityIronGolem.class));
        } else if (e instanceof EntitySilverfish) {
            speed.setBaseValue(0.25D);
            Summon summon = new Summon((EntitySilverfish) e);
            e.tasks.addTask(1, new EntityAISwimming(e));
            e.tasks.addTask(3, summon);
            e.tasks.addTask(4, new Melee(e, 1.0D, false, -1.0D, false));
            e.tasks.addTask(5, new Hide((EntitySilverfish) e));
            e.targetTasks.addTask(1, new EntityAIHurtByTarget(e, true));
            e.targetTasks.addTask(2, new EntityAINearestAttackableTarget(e, EntityPlayer.class, 0, true));
        } else {
            speed.setBaseValue(0.23000000417232513D);
            if (speed.getModifier(BOOST) != null) {
                speed.removeModifier(speed.getModifier(BOOST));
            }
            e.tasks.addTask(0, new EntityAISwimming(e));
            e.tasks.addTask(2, new Melee(e, 1.0D, false, -1.0D, false));
            e.tasks.addTask(5, new EntityAIMoveTowardsRestriction(e, 1.0D));
            e.tasks.addTask(7, new EntityAIWander(e, 1.0D));
            e.tasks.addTask(8, new EntityAIWatchClosest(e, EntityPlayer.class, 8.0F));
            e.tasks.addTask(8, new EntityAILookIdle(e));
            e.targetTasks.addTask(1, new HurtByAggressor((EntityPigZombie) e));
            e.targetTasks.addTask(2, new TargetAggressor((EntityPigZombie) e));
        }
    }

    /** EntitySilverfish.attackEntityFrom of 1.12.2: hit by an entity or magic, it calls for help. */
    @SubscribeEvent
    public void onHurt(LivingHurtEvent event) {
        if (OFF || !(event.entityLiving instanceof EntitySilverfish) || event.entityLiving.worldObj.isRemote) {
            return;
        }
        if (event.source instanceof EntityDamageSource || event.source == DamageSource.magic) {
            for (Object o : ((EntitySilverfish) event.entityLiving).tasks.taskEntries) {
                Object a = ((EntityAITasks.EntityAITaskEntry) o).action;
                if (a instanceof Summon) {
                    ((Summon) a).notifyHurt();
                }
            }
        }
    }

    /** EntityPigZombie.updateAITasks of 1.12.2: anger counts down, the 0.05 speed boost while angry. */
    @SubscribeEvent
    public void onUpdate(LivingUpdateEvent event) {
        if (OFF || !(event.entityLiving instanceof EntityPigZombie) || event.entityLiving.worldObj.isRemote) {
            return;
        }
        EntityPigZombie p = (EntityPigZombie) event.entityLiving;
        IAttributeInstance speed = p.getEntityAttribute(SharedMonsterAttributes.movementSpeed);
        int anger = anger(p);
        if (anger > 0) {
            if (!p.isChild() && speed.getModifier(BOOST_112) == null) {
                speed.applyModifier(ANGRY);
            }
            setAnger(p, anger - 1);
        } else if (speed.getModifier(BOOST_112) != null) {
            speed.removeModifier(ANGRY);
        }
        // the 1.7.10 boost (0.45) follows entityToAttack of the old AI; it must not stay on
        if (speed.getModifier(BOOST) != null) {
            speed.removeModifier(speed.getModifier(BOOST));
        }
    }

    public static int anger(EntityPigZombie p) {
        return ReflectionHelper.<Integer, EntityPigZombie>getPrivateValue(EntityPigZombie.class, p, "angerLevel", "field_70837_d");
    }

    private static void setAnger(EntityPigZombie p, int v) {
        ReflectionHelper.setPrivateValue(EntityPigZombie.class, p, Integer.valueOf(v), "angerLevel", "field_70837_d");
    }

    /** EntityPigZombie.becomeAngryAt of 1.12.2: 400 - 799 ticks, a sound soon. */
    static void becomeAngryAt(EntityPigZombie p, EntityLivingBase target) {
        setAnger(p, 400 + p.getRNG().nextInt(400));
        ReflectionHelper.setPrivateValue(EntityPigZombie.class, p, Integer.valueOf(p.getRNG().nextInt(40)), "randomSoundDelay", "field_70838_e");
        p.setRevengeTarget(target);
    }

    // ---- tasks ----

    /** EntityAIAttackMelee of 1.12.2, with the reach a mob may change (spider 4 + target width), and the spider's daylight rule. */
    public static final class Melee extends EntityAIBase {
        private final EntityCreature attacker;
        private final double speed;
        private final boolean longMemory;
        /** The fixed part of the reach, or -1 for (width * 2)^2. */
        private final double reach;
        private final boolean spider;
        private int attackTick;
        private PathEntity path;
        private int delayCounter;
        private double targetX;
        private double targetY;
        private double targetZ;

        Melee(EntityCreature attacker, double speed, boolean longMemory, double reach, boolean spider) {
            this.attacker = attacker;
            this.speed = speed;
            this.longMemory = longMemory;
            this.reach = reach;
            this.spider = spider;
            setMutexBits(3);
        }

        private double reachSqr(EntityLivingBase target) {
            double base = reach >= 0.0D ? reach : (double) (attacker.width * 2.0F * attacker.width * 2.0F);
            return base + (double) target.width;
        }

        @Override
        public boolean shouldExecute() {
            EntityLivingBase target = attacker.getAttackTarget();
            if (target == null || !target.isEntityAlive()) {
                return false;
            }
            path = attacker.getNavigator().getPathToEntityLiving(target);
            return path != null || reachSqr(target) >= attacker.getDistanceSq(target.posX, target.boundingBox.minY, target.posZ);
        }

        @Override
        public boolean continueExecuting() {
            // EntitySpider.AISpiderAttack: in bright light it may give up (1 in 100 a tick)
            if (spider && attacker.getBrightness(1.0F) >= 0.5F && attacker.getRNG().nextInt(100) == 0) {
                attacker.setAttackTarget(null);
                return false;
            }
            EntityLivingBase target = attacker.getAttackTarget();
            if (target == null || !target.isEntityAlive()) {
                return false;
            }
            if (!longMemory) {
                return !attacker.getNavigator().noPath();
            }
            if (!attacker.isWithinHomeDistance(MathHelper.floor_double(target.posX), MathHelper.floor_double(target.posY), MathHelper.floor_double(target.posZ))) {
                return false;
            }
            return !(target instanceof EntityPlayer) || !((EntityPlayer) target).capabilities.isCreativeMode && !Spectator112.is((EntityPlayer) target);
        }

        @Override
        public void startExecuting() {
            attacker.getNavigator().setPath(path, speed);
            delayCounter = 0;
        }

        @Override
        public void resetTask() {
            EntityLivingBase target = attacker.getAttackTarget();
            if (target instanceof EntityPlayer && (((EntityPlayer) target).capabilities.isCreativeMode || Spectator112.is((EntityPlayer) target))) {
                attacker.setAttackTarget(null);
            }
            attacker.getNavigator().clearPathEntity();
        }

        @Override
        public void updateTask() {
            EntityLivingBase target = attacker.getAttackTarget();
            if (target == null) {
                return;
            }
            attacker.getLookHelper().setLookPositionWithEntity(target, 30.0F, 30.0F);
            double d0 = attacker.getDistanceSq(target.posX, target.boundingBox.minY, target.posZ);
            --delayCounter;
            if ((longMemory || attacker.getEntitySenses().canSee(target)) && delayCounter <= 0
                && (targetX == 0.0D && targetY == 0.0D && targetZ == 0.0D || target.getDistanceSq(targetX, targetY, targetZ) >= 1.0D || attacker.getRNG().nextFloat() < 0.05F)) {
                targetX = target.posX;
                targetY = target.boundingBox.minY;
                targetZ = target.posZ;
                delayCounter = 4 + attacker.getRNG().nextInt(7);
                if (d0 > 1024.0D) {
                    delayCounter += 10;
                } else if (d0 > 256.0D) {
                    delayCounter += 5;
                }
                if (!attacker.getNavigator().tryMoveToEntityLiving(target, speed)) {
                    delayCounter += 15;
                }
            }
            attackTick = Math.max(attackTick - 1, 0);
            if (d0 <= reachSqr(target) && attackTick <= 0) {
                attackTick = 20;
                attacker.swingItem();
                attacker.attackEntityAsMob(target);
            }
        }
    }

    /** EntitySpider.AISpiderTarget: hunts only where it is dark. */
    public static final class DarkTarget extends EntityAINearestAttackableTarget {
        private final EntityCreature owner;

        DarkTarget(EntityCreature owner, Class<?> type) {
            super(owner, type, 0, true);
            this.owner = owner;
        }

        @Override
        public boolean shouldExecute() {
            return owner.getBrightness(1.0F) < 0.5F && super.shouldExecute();
        }
    }

    /** EntityPigZombie.AIHurtByAggressor: the attacker and the pigmen around become angry at it. */
    public static final class HurtByAggressor extends EntityAIHurtByTarget {
        private final EntityPigZombie pigman;

        HurtByAggressor(EntityPigZombie pigman) {
            super(pigman, true);
            this.pigman = pigman;
        }

        @Override
        public void startExecuting() {
            super.startExecuting();
            EntityLivingBase target = pigman.getAITarget();
            if (target != null) {
                becomeAngryAt(pigman, target);
                double r = 16.0D;
                for (Object o : pigman.worldObj.getEntitiesWithinAABB(EntityPigZombie.class, pigman.boundingBox.expand(r, 10.0D, r))) {
                    EntityPigZombie other = (EntityPigZombie) o;
                    if (other != pigman && other.getAttackTarget() == null && !other.isOnSameTeam(target)) {
                        other.setAttackTarget(target);
                        becomeAngryAt(other, target);
                    }
                }
            }
        }
    }

    /** EntityPigZombie.AITargetAggressor: the nearest player, only while angry. */
    public static final class TargetAggressor extends EntityAINearestAttackableTarget {
        private final EntityPigZombie pigman;

        TargetAggressor(EntityPigZombie pigman) {
            super(pigman, EntityPlayer.class, 0, true);
            this.pigman = pigman;
        }

        @Override
        public boolean shouldExecute() {
            return anger(pigman) > 0 && super.shouldExecute();
        }
    }

    /** EntitySilverfish.AISummonSilverfish of 1.12.2. */
    public static final class Summon extends EntityAIBase {
        private final EntitySilverfish silverfish;
        private int lookForFriends;

        Summon(EntitySilverfish silverfish) {
            this.silverfish = silverfish;
        }

        public void notifyHurt() {
            if (lookForFriends == 0) {
                lookForFriends = 20;
            }
        }

        @Override
        public boolean shouldExecute() {
            return lookForFriends > 0;
        }

        @Override
        public void updateTask() {
            --lookForFriends;
            if (lookForFriends > 0) {
                return;
            }
            World w = silverfish.worldObj;
            Random rand = silverfish.getRNG();
            int bx = MathHelper.floor_double(silverfish.posX);
            int by = MathHelper.floor_double(silverfish.posY);
            int bz = MathHelper.floor_double(silverfish.posZ);
            boolean griefing = w.getGameRules().getGameRuleBooleanValue("mobGriefing");
            for (int i = 0; i <= 5 && i >= -5; i = (i <= 0 ? 1 : 0) - i) {
                for (int j = 0; j <= 10 && j >= -10; j = (j <= 0 ? 1 : 0) - j) {
                    for (int k = 0; k <= 10 && k >= -10; k = (k <= 0 ? 1 : 0) - k) {
                        int x = bx + j;
                        int y = by + i;
                        int z = bz + k;
                        if (w.getBlock(x, y, z) == Blocks.monster_egg) {
                            int meta = w.getBlockMetadata(x, y, z);
                            if (griefing) {
                                // destroyBlock of 1.12.2 lets the silverfish out (BlockSilverfish.dropBlockAsItemWithChance)
                                w.func_147480_a(x, y, z, false);
                                Blocks.monster_egg.onBlockDestroyedByPlayer(w, x, y, z, meta);
                            } else {
                                ImmutablePair<?, ?> model = BlockSilverfish.func_150197_b(meta);
                                w.setBlock(x, y, z, (Block) model.getLeft(), (Integer) model.getRight(), 3);
                            }
                            if (rand.nextBoolean()) {
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    /** EntitySilverfish.AIHideInStone of 1.12.2: idle, it may turn a stone next to it into infested stone. */
    public static final class Hide extends EntityAIBase {
        private final EntitySilverfish silverfish;
        private int side;
        private boolean doMerge;
        private double wx;
        private double wy;
        private double wz;

        Hide(EntitySilverfish silverfish) {
            this.silverfish = silverfish;
            setMutexBits(1);
        }

        /** EntityAIWander(entity, 1.0, 10) of 1.12.2: the 1.7.10 task has no chance argument (always 1 in 120). */
        private boolean wander() {
            if (silverfish.getRNG().nextInt(10) != 0) {
                return false;
            }
            net.minecraft.util.Vec3 v = net.minecraft.entity.ai.RandomPositionGenerator.findRandomTarget(silverfish, 10, 7);
            if (v == null) {
                return false;
            }
            wx = v.xCoord;
            wy = v.yCoord;
            wz = v.zCoord;
            return true;
        }

        @Override
        public boolean shouldExecute() {
            if (silverfish.getAttackTarget() != null || !silverfish.getNavigator().noPath()) {
                return false;
            }
            Random rand = silverfish.getRNG();
            if (silverfish.worldObj.getGameRules().getGameRuleBooleanValue("mobGriefing") && rand.nextInt(10) == 0) {
                side = rand.nextInt(6);
                int[] p = target();
                Block b = silverfish.worldObj.getBlock(p[0], p[1], p[2]);
                if (BlockSilverfish.func_150196_a(b)) {
                    doMerge = true;
                    return true;
                }
            }
            doMerge = false;
            return wander();
        }

        private int[] target() {
            int x = MathHelper.floor_double(silverfish.posX);
            int y = MathHelper.floor_double(silverfish.posY + 0.5D);
            int z = MathHelper.floor_double(silverfish.posZ);
            int[][] off = {{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};
            return new int[] {x + off[side][0], y + off[side][1], z + off[side][2]};
        }

        @Override
        public boolean continueExecuting() {
            return !doMerge && !silverfish.getNavigator().noPath();
        }

        @Override
        public void startExecuting() {
            if (!doMerge) {
                silverfish.getNavigator().tryMoveToXYZ(wx, wy, wz, 1.0D);
                return;
            }
            World w = silverfish.worldObj;
            int[] p = target();
            Block b = w.getBlock(p[0], p[1], p[2]);
            if (BlockSilverfish.func_150196_a(b)) {
                w.setBlock(p[0], p[1], p[2], Blocks.monster_egg, BlockSilverfish.func_150195_a(b, w.getBlockMetadata(p[0], p[1], p[2])), 3);
                silverfish.spawnExplosionParticle();
                silverfish.setDead();
            }
        }
    }
}
