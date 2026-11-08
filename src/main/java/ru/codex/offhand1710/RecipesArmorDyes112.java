package ru.codex.offhand1710;

import net.minecraft.block.BlockColored;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.RecipesArmorDyes;

/**
 * Leather armor dyeing: the vanilla 1.7.10 RecipesArmorDyes algorithm (average of the colors, brightness
 * kept), with the 1.12.2 dye colors (EnumDyeColor.getColorComponentValues) instead of the sheep wool table.
 */
public final class RecipesArmorDyes112 extends RecipesArmorDyes {
    @Override
    public ItemStack getCraftingResult(InventoryCrafting inventory) {
        ItemStack result = null;
        int[] sum = new int[3];
        int brightness = 0;
        int count = 0;
        ItemArmor armor = null;

        for (int slot = 0; slot < inventory.getSizeInventory(); ++slot) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack == null) {
                continue;
            }
            if (stack.getItem() instanceof ItemArmor) {
                armor = (ItemArmor) stack.getItem();
                if (armor.getArmorMaterial() != ItemArmor.ArmorMaterial.CLOTH || result != null) {
                    return null;
                }
                result = stack.copy();
                result.stackSize = 1;
                if (armor.hasColor(stack)) {
                    int color = armor.getColor(result);
                    float r = (float) (color >> 16 & 255) / 255.0F;
                    float g = (float) (color >> 8 & 255) / 255.0F;
                    float b = (float) (color & 255) / 255.0F;
                    brightness = (int) ((float) brightness + Math.max(r, Math.max(g, b)) * 255.0F);
                    sum[0] = (int) ((float) sum[0] + r * 255.0F);
                    sum[1] = (int) ((float) sum[1] + g * 255.0F);
                    sum[2] = (int) ((float) sum[2] + b * 255.0F);
                    ++count;
                }
            } else {
                if (stack.getItem() != Items.dye) {
                    return null;
                }
                float[] dye = Colors112.dye(BlockColored.func_150032_b(stack.getItemDamage()));
                int r = (int) (dye[0] * 255.0F);
                int g = (int) (dye[1] * 255.0F);
                int b = (int) (dye[2] * 255.0F);
                brightness += Math.max(r, Math.max(g, b));
                sum[0] += r;
                sum[1] += g;
                sum[2] += b;
                ++count;
            }
        }

        if (armor == null) {
            return null;
        }
        int r = sum[0] / count;
        int g = sum[1] / count;
        int b = sum[2] / count;
        float average = (float) brightness / (float) count;
        float max = (float) Math.max(r, Math.max(g, b));
        r = (int) ((float) r * average / max);
        g = (int) ((float) g * average / max);
        b = (int) ((float) b * average / max);
        int color = (r << 8) + g;
        color = (color << 8) + b;
        armor.func_82813_b(result, color);
        return result;
    }
}
