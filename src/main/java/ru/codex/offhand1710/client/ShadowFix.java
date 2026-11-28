package ru.codex.offhand1710.client;

import net.minecraft.entity.Entity;

/** Called from Render.renderShadow in place of Entity.getShadowSize (see Transformer, -Dmc112.shadow17). */
public final class ShadowFix {
    private ShadowFix() {
    }

    /** The height of the feet relative to posY, which is where 1.12.2 measures the shadow from. */
    public static float feet(Entity e) {
        return e.boundingBox == null ? 0.0F : (float) (e.boundingBox.minY - e.posY);
    }
}
