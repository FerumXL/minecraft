package ru.codex.offhand1710;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIFollowOwner;
import net.minecraft.entity.ai.EntityAIFollowParent;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIMate;
import net.minecraft.entity.ai.EntityAIPanic;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAITempt;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

/** Parrot, llama and shulker of 1.12.2 on 1.7.10. See the changelog for what these do not have yet. */
public final class MoreMobs {
    private static final String SOUND = MC112Bridge.MOD_ID + ":entity.";

    private MoreMobs() {
    }

    /** EntityParrot: tamed with seeds, sits, flutters instead of falling and dances near a playing jukebox. */
    public static class Parrot extends EntityTameable {
        private static final int WATCH_VARIANT = 20;
        public float flap;
        public float prevFlap;
        public float flapSpeed;
        public float prevFlapSpeed;
        private float flapping = 1.0F;
        private boolean dancing;

        public Parrot(World world) {
            super(world);
            setSize(0.5F, 0.9F);
            tasks.addTask(0, new EntityAIPanic(this, 1.25D));
            tasks.addTask(0, new EntityAISwimming(this));
            tasks.addTask(1, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
            tasks.addTask(2, aiSit);
            tasks.addTask(2, new EntityAIFollowOwner(this, 1.0D, 5.0F, 1.0F));
            tasks.addTask(3, new EntityAIWander(this, 1.0D));
        }

        @Override
        protected void applyEntityAttributes() {
            super.applyEntityAttributes();
            getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(6.0D);
            getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.20000000298023224D);
        }

        @Override
        protected void entityInit() {
            super.entityInit();
            dataWatcher.addObject(WATCH_VARIANT, Integer.valueOf(0));
        }

        public int variant() {
            return dataWatcher.getWatchableObjectInt(WATCH_VARIANT);
        }

        @Override
        public IEntityLivingData onSpawnWithEgg(IEntityLivingData data) {
            IEntityLivingData result = super.onSpawnWithEgg(data);
            dataWatcher.updateObject(WATCH_VARIANT, Integer.valueOf(rand.nextInt(5)));
            return result;
        }

        @Override
        public void writeEntityToNBT(NBTTagCompound tag) {
            super.writeEntityToNBT(tag);
            tag.setInteger("Variant", variant());
        }

        @Override
        public void readEntityFromNBT(NBTTagCompound tag) {
            super.readEntityFromNBT(tag);
            dataWatcher.updateObject(WATCH_VARIANT, Integer.valueOf(tag.getInteger("Variant")));
        }

        @Override
        public boolean isAIEnabled() {
            return true;
        }

        public boolean isDancing() {
            return dancing;
        }

        public boolean isFlying() {
            return !onGround;
        }

        @Override
        public void onLivingUpdate() {
            super.onLivingUpdate();
            if (!worldObj.isRemote && onGround && !isSitting() && !getNavigator().noPath() && rand.nextInt(30) == 0) {
                motionY = 0.5D;
            }
            if (!onGround && motionY < 0.0D) {
                motionY *= 0.6D;
            }
            fallDistance = 0.0F;
            prevFlap = flap;
            prevFlapSpeed = flapSpeed;
            flapSpeed = Math.max(0.0F, Math.min(1.0F, flapSpeed + (float) (onGround ? -1 : 4) * 0.3F));
            if (!onGround && flapping < 1.0F) {
                flapping = 1.0F;
            }
            flapping *= 0.9F;
            flap += flapping * 2.0F;
            if (worldObj.isRemote && ticksExisted % 20 == 0) {
                dancing = jukeboxNear();
            }
        }

