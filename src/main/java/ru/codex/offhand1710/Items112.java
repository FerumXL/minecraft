package ru.codex.offhand1710;

import java.util.ArrayList;
import java.util.Random;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.IGuiHandler;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockCrops;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemSeeds;
import net.minecraft.item.ItemSoup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.Direction;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.ChestGenHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingFallEvent;

/** Beetroot, shulker boxes, armor stands and coloured beds of 1.9-1.12.2. */
public final class Items112 {
    public static BlockBeetroot beetroots;
    public static Item beetroot;
    public static Item beetrootSeeds;
    public static Item beetrootSoup;
    public static Item shulkerShell;
    public static Item armorStand;
    public static final BlockShulkerBox[] BOXES = new BlockShulkerBox[16];
    public static final Block[] BEDS = new Block[16];
    public static final Item[] BED_ITEMS = new Item[16];
    public static final int RED = 14;
    public static final int GUI_BOX = 0;

    private Items112() {
    }

    /** The texture names of 1.12.2 call light gray "silver". */
    public static String textureColor(int i) {
        return "light_gray".equals(MC112Bridge.COLOR_NAMES[i]) ? "silver" : MC112Bridge.COLOR_NAMES[i];
    }

    private static Item item(Item item, String name, CreativeTabs tab) {
        item.setUnlocalizedName(MC112Bridge.MOD_ID + "." + name).setTextureName(MC112Bridge.MOD_ID + ":" + name).setCreativeTab(tab);
        GameRegistry.registerItem(item, name);
        return item;
    }

    public static void register(Object mod, IGuiHandler gui) {
        beetroots = new BlockBeetroot();
        GameRegistry.registerBlock(beetroots, (Class<? extends net.minecraft.item.ItemBlock>) null, "beetroots");
        beetroot = item(new ItemFood(1, 0.6F, false), "beetroot", CreativeTabs.tabFood);
        beetrootSeeds = item(new ItemSeeds(beetroots, Blocks.farmland), "beetroot_seeds", CreativeTabs.tabMaterials);
        beetrootSoup = item(new ItemSoup(6), "beetroot_soup", CreativeTabs.tabFood);
        GameRegistry.addShapedRecipe(new ItemStack(beetrootSoup), "OOO", "OOO", " B ", 'O', beetroot, 'B', Items.bowl);
        GameRegistry.addShapelessRecipe(new ItemStack(Items.dye, 1, 1), beetroot);

        shulkerShell = item(new Item(), "shulker_shell", CreativeTabs.tabMaterials);
        for (int i = 0; i < 16; i++) {
            BOXES[i] = new BlockShulkerBox(i);
            GameRegistry.registerBlock(BOXES[i], BlockShulkerBox.BoxItem.class, "shulker_box_" + MC112Bridge.COLOR_NAMES[i]);
        }
        GameRegistry.registerTileEntity(BlockShulkerBox.Tile.class, "mc112bridge.shulker_box");
        // Purple is the uncoloured box of 1.12.2: shells and a chest make it, a dye recolours any box.
        GameRegistry.addShapedRecipe(new ItemStack(BOXES[10]), "-", "#", "-", '-', shulkerShell, '#', Blocks.chest);
        CraftingManager.getInstance().getRecipeList().add(new BlockShulkerBox.DyeRecipe());
        NetworkRegistry.INSTANCE.registerGuiHandler(mod, gui);

        armorStand = item(new EntityArmorStand112.StandItem(), "armor_stand", CreativeTabs.tabDecorations);
        EntityRegistry.registerModEntity(EntityArmorStand112.class, "ArmorStand", 60, mod, 80, 3, true);
        GameRegistry.addShapedRecipe(new ItemStack(armorStand), "///", " / ", "/_/", '/', Items.stick, '_', new ItemStack(Blocks.stone_slab, 1, 0));

        registerBeds();
        MinecraftForge.EVENT_BUS.register(new Bounce());
    }

