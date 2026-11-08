package ru.codex.offhand1710;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAvoidEntity;
import net.minecraft.entity.ai.EntityAIMate;
import net.minecraft.entity.ai.EntityAIPanic;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAITempt;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.BiomeGenDesert;

/** EntityRabbit of 1.12.2 with its six coats. It moves in hops: on the ground it only turns between jumps. */
public class EntityRabbit extends EntityAnimal {
    private static final int WATCH_TYPE = 18;
    private static final byte STATE_HOP = 1;
    private static final String SOUND = MC112Bridge.MOD_ID + ":entity.rabbit.";

    private int hopCooldown;
    private int jumpTicks;
    private int jumpDuration;

    public EntityRabbit(World world) {
        super(world);
        setSize(0.4F, 0.5F);
        tasks.addTask(1, new EntityAISwimming(this));
        tasks.addTask(1, new EntityAIPanic(this, 2.2D));
        tasks.addTask(2, new EntityAIMate(this, 0.8D));
        tasks.addTask(3, new EntityAITempt(this, 1.0D, Items.carrot, false));
        tasks.addTask(3, new EntityAITempt(this, 1.0D, Items.golden_carrot, false));
        tasks.addTask(3, new EntityAITempt(this, 1.0D, Item.getItemFromBlock(Blocks.yellow_flower), false));
        tasks.addTask(4, new EntityAIAvoidEntity(this, EntityPlayer.class, 8.0F, 2.2D, 2.2D));
        tasks.addTask(4, new EntityAIAvoidEntity(this, EntityWolf.class, 10.0F, 2.2D, 2.2D));
        tasks.addTask(4, new EntityAIAvoidEntity(this, EntityMob.class, 4.0F, 2.2D, 2.2D));
        tasks.addTask(6, new EntityAIWander(this, 0.6D));
        tasks.addTask(11, new EntityAIWatchClosest(this, EntityPlayer.class, 10.0F));
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(3.0D);
        getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.30000001192092896D);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataWatcher.addObject(WATCH_TYPE, Integer.valueOf(0));
    }

    @Override
    public boolean isAIEnabled() {
        return true;
    }

    public int getRabbitType() {
        return dataWatcher.getWatchableObjectInt(WATCH_TYPE);
    }

    public void setRabbitType(int type) {
        dataWatcher.updateObject(WATCH_TYPE, Integer.valueOf(type));
    }

    public float jump(float partial) {
        return jumpDuration == 0 ? 0.0F : ((float) jumpTicks + partial) / (float) jumpDuration;
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setInteger("RabbitType", getRabbitType());
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        setRabbitType(tag.getInteger("RabbitType"));
    }

    /** getRandomRabbitType: the coat depends on the biome. */
    private int biomeType() {
        BiomeGenBase biome = worldObj.getBiomeGenForCoords(MathHelper.floor_double(posX), MathHelper.floor_double(posZ));
        int roll = rand.nextInt(100);
        if (biome.getEnableSnow()) {
            return roll < 80 ? 1 : 3;
        }
        if (biome instanceof BiomeGenDesert) {
            return 4;
        }
        return roll < 50 ? 0 : (roll < 90 ? 5 : 2);
    }

    @Override
    public IEntityLivingData onSpawnWithEgg(IEntityLivingData data) {
        IEntityLivingData result = super.onSpawnWithEgg(data);
        setRabbitType(biomeType());
        return result;
    }

    @Override
    public EntityAgeable createChild(EntityAgeable other) {
        EntityRabbit child = new EntityRabbit(worldObj);
        int type = biomeType();
        if (rand.nextInt(20) != 0) {
            type = other instanceof EntityRabbit && rand.nextBoolean() ? ((EntityRabbit) other).getRabbitType() : getRabbitType();
        }
        child.setRabbitType(type);
        return child;
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        Item item = stack.getItem();
        return item == Items.carrot || item == Items.golden_carrot || item == Item.getItemFromBlock(Blocks.yellow_flower);
    }

    @Override
    protected void updateAITasks() {
        super.updateAITasks();
        if (hopCooldown > 0) {
            hopCooldown--;
        }
        if (onGround && hopCooldown == 0 && !getNavigator().noPath()) {
            motionY = 0.3D;
            isAirBorne = true;
            float yaw = rotationYaw * 0.017453292F;
            double speed = getEntityAttribute(SharedMonsterAttributes.movementSpeed).getAttributeValue() * 0.6D;
            motionX += (double) (-MathHelper.sin(yaw)) * speed;
            motionZ += (double) MathHelper.cos(yaw) * speed;
            hopCooldown = getAIMoveSpeed() < 0.2F ? 10 : 6;
            worldObj.setEntityState(this, STATE_HOP);
            playSound(SOUND + "jump", getSoundVolume(), ((rand.nextFloat() - rand.nextFloat()) * 0.2F + 1.0F) * 0.8F);
        }
    }

    /** Between hops the rabbit stays where it landed. */
    @Override
    public void moveEntityWithHeading(float strafe, float forward) {
        if (onGround && !isInWater()) {
            super.moveEntityWithHeading(0.0F, 0.0F);
        } else {
            super.moveEntityWithHeading(strafe, forward);
        }
    }

    @Override
    public void handleHealthUpdate(byte state) {
        if (state == STATE_HOP) {
            jumpDuration = 10;
            jumpTicks = 0;
        } else {
            super.handleHealthUpdate(state);
        }
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (jumpTicks != jumpDuration) {
            jumpTicks++;
        } else if (jumpDuration != 0) {
            jumpTicks = 0;
            jumpDuration = 0;
        }
    }

    @Override
    protected String getLivingSound() {
        return SOUND + "ambient";
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
    protected void func_145780_a(int x, int y, int z, Block block) {
    }

    /** loot_tables/entities/rabbit.json */
    @Override
    protected void dropFewItems(boolean byPlayer, int looting) {
        int extra = looting > 0 ? rand.nextInt(looting + 1) : 0;
        dropItem(Mobs112.rabbitHide, rand.nextInt(2) + extra);
        dropItem(isBurning() ? Mobs112.rabbitCooked : Mobs112.rabbitRaw, rand.nextInt(2) + extra);
        if (byPlayer && rand.nextFloat() < 0.1F + (float) looting * 0.03F) {
            dropItem(Mobs112.rabbitFoot, 1);
        }
    }

}
