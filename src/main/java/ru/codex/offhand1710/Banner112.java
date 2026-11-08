package ru.codex.offhand1710;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.StatCollector;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/** Boats of five more woods (1.9) and banners with their crafted patterns (1.8). */
public final class Banner112 {
    public static final String[] WOODS = {"spruce", "birch", "jungle", "acacia", "dark_oak"};
    /** Names used by the translation keys of banners, by dye damage value reversed (0 = white). */
    public static final String[] COLOR_KEYS = {"white", "orange", "magenta", "lightBlue", "yellow", "lime", "pink", "gray",
            "silver", "cyan", "purple", "blue", "brown", "green", "red", "black"};
    /** EnumDyeColor.getColorValue of 1.12.2, by dye damage value (0 = black). */
    public static final int[] COLOR_VALUES = {1908001, 11546150, 6192150, 8606770, 3949738, 8991416, 1481884, 10329495,
            4673362, 15961002, 8439583, 16701501, 3847130, 13061821, 16351261, 16383998};
    public static final int MAX_PATTERNS = 6;
    public static final Item[] BOAT_ITEMS = new Item[5];
    public static BlockBanner standing;
    public static BlockBanner wall;
    public static Item banner;

    /** BannerPattern of 1.12.2: texture, short code, and either a 3x3 shape or an ingredient. */
    public static final Pattern[] PATTERNS = {
            new Pattern("base", "b"),
            new Pattern("square_bottom_left", "bl", "   ", "   ", "#  "),
            new Pattern("square_bottom_right", "br", "   ", "   ", "  #"),
            new Pattern("square_top_left", "tl", "#  ", "   ", "   "),
            new Pattern("square_top_right", "tr", "  #", "   ", "   "),
            new Pattern("stripe_bottom", "bs", "   ", "   ", "###"),
            new Pattern("stripe_top", "ts", "###", "   ", "   "),
            new Pattern("stripe_left", "ls", "#  ", "#  ", "#  "),
            new Pattern("stripe_right", "rs", "  #", "  #", "  #"),
            new Pattern("stripe_center", "cs", " # ", " # ", " # "),
            new Pattern("stripe_middle", "ms", "   ", "###", "   "),
            new Pattern("stripe_downright", "drs", "#  ", " # ", "  #"),
            new Pattern("stripe_downleft", "dls", "  #", " # ", "#  "),
            new Pattern("small_stripes", "ss", "# #", "# #", "   "),
            new Pattern("cross", "cr", "# #", " # ", "# #"),
            new Pattern("straight_cross", "sc", " # ", "###", " # "),
            new Pattern("triangle_bottom", "bt", "   ", " # ", "# #"),
            new Pattern("triangle_top", "tt", "# #", " # ", "   "),
            new Pattern("triangles_bottom", "bts", "   ", "# #", " # "),
            new Pattern("triangles_top", "tts", " # ", "# #", "   "),
            new Pattern("diagonal_left", "ld", "## ", "#  ", "   "),
            new Pattern("diagonal_up_right", "rd", "   ", "  #", " ##"),
            new Pattern("diagonal_up_left", "lud", "   ", "#  ", "## "),
            new Pattern("diagonal_right", "rud", " ##", "  #", "   "),
            new Pattern("circle", "mc", "   ", " # ", "   "),
            new Pattern("rhombus", "mr", " # ", "# #", " # "),
            new Pattern("half_vertical", "vh", "## ", "## ", "## "),
            new Pattern("half_horizontal", "hh", "###", "###", "   "),
            new Pattern("half_vertical_right", "vhr", " ##", " ##", " ##"),
            new Pattern("half_horizontal_bottom", "hhb", "   ", "###", "###"),
            new Pattern("border", "bo", "###", "# #", "###"),
            new Pattern("curly_border", "cbo"),
            new Pattern("creeper", "cre"),
            new Pattern("gradient", "gra", "# #", " # ", " # "),
            new Pattern("gradient_up", "gru", " # ", " # ", "# #"),
            new Pattern("bricks", "bri"),
            new Pattern("skull", "sku"),
            new Pattern("flower", "flo"),
            new Pattern("mojang", "moj")
    };

    private Banner112() {
    }

    public static final class Pattern {
        public final String name;
        public final String code;
        final String[] shape;
        ItemStack ingredient;

        Pattern(String name, String code, String... shape) {
            this.name = name;
            this.code = code;
            this.shape = shape.length == 3 ? shape : null;
        }

        boolean craftable() {
            return shape != null || ingredient != null;
        }
    }

