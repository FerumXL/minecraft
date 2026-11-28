package ru.codex.offhand1710;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import java.lang.reflect.Field;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIArrowAttack;
import net.minecraft.entity.ai.EntityAIAvoidEntity;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.ai.EntityMoveHelper;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.init.Items;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumDifficulty;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;

/**
 * The skeleton of 1.9 - 1.12.2 (AbstractSkeleton, EntityAIAttackRangedBow): it draws the bow for 20 ticks and shoots
 * at full strength, then waits 40 ticks (20 on hard); within 15 blocks and seeing the target for a second it stops,
 * strafes around it and backs off when close. It flees from wolves and attacks iron golems.
 * -Dmc112.skeleton17 leaves the skeleton of 1.7.10.
 */
public final class Skeleton112 {
    public static final boolean OFF = System.getProperty("mc112.skeleton17") != null;
    private static Field arrowAttack;
    private static Field moveHelper;

    public Skeleton112() {
    }

    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent event) {
        if (OFF || event.world.isRemote || !(event.entity instanceof EntitySkeleton)) {
            return;
        }
        EntitySkeleton s = (EntitySkeleton) event.entity;
        if (has(s.tasks, BowAttack.class)) {
            return;
        }
        try {
            if (arrowAttack == null) {
                arrowAttack = ReflectionHelper.findField(EntitySkeleton.class, "aiArrowAttack", "field_85037_d");
                moveHelper = ReflectionHelper.findField(EntityLiving.class, "moveHelper", "field_70765_h");
            }
            // the task of 1.7.10 may already be in the list (an item given before spawning): take it out first
            Object old = arrowAttack.get(s);
            if (old instanceof net.minecraft.entity.ai.EntityAIBase) {
                s.tasks.removeTask((net.minecraft.entity.ai.EntityAIBase) old);
            }
            arrowAttack.set(s, new BowAttack(s));
            if (!(s.getMoveHelper() instanceof Strafer)) {
                moveHelper.set(s, new Strafer(s));
            }
        } catch (IllegalAccessException e) {
            return;
        }
        s.setCombatTask();
        s.tasks.addTask(3, new EntityAIAvoidEntity(s, EntityWolf.class, 6.0F, 1.0D, 1.2D));
        s.targetTasks.addTask(3, new EntityAINearestAttackableTarget(s, EntityIronGolem.class, 0, true));
    }

    private static boolean has(EntityAITasks tasks, Class<?> type) {
        for (Object o : tasks.taskEntries) {
            if (type.isInstance(((EntityAITasks.EntityAITaskEntry) o).action)) {
                return true;
            }
        }
        return false;
    }

    /** EntityMoveHelper of 1.12.2 with the STRAFE action (the 1.7.10 helper resets moveForward every tick). */
    public static final class Strafer extends EntityMoveHelper {
        private final EntityLiving entity;
        private boolean strafing;
        private float forward;
        private float side;

        public Strafer(EntityLiving entity) {
            super(entity);
            this.entity = entity;
        }

        /** EntityMoveHelper.strafe. */
        public void strafe(float forward, float side) {
            this.strafing = true;
            this.forward = forward;
            this.side = side;
        }

        @Override
        public void onUpdateMoveHelper() {
            if (!strafing) {
                super.onUpdateMoveHelper();
                return;
            }
            strafing = false;
            float speedAttr = (float) entity.getEntityAttribute(SharedMonsterAttributes.movementSpeed).getAttributeValue();
            float speed = speedAttr;
            float f2 = forward;
            float f3 = side;
            float len = MathHelper.sqrt_float(f2 * f2 + f3 * f3);
            if (len < 1.0F) {
                len = 1.0F;
            }
            len = speed / len;
            f2 *= len;
            f3 *= len;
            float sin = MathHelper.sin(entity.rotationYaw * 0.017453292F);
            float cos = MathHelper.cos(entity.rotationYaw * 0.017453292F);
            float dx = f2 * cos - f3 * sin;
            float dz = f3 * cos + f2 * sin;
            // PathNodeType.WALKABLE of 1.12.2: free for the body, something to stand on
            int x = MathHelper.floor_double(entity.posX + dx);
            int y = MathHelper.floor_double(entity.posY);
            int z = MathHelper.floor_double(entity.posZ + dz);
            Block feet = entity.worldObj.getBlock(x, y, z);
            Block below = entity.worldObj.getBlock(x, y - 1, z);
            float f = forward;
            float s = side;
            if (feet.getMaterial().blocksMovement() || feet.getMaterial().isLiquid() || !below.getMaterial().blocksMovement()) {
                f = 1.0F;
                s = 0.0F;
                speed = speedAttr;
            }
            entity.setAIMoveSpeed(speed);
            entity.setMoveForward(f);
            entity.moveStrafing = s;
        }
    }

    /** EntityAIAttackRangedBow of 1.12.2, in place of EntityAIArrowAttack (the field of EntitySkeleton has that type). */
    public static final class BowAttack extends EntityAIArrowAttack {
        private final EntitySkeleton entity;
        private final double moveSpeedAmp = 1.0D;
        private final float maxAttackDistance = 15.0F * 15.0F;
        private int attackTime = -1;
        private int seeTime;
        private boolean strafingClockwise;
        private boolean strafingBackwards;
        private int strafingTime = -1;
        /** The active hand of 1.9: ticks the bow has been drawn, -1 when not drawing. */
        public int drawTicks = -1;
        /** For the autotest: arrows shot and ticks spent strafing. */
        public int shots;
        /** entity.ticksExisted at each shot. */
        public final java.util.List<Integer> shotTicks = new java.util.ArrayList<Integer>();
        public int strafed;

        public BowAttack(EntitySkeleton entity) {
            super(entity, 1.0D, 20, 15.0F);
            this.entity = entity;
            setMutexBits(3);
        }

        private boolean bowInHand() {
            return entity.getHeldItem() != null && entity.getHeldItem().getItem() == Items.bow;
        }

        /** setAttackCooldown in AbstractSkeleton.setCombatTask: 40 ticks, 20 on hard. */
        private int cooldown() {
            return entity.worldObj.difficultySetting == EnumDifficulty.HARD ? 20 : 40;
        }

        @Override
        public boolean shouldExecute() {
            return entity.getAttackTarget() != null && bowInHand();
        }

        @Override
        public boolean continueExecuting() {
            return (shouldExecute() || !entity.getNavigator().noPath()) && bowInHand();
        }

        @Override
        public void startExecuting() {
        }

        @Override
        public void resetTask() {
            seeTime = 0;
            attackTime = -1;
            drawTicks = -1;
        }

        @Override
        public void updateTask() {
            EntityLivingBase target = entity.getAttackTarget();
            if (target == null) {
                return;
            }
            double d0 = entity.getDistanceSq(target.posX, target.boundingBox.minY, target.posZ);
            boolean see = entity.getEntitySenses().canSee(target);
            boolean saw = seeTime > 0;
            if (see != saw) {
                seeTime = 0;
            }
            if (see) {
                ++seeTime;
            } else {
                --seeTime;
            }
            if (d0 <= (double) maxAttackDistance && seeTime >= 20) {
                entity.getNavigator().clearPathEntity();
                ++strafingTime;
            } else {
                entity.getNavigator().tryMoveToEntityLiving(target, moveSpeedAmp);
                strafingTime = -1;
            }
            if (strafingTime >= 20) {
                if (entity.getRNG().nextFloat() < 0.3F) {
                    strafingClockwise = !strafingClockwise;
                }
                if (entity.getRNG().nextFloat() < 0.3F) {
                    strafingBackwards = !strafingBackwards;
                }
                strafingTime = 0;
            }
            if (strafingTime > -1) {
                if (d0 > (double) (maxAttackDistance * 0.75F)) {
                    strafingBackwards = false;
                } else if (d0 < (double) (maxAttackDistance * 0.25F)) {
                    strafingBackwards = true;
                }
                if (entity.getMoveHelper() instanceof Strafer) {
                    ((Strafer) entity.getMoveHelper()).strafe(strafingBackwards ? -0.5F : 0.5F, strafingClockwise ? 0.5F : -0.5F);
                    ++strafed;
                }
                entity.faceEntity(target, 30.0F, 30.0F);
            } else {
                entity.getLookHelper().setLookPositionWithEntity(target, 30.0F, 30.0F);
            }
            if (drawTicks >= 0) {
                if (!see && seeTime < -60) {
                    drawTicks = -1;
                } else if (see) {
                    ++drawTicks;
                    if (drawTicks >= 20) {
                        drawTicks = -1;
                        // ItemBow.getArrowVelocity(20) = 1: full strength
                        entity.attackEntityWithRangedAttack(target, 1.0F);
                        ++shots;
                        shotTicks.add(entity.ticksExisted);
                        attackTime = cooldown();
                    }
                }
            } else if (--attackTime <= 0 && seeTime >= -60) {
                drawTicks = 0;
            }
        }
    }
}
