package ru.codex.offhand1710.client;

import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.common.collect.Multimap;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import org.lwjgl.opengl.GL11;
import ru.codex.offhand1710.Combat112;

/** The client side of the 1.12.2 combat: attack indicator, weapon lowering after a swing, tooltips, swings at nothing. */
public final class CombatClient {
    private static final ResourceLocation ICONS = new ResourceLocation("mc112bridge", "textures/gui/icons.png");
    private static final UUID DAMAGE_MODIFIER = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");
    private static final DecimalFormat FORMAT = new DecimalFormat("#.##");

    private static Field equipped;
    private static Field prevEquipped;
    private static boolean equipBroken;

    /** GuiIngame.renderAttackIndicator of 1.12.2, crosshair mode, drawn with the crosshair's inverting blend. */
    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.CROSSHAIRS) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.gameSettings.thirdPersonView != 0) {
            return;
        }
        float strength = Combat112.attackStrength(mc.thePlayer, 0.0F);
        boolean ready = strength >= 1.0F && mc.pointedEntity instanceof EntityLivingBase
                && Combat112.cooldownPeriod(mc.thePlayer) > 5.0F && mc.pointedEntity.isEntityAlive();
        if (!ready && strength >= 1.0F) {
            return;
        }
        int y = event.resolution.getScaledHeight() / 2 - 7 + 16;
        int x = event.resolution.getScaledWidth() / 2 - 8;
        Gui gui = mc.ingameGUI;
        mc.getTextureManager().bindTexture(ICONS);
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(GL11.GL_ONE_MINUS_DST_COLOR, GL11.GL_ONE_MINUS_SRC_COLOR, 1, 0);
        if (ready) {
            gui.drawTexturedModalRect(x, y, 68, 94, 16, 16);
        } else {
            gui.drawTexturedModalRect(x, y, 36, 94, 16, 4);
            gui.drawTexturedModalRect(x, y, 52, 94, (int) (strength * 17.0F), 4);
        }
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GL11.glDisable(GL11.GL_BLEND);
        mc.getTextureManager().bindTexture(Gui.icons);
    }

    /** Minecraft.clickMouse of 1.12.2: a swing at nothing restarts the cooldown (only on the client there too). */
    @SubscribeEvent
    public void onMouse(MouseEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!event.buttonstate || mc.thePlayer == null || mc.currentScreen != null
                || event.button - 100 != mc.gameSettings.keyBindAttack.getKeyCode()) {
            return;
        }
        MovingObjectPosition hit = mc.objectMouseOver;
        if (hit != null && hit.typeOfHit != MovingObjectPosition.MovingObjectType.MISS) {
            return;
        }
        Integer counter = ReflectionHelper.getPrivateValue(Minecraft.class, mc, "field_71429_W", "leftClickCounter");
        if (counter.intValue() <= 0) {
            Combat112.resetCooldown(mc.thePlayer);
        }
    }

    /**
     * ItemRenderer.updateEquippedItem of 1.12.2: the held item sinks after a swing and rises with the cooldown. It runs
     * after the vanilla update of the tick, whose direction tells whether the item in the hand is still the same one.
     */
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.phase != TickEvent.Phase.END || mc.thePlayer == null || mc.isGamePaused() || equipBroken) {
            return;
        }
        ItemRenderer renderer = mc.entityRenderer.itemRenderer;
        try {
            if (equipped == null) {
                equipped = ReflectionHelper.findField(ItemRenderer.class, "field_78454_c", "equippedProgress");
                prevEquipped = ReflectionHelper.findField(ItemRenderer.class, "field_78451_d", "prevEquippedProgress");
            }
            float previous = prevEquipped.getFloat(renderer);
            float current = equipped.getFloat(renderer);
            boolean same = current > previous || current >= 1.0F;
            float strength = Combat112.attackStrength(mc.thePlayer, 1.0F);
            float target = same ? strength * strength * strength : 0.0F;
            float delta = target - previous;
            delta = delta < -0.4F ? -0.4F : (delta > 0.4F ? 0.4F : delta);
            equipped.setFloat(renderer, previous + delta);
        } catch (Exception e) {
            equipBroken = true;
            FMLLog.warning("[mc112bridge] weapon lowering after a swing disabled: %s", e);
        }
    }

    /** ItemStack.getTooltip of 1.12.2: the attack values are shown as totals under "when in main hand". */
    @SubscribeEvent
    public void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.itemStack;
        Double speed = Combat112.attackSpeedModifier(stack);
        Multimap<?, ?> modifiers = stack.getAttributeModifiers();
        boolean hasDamage = stack.getItem() instanceof ItemHoe;
        for (Object value : modifiers.values()) {
            hasDamage |= DAMAGE_MODIFIER.equals(((AttributeModifier) value).getID());
        }
        if (!hasDamage && speed == null) {
            return;
        }
        List<String> otherLines = new ArrayList<String>();
        int insertAt = removeVanillaLines(event.toolTip, stack, modifiers, otherLines);

        List<String> block = new ArrayList<String>();
        if (insertAt < 0) {
            insertAt = event.toolTip.size();
            block.add("");
        }
        block.add(StatCollector.translateToLocal("item.modifiers.mainhand"));
        EntityPlayer player = event.entityPlayer;
        if (hasDamage) {
            double base = player != null
                    ? player.getEntityAttribute(SharedMonsterAttributes.attackDamage).getBaseValue() : 1.0D;
            double amount = base + damageModifier(modifiers) + (double) Combat112.tooltipEnchantmentDamage(stack);
            block.add(equalsLine(amount, "generic.attackDamage"));
        }
        if (speed != null) {
            block.add(equalsLine(4.0D + speed.doubleValue(), "generic.attackSpeed"));
        }
        block.addAll(otherLines);
        event.toolTip.addAll(insertAt, block);
    }

    private static double damageModifier(Multimap<?, ?> modifiers) {
        double amount = 0.0D;
        for (Object value : modifiers.values()) {
            AttributeModifier modifier = (AttributeModifier) value;
            if (DAMAGE_MODIFIER.equals(modifier.getID())) {
                amount += modifier.getAmount();
            }
        }
        return amount;
    }

    private static String equalsLine(double amount, String attribute) {
        return " " + StatCollector.translateToLocalFormatted("attribute.modifier.equals.0", FORMAT.format(amount),
                StatCollector.translateToLocal("attribute.name." + attribute));
    }

    /**
     * Removes the attribute section that 1.7.10 wrote and returns where it was, after its empty line, or -1. Modifiers
     * other than the weapon damage are collected to be shown again, unchanged.
     */
    private static int removeVanillaLines(List<String> tooltip, ItemStack stack, Multimap<?, ?> modifiers, List<String> otherLines) {
        int first = -1;
        for (Map.Entry<?, ?> entry : modifiers.entries()) {
            AttributeModifier modifier = (AttributeModifier) entry.getValue();
            boolean weapon = DAMAGE_MODIFIER.equals(modifier.getID());
            String line = vanillaLine(stack, (String) entry.getKey(), modifier, weapon);
            int index = line == null ? -1 : tooltip.indexOf(line);
            if (index < 0) {
                continue;
            }
            tooltip.remove(index);
            first = first < 0 ? index : Math.min(first, index);
            if (!weapon) {
                otherLines.add(line);
            }
        }
        return first;
    }

    /** One attribute line exactly as ItemStack.getTooltip of 1.7.10 builds it. */
    private static String vanillaLine(ItemStack stack, String name, AttributeModifier modifier, boolean weapon) {
        double amount = modifier.getAmount();
        if (weapon) {
            amount += (double) EnchantmentHelper.func_152377_a(stack, EnumCreatureAttribute.UNDEFINED);
        }
        int operation = modifier.getOperation();
        double shown = operation != 1 && operation != 2 ? amount : amount * 100.0D;
        String attribute = StatCollector.translateToLocal("attribute.name." + name);
        if (amount > 0.0D) {
            return EnumChatFormatting.BLUE + StatCollector.translateToLocalFormatted("attribute.modifier.plus." + operation,
                    FORMAT.format(shown), attribute);
        }
        if (amount < 0.0D) {
            return EnumChatFormatting.RED + StatCollector.translateToLocalFormatted("attribute.modifier.take." + operation,
                    FORMAT.format(-shown), attribute);
        }
        return null;
    }
}