    public static Pattern byCode(String code) {
        for (Pattern pattern : PATTERNS) {
            if (pattern.code.equals(code)) {
                return pattern;
            }
        }
        return null;
    }

    private static void ingredient(String code, ItemStack stack) {
        byCode(code).ingredient = stack;
    }

    public static void register(Object mod) {
        ingredient("cbo", new ItemStack(Blocks.vine));
        ingredient("cre", new ItemStack(Items.skull, 1, 4));
        ingredient("bri", new ItemStack(Blocks.brick_block));
        ingredient("sku", new ItemStack(Items.skull, 1, 1));
        ingredient("flo", new ItemStack(Blocks.red_flower, 1, 8));
        ingredient("moj", new ItemStack(Items.golden_apple, 1, 1));

        for (Iterator<?> recipes = CraftingManager.getInstance().getRecipeList().iterator(); recipes.hasNext();) {
            ItemStack output = ((IRecipe) recipes.next()).getRecipeOutput();
            if (output != null && output.getItem() == Items.boat) {
                recipes.remove();
            }
        }
        GameRegistry.addShapedRecipe(new ItemStack(Items.boat), "# #", "###", '#', new ItemStack(Blocks.planks, 1, 0));
        for (int i = 0; i < 5; i++) {
            Item boat = new BoatItem(i + 1).setUnlocalizedName(MC112Bridge.MOD_ID + ".boat_" + WOODS[i]).setTextureName(MC112Bridge.MOD_ID + ":boat_" + WOODS[i]).setCreativeTab(CreativeTabs.tabTransport).setMaxStackSize(1);
            GameRegistry.registerItem(boat, "boat_" + WOODS[i]);
            BOAT_ITEMS[i] = boat;
            GameRegistry.addShapedRecipe(new ItemStack(boat), "# #", "###", '#', new ItemStack(Blocks.planks, 1, i + 1));
        }
        EntityRegistry.registerModEntity(EntityBoat112.class, "Boat", 62, mod, 80, 3, true);

        standing = new BlockBanner(true);
        wall = new BlockBanner(false);
        GameRegistry.registerBlock(standing, (Class<? extends net.minecraft.item.ItemBlock>) null, "standing_banner");
        GameRegistry.registerBlock(wall, (Class<? extends net.minecraft.item.ItemBlock>) null, "wall_banner");
        GameRegistry.registerTileEntity(BannerTile.class, "mc112bridge.banner");
        banner = new BannerItem().setUnlocalizedName(MC112Bridge.MOD_ID + ".banner").setTextureName(MC112Bridge.MOD_ID + ":banner").setCreativeTab(CreativeTabs.tabDecorations);
        GameRegistry.registerItem(banner, "banner");
        for (int i = 0; i < 16; i++) {
            GameRegistry.addShapedRecipe(new ItemStack(banner, 1, 15 - i), "###", "###", " | ", '#', new ItemStack(Blocks.wool, 1, i), '|', Items.stick);
        }
        CraftingManager.getInstance().getRecipeList().add(new PatternRecipe());
    }

    /** RecipesBanners.RecipeAddPattern. */
    public static final class PatternRecipe implements IRecipe {
        private static ItemStack banner(InventoryCrafting grid) {
            ItemStack found = null;
            for (int i = 0; i < grid.getSizeInventory(); i++) {
                ItemStack stack = grid.getStackInSlot(i);
                if (stack != null && stack.getItem() == banner) {
                    if (found != null) {
                        return null;
                    }
                    found = stack;
                }
            }
            return found;
        }

        private static int patternCount(ItemStack stack) {
            if (!stack.hasTagCompound() || !stack.getTagCompound().hasKey("BlockEntityTag")) {
                return 0;
            }
            return stack.getTagCompound().getCompoundTag("BlockEntityTag").getTagList("Patterns", 10).tagCount();
        }

        private static Pattern find(InventoryCrafting grid) {
            if (grid.getSizeInventory() != 9) {
                return null;
            }
            for (Pattern pattern : PATTERNS) {
                if (pattern.craftable() && (pattern.ingredient != null ? matchesIngredient(pattern, grid) : matchesShape(pattern, grid))) {
                    return pattern;
                }
            }
            return null;
        }

        private static boolean matchesIngredient(Pattern pattern, InventoryCrafting grid) {
            boolean ingredient = false;
            boolean dye = false;
            for (int i = 0; i < 9; i++) {
                ItemStack stack = grid.getStackInSlot(i);
                if (stack == null || stack.getItem() == banner) {
                    continue;
                }
                if (stack.getItem() == Items.dye) {
                    if (dye) {
                        return false;
                    }
                    dye = true;
                } else {
                    if (ingredient || stack.getItem() != pattern.ingredient.getItem() || stack.getItemDamage() != pattern.ingredient.getItemDamage()) {
                        return false;
                    }
                    ingredient = true;
                }
            }
            return ingredient && dye;
        }

