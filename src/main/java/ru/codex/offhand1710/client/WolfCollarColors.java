package ru.codex.offhand1710.client;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraftforge.client.event.RenderLivingEvent;
import ru.codex.offhand1710.Colors112;

/**
 * RenderWolf tints the collar from EntitySheep.fleeceColorTable, which holds the darker sheep colors. In
 * 1.12.2 the collar has the plain dye color, so the table holds the dye colors while a wolf is drawn.
 */
public final class WolfCollarColors {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void before(RenderLivingEvent.Pre event) {
        if (event.entity instanceof EntityWolf && !event.isCanceled()) {
            Colors112.useDyeColors();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void after(RenderLivingEvent.Post event) {
        if (event.entity instanceof EntityWolf) {
            Colors112.useSheepColors();
        }
    }
}
