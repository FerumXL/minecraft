package ru.codex.offhand1710;

import java.util.List;

import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;

/** EntityGuardian of 1.12.2. Its AI tasks and swim controller are written out in updateAITasks for 1.7.10. */
public class EntityGuardian extends EntityMob {
    private static final int WATCH_MOVING = 16;
    private static final int WATCH_TARGET = 17;
    private static final String SOUND = MC112Bridge.MOD_ID + ":entity.guardian.";
    private static final byte STATE_ATTACK = 21;

    public float tailAnimation;
    public float prevTailAnimation;
    private float tailSpeed;
    public float spikesAnimation;
    public float prevSpikesAnimation;
    private EntityLivingBase clientTarget;
    private int clientTargetId;
    private int clientAttackTime;
    private boolean wasFlopping;

    private int attackTime;
    private boolean hasWander;
    private double wanderX;
    private double wanderY;
    private double wanderZ;

    public EntityGuardian(World world) {
        super(world);
        experienceValue = 10;
        setSize(0.85F, 0.85F);
        tailAnimation = rand.nextFloat();
        prevTailAnimation = tailAnimation;
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(6.0D);
        getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.5D);
        getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(16.0D);
        getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(30.0D);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataWatcher.addObject(WATCH_MOVING, Byte.valueOf((byte) 0));
        dataWatcher.addObject(WATCH_TARGET, Integer.valueOf(0));
    }

    @Override
    protected boolean isAIEnabled() {
        return true;
    }

    public boolean isMoving() {
        return dataWatcher.getWatchableObjectByte(WATCH_MOVING) != 0;
    }

    private void setMoving(boolean moving) {
        dataWatcher.updateObject(WATCH_MOVING, Byte.valueOf((byte) (moving ? 1 : 0)));
    }

    public int attackDuration() {
        return 80;
    }

    protected int wanderChance() {
        return 80;
    }

    private void setTargetId(int id) {
        dataWatcher.updateObject(WATCH_TARGET, Integer.valueOf(id));
    }

    public boolean hasTargeted() {
        return dataWatcher.getWatchableObjectInt(WATCH_TARGET) != 0;
    }

    public EntityLivingBase targeted() {
        if (!hasTargeted()) {
            return null;
        }
        if (!worldObj.isRemote) {
            return getAttackTarget();
        }
        if (clientTarget == null) {
            Entity entity = worldObj.getEntityByID(dataWatcher.getWatchableObjectInt(WATCH_TARGET));
            if (entity instanceof EntityLivingBase) {
                clientTarget = (EntityLivingBase) entity;
            }
        }
        return clientTarget;
    }

    @Override
    public int getTalkInterval() {
        return 160;
    }

    @Override
    protected String getLivingSound() {
        return soundPrefix() + (isInWater() ? "ambient" : "ambient_land");
    }

    @Override
    protected String getHurtSound() {
        return soundPrefix() + (isInWater() ? "hurt" : "hurt_land");
    }

    @Override
    protected String getDeathSound() {
        return soundPrefix() + (isInWater() ? "death" : "death_land");
    }

    protected String soundPrefix() {
        return SOUND;
    }

    @Override
    protected boolean canTriggerWalking() {
        return false;
    }

    @Override
    public float getEyeHeight() {
        return height * 0.5F;
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean isPushedByWater() {
        return false;
    }

    @Override
    public float getBlockPathWeight(int x, int y, int z) {
        return worldObj.getBlock(x, y, z).getMaterial() == Material.water
                ? 10.0F + worldObj.getLightBrightness(x, y, z) - 0.5F : super.getBlockPathWeight(x, y, z);
    }

    @Override
    protected boolean isValidLightLevel() {
        return true;
    }

    @Override
    public boolean getCanSpawnHere() {
        int x = MathHelper.floor_double(posX);
        int y = MathHelper.floor_double(boundingBox.minY);
        int z = MathHelper.floor_double(posZ);
        return worldObj.difficultySetting != EnumDifficulty.PEACEFUL
                && (rand.nextInt(20) == 0 || !worldObj.canBlockSeeTheSky(x, y, z))
                && worldObj.checkNoEntityCollision(boundingBox) && worldObj.getCollidingBoundingBoxes(this, boundingBox).isEmpty();
    }

    @Override
    public int getMaxSpawnedInChunk() {
        return 4;
    }

    @Override
    public int getVerticalFaceSpeed() {
        return 180;
    }

    @Override
    public void onLivingUpdate() {
        if (worldObj.isRemote) {
            clientTick();
        }
        if (isInWater()) {
            setAir(300);
        } else if (onGround) {
            motionY += 0.5D;
            motionX += (double) ((rand.nextFloat() * 2.0F - 1.0F) * 0.4F);
            motionZ += (double) ((rand.nextFloat() * 2.0F - 1.0F) * 0.4F);
            rotationYaw = rand.nextFloat() * 360.0F;
            onGround = false;
            isAirBorne = true;
        }
        if (hasTargeted()) {
            rotationYaw = rotationYawHead;
        }
        super.onLivingUpdate();
    }

    private void clientTick() {
        int id = dataWatcher.getWatchableObjectInt(WATCH_TARGET);
        if (id != clientTargetId) {
            clientTargetId = id;
            clientAttackTime = 0;
            clientTarget = null;
        }
        prevTailAnimation = tailAnimation;
        if (!isInWater()) {
            tailSpeed = 2.0F;
            if (motionY > 0.0D && wasFlopping) {
                worldObj.playSound(posX, posY, posZ, soundPrefix() + "flop", 1.0F, 1.0F, false);
            }
            wasFlopping = motionY < 0.0D && worldObj.isBlockNormalCubeDefault(MathHelper.floor_double(posX),
                    MathHelper.floor_double(posY) - 1, MathHelper.floor_double(posZ), false);
        } else if (isMoving()) {
            tailSpeed = tailSpeed < 0.5F ? 4.0F : tailSpeed + (0.5F - tailSpeed) * 0.1F;
        } else {
            tailSpeed += (0.125F - tailSpeed) * 0.2F;
        }
        tailAnimation += tailSpeed;
        prevSpikesAnimation = spikesAnimation;
        if (!isInWater()) {
            spikesAnimation = rand.nextFloat();
        } else if (isMoving()) {
            spikesAnimation += (0.0F - spikesAnimation) * 0.25F;
        } else {
            spikesAnimation += (1.0F - spikesAnimation) * 0.06F;
        }
        if (isMoving() && isInWater()) {
            Vec3 look = getLook(0.0F);
            for (int i = 0; i < 2; i++) {
                worldObj.spawnParticle("bubble", posX + (rand.nextDouble() - 0.5D) * (double) width - look.xCoord * 1.5D,
                        boundingBox.minY + rand.nextDouble() * (double) height - look.yCoord * 1.5D,
                        posZ + (rand.nextDouble() - 0.5D) * (double) width - look.zCoord * 1.5D, 0.0D, 0.0D, 0.0D);
            }
        }
        if (hasTargeted()) {
            if (clientAttackTime < attackDuration()) {
                clientAttackTime++;
            }
            EntityLivingBase target = targeted();
            if (target != null) {
                getLookHelper().setLookPositionWithEntity(target, 90.0F, 90.0F);
                getLookHelper().onUpdateLook();
                double progress = (double) attackProgress(0.0F);
                double dx = target.posX - posX;
                double dy = target.boundingBox.minY + (double) (target.height * 0.5F) - (boundingBox.minY + (double) getEyeHeight());
                double dz = target.posZ - posZ;
                double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                dx /= distance;
                dy /= distance;
                dz /= distance;
                double along = rand.nextDouble();
                while (along < distance) {
                    along += 1.8D - progress + rand.nextDouble() * (1.7D - progress);
                    worldObj.spawnParticle("bubble", posX + dx * along, boundingBox.minY + dy * along + (double) getEyeHeight(),
                            posZ + dz * along, 0.0D, 0.0D, 0.0D);
                }
            }
        }
    }

    public float tail(float partial) {
        return prevTailAnimation + (tailAnimation - prevTailAnimation) * partial;
    }

    public float spikes(float partial) {
        return prevSpikesAnimation + (spikesAnimation - prevSpikesAnimation) * partial;
    }

    public float attackProgress(float partial) {
        return ((float) clientAttackTime + partial) / (float) attackDuration();
    }

    @Override
    public void handleHealthUpdate(byte state) {
        if (state == STATE_ATTACK) {
            worldObj.playSound(posX, posY, posZ, SOUND + "attack", 1.0F, 1.0F, false);
        } else {
            super.handleHealthUpdate(state);
        }
    }

    private boolean validTarget(EntityLivingBase entity) {
        return (entity instanceof EntityPlayer || entity instanceof EntitySquid) && entity.isEntityAlive()
                && entity.getDistanceSqToEntity(this) > 9.0D
                && !(entity instanceof EntityPlayer && ((EntityPlayer) entity).capabilities.disableDamage)
                && canEntityBeSeen(entity);
    }

    private EntityLivingBase findTarget() {
        double range = getEntityAttribute(SharedMonsterAttributes.followRange).getAttributeValue();
        AxisAlignedBB area = boundingBox.expand(range, 4.0D, range);
        List<?> found = worldObj.getEntitiesWithinAABB(EntityLivingBase.class, area);
        EntityLivingBase best = null;
        double bestDistance = range * range;
        for (Object object : found) {
            EntityLivingBase entity = (EntityLivingBase) object;
            double distance = entity.getDistanceSqToEntity(this);
            if (distance < bestDistance && validTarget(entity)) {
                best = entity;
                bestDistance = distance;
            }
        }
        return best;
    }

    /** EntityGuardian.AIGuardianAttack, the wander task and the nearest attackable target task. */
    @Override
    protected void updateAITasks() {
        EntityLivingBase target = getAttackTarget();
        if (target == null || !target.isEntityAlive() || (!isElder() && getDistanceSqToEntity(target) <= 9.0D)) {
            if (target != null || hasTargeted()) {
                stopAttack();
            }
            target = rand.nextInt(10) == 0 ? findTarget() : null;
            if (target != null) {
                setAttackTarget(target);
                attackTime = -10;
                setMoving(false);
                hasWander = false;
            }
        }
        if (target != null) {
            attack(target);
            setAIMoveSpeed(0.0F);
            moveForward = 0.0F;
        } else {
            wander();
            swim();
        }
        super.updateAITasks();
    }

    protected boolean isElder() {
        return false;
    }

    private void stopAttack() {
        setTargetId(0);
        setAttackTarget(null);
        hasWander = false;
    }

    private void attack(EntityLivingBase target) {
        getLookHelper().setLookPositionWithEntity(target, 90.0F, 90.0F);
        if (!canEntityBeSeen(target)) {
            stopAttack();
            return;
        }
        attackTime++;
        if (attackTime == 0) {
            setTargetId(target.getEntityId());
            worldObj.setEntityState(this, STATE_ATTACK);
        } else if (attackTime >= attackDuration()) {
            float magic = 1.0F;
            if (worldObj.difficultySetting == EnumDifficulty.HARD) {
                magic += 2.0F;
            }
            if (isElder()) {
                magic += 2.0F;
            }
            target.attackEntityFrom(DamageSource.causeIndirectMagicDamage(this, this), magic);
            target.attackEntityFrom(DamageSource.causeMobDamage(this),
                    (float) getEntityAttribute(SharedMonsterAttributes.attackDamage).getAttributeValue());
            stopAttack();
        }
    }

    private void wander() {
        boolean arrived = hasWander && getDistanceSq(wanderX, wanderY, wanderZ) < 2.0D;
        if (hasWander && !arrived && rand.nextInt(200) != 0) {
            return;
        }
        if (hasWander && !arrived) {
            hasWander = false;
        }
        if (arrived) {
            hasWander = false;
        }
        if (rand.nextInt(wanderChance()) != 0) {
            return;
        }
        for (int i = 0; i < 10; i++) {
            int x = MathHelper.floor_double(posX) + rand.nextInt(15) - 7;
            int y = MathHelper.floor_double(posY) + rand.nextInt(7) - 3;
            int z = MathHelper.floor_double(posZ) + rand.nextInt(15) - 7;
            if (worldObj.getBlock(x, y, z) == Blocks.water) {
                wanderX = (double) x + 0.5D;
                wanderY = (double) y + 0.5D;
                wanderZ = (double) z + 0.5D;
                hasWander = true;
                return;
            }
        }
    }

    /** EntityGuardian.GuardianMoveHelper.onUpdateMoveHelper. */
    private void swim() {
        if (!hasWander) {
            setAIMoveSpeed(0.0F);
            moveForward = 0.0F;
            setMoving(false);
            return;
        }
        double dx = wanderX - posX;
        double dy = wanderY - posY;
        double dz = wanderZ - posZ;
        double distance = (double) MathHelper.sqrt_double(dx * dx + dy * dy + dz * dz);
        double vertical = dy / distance;
        float yaw = (float) (Math.atan2(dz, dx) * 57.2957763671875D) - 90.0F;
        float change = MathHelper.wrapAngleTo180_float(yaw - rotationYaw);
        change = Math.max(-90.0F, Math.min(90.0F, change));
        rotationYaw += change;
        renderYawOffset = rotationYaw;
        float speed = (float) getEntityAttribute(SharedMonsterAttributes.movementSpeed).getAttributeValue();
        float current = getAIMoveSpeed() + (speed - getAIMoveSpeed()) * 0.125F;
        setAIMoveSpeed(current);
        moveForward = current;
        double wave = Math.sin((double) (ticksExisted + getEntityId()) * 0.5D) * 0.05D;
        double cos = Math.cos((double) (rotationYaw * 0.017453292F));
        double sin = Math.sin((double) (rotationYaw * 0.017453292F));
        motionX += wave * cos;
        motionZ += wave * sin;
        wave = Math.sin((double) (ticksExisted + getEntityId()) * 0.75D) * 0.05D;
        motionY += wave * (sin + cos) * 0.25D;
        motionY += (double) current * vertical * 0.1D;
        getLookHelper().setLookPosition(posX + dx / distance * 2.0D, (double) getEyeHeight() + posY + vertical / distance,
                posZ + dz / distance * 2.0D, 10.0F, 40.0F);
        setMoving(true);
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (!isMoving() && !source.isMagicDamage() && source.getSourceOfDamage() instanceof EntityLivingBase && !source.isExplosion()) {
            source.getSourceOfDamage().attackEntityFrom(DamageSource.causeThornsDamage(this), 2.0F);
        }
        hasWander = false;
        return super.attackEntityFrom(source, amount);
    }

    @Override
    public void moveEntityWithHeading(float strafe, float forward) {
        if (!worldObj.isRemote && isInWater()) {
            moveFlying(strafe, forward, 0.1F);
            moveEntity(motionX, motionY, motionZ);
            motionX *= 0.8999999761581421D;
            motionY *= 0.8999999761581421D;
            motionZ *= 0.8999999761581421D;
            if (!isMoving() && getAttackTarget() == null) {
                motionY -= 0.005D;
            }
        } else {
            super.moveEntityWithHeading(strafe, forward);
        }
    }

    /** loot_tables/entities/guardian.json */
    @Override
    protected void dropFewItems(boolean byPlayer, int looting) {
        dropItem(PortedBlocks.prismarineShard, rand.nextInt(3) + (looting > 0 ? rand.nextInt(looting + 1) : 0));
        int fishWeight = isElder() ? 3 : 2;
        int roll = rand.nextInt(fishWeight + 3);
        int bonus = looting > 0 ? rand.nextInt(looting + 1) : 0;
        if (roll < fishWeight) {
            dropItem(isBurning() ? Items.cooked_fished : Items.fish, 1 + bonus);
        } else if (roll < fishWeight + 2) {
            dropItem(PortedBlocks.prismarineCrystals, 1 + bonus);
        }
        if (byPlayer && rand.nextFloat() < 0.025F + (float) looting * 0.01F) {
            entityDropItem(new ItemStack(Items.fish, 1, rand.nextInt(4)), 0.0F);
        }
    }
}
