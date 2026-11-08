package ru.codex.offhand1710;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackOnCollide;
import net.minecraft.entity.ai.EntityAIFollowParent;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAIPanic;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemFishFood;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

/** EntityPolarBear of 1.12.2: neutral, but attacks players near its cubs and rears up before the bite. */
public class EntityPolarBear extends EntityAnimal {
    private static final int WATCH_STANDING = 18;
    private static final String SOUND = MC112Bridge.MOD_ID + ":entity.polar_bear.";

    private float standing;
    private float prevStanding;
    private int warningCooldown;
    private int standTimer;

    public EntityPolarBear(World world) {
        super(world);
        setSize(1.3F, 1.4F);
        tasks.addTask(0, new EntityAISwimming(this));
        tasks.addTask(1, new EntityAIAttackOnCollide(this, EntityPlayer.class, 1.25D, true));
        tasks.addTask(1, new EntityAIPanic(this, 2.0D) {
            @Override
            public boolean shouldExecute() {
                return (isChild() || isBurning()) && super.shouldExecute();
            }
        });
        tasks.addTask(4, new EntityAIFollowParent(this, 1.25D));
        tasks.addTask(5, new EntityAIWander(this, 1.0D));
        tasks.addTask(6, new EntityAIWatchClosest(this, EntityPlayer.class, 6.0F));
        tasks.addTask(7, new EntityAILookIdle(this));
        targetTasks.addTask(1, new EntityAIHurtByTarget(this, true) {
            @Override
            public boolean shouldExecute() {
                return !isChild() && super.shouldExecute();
            }
        });
        targetTasks.addTask(2, new EntityAINearestAttackableTarget(this, EntityPlayer.class, 20, true, true) {
            @Override
            public boolean shouldExecute() {
                if (isChild() || !super.shouldExecute()) {
                    return false;
                }
                for (Object other : worldObj.getEntitiesWithinAABB(EntityPolarBear.class, boundingBox.expand(8.0D, 4.0D, 8.0D))) {
                    if (((EntityPolarBear) other).isChild()) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            protected double getTargetDistance() {
                return super.getTargetDistance() * 0.5D;
            }
        });
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(30.0D);
        getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(20.0D);
        getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.25D);
        getAttributeMap().registerAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(6.0D);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataWatcher.addObject(WATCH_STANDING, Byte.valueOf((byte) 0));
    }

    @Override
    public boolean isAIEnabled() {
        return true;
    }

    @Override
    public EntityAgeable createChild(EntityAgeable other) {
        return new EntityPolarBear(worldObj);
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return false;
    }

    @Override
    protected String getLivingSound() {
        return SOUND + (isChild() ? "baby_ambient" : "ambient");
    }

    @Override
    protected String getHurtSound() {
        return SOUND + "hurt";
    }

    @Override
    protected String getDeathSound() {
        return SOUND + "death";
    }

    @Override
    protected void func_145780_a(int x, int y, int z, net.minecraft.block.Block block) {
        playSound(SOUND + "step", 0.15F, 1.0F);
    }

    public boolean isStanding() {
        return dataWatcher.getWatchableObjectByte(WATCH_STANDING) != 0;
    }

    private void setStanding(boolean value) {
        dataWatcher.updateObject(WATCH_STANDING, Byte.valueOf((byte) (value ? 1 : 0)));
    }

    public float standing(float partial) {
        return (prevStanding + (standing - prevStanding) * partial) / 6.0F;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (worldObj.isRemote) {
            prevStanding = standing;
            standing = Math.max(0.0F, Math.min(6.0F, standing + (isStanding() ? 1.0F : -1.0F)));
        } else {
            updateWarning();
        }
        if (warningCooldown > 0) {
            warningCooldown--;
        }
    }

    /** The checkAndPerformAttack of the bear's melee task: inside twice the reach it rears up and warns. */
    private void updateWarning() {
        EntityLivingBase target = getAttackTarget();
        if (target == null) {
            setStanding(false);
            standTimer = 20;
            return;
        }
        double reach = (double) (4.0F + target.width);
        double distance = getDistanceSqToEntity(target);
        if (distance <= reach) {
            setStanding(false);
            standTimer = 20;
        } else if (distance <= reach * 2.0D) {
            standTimer--;
            if (standTimer <= 0) {
                setStanding(false);
                standTimer = 20;
            } else if (standTimer <= 10) {
                setStanding(true);
                if (warningCooldown <= 0) {
                    playSound(SOUND + "warning", 1.0F, 1.0F);
                    warningCooldown = 40;
                }
            }
        } else {
            standTimer = 20;
            setStanding(false);
        }
    }

    @Override
    public boolean attackEntityAsMob(Entity target) {
        return target.attackEntityFrom(DamageSource.causeMobDamage(this),
                (float) (int) getEntityAttribute(SharedMonsterAttributes.attackDamage).getAttributeValue());
    }

    @Override
    protected float getSoundVolume() {
        return 1.0F;
    }

    /** loot_tables/entities/polar_bear.json */
    @Override
    protected void dropFewItems(boolean byPlayer, int looting) {
        int count = rand.nextInt(3) + (looting > 0 ? rand.nextInt(looting + 1) : 0);
        if (rand.nextInt(4) == 0) {
            entityDropItem(new ItemStack(isBurning() ? Items.cooked_fished : Items.fish, count, ItemFishFood.FishType.SALMON.func_150976_a()), 0.0F);
        } else {
            dropItem(isBurning() ? Items.cooked_fished : Items.fish, count);
        }
    }
}