        private boolean jukeboxNear() {
            int x = MathHelper.floor_double(posX);
            int y = MathHelper.floor_double(boundingBox.minY);
            int z = MathHelper.floor_double(posZ);
            for (int dx = -3; dx <= 3; dx++) {
                for (int dy = -2; dy <= 2; dy++) {
                    for (int dz = -3; dz <= 3; dz++) {
                        if (worldObj.getBlock(x + dx, y + dy, z + dz) == Blocks.jukebox && worldObj.getBlockMetadata(x + dx, y + dy, z + dz) > 0) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        private static boolean isSeed(Item item) {
            return item == Items.wheat_seeds || item == Items.melon_seeds || item == Items.pumpkin_seeds;
        }

        @Override
        public boolean interact(EntityPlayer player) {
            ItemStack held = player.getCurrentEquippedItem();
            if (held != null && !isTamed() && isSeed(held.getItem())) {
                if (!player.capabilities.isCreativeMode) {
                    held.stackSize--;
                }
                playSound(SOUND + "parrot.eat", 1.0F, 1.0F);
                if (!worldObj.isRemote) {
                    if (rand.nextInt(10) == 0) {
                        setTamed(true);
                        func_152115_b(player.getUniqueID().toString());
                        playTameEffect(true);
                        worldObj.setEntityState(this, (byte) 7);
                    } else {
                        playTameEffect(false);
                        worldObj.setEntityState(this, (byte) 6);
                    }
                }
                return true;
            }
            if (held != null && held.getItem() == Items.cookie) {
                if (!player.capabilities.isCreativeMode) {
                    held.stackSize--;
                }
                addPotionEffect(new PotionEffect(Potion.poison.id, 900));
                attackEntityFrom(DamageSource.causePlayerDamage(player), Float.MAX_VALUE);
                return true;
            }
            if (!worldObj.isRemote && !isFlying() && isTamed() && func_152114_e(player)) {
                aiSit.setSitting(!isSitting());
            }
            return super.interact(player);
        }

        @Override
        public boolean isBreedingItem(ItemStack stack) {
            return false;
        }

        @Override
        public EntityAgeable createChild(EntityAgeable other) {
            return null;
        }

        @Override
        protected String getLivingSound() {
            return SOUND + "parrot.ambient";
        }

        @Override
        protected String getHurtSound() {
            return SOUND + "parrot.hurt";
        }

        @Override
        protected String getDeathSound() {
            return SOUND + "parrot.death";
        }

        @Override
        protected void fall(float distance) {
        }

        @Override
        protected void dropFewItems(boolean byPlayer, int looting) {
            dropItem(Items.feather, 1 + rand.nextInt(2) + (looting > 0 ? rand.nextInt(looting + 1) : 0));
        }
    }

    /** EntityLlama: four coats, spits at whoever hurts it. No caravans, carpets or chests yet. */
    public static class Llama extends EntityAnimal {
        private static final int WATCH_VARIANT = 18;
        private int spitCooldown;

        public Llama(World world) {
            super(world);
            setSize(0.9F, 1.87F);
            tasks.addTask(0, new EntityAISwimming(this));
            tasks.addTask(1, new EntityAIPanic(this, 1.2D));
            tasks.addTask(3, new EntityAIMate(this, 1.0D));
            tasks.addTask(4, new EntityAITempt(this, 1.25D, Items.wheat, false));
            tasks.addTask(5, new EntityAIFollowParent(this, 1.0D));
            tasks.addTask(6, new EntityAIWander(this, 0.7D));
            tasks.addTask(7, new EntityAIWatchClosest(this, EntityPlayer.class, 6.0F));
            tasks.addTask(8, new EntityAILookIdle(this));
            targetTasks.addTask(1, new EntityAIHurtByTarget(this, false));
        }

        @Override
        protected void applyEntityAttributes() {
            super.applyEntityAttributes();
            getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(22.0D);
            getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.17499999701976776D);
            getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(40.0D);
        }

        @Override
        protected void entityInit() {
            super.entityInit();
            dataWatcher.addObject(WATCH_VARIANT, Integer.valueOf(0));
        }

        public int variant() {
            return dataWatcher.getWatchableObjectInt(WATCH_VARIANT);
        }

        @Override
        public IEntityLivingData onSpawnWithEgg(IEntityLivingData data) {
            IEntityLivingData result = super.onSpawnWithEgg(data);
            dataWatcher.updateObject(WATCH_VARIANT, Integer.valueOf(rand.nextInt(4)));
            return result;
        }

        @Override
        public void writeEntityToNBT(NBTTagCompound tag) {
            super.writeEntityToNBT(tag);
            tag.setInteger("Variant", variant());
        }

        @Override
        public void readEntityFromNBT(NBTTagCompound tag) {
            super.readEntityFromNBT(tag);
            dataWatcher.updateObject(WATCH_VARIANT, Integer.valueOf(tag.getInteger("Variant")));
        }

        @Override
        public boolean isAIEnabled() {
            return true;
        }

        @Override
        protected void updateAITasks() {
            super.updateAITasks();
            EntityLivingBase target = getAttackTarget();
            if (spitCooldown > 0) {
                spitCooldown--;
            }
            if (target != null && target.isEntityAlive() && spitCooldown == 0 && getDistanceSqToEntity(target) < 400.0D && canEntityBeSeen(target)) {
                spitCooldown = 40;
                Spit spit = new Spit(worldObj, this);
                double dx = target.posX - spit.posX;
                double dy = target.boundingBox.minY + (double) (target.height / 3.0F) - spit.posY;
                double dz = target.posZ - spit.posZ;
                float lob = MathHelper.sqrt_double(dx * dx + dz * dz) * 0.2F;
                spit.setThrowableHeading(dx, dy + (double) lob, dz, 1.5F, 10.0F);
                playSound(SOUND + "llama.spit", 1.0F, 1.0F + (rand.nextFloat() - rand.nextFloat()) * 0.2F);
                worldObj.spawnEntityInWorld(spit);
            }
            if (target != null && (!target.isEntityAlive() || rand.nextInt(200) == 0)) {
                setAttackTarget(null);
            }
        }

        @Override
        public boolean isBreedingItem(ItemStack stack) {
            return stack.getItem() == Item.getItemFromBlock(Blocks.hay_block);
        }

        @Override
        public EntityAgeable createChild(EntityAgeable other) {
            Llama child = new Llama(worldObj);
            child.dataWatcher.updateObject(WATCH_VARIANT, Integer.valueOf(rand.nextBoolean() ? variant() : rand.nextInt(4)));
            return child;
        }

        @Override
        protected String getLivingSound() {
            return SOUND + "llama.ambient";
        }

        @Override
        protected String getHurtSound() {
            return SOUND + "llama.hurt";
        }

        @Override
        protected String getDeathSound() {
            return SOUND + "llama.death";
        }

        @Override
        protected void func_145780_a(int x, int y, int z, net.minecraft.block.Block block) {
            playSound(SOUND + "llama.step", 0.15F, 1.0F);
        }

        @Override
        protected void dropFewItems(boolean byPlayer, int looting) {
            dropItem(Items.leather, rand.nextInt(3) + (looting > 0 ? rand.nextInt(looting + 1) : 0));
        }
    }

    public static class Spit extends EntityThrowable {
        public Spit(World world) {
            super(world);
        }

        public Spit(World world, EntityLivingBase thrower) {
            super(world, thrower);
        }

        @Override
        protected void onImpact(MovingObjectPosition hit) {
            if (hit.entityHit != null && !worldObj.isRemote) {
                hit.entityHit.attackEntityFrom(DamageSource.causeThrownDamage(this, getThrower()), 1.0F);
            }
            if (!worldObj.isRemote) {
                setDead();
            }
        }
    }

    /** EntityShulker: sits on the block below it, opens to shoot homing bullets and teleports when badly hurt. */
    public static class Shulker extends EntityMob {
        private static final int WATCH_PEEK = 16;
        private float peek;
        private float prevPeek;
        private int shootTimer;
        private int closeTimer;
        private int homeX;
        private int homeY;
        private int homeZ;
        private boolean anchored;

        public Shulker(World world) {
            super(world);
            setSize(1.0F, 1.0F);
            experienceValue = 5;
        }

        @Override
        protected void applyEntityAttributes() {
            super.applyEntityAttributes();
            getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(30.0D);
        }

        @Override
        protected void entityInit() {
            super.entityInit();
            dataWatcher.addObject(WATCH_PEEK, Byte.valueOf((byte) 0));
        }

        @Override
        protected boolean isAIEnabled() {
            return true;
        }

        private int peekValue() {
            return dataWatcher.getWatchableObjectByte(WATCH_PEEK);
        }

        private void setPeek(int value) {
            if (peekValue() != value) {
                dataWatcher.updateObject(WATCH_PEEK, Byte.valueOf((byte) value));
                if (!worldObj.isRemote) {
                    playSound(SOUND + (value == 0 ? "shulker.close" : "shulker.open"), 1.0F, 1.0F);
                }
            }
        }

        public float peek(float partial) {
            return prevPeek + (peek - prevPeek) * partial;
        }

        private boolean isClosed() {
            return peekValue() == 0;
        }

        @Override
        public void onUpdate() {
            super.onUpdate();
            prevPeek = peek;
            float goal = (float) peekValue() * 0.01F;
            if (peek > goal) {
                peek = Math.max(peek - 0.05F, goal);
            } else if (peek < goal) {
                peek = Math.min(peek + 0.05F, goal);
            }
            motionX = 0.0D;
            motionY = 0.0D;
            motionZ = 0.0D;
            if (!worldObj.isRemote) {
                if (!anchored) {
                    homeX = MathHelper.floor_double(posX);
                    homeY = MathHelper.floor_double(boundingBox.minY + 0.5D);
                    homeZ = MathHelper.floor_double(posZ);
                    anchored = true;
                }
                setPosition((double) homeX + 0.5D, (double) homeY, (double) homeZ + 0.5D);
                renderYawOffset = 0.0F;
                prevRenderYawOffset = 0.0F;
                rotationYaw = 0.0F;
                prevRotationYaw = 0.0F;
            }
        }

        @Override
        protected void updateAITasks() {
            super.updateAITasks();
            EntityLivingBase target = getAttackTarget();
            if (target == null || !target.isEntityAlive() || target.getDistanceSqToEntity(this) > 400.0D) {
                target = worldObj.getClosestVulnerablePlayerToEntity(this, 16.0D);
                setAttackTarget(target);
            }
            if (target != null && worldObj.difficultySetting.getDifficultyId() > 0) {
                setPeek(100);
                getLookHelper().setLookPositionWithEntity(target, 180.0F, 180.0F);
                if (--shootTimer <= 0) {
                    shootTimer = 20 + rand.nextInt(10) * 20 / 2;
                    worldObj.spawnEntityInWorld(new Bullet(worldObj, this, target));
                    playSound(SOUND + "shulker.shoot", 2.0F, (rand.nextFloat() - rand.nextFloat()) * 0.2F + 1.0F);
                }
                return;
            }
            if (closeTimer > 0) {
                if (--closeTimer == 0) {
                    setPeek(0);
                }
            } else if (rand.nextInt(40) == 0) {
                setPeek(30);
                closeTimer = 20 * (1 + rand.nextInt(3));
            }
        }

        @Override
        public boolean attackEntityFrom(DamageSource source, float amount) {
            if (isClosed() && source.isProjectile()) {
                return false;
            }
            boolean result = super.attackEntityFrom(source, isClosed() ? amount * 0.2F : amount);
            if (result && !worldObj.isRemote && (double) getHealth() < (double) getMaxHealth() * 0.5D && rand.nextInt(4) == 0) {
                teleport();
            }
            return result;
        }

        private void teleport() {
            for (int i = 0; i < 5; i++) {
                int x = homeX + 8 - rand.nextInt(17);
                int y = homeY + 8 - rand.nextInt(17);
                int z = homeZ + 8 - rand.nextInt(17);
                if (y > 0 && worldObj.isAirBlock(x, y, z) && worldObj.getBlock(x, y - 1, z).isNormalCube()) {
                    homeX = x;
                    homeY = y;
                    homeZ = z;
                    setPosition((double) x + 0.5D, (double) y, (double) z + 0.5D);
                    playSound(SOUND + "shulker.teleport", 1.0F, 1.0F);
                    return;
                }
            }
        }

        @Override
        protected boolean isValidLightLevel() {
            return true;
        }

        @Override
        public boolean canBePushed() {
            return false;
        }

        @Override
        protected void collideWithEntity(Entity entity) {
        }

        /** loot_tables/entities/shulker.json */
        @Override
        protected void dropFewItems(boolean byPlayer, int looting) {
            if (rand.nextFloat() < 0.5F + (float) looting * 0.0625F) {
                dropItem(Items112.shulkerShell, 1);
            }
        }

        @Override
        protected String getLivingSound() {
            return isClosed() ? null : SOUND + "shulker.ambient";
        }

        @Override
        protected String getHurtSound() {
            return SOUND + (isClosed() ? "shulker.hurt_closed" : "shulker.hurt");
        }

        @Override
        protected String getDeathSound() {
            return SOUND + "shulker.death";
        }
    }

    /** EntityShulkerBullet: follows its target; a hit deals 4 damage and lifts the target for 10 seconds. */
    public static class Bullet extends Entity {
        private EntityLivingBase target;
        private EntityLivingBase owner;
        private int age;

        public Bullet(World world) {
            super(world);
            setSize(0.3125F, 0.3125F);
            noClip = true;
        }

        public Bullet(World world, EntityLivingBase owner, EntityLivingBase target) {
            this(world);
            this.owner = owner;
            this.target = target;
            setPosition(owner.posX, owner.boundingBox.minY + 0.5D, owner.posZ);
        }

        @Override
        protected void entityInit() {
        }

        @Override
        protected void readEntityFromNBT(NBTTagCompound tag) {
            setDead();
        }

        @Override
        protected void writeEntityToNBT(NBTTagCompound tag) {
        }

        @Override
        public void onUpdate() {
            super.onUpdate();
            if (worldObj.isRemote) {
                worldObj.spawnParticle("portal", posX, posY + 0.15D, posZ, 0.0D, 0.0D, 0.0D);
            } else {
                if (target == null || !target.isEntityAlive() || ++age > 300) {
                    setDead();
                    return;
                }
                double dx = target.posX - posX;
                double dy = target.boundingBox.minY + (double) target.height * 0.5D - posY;
                double dz = target.posZ - posZ;
                double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                motionX += (dx / distance * 0.15D - motionX) * 0.2D;
                motionY += (dy / distance * 0.15D - motionY) * 0.2D;
                motionZ += (dz / distance * 0.15D - motionZ) * 0.2D;
                if (distance < 0.8D) {
                    target.attackEntityFrom(DamageSource.causeIndirectMagicDamage(this, owner).setProjectile(), 4.0F);
                    if (target instanceof net.minecraft.entity.EntityLivingBase) {
                        ((net.minecraft.entity.EntityLivingBase) target).addPotionEffect(new net.minecraft.potion.PotionEffect(Content120.levitation.id, 200, 0));
                    }
                    playSound(SOUND + "shulker_bullet.hit", 1.0F, 1.0F);
                    setDead();
                    return;
                }
            }
            setPosition(posX + motionX, posY + motionY, posZ + motionZ);
        }

        @Override
        public boolean canBeCollidedWith() {
            return true;
        }

        @Override
        public boolean attackEntityFrom(DamageSource source, float amount) {
            if (!worldObj.isRemote) {
                playSound(SOUND + "shulker_bullet.hurt", 1.0F, 1.0F);
                setDead();
            }
            return true;
        }
    }
}
