package ru.codex.offhand1710;

import java.util.List;

import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.RecipesArmorDyes;

/**
 * The dye palette of 1.12.2 ("World of Color"). Values are read from EnumDyeColor of the 1.12.2 jar.
 *
 * 1.7.10 keeps a single table, EntitySheep.fleeceColorTable, for sheep wool, wolf collars and leather armor
 * dyeing. 1.12.2 uses the dye color directly for collars and leather, and dye color * 0.75 (white 0.9019608)
 * for sheep. So the table gets the 1.12.2 sheep colors, wolf collars swap in the plain dye colors while they
 * are drawn (client.WolfCollarColors), and leather dyeing gets its own recipe with the plain dye colors.
 * The 1.12.2 block and entity textures are in assets/minecraft of the mod and replace the vanilla ones.
 */
public final class Colors112 {
    /** EnumDyeColor color values of 1.12.2 by wool metadata: white, orange, magenta, light blue ... black. */
    public static final int[] DYE_COLORS = {
            16383998, 16351261, 13061821, 3847130, 16701501, 8439583, 15961002, 4673362,
            10329495, 1481884, 8991416, 3949738, 8606770, 6192150, 11546150, 1908001
    };

    private static final float[][] SHEEP = new float[16][];
    private static final float[][] DYE = new float[16][];

    static {
        for (int i = 0; i < 16; i++) {
            DYE[i] = components(DYE_COLORS[i]);
            SHEEP[i] = i == 0
                    ? new float[] {0.9019608F, 0.9019608F, 0.9019608F}
                    : new float[] {DYE[i][0] * 0.75F, DYE[i][1] * 0.75F, DYE[i][2] * 0.75F};
        }
    }

    private Colors112() {
    }

    /** EnumDyeColor.getColorComponentValues. */
    private static float[] components(int color) {
        return new float[] {
                (float) ((color & 0xFF0000) >> 16) / 255.0F,
                (float) ((color & 0x00FF00) >> 8) / 255.0F,
                (float) (color & 0x0000FF) / 255.0F
        };
    }

    /** Plain 1.12.2 dye color components for a wool metadata. */
    public static float[] dye(int woolMeta) {
        return DYE[woolMeta & 15];
    }

    public static void apply() {
        useSheepColors();
        replaceArmorDyeRecipe();
    }

    /** The table as sheep wool needs it (the normal state). */
    public static void useSheepColors() {
        copyIntoFleeceTable(SHEEP);
    }

    /** The table as wolf collars need it; only while a wolf is drawn. */
    public static void useDyeColors() {
        copyIntoFleeceTable(DYE);
    }

    private static void copyIntoFleeceTable(float[][] colors) {
        for (int i = 0; i < 16 && i < EntitySheep.fleeceColorTable.length; i++) {
            System.arraycopy(colors[i], 0, EntitySheep.fleeceColorTable[i], 0, 3);
        }
    }

    @SuppressWarnings("unchecked")
    private static void replaceArmorDyeRecipe() {
        List<Object> recipes = CraftingManager.getInstance().getRecipeList();
        for (int i = 0; i < recipes.size(); i++) {
            if (recipes.get(i).getClass() == RecipesArmorDyes.class) {
                recipes.set(i, new RecipesArmorDyes112());
            }
        }
    }
}
