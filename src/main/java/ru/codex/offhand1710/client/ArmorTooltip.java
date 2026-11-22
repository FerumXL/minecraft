package ru.codex.offhand1710.client;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.item.ItemArmor;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.ISpecialArmor;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import ru.codex.offhand1710.Armor112;

/** ItemStack.getTooltip of 1.12.2 for armor: "When on Head:" and the armor and toughness it gives, in blue. */
public final class ArmorTooltip {
    private static final String[] SLOTS = {"head", "chest", "legs", "feet"};

    @SubscribeEvent
    public void onTooltip(ItemTooltipEvent event) {
        if (Armor112.OFF || event.itemStack == null || !(event.itemStack.getItem() instanceof ItemArmor) || event.itemStack.getItem() instanceof ISpecialArmor) {
            return;
        }
        ItemArmor armor = (ItemArmor) event.itemStack.getItem();
        if (armor.armorType < 0 || armor.armorType > 3) {
            return;
        }
        event.toolTip.add("");
        event.toolTip.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal("item.modifiers." + SLOTS[armor.armorType]));
        event.toolTip.add(EnumChatFormatting.BLUE + " " + StatCollector.translateToLocalFormatted("attribute.modifier.plus.0", armor.damageReduceAmount,
            StatCollector.translateToLocal("attribute.name.generic.armor")));
        float tough = Armor112.toughness(armor);
        if (tough > 0.0F) {
            event.toolTip.add(EnumChatFormatting.BLUE + " " + StatCollector.translateToLocalFormatted("attribute.modifier.plus.0", (int) tough,
                StatCollector.translateToLocal("attribute.name.generic.armorToughness")));
        }
    }
}