    private static void registerBeds() {
        for (int i = 0; i < 16; i++) {
            if (i == RED) {
                BEDS[i] = Blocks.bed;
                BED_ITEMS[i] = Items.bed;
            } else {
                ColoredBed block = new ColoredBed(i);
                BEDS[i] = block;
                GameRegistry.registerBlock(block, (Class<? extends net.minecraft.item.ItemBlock>) null, "bed_block_" + MC112Bridge.COLOR_NAMES[i]);
                BED_ITEMS[i] = item(new BedItem(block), "bed_" + MC112Bridge.COLOR_NAMES[i], CreativeTabs.tabDecorations);
                block.item = BED_ITEMS[i];
            }
        }
        // The vanilla recipe takes any wool and gives a red bed; 1.12.2 keeps the colour of the wool.
        java.util.Iterator<?> recipes = CraftingManager.getInstance().getRecipeList().iterator();
        while (recipes.hasNext()) {
            ItemStack output = ((IRecipe) recipes.next()).getRecipeOutput();
            if (output != null && output.getItem() == Items.bed) {
                recipes.remove();
            }
        }
        for (int i = 0; i < 16; i++) {
            for (int wood = 0; wood < 6; wood++) {
                GameRegistry.addShapedRecipe(new ItemStack(BED_ITEMS[i]), "###", "XXX", '#', new ItemStack(Blocks.wool, 1, i), 'X', new ItemStack(Blocks.planks, 1, wood));
            }
            if (i != 0) {
                GameRegistry.addShapelessRecipe(new ItemStack(BED_ITEMS[i]), BED_ITEMS[0], new ItemStack(Items.dye, 1, 15 - i));
            }
        }
    }

    /** BlockBeetroot: four stages instead of eight, seeds come from dungeons and End cities. */
    public static final class BlockBeetroot extends BlockCrops {
        private IIcon[] stages;

        BlockBeetroot() {
            setBlockName(MC112Bridge.MOD_ID + ".beetroots");
        }

        @Override
        protected Item func_149866_i() {
            return beetrootSeeds;
        }

        @Override
        protected Item func_149865_P() {
            return beetroot;
        }

        @Override
        public void registerBlockIcons(IIconRegister register) {
            stages = new IIcon[4];
            for (int i = 0; i < 4; i++) {
                stages[i] = register.registerIcon(MC112Bridge.MOD_ID + ":beetroots_stage_" + i);
            }
        }

        @Override
        public IIcon getIcon(int side, int meta) {
            return stages[Math.min(meta, 3)];
        }

