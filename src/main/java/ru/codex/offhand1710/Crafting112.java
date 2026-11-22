package ru.codex.offhand1710;

import cpw.mods.fml.relauncher.ReflectionHelper;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;

/**
 * doLimitedCrafting of 1.12.2 (ContainerWorkbench/ContainerPlayer.slotChangedCraftingGrid): with the rule on, a grid
 * gives its result only when the player has unlocked that recipe. Each recipe of the 1.12.2 book (Data112) is tied to
 * the 1.7.10 IRecipe that crafts it, by laying it out in a grid once. Recipes outside the book (the special ones of
 * 1.12.2 such as fireworks and dyed armour, and those of other mods) always craft.
 */
public final class Crafting112 {
    private static Map<IRecipe, Set<String>> book;
    private static int builtFrom = -1;

    private Crafting112() {
    }

    /** CraftingManager.findMatchingRecipe hook: the result, or null when the rule hides it. */
    public static ItemStack limit(ItemStack result, InventoryCrafting grid, World world) {
        if (result == null || Rules112.OFF || world == null || !MC112Bridge.proxy.limitedCrafting(world)) {
            return result;
        }
        EntityPlayer player = player(grid, world);
        if (player == null) {
            return result;
        }
        IRecipe recipe = recipeFor(grid, world);
        Set<String> ids = recipe == null ? null : book(world).get(recipe);
        if (ids == null) {
            return result;
        }
        for (String id : ids) {
            if (MC112Bridge.proxy.knowsRecipe(player, id)) {
                return result;
            }
        }
        return null;
    }

    private static EntityPlayer player(InventoryCrafting grid, World world) {
        if (world.isRemote) {
            return MC112Bridge.proxy.clientPlayer();
        }
        Container c;
        try {
            c = ReflectionHelper.getPrivateValue(InventoryCrafting.class, grid, "field_70465_c", "eventHandler");
        } catch (Exception e) {
            return null;
        }
        if (c instanceof ContainerPlayer) {
            return ReflectionHelper.getPrivateValue(ContainerPlayer.class, (ContainerPlayer) c, "field_82862_h", "thePlayer");
        }
        if (c == null) {
            return null;
        }
        List<?> crafters = ReflectionHelper.getPrivateValue(Container.class, c, "field_75149_d", "crafters");
        for (Object o : crafters) {
            if (o instanceof EntityPlayer) {
                return (EntityPlayer) o;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static IRecipe recipeFor(InventoryCrafting grid, World world) {
        for (Object o : CraftingManager.getInstance().getRecipeList()) {
            IRecipe r = (IRecipe) o;
            try {
                if (r.matches(grid, world)) {
                    return r;
                }
            } catch (RuntimeException e) {
                // a mod recipe that cannot look at a grid without its container
            }
        }
        return null;
    }

    /** The 1.7.10 recipe of each 1.12.2 book recipe, by crafting its first choice of every slot once. */
    static synchronized Map<IRecipe, Set<String>> book(World world) {
        if (book != null && builtFrom == Data112.RECIPES.size()) {
            return book;
        }
        Map<IRecipe, Set<String>> m = new IdentityHashMap<IRecipe, Set<String>>();
        for (Data112.Recipe r : Data112.RECIPES.values()) {
            InventoryCrafting grid = grid(r);
            if (grid == null) {
                continue;
            }
            IRecipe found = recipeFor(grid, world);
            if (found != null) {
                Set<String> ids = m.get(found);
                if (ids == null) {
                    ids = new HashSet<String>();
                    m.put(found, ids);
                }
                ids.add(r.id);
            }
        }
        book = m;
        builtFrom = Data112.RECIPES.size();
        return m;
    }

    static InventoryCrafting grid(Data112.Recipe r) {
        InventoryCrafting grid = new InventoryCrafting(new Container() {
            @Override
            public boolean canInteractWith(EntityPlayer player) {
                return false;
            }
        }, 3, 3);
        for (int i = 0; i < r.slots.size(); i++) {
            List<ItemStack> options = r.slots.get(i);
            if (options == null || options.isEmpty()) {
                continue;
            }
            int index = r.shaped ? (i / Math.max(1, r.width)) * 3 + i % Math.max(1, r.width) : i;
            if (index >= 9) {
                return null;
            }
            ItemStack s = options.get(0).copy();
            s.stackSize = 1;
            grid.setInventorySlotContents(index, s);
        }
        return grid;
    }
}
