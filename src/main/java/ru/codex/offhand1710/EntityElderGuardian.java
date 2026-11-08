package ru.codex.offhand1710;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.World;

/** EntityElderGuardian of 1.12.2: bigger, stronger, and it curses nearby players with Mining Fatigue III. */
public class EntityElderGuardian extends EntityGuardian {
    private static final String SOUND = MC112Bridge.MOD_ID + ":entity.elder_guardian.";

    public EntityElderGuardian(World world) {
        super(world);
        setSize(width * 2.35F, height * 2.35F);
        func_110163_bv();
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.30000001192092896D);
        getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(8.0D);
        getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(80.0D);
    }

    @Override
    public int attackDuration() {
        return 60;
    }

    @Override
    protected int wanderChance() {
        return 400;
    }

    @Override
    protected boolean isElder() {
        return true;
    }

    @Override
    protected String soundPrefix() {
        return SOUND;
    }

    @Override
    protected void updateAITasks() {
        super.updateAITasks();
        if ((ticksExisted + getEntityId()) % 1200 != 0) {
            return;
        }
        for (Object object : worldObj.playerEntities) {
            EntityPlayer player = (EntityPlayer) object;
            if (!(player instanceof EntityPlayerMP) || player.capabilities.isCreativeMode || player.getDistanceSqToEntity(this) >= 2500.0D) {
                continue;
            }
            PotionEffect current = player.getActivePotionEffect(Potion.digSlowdown);
            if (current == null || current.getAmplifier() < 2 || current.getDuration() < 1200) {
                player.addPotionEffect(new PotionEffect(Potion.digSlowdown.id, 6000, 2));
                worldObj.playSoundAtEntity(player, SOUND + "curse", 1.0F, 1.0F);
            }
        }
    }

    /** loot_tables/entities/elder_guardian.json adds a sponge for the killing player. */
    @Override
    protected void dropFewItems(boolean byPlayer, int looting) {
        super.dropFewItems(byPlayer, looting);
        if (byPlayer) {
            dropItem(Item.getItemFromBlock(Blocks.sponge), 1);
        }
    }
}
