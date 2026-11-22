package ru.codex.offhand1710;

import cpw.mods.fml.relauncher.ReflectionHelper;
import java.lang.reflect.Field;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.FoodStats;
import net.minecraft.world.EnumDifficulty;

/**
 * FoodStats.onUpdate of 1.9 - 1.12.2: a full bar with saturation heals fast (every 10 ticks, min(saturation, 6) / 6,
 * costing that much exhaustion); from 18 food it heals 1 every 80 ticks for 6 exhaustion (3 in 1.7.10).
 * -Dmc112.food17 keeps the regeneration of 1.7.10.
 */
public final class Food112 {
    public static final boolean OFF = System.getProperty("mc112.food17") != null;
    private static Field level, saturation, exhaustion, timer, previous;
    private static boolean broken;

    private Food112() {
    }

    private static void fields() {
        if (level == null) {
            level = ReflectionHelper.findField(FoodStats.class, "field_75127_a", "foodLevel");
            saturation = ReflectionHelper.findField(FoodStats.class, "field_75125_b", "foodSaturationLevel");
            exhaustion = ReflectionHelper.findField(FoodStats.class, "field_75126_c", "foodExhaustionLevel");
            timer = ReflectionHelper.findField(FoodStats.class, "field_75123_d", "foodTimer");
            previous = ReflectionHelper.findField(FoodStats.class, "field_75124_e", "prevFoodLevel");
        }
    }

    /** Replaces FoodStats.onUpdate; false leaves it to 1.7.10. */
    public static boolean update(FoodStats food, EntityPlayer player) {
        if (OFF || broken) {
            return false;
        }
        try {
            fields();
            EnumDifficulty difficulty = player.worldObj.difficultySetting;
            int foodLevel = level.getInt(food);
            float sat = saturation.getFloat(food);
            float exh = exhaustion.getFloat(food);
            int t = timer.getInt(food);
            previous.setInt(food, foodLevel);
            if (exh > 4.0F) {
                exh -= 4.0F;
                if (sat > 0.0F) {
                    sat = Math.max(sat - 1.0F, 0.0F);
                } else if (difficulty != EnumDifficulty.PEACEFUL) {
                    foodLevel = Math.max(foodLevel - 1, 0);
                }
            }
            level.setInt(food, foodLevel);
            saturation.setFloat(food, sat);
            exhaustion.setFloat(food, exh);
            boolean regen = player.worldObj.getGameRules().getGameRuleBooleanValue("naturalRegeneration");
            if (regen && sat > 0.0F && player.shouldHeal() && foodLevel >= 20) {
                if (++t >= 10) {
                    float f = Math.min(sat, 6.0F);
                    player.heal(f / 6.0F);
                    food.addExhaustion(f);
                    t = 0;
                }
            } else if (regen && foodLevel >= 18 && player.shouldHeal()) {
                if (++t >= 80) {
                    player.heal(1.0F);
                    food.addExhaustion(6.0F);
                    t = 0;
                }
            } else if (foodLevel <= 0) {
                if (++t >= 80) {
                    if (player.getHealth() > 10.0F || difficulty == EnumDifficulty.HARD || player.getHealth() > 1.0F && difficulty == EnumDifficulty.NORMAL) {
                        player.attackEntityFrom(DamageSource.starve, 1.0F);
                    }
                    t = 0;
                }
            } else {
                t = 0;
            }
            timer.setInt(food, t);
            return true;
        } catch (Exception e) {
            broken = true;
            org.apache.logging.log4j.LogManager.getLogger("mc112bridge").error("1.12 food off: " + e);
            return false;
        }
    }
}
