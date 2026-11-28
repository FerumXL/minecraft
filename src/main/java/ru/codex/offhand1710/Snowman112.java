package ru.codex.offhand1710;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.monster.EntitySnowman;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.EntityInteractEvent;

/**
 * The snow golem of 1.9 - 1.12.2 (EntitySnowman.PUMPKIN_EQUIPPED, processInteract): shears take its pumpkin off for
 * good, the face shows; saved as "Pumpkin". -Dmc112.snowman17 leaves the golem of 1.7.10.
 */
public final class Snowman112 {
    public static final boolean OFF = System.getProperty("mc112.snowman17") != null;
    /** DataWatcher slot of the pumpkin flag (16 = on, as the byte of 1.12.2). */
    public static final int WATCH_PUMPKIN = 30;
    private static final String KEY = "Pumpkin";

    public Snowman112() {
    }

    public static boolean hasPumpkin(EntitySnowman golem) {
        return OFF || (golem.getDataWatcher().getWatchableObjectByte(WATCH_PUMPKIN) & 16) != 0;
    }

    public static void setPumpkin(EntitySnowman golem, boolean on) {
        golem.getDataWatcher().updateObject(WATCH_PUMPKIN, Byte.valueOf((byte) (on ? 16 : 0)));
        golem.getEntityData().setBoolean(KEY, on);
    }

    @SubscribeEvent
    public void onConstruct(EntityEvent.EntityConstructing event) {
        if (!OFF && event.entity instanceof EntitySnowman) {
            event.entity.getDataWatcher().addObject(WATCH_PUMPKIN, Byte.valueOf((byte) 16));
        }
    }

    /** readEntityFromNBT: "Pumpkin" (kept in the Forge data of the entity). */
    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent event) {
        if (!OFF && !event.world.isRemote && event.entity instanceof EntitySnowman && event.entity.getEntityData().hasKey(KEY)) {
            setPumpkin((EntitySnowman) event.entity, event.entity.getEntityData().getBoolean(KEY));
        }
    }

    /** EntitySnowman.processInteract of 1.12.2. */
    @SubscribeEvent
    public void onInteract(EntityInteractEvent event) {
        if (OFF || !(event.target instanceof EntitySnowman)) {
            return;
        }
        EntityPlayer player = event.entityPlayer;
        ItemStack held = player.getCurrentEquippedItem();
        EntitySnowman golem = (EntitySnowman) event.target;
        if (held == null || held.getItem() != Items.shears || !hasPumpkin(golem)) {
            return;
        }
        if (player.worldObj.isRemote) {
            player.swingItem();
            return;
        }
        setPumpkin(golem, false);
        held.damageItem(1, player);
        if (held.stackSize <= 0) {
            player.destroyCurrentEquippedItem();
        }
        event.setCanceled(true);
    }
}