        @Override
        public void updateTick(World world, int x, int y, int z, Random random) {
            // 1.12.2 skips a third of the growth ticks; the rate itself is that of wheat.
            if (random.nextInt(3) != 0 && world.getBlockMetadata(x, y, z) < 3) {
                super.updateTick(world, x, y, z, random);
            } else if (!canBlockStay(world, x, y, z)) {
                dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z), 0);
                world.setBlockToAir(x, y, z);
            }
        }

        @Override
        public boolean func_149851_a(World world, int x, int y, int z, boolean client) {
            return world.getBlockMetadata(x, y, z) < 3;
        }

        @Override
        public void func_149863_m(World world, int x, int y, int z) {
            world.setBlockMetadataWithNotify(x, y, z, Math.min(3, world.getBlockMetadata(x, y, z) + 1), 2);
        }

        @Override
        public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int meta, int fortune) {
            ArrayList<ItemStack> drops = new ArrayList<ItemStack>();
            drops.add(new ItemStack(meta >= 3 ? beetroot : beetrootSeeds));
            if (meta >= 3) {
                for (int i = 0; i < 3 + fortune; i++) {
                    if (world.rand.nextInt(6) <= 3) {
                        drops.add(new ItemStack(beetrootSeeds));
                    }
                }
            }
            return drops;
        }
    }

    public static final class ColoredBed extends BlockBed {
        private final int color;
        Item item;
        private IIcon[] end;
        private IIcon[] top;
        private IIcon[] side;

        ColoredBed(int color) {
            this.color = color;
            setHardness(0.2F);
            setStepSound(soundTypeWood);
            setBlockName(MC112Bridge.MOD_ID + ".bed_" + MC112Bridge.COLOR_NAMES[color]);
        }

        @Override
        public boolean isBed(IBlockAccess world, int x, int y, int z, net.minecraft.entity.EntityLivingBase player) {
            return true;
        }

        @Override
        public void registerBlockIcons(IIconRegister register) {
            String name = MC112Bridge.MOD_ID + ":bed/" + textureColor(color);
            top = new IIcon[] {register.registerIcon(name + "_feet_top"), register.registerIcon(name + "_head_top")};
            end = new IIcon[] {register.registerIcon(name + "_feet_end"), register.registerIcon(name + "_head_end")};
            side = new IIcon[] {register.registerIcon(name + "_feet_side"), register.registerIcon(name + "_head_side")};
        }

        @Override
        public IIcon getIcon(int face, int meta) {
            if (face == 0) {
                return Blocks.planks.getIcon(face, 0);
            }
            int shape = Direction.bedDirection[BlockBed.getDirection(meta)][face];
            int head = BlockBed.isBlockHeadOfBed(meta) ? 1 : 0;
            if ((head == 1 && shape == 2) || (head == 0 && shape == 3)) {
                return end[head];
            }
            return shape != 5 && shape != 4 ? top[head] : side[head];
        }

        @Override
        public Item getItemDropped(int meta, Random random, int fortune) {
            return BlockBed.isBlockHeadOfBed(meta) ? null : item;
        }

        @Override
        public Item getItem(World world, int x, int y, int z) {
            return item;
        }
    }

    /** ItemBed of 1.7.10 for a bed block of any colour. */
    public static final class BedItem extends Item {
        private final Block bed;

        BedItem(Block bed) {
            this.bed = bed;
            setMaxStackSize(1);
        }

        @Override
        public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
            if (world.isRemote) {
                return true;
            }
            if (side != 1) {
                return false;
            }
            y++;
            int facing = MathHelper.floor_double((double) (player.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
            int dx = facing == 1 ? -1 : (facing == 3 ? 1 : 0);
            int dz = facing == 0 ? 1 : (facing == 2 ? -1 : 0);
            if (player.canPlayerEdit(x, y, z, side, stack) && player.canPlayerEdit(x + dx, y, z + dz, side, stack)
                    && world.isAirBlock(x, y, z) && world.isAirBlock(x + dx, y, z + dz)
                    && World.doesBlockHaveSolidTopSurface(world, x, y - 1, z) && World.doesBlockHaveSolidTopSurface(world, x + dx, y - 1, z + dz)) {
                world.setBlock(x, y, z, bed, facing, 3);
                if (world.getBlock(x, y, z) == bed) {
                    world.setBlock(x + dx, y, z + dz, bed, facing + 8, 3);
                }
                stack.stackSize--;
                return true;
            }
            return false;
        }
    }

    /** BlockBed.onFallenUpon and onLanded of 1.12.2, for every bed including the vanilla red one. */
    public static final class Bounce {
        /** Creative players do not get a LivingFallEvent in 1.7.10. */
        @SubscribeEvent
        public void onFlyableFall(net.minecraftforge.event.entity.player.PlayerFlyableFallEvent event) {
            EntityPlayer player = event.entityPlayer;
            int x = MathHelper.floor_double(player.posX);
            int y = MathHelper.floor_double(player.boundingBox.minY - 0.2D);
            int z = MathHelper.floor_double(player.posZ);
            Block below = player.worldObj.getBlock(x, y, z);
            if (player.worldObj.isRemote && !player.isSneaking() && player.motionY < 0.0D && (below instanceof BlockBed || below == Content119.slime)) {
                OffhandInventory.get(player).bedBounce = -player.motionY * (below == Content119.slime ? 1.0D : 0.6600000262260437D);
            }
        }

        @SubscribeEvent
        public void onFall(LivingFallEvent event) {
            int x = MathHelper.floor_double(event.entity.posX);
            int y = MathHelper.floor_double(event.entity.boundingBox.minY - 0.2D);
            int z = MathHelper.floor_double(event.entity.posZ);
            if (event.entity.worldObj.getBlock(x, y, z) == Content119.slime) {
                if (event.entity instanceof EntityPlayer && event.entity.worldObj.isRemote && !event.entity.isSneaking() && event.entity.motionY < 0.0D) {
                    OffhandInventory.get((EntityPlayer) event.entity).bedBounce = -event.entity.motionY;
                }
                return;
            }
            if (!(event.entity.worldObj.getBlock(x, y, z) instanceof BlockBed)) {
                return;
            }
            event.distance *= 0.5F;
            if (event.entity instanceof EntityPlayer && event.entity.worldObj.isRemote && !event.entity.isSneaking() && event.entity.motionY < 0.0D) {
                OffhandInventory.get((EntityPlayer) event.entity).bedBounce = -event.entity.motionY * 0.6600000262260437D;
            }
        }
    }
}