        private static boolean matchesShape(Pattern pattern, InventoryCrafting grid) {
            int color = -1;
            for (int i = 0; i < 9; i++) {
                ItemStack stack = grid.getStackInSlot(i);
                boolean wanted = pattern.shape[i / 3].charAt(i % 3) != ' ';
                if (stack == null || stack.getItem() == banner) {
                    if (wanted) {
                        return false;
                    }
                    continue;
                }
                if (stack.getItem() != Items.dye || !wanted || (color != -1 && color != stack.getItemDamage())) {
                    return false;
                }
                color = stack.getItemDamage();
            }
            return color != -1;
        }

        @Override
        public boolean matches(InventoryCrafting grid, World world) {
            ItemStack stack = banner(grid);
            return stack != null && patternCount(stack) < MAX_PATTERNS && find(grid) != null;
        }

        @Override
        public ItemStack getCraftingResult(InventoryCrafting grid) {
            ItemStack result = banner(grid).copy();
            result.stackSize = 1;
            Pattern pattern = find(grid);
            int color = 0;
            for (int i = 0; i < 9; i++) {
                ItemStack stack = grid.getStackInSlot(i);
                if (stack != null && stack.getItem() == Items.dye) {
                    color = stack.getItemDamage();
                    break;
                }
            }
            if (!result.hasTagCompound()) {
                result.setTagCompound(new NBTTagCompound());
            }
            if (!result.getTagCompound().hasKey("BlockEntityTag")) {
                result.getTagCompound().setTag("BlockEntityTag", new NBTTagCompound());
            }
            NBTTagCompound tag = result.getTagCompound().getCompoundTag("BlockEntityTag");
            NBTTagList list = tag.hasKey("Patterns") ? tag.getTagList("Patterns", 10) : new NBTTagList();
            NBTTagCompound entry = new NBTTagCompound();
            entry.setString("Pattern", pattern.code);
            entry.setInteger("Color", color);
            list.appendTag(entry);
            tag.setTag("Patterns", list);
            return result;
        }

        @Override
        public int getRecipeSize() {
            return 9;
        }

        @Override
        public ItemStack getRecipeOutput() {
            return null;
        }
    }

    public static final class BannerTile extends TileEntity {
        public int base = 15;
        public NBTTagList patterns;
        boolean creativeBroken;

        @Override
        public boolean canUpdate() {
            return false;
        }

        void setFrom(ItemStack stack) {
            base = stack.getItemDamage() & 15;
            patterns = null;
            if (stack.hasTagCompound() && stack.getTagCompound().hasKey("BlockEntityTag")) {
                patterns = (NBTTagList) stack.getTagCompound().getCompoundTag("BlockEntityTag").getTagList("Patterns", 10).copy();
            }
        }

        ItemStack toStack() {
            ItemStack stack = new ItemStack(banner, 1, base);
            if (patterns != null && patterns.tagCount() > 0) {
                NBTTagCompound inner = new NBTTagCompound();
                inner.setTag("Patterns", patterns.copy());
                NBTTagCompound tag = new NBTTagCompound();
                tag.setTag("BlockEntityTag", inner);
                stack.setTagCompound(tag);
            }
            return stack;
        }

        @Override
        public void writeToNBT(NBTTagCompound tag) {
            super.writeToNBT(tag);
            tag.setInteger("Base", base);
            if (patterns != null) {
                tag.setTag("Patterns", patterns);
            }
        }

        @Override
        public void readFromNBT(NBTTagCompound tag) {
            super.readFromNBT(tag);
            base = tag.getInteger("Base");
            patterns = tag.hasKey("Patterns") ? tag.getTagList("Patterns", 10) : null;
        }

        @Override
        public Packet getDescriptionPacket() {
            NBTTagCompound tag = new NBTTagCompound();
            writeToNBT(tag);
            return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 6, tag);
        }

        @Override
        public void onDataPacket(NetworkManager manager, S35PacketUpdateTileEntity packet) {
            readFromNBT(packet.func_148857_g());
        }

