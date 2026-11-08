package ru.codex.offhand1710;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.IRangedAttackMob;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIArrowAttack;
import net.minecraft.entity.ai.EntityAIAttackOnCollide;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

/** Vindicator, evoker, illusioner, vex and evoker fangs of 1.12.2, with the AI written for 1.7.10. */
public final class Illagers {
    private static final String SOUND = MC112Bridge.MOD_ID + ":entity.";

    public static final int POSE_CROSSED = 0;
    public static final int POSE_ATTACKING = 1;
    public static final int POSE_SPELL = 2;
    public static final int POSE_BOW = 3;
    private static final int WATCH_POSE = 16;

    private Illagers() {
    }

    public abstract static class Base extends EntityMob {
        private final String soundName;

        Base(World world, String soundName) {
            super(world);
            this.soundName = SOUND + soundName + ".";
            setSize(0.6F, 1.95F);
            tasks.addTask(0, new EntityAISwimming(this));
            tasks.addTask(8, new EntityAIWander(this, 0.6D));
            tasks.addTask(9, new EntityAIWatchClosest(this, EntityPlayer.class, 3.0F, 1.0F));
            tasks.addTask(10, new EntityAIWatchClosest(this, EntityLiving.class, 8.0F));
            targetTasks.addTask(1, new EntityAIHurtByTarget(this, true));
            targetTasks.addTask(2, new EntityAINearestAttackableTarget(this, EntityPlayer.class, 0, true));
            targetTasks.addTask(3, new EntityAINearestAttackableTarget(this, EntityVillager.class, 0, false));
            targetTasks.addTask(3, new EntityAINearestAttackableTarget(this, EntityIronGolem.class, 0, false));
        }

        @Override
        protected void entityInit() {
            super.entityInit();
            dataWatcher.addObject(WATCH_POSE, Byte.valueOf((byte) 0));
        }

        @Override
        protected boolean isAIEnabled() {
            return true;
        }

        public int pose() {
            return dataWatcher.getWatchableObjectByte(WATCH_POSE);
        }

        protected void setPose(int pose) {
            dataWatcher.updateObject(WATCH_POSE, Byte.valueOf((byte) pose));
        }

        @Override
        protected String getLivingSound() {
            return soundName + "ambient";
        }

        @Override
        protected String getHurtSound() {
            return soundName + "hurt";
        }

        @Override
        protected String getDeathSound() {
            return soundName + "death";
        }

        @Override
        public boolean isOnSameTeam(EntityLivingBase other) {
            return other instanceof Base || other instanceof Vex || super.isOnSameTeam(other);
        }
    }

    public static class Vindicator extends Base {
        public Vindicator(World world) {
            super(world, "vindication_illager");
            experienceValue = 5;
            tasks.addTask(4, new EntityAIAttackOnCollide(this, EntityLivingBase.class, 1.0D, false));
        }

        @Override
        protected void applyEntityAttributes() {
            super.applyEntityAttributes();
            getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.3499999940395355D);
            getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(12.0D);
            getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(24.0D);
            getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(5.0D);
        }

        @Override
        public IEntityLivingData onSpawnWithEgg(IEntityLivingData data) {
            IEntityLivingData result = super.onSpawnWithEgg(data);
            setCurrentItemOrArmor(0, new ItemStack(Items.iron_axe));
            equipmentDropChances[0] = 0.0F;
            return result;
        }

        @Override
        protected void updateAITasks() {
            super.updateAITasks();
            setPose(getAttackTarget() != null ? POSE_ATTACKING : POSE_CROSSED);
        }

