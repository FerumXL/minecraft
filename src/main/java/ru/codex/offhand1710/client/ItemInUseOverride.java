package ru.codex.offhand1710.client;

import java.lang.reflect.Field;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import ru.codex.offhand1710.OffhandInventory;

/**
 * While the offhand item is drawn, makes the player report it as the item in use (EntityPlayer.itemInUse and
 * itemInUseCount), so EntityPlayer.getItemIcon and Item.getIcon(..., usingItem, useRemaining) pick the
 * pulling frames of a bow, and custom item renderers see the item as used. Restored right after drawing.
 */
final class ItemInUseOverride {
    private static Field itemInUse;
    private static Field itemInUseCount;
    private static boolean broken;

    private ItemStack savedItem;
    private int savedCount;
    private boolean applied;

    private ItemInUseOverride() {
    }

    /** Returns an override to undo with restore(), or null when the offhand item is not in use. */
    static ItemInUseOverride apply(EntityPlayer player, ItemStack stack) {
        OffhandInventory offhand = OffhandInventory.get(player);
        if (broken || !offhand.isUsing() || stack == null) {
            return null;
        }
        try {
            if (itemInUse == null) {
                itemInUse = ReflectionHelper.findField(EntityPlayer.class, "field_71074_e", "itemInUse");
                itemInUseCount = ReflectionHelper.findField(EntityPlayer.class, "field_71072_f", "itemInUseCount");
            }
            ItemInUseOverride override = new ItemInUseOverride();
            override.savedItem = (ItemStack) itemInUse.get(player);
            override.savedCount = itemInUseCount.getInt(player);
            itemInUse.set(player, stack);
            itemInUseCount.setInt(player, offhand.useCount);
            override.applied = true;
            return override;
        } catch (Exception e) {
            broken = true;
            FMLLog.warning("[mc112bridge] offhand bow frames disabled: %s", e);
            return null;
        }
    }

    void restore(EntityPlayer player) {
        if (!applied) {
            return;
        }
        try {
            itemInUse.set(player, savedItem);
            itemInUseCount.setInt(player, savedCount);
        } catch (Exception e) {
            broken = true;
        }
    }
}