        @Override
        public AxisAlignedBB getRenderBoundingBox() {
            return AxisAlignedBB.getBoundingBox(xCoord - 1, yCoord - 2, zCoord - 1, xCoord + 2, yCoord + 3, zCoord + 2);
        }
    }

    /** BlockBanner: the standing one keeps 16 rotations in metadata, the wall one the side it faces. */
    public static final class BlockBanner extends BlockContainer {
        public final boolean isStanding;

        BlockBanner(boolean isStanding) {
            super(Material.wood);
            this.isStanding = isStanding;
            setHardness(1.0F);
            setStepSound(Block.soundTypeWood);
            setBlockName(MC112Bridge.MOD_ID + ".banner");
            disableStats();
        }

        @Override
        public TileEntity createNewTileEntity(World world, int meta) {
            return new BannerTile();
        }

        @Override
        public void registerBlockIcons(IIconRegister register) {
        }

        @Override
        public IIcon getIcon(int side, int meta) {
            return Blocks.planks.getIcon(side, 0);
        }

        @Override
        public boolean isOpaqueCube() {
            return false;
        }

        @Override
        public boolean renderAsNormalBlock() {
            return false;
        }

        @Override
        public int getRenderType() {
            return -1;
        }

        @Override
        public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
            return null;
        }

        @Override
        public boolean getBlocksMovement(IBlockAccess world, int x, int y, int z) {
            return true;
        }

        @Override
        public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
            if (isStanding) {
                setBlockBounds(0.25F, 0.0F, 0.25F, 0.75F, 1.0F, 0.75F);
                return;
            }
            switch (world.getBlockMetadata(x, y, z)) {
                case 3:
                    setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.78125F, 0.125F);
                    break;
                case 4:
                    setBlockBounds(0.875F, 0.0F, 0.0F, 1.0F, 0.78125F, 1.0F);
                    break;
                case 5:
                    setBlockBounds(0.0F, 0.0F, 0.0F, 0.125F, 0.78125F, 1.0F);
                    break;
                default:
                    setBlockBounds(0.0F, 0.0F, 0.875F, 1.0F, 0.78125F, 1.0F);
                    break;
            }
        }

        @Override
        public void onNeighborBlockChange(World world, int x, int y, int z, Block block) {
            boolean ok;
            if (isStanding) {
                ok = world.getBlock(x, y - 1, z).getMaterial().isSolid();
            } else {
                int meta = world.getBlockMetadata(x, y, z);
                int bx = x + (meta == 4 ? 1 : (meta == 5 ? -1 : 0));
                int bz = z + (meta == 2 || meta < 2 ? 1 : (meta == 3 ? -1 : 0));
                ok = world.getBlock(bx, y, bz).getMaterial().isSolid();
            }
            if (!ok) {
                world.setBlockToAir(x, y, z);
            }
        }

        @Override
        public int quantityDropped(Random random) {
            return 0;
        }

        @Override
        public void onBlockHarvested(World world, int x, int y, int z, int meta, EntityPlayer player) {
            TileEntity tile = world.getTileEntity(x, y, z);
            if (player.capabilities.isCreativeMode && tile instanceof BannerTile) {
                ((BannerTile) tile).creativeBroken = true;
            }
        }

        @Override
        public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
            TileEntity tile = world.getTileEntity(x, y, z);
            if (!world.isRemote && tile instanceof BannerTile && !((BannerTile) tile).creativeBroken) {
                EntityItem item = new EntityItem(world, (double) x + 0.5D, (double) y + 0.5D, (double) z + 0.5D, ((BannerTile) tile).toStack());
                item.delayBeforeCanPickup = 10;
                world.spawnEntityInWorld(item);
            }
            super.breakBlock(world, x, y, z, block, meta);
        }

        @Override
        public ItemStack getPickBlock(MovingObjectPosition target, World world, int x, int y, int z) {
            TileEntity tile = world.getTileEntity(x, y, z);
            return tile instanceof BannerTile ? ((BannerTile) tile).toStack() : new ItemStack(banner, 1, 15);
        }
    }

    public static final class BannerItem extends Item {
        BannerItem() {
            setHasSubtypes(true);
            setMaxDamage(0);
            setMaxStackSize(16);
        }

        @Override
        public String getItemStackDisplayName(ItemStack stack) {
            return StatCollector.translateToLocal("item.banner." + COLOR_KEYS[15 - (stack.getItemDamage() & 15)] + ".name");
        }

        @Override
        @SuppressWarnings({"rawtypes", "unchecked"})
        public void getSubItems(Item item, CreativeTabs tab, List list) {
            for (int i = 15; i >= 0; i--) {
                list.add(new ItemStack(item, 1, i));
            }
        }

        @Override
        @SuppressWarnings({"rawtypes", "unchecked"})
        public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
            if (!stack.hasTagCompound() || !stack.getTagCompound().hasKey("BlockEntityTag")) {
                return;
            }
            NBTTagList patterns = stack.getTagCompound().getCompoundTag("BlockEntityTag").getTagList("Patterns", 10);
            for (int i = 0; i < patterns.tagCount() && i < MAX_PATTERNS; i++) {
                NBTTagCompound entry = patterns.getCompoundTagAt(i);
                Pattern pattern = byCode(entry.getString("Pattern"));
                if (pattern != null) {
                    lines.add(StatCollector.translateToLocal("item.banner." + pattern.name + "." + COLOR_KEYS[15 - (entry.getInteger("Color") & 15)]));
                }
            }
        }

        @Override
        public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
            if (side == 0 || !world.getBlock(x, y, z).getMaterial().isSolid()) {
                return false;
            }
            int[][] offsets = {{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};
            int px = x + offsets[side][0];
            int py = y + offsets[side][1];
            int pz = z + offsets[side][2];
            if (!player.canPlayerEdit(px, py, pz, side, stack) || !world.getBlock(px, py, pz).isReplaceable(world, px, py, pz)
                    || py >= 255 || (side == 1 && !world.isAirBlock(px, py + 1, pz) && !world.getBlock(px, py + 1, pz).isReplaceable(world, px, py + 1, pz))) {
                return false;
            }
            if (world.isRemote) {
                return true;
            }
            if (side == 1) {
                int rotation = MathHelper.floor_double((double) ((player.rotationYaw + 180.0F) * 16.0F / 360.0F) + 0.5D) & 15;
                world.setBlock(px, py, pz, standing, rotation, 3);
            } else {
                world.setBlock(px, py, pz, wall, side, 3);
            }
            TileEntity tile = world.getTileEntity(px, py, pz);
            if (tile instanceof BannerTile) {
                ((BannerTile) tile).setFrom(stack);
                world.markBlockForUpdate(px, py, pz);
            }
            stack.stackSize--;
            return true;
        }
    }

    public static final class BoatItem extends Item {
        private final int wood;

        BoatItem(int wood) {
            this.wood = wood;
        }

        @Override
        public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
            MovingObjectPosition hit = getMovingObjectPositionFromPlayer(world, player, true);
            if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
                return stack;
            }
            int x = hit.blockX;
            int y = hit.blockY;
            int z = hit.blockZ;
            if (world.getBlock(x, y, z) == Blocks.snow_layer) {
                y--;
            }
            EntityBoat112 boat = new EntityBoat112(world, (double) x + 0.5D, (double) y + 1.0D, (double) z + 0.5D, wood);
            boat.rotationYaw = (float) (((MathHelper.floor_double((double) (player.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3) - 1) * 90);
            if (!world.getCollidingBoundingBoxes(boat, boat.boundingBox.expand(-0.1D, -0.1D, -0.1D)).isEmpty()) {
                return stack;
            }
            if (!world.isRemote) {
                world.spawnEntityInWorld(boat);
            }
            if (!player.capabilities.isCreativeMode) {
                stack.stackSize--;
            }
            return stack;
        }
    }

    /** EntityBoat with a wood type (1 spruce .. 5 dark oak): its own item and planks when it breaks. */
    public static final class EntityBoat112 extends EntityBoat {
        private static final int WATCH_WOOD = 20;

        public EntityBoat112(World world) {
            super(world);
        }

        EntityBoat112(World world, double x, double y, double z, int wood) {
            super(world, x, y, z);
            dataWatcher.updateObject(WATCH_WOOD, Integer.valueOf(wood));
        }

        @Override
        protected void entityInit() {
            super.entityInit();
            dataWatcher.addObject(WATCH_WOOD, Integer.valueOf(1));
        }

        public int wood() {
            return dataWatcher.getWatchableObjectInt(WATCH_WOOD);
        }

        @Override
        protected void writeEntityToNBT(NBTTagCompound tag) {
            super.writeEntityToNBT(tag);
            tag.setInteger("Wood", wood());
        }

        @Override
        protected void readEntityFromNBT(NBTTagCompound tag) {
            super.readEntityFromNBT(tag);
            dataWatcher.updateObject(WATCH_WOOD, Integer.valueOf(Math.max(1, Math.min(5, tag.getInteger("Wood")))));
        }

        @Override
        public EntityItem func_145778_a(Item item, int count, float offset) {
            if (item == Items.boat) {
                return entityDropItem(new ItemStack(BOAT_ITEMS[wood() - 1], count), offset);
            }
            if (item == Item.getItemFromBlock(Blocks.planks)) {
                return entityDropItem(new ItemStack(Blocks.planks, count, wood()), offset);
            }
            return super.func_145778_a(item, count, offset);
        }
    }
}