        @Override
        protected void dropFewItems(boolean byPlayer, int looting) {
            if (byPlayer) {
                dropItem(Items.emerald, rand.nextInt(2) + (looting > 0 ? rand.nextInt(looting + 1) : 0));
            }
        }
    }

    public static class Evoker extends Base {
        private int castTime;
        private int cooldown;
        private int spell;

        public Evoker(World world) {
            super(world, "evocation_illager");
            experienceValue = 10;
        }

        @Override
        protected void applyEntityAttributes() {
            super.applyEntityAttributes();
            getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.5D);
            getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(12.0D);
            getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(24.0D);
        }

        private int vexesNear() {
            return worldObj.getEntitiesWithinAABB(Vex.class, boundingBox.expand(16.0D, 8.0D, 16.0D)).size();
        }

        @Override
        protected void updateAITasks() {
            super.updateAITasks();
            if (cooldown > 0) {
                cooldown--;
            }
            EntityLivingBase target = getAttackTarget();
            if (castTime > 0) {
                castTime--;
                getNavigator().clearPathEntity();
                if (target != null) {
                    getLookHelper().setLookPositionWithEntity(target, 10.0F, (float) getVerticalFaceSpeed());
                }
                if (castTime == 0) {
                    playSound(SOUND + "evocation_illager.cast_spell", 1.0F, 1.0F);
                    if (spell == 1 && target != null) {
                        castFangs(target);
                    } else if (spell == 2) {
                        summonVexes(target);
                    }
                    setPose(POSE_CROSSED);
                }
                return;
            }
            if (target != null && cooldown <= 0) {
                spell = vexesNear() < 8 && rand.nextInt(3) == 0 ? 2 : 1;
                castTime = spell == 1 ? 40 : 100;
                cooldown = spell == 1 ? 100 : 340;
                setPose(POSE_SPELL);
                playSound(SOUND + (spell == 1 ? "evocation_illager.prepare_attack" : "evocation_illager.prepare_summon"), 1.0F, 1.0F);
            }
        }

        /** AICastingSpell attack: a ring round itself when the target is close, otherwise a line of fangs towards it. */
        private void castFangs(EntityLivingBase target) {
            double minY = Math.min(target.posY, posY);
            double maxY = Math.max(target.posY, posY) + 1.0D;
            float angle = (float) Math.atan2(target.posZ - posZ, target.posX - posX);
            if (getDistanceSqToEntity(target) < 9.0D) {
                for (int i = 0; i < 5; i++) {
                    float a = angle + (float) i * (float) Math.PI * 0.4F;
                    fang(posX + (double) MathHelper.cos(a) * 1.5D, posZ + (double) MathHelper.sin(a) * 1.5D, minY, maxY, a, 0);
                }
                for (int i = 0; i < 8; i++) {
                    float a = angle + (float) i * (float) Math.PI * 2.0F / 8.0F + 1.2566371F;
                    fang(posX + (double) MathHelper.cos(a) * 2.5D, posZ + (double) MathHelper.sin(a) * 2.5D, minY, maxY, a, 3);
                }
            } else {
                for (int i = 0; i < 16; i++) {
                    double distance = 1.25D * (double) (i + 1);
                    fang(posX + (double) MathHelper.cos(angle) * distance, posZ + (double) MathHelper.sin(angle) * distance, minY, maxY, angle, i);
                }
            }
        }

        private void fang(double x, double z, double minY, double maxY, float angle, int delay) {
            int bx = MathHelper.floor_double(x);
            int bz = MathHelper.floor_double(z);
            int by = MathHelper.floor_double(maxY);
            while (by >= MathHelper.floor_double(minY) - 1) {
                if (worldObj.isBlockNormalCubeDefault(bx, by - 1, bz, false) && !worldObj.getBlock(bx, by, bz).isNormalCube()) {
                    worldObj.spawnEntityInWorld(new Fangs(worldObj, x, (double) by, z, angle * 57.295776F, delay, this));
                    return;
                }
                by--;
            }
        }

        private void summonVexes(EntityLivingBase target) {
            for (int i = 0; i < 3; i++) {
                Vex vex = new Vex(worldObj);
                vex.setLocationAndAngles(posX - 0.5D + rand.nextDouble(), posY + 1.0D, posZ - 0.5D + rand.nextDouble(), 0.0F, 0.0F);
                vex.setAttackTarget(target);
                worldObj.spawnEntityInWorld(vex);
            }
        }

        @Override
        protected void dropFewItems(boolean byPlayer, int looting) {
            dropItem(Mobs112.totem, 1);
            if (byPlayer) {
                dropItem(Items.emerald, rand.nextInt(2) + (looting > 0 ? rand.nextInt(looting + 1) : 0));
            }
        }
    }

    public static class Illusioner extends Base implements IRangedAttackMob {
        public Illusioner(World world) {
            super(world, "illusion_illager");
            experienceValue = 5;
            tasks.addTask(4, new EntityAIArrowAttack(this, 0.5D, 20, 15.0F));
        }

        @Override
        protected void applyEntityAttributes() {
            super.applyEntityAttributes();
            getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.5D);
            getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(18.0D);
            getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(32.0D);
        }

        @Override
        public IEntityLivingData onSpawnWithEgg(IEntityLivingData data) {
            IEntityLivingData result = super.onSpawnWithEgg(data);
            setCurrentItemOrArmor(0, new ItemStack(Items.bow));
            return result;
        }

        @Override
        protected void updateAITasks() {
            super.updateAITasks();
            setPose(getAttackTarget() != null ? POSE_BOW : POSE_CROSSED);
        }

        @Override
        public void attackEntityWithRangedAttack(EntityLivingBase target, float strength) {
            EntityArrow arrow = new EntityArrow(worldObj, this, target, 1.6F, (float) (14 - worldObj.difficultySetting.getDifficultyId() * 4));
            playSound("random.bow", 1.0F, 1.0F / (getRNG().nextFloat() * 0.4F + 0.8F));
            worldObj.spawnEntityInWorld(arrow);
        }
    }

    /** EntityVex: flies through blocks, charges its target and withers away after its time is up. */
    public static class Vex extends EntityMob {
        private static final int WATCH_CHARGING = 16;
        private int lifetime;
        private int chargeCooldown;
        private double destX;
        private double destY;
        private double destZ;
        private boolean hasDest;

        public Vex(World world) {
            super(world);
            setSize(0.4F, 0.8F);
            isImmuneToFire = true;
            experienceValue = 3;
            noClip = true;
            lifetime = 20 * (30 + world.rand.nextInt(90));
            setCurrentItemOrArmor(0, new ItemStack(Items.iron_sword));
            equipmentDropChances[0] = 0.0F;
        }

        @Override
        protected void applyEntityAttributes() {
            super.applyEntityAttributes();
            getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(14.0D);
            getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(4.0D);
        }

        @Override
        protected void entityInit() {
            super.entityInit();
            dataWatcher.addObject(WATCH_CHARGING, Byte.valueOf((byte) 0));
        }

        @Override
        protected boolean isAIEnabled() {
            return true;
        }

        public boolean isCharging() {
            return dataWatcher.getWatchableObjectByte(WATCH_CHARGING) != 0;
        }

        private void setCharging(boolean value) {
            dataWatcher.updateObject(WATCH_CHARGING, Byte.valueOf((byte) (value ? 1 : 0)));
        }

        @Override
        protected void updateAITasks() {
            super.updateAITasks();
            if (--lifetime <= 0) {
                lifetime = 20;
                attackEntityFrom(DamageSource.starve, 1.0F);
            }
            EntityLivingBase target = getAttackTarget();
            if (target == null || !target.isEntityAlive()) {
                setAttackTarget(null);
                target = worldObj.getClosestVulnerablePlayerToEntity(this, 16.0D);
                if (target != null) {
                    setAttackTarget(target);
                }
            }
            if (chargeCooldown > 0) {
                chargeCooldown--;
            }
            if (target != null && chargeCooldown == 0 && !isCharging() && getDistanceSqToEntity(target) > 4.0D && rand.nextInt(7) == 0) {
                setCharging(true);
                playSound(SOUND + "vex.charge", 1.0F, 1.0F);
                chargeCooldown = 80;
            }
            if (target != null && isCharging()) {
                destX = target.posX;
                destY = target.posY + (double) target.getEyeHeight() * 0.5D;
                destZ = target.posZ;
                hasDest = true;
                if (boundingBox.intersectsWith(target.boundingBox)) {
                    attackEntityAsMob(target);
                    setCharging(false);
                }
            } else if (!hasDest || rand.nextInt(7) == 0) {
                destX = posX + (double) (rand.nextInt(15) - 7);
                destY = posY + (double) (rand.nextInt(11) - 5);
                destZ = posZ + (double) (rand.nextInt(15) - 7);
                hasDest = true;
                if (target == null) {
                    setCharging(false);
                }
            }
            double dx = destX - posX;
            double dy = destY - posY;
            double dz = destZ - posZ;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance < boundingBox.getAverageEdgeLength()) {
                motionX *= 0.5D;
                motionY *= 0.5D;
                motionZ *= 0.5D;
            } else {
                double speed = isCharging() ? 0.1D : 0.05D;
                motionX += dx / distance * speed;
                motionY += dy / distance * speed;
                motionZ += dz / distance * speed;
                rotationYaw = -((float) Math.atan2(motionX, motionZ)) * 57.295776F;
                renderYawOffset = rotationYaw;
            }
        }

        /** Vexes fly: no gravity and no ground friction. */
        @Override
        public void moveEntityWithHeading(float strafe, float forward) {
            moveEntity(motionX, motionY, motionZ);
            motionX *= 0.91D;
            motionY *= 0.91D;
            motionZ *= 0.91D;
        }

        @Override
        protected String getLivingSound() {
            return SOUND + "vex.ambient";
        }

        @Override
        protected String getHurtSound() {
            return SOUND + "vex.hurt";
        }

        @Override
        protected String getDeathSound() {
            return SOUND + "vex.death";
        }

        @Override
        protected void fall(float distance) {
        }

        @Override
        protected void updateFallState(double y, boolean ground) {
        }

        @Override
        public boolean isOnLadder() {
            return false;
        }

        @Override
        protected boolean isValidLightLevel() {
            return true;
        }
    }

    /** EntityEvokerFangs. */
    public static class Fangs extends Entity {
        private static final int WATCH_WARMUP = 16;
        private int warmup;
        private int life = 22;
        private boolean bitten;
        private EntityLivingBase caster;
        public int biteTicks;

        public Fangs(World world) {
            super(world);
            setSize(0.5F, 0.8F);
        }

        public Fangs(World world, double x, double y, double z, float yaw, int delay, EntityLivingBase caster) {
            this(world);
            warmup = delay;
            this.caster = caster;
            rotationYaw = yaw;
            setPosition(x, y, z);
            dataWatcher.updateObject(WATCH_WARMUP, Integer.valueOf(delay));
        }

        @Override
        protected void entityInit() {
            dataWatcher.addObject(WATCH_WARMUP, Integer.valueOf(0));
        }

        @Override
        protected void readEntityFromNBT(NBTTagCompound tag) {
            warmup = tag.getInteger("Warmup");
        }

        @Override
        protected void writeEntityToNBT(NBTTagCompound tag) {
            tag.setInteger("Warmup", warmup);
        }

        @Override
        public void onUpdate() {
            super.onUpdate();
            if (worldObj.isRemote) {
                biteTicks = biteTicks > 0 ? biteTicks + 1 : biteTicks;
                if (ticksExisted == dataWatcher.getWatchableObjectInt(WATCH_WARMUP) + 2) {
                    biteTicks = 1;
                }
                return;
            }
            if (--warmup >= 0) {
                return;
            }
            if (warmup == -8) {
                AxisAlignedBB area = boundingBox.expand(0.2D, 0.0D, 0.2D);
                for (Object object : worldObj.getEntitiesWithinAABB(EntityLivingBase.class, area)) {
                    EntityLivingBase entity = (EntityLivingBase) object;
                    if (entity.isEntityAlive() && entity != caster && !(entity instanceof Base) && !(entity instanceof Vex)) {
                        entity.attackEntityFrom(DamageSource.causeIndirectMagicDamage(this, caster), 6.0F);
                    }
                }
            }
            if (!bitten) {
                bitten = true;
                worldObj.playSoundAtEntity(this, SOUND + "evocation_fangs.attack", 1.0F, rand.nextFloat() * 0.2F + 0.85F);
            }
            if (--life < 0) {
                setDead();
            }
        }

        public float bite(float partial) {
            if (biteTicks <= 0) {
                return 0.0F;
            }
            float time = (float) biteTicks + partial;
            return time >= 20.0F ? 1.0F : time / 20.0F;
        }
    }

}
