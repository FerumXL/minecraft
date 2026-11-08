package ru.codex.offhand1710;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBreakable;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockIce;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackOnCollide;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.Facing;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.EnderTeleportEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.player.PlayerDropsEvent;
import net.minecraftforge.event.entity.player.PlayerPickupXpEvent;

/** Slime block, barrier, observer, frosted ice, end crystal, the enchantments of 1.9-1.11, endermite, /fill and /clone. */
public final class Content119 {
    public static Block slime;
    public static Block barrier;
    public static Block observer;
    public static Block frostedIce;
    public static Item endCrystal;
    public static Enchantment depthStrider;
    public static Enchantment frostWalker;
    public static Enchantment bindingCurse;
    public static Enchantment sweeping;
    public static Enchantment mending;
    public static Enchantment vanishingCurse;

    private Content119() {
    }

    private static void block(Block block, String name) {
        block.setBlockName(MC112Bridge.MOD_ID + "." + name).setBlockTextureName(MC112Bridge.MOD_ID + ":" + name);
        GameRegistry.registerBlock(block, name);
    }

    public static void register(Object mod) {
        slime = new BlockSlime();
        block(slime, "slime");
        GameRegistry.addShapedRecipe(new ItemStack(slime), "###", "###", "###", '#', Items.slime_ball);
        GameRegistry.addShapelessRecipe(new ItemStack(Items.slime_ball, 9), slime);
        barrier = new BlockBarrier();
        block(barrier, "barrier");
        observer = new BlockObserver();
        block(observer, "observer");
        GameRegistry.registerTileEntity(ObserverTile.class, "mc112bridge.observer");
        GameRegistry.addShapedRecipe(new ItemStack(observer), "###", "RRQ", "###", '#', Blocks.cobblestone, 'R', Items.redstone, 'Q', Items.quartz);
        frostedIce = new BlockFrostedIce();
        block(frostedIce, "frosted_ice");

        endCrystal = new ItemEndCrystal().setUnlocalizedName(MC112Bridge.MOD_ID + ".end_crystal").setTextureName(MC112Bridge.MOD_ID + ":end_crystal").setCreativeTab(CreativeTabs.tabDecorations);
        GameRegistry.registerItem(endCrystal, "end_crystal");
        GameRegistry.addShapedRecipe(new ItemStack(endCrystal), "###", "#E#", "#T#", '#', Blocks.glass, 'E', Items.ender_eye, 'T', Items.ghast_tear);

        depthStrider = new Ench(8, 2, EnumEnchantmentType.armor_feet, "waterWalker", 10, 10, 15, 3, false);
        frostWalker = new Ench(9, 2, EnumEnchantmentType.armor_feet, "frostWalker", 10, 10, 15, 2, true);
        bindingCurse = new Ench(10, 1, EnumEnchantmentType.armor, "binding_curse", 25, 0, 25, 1, true);
        sweeping = new Ench(22, 2, EnumEnchantmentType.weapon, "sweeping", 5, 9, 15, 3, false);
        mending = new Ench(70, 2, EnumEnchantmentType.breakable, "mending", 25, 25, 50, 1, true);
        vanishingCurse = new Ench(71, 1, EnumEnchantmentType.all, "vanishing_curse", 25, 0, 25, 1, true);

        EntityRegistry.registerGlobalEntityID(EntityEndermite.class, "Endermite", EntityRegistry.findGlobalUniqueEntityId(), 1447446, 7237230);
        EntityRegistry.registerModEntity(EntityEndermite.class, "Endermite", 61, mod, 80, 3, true);
        MinecraftForge.EVENT_BUS.register(new Events());
    }

    /** True for an entity wearing Frost Walker boots, which also makes magma blocks safe. */
    public static boolean hasFrostWalker(Entity entity) {
        return entity instanceof EntityLivingBase && level(frostWalker, ((EntityLivingBase) entity).getEquipmentInSlot(1)) > 0;
    }

    /** EnchantmentSweepingEdge.getSweepingDamageRatio. */
    public static float sweepRatio(ItemStack weapon) {
        int level = level(sweeping, weapon);
        return level <= 0 ? 0.0F : 1.0F - 1.0F / (float) (level + 1);
    }

    public static boolean isTreasure(Enchantment enchantment) {
        return enchantment instanceof Ench && ((Ench) enchantment).treasure;
    }

    public static boolean isBound(ItemStack stack) {
        return level(bindingCurse, stack) > 0;
    }

    private static int level(Enchantment enchantment, ItemStack stack) {
        return stack == null ? 0 : EnchantmentHelper.getEnchantmentLevel(enchantment.effectId, stack);
    }

    private static final class Ench extends Enchantment {
        private final int min;
        private final int step;
        private final int span;
        private final int max;
        private final boolean treasure;

        Ench(int id, int weight, EnumEnchantmentType type, String name, int min, int step, int span, int max, boolean treasure) {
            super(id, weight, type);
            setName(name);
            this.min = min;
            this.step = step;
            this.span = span;
            this.max = max;
            this.treasure = treasure;
        }

        @Override
        public int getMaxLevel() {
            return max;
        }

        @Override
        public int getMinEnchantability(int level) {
            return min + (level - 1) * step;
        }

        @Override
        public int getMaxEnchantability(int level) {
            return getMinEnchantability(level) + span;
        }

        /** Treasure enchantments come only from books found in the world. */
        @Override
        public boolean canApplyAtEnchantingTable(ItemStack stack) {
            return !treasure && super.canApplyAtEnchantingTable(stack);
        }

        @Override
        public boolean canApplyTogether(Enchantment other) {
            if ((this == frostWalker && other == depthStrider) || (this == depthStrider && other == frostWalker)) {
                return false;
            }
            return super.canApplyTogether(other);
        }
    }

    public static final class Events {
        @SubscribeEvent
        public void onLiving(LivingUpdateEvent event) {
            EntityLivingBase entity = event.entityLiving;
            ItemStack boots = entity.getEquipmentInSlot(1);
            if (boots == null) {
                return;
            }
            int frost = level(frostWalker, boots);
            if (frost > 0 && !entity.worldObj.isRemote && entity.onGround) {
                freeze(entity, frost);
            }
            int strider = Math.min(3, level(depthStrider, boots));
            if (strider > 0 && entity.isInWater()) {
                // EntityLivingBase.travel: the drag of water moves from 0.8 towards 0.546 of the air value.
                float power = (entity.onGround ? (float) strider : (float) strider * 0.5F) / 3.0F;
                double factor = (0.8F + (0.54600006F - 0.8F) * power) / 0.8F;
                entity.motionX *= 1.0D + (1.0D / factor - 1.0D) * 0.5D;
                entity.motionZ *= 1.0D + (1.0D / factor - 1.0D) * 0.5D;
            }
        }

        /** EnchantmentFrostWalker.freezeNearby. */
        private void freeze(EntityLivingBase entity, int level) {
            World world = entity.worldObj;
            int radius = Math.min(16, 2 + level);
            int ex = MathHelper.floor_double(entity.posX);
            int ey = MathHelper.floor_double(entity.boundingBox.minY) - 1;
            int ez = MathHelper.floor_double(entity.posZ);
            for (int x = ex - radius; x <= ex + radius; x++) {
                for (int z = ez - radius; z <= ez + radius; z++) {
                    double dx = (double) x + 0.5D - entity.posX;
                    double dz = (double) z + 0.5D - entity.posZ;
                    if (dx * dx + dz * dz <= (double) (radius * radius) && world.isAirBlock(x, ey + 1, z)
                            && world.getBlock(x, ey, z) == Blocks.water && world.getBlockMetadata(x, ey, z) == 0) {
                        world.setBlock(x, ey, z, frostedIce, 0, 3);
                        world.scheduleBlockUpdate(x, ey, z, frostedIce, 60 + world.rand.nextInt(61));
                    }
                }
            }
        }

        /** EntityXPOrb.onCollideWithPlayer: a random damaged Mending item takes the experience first. */
        @SubscribeEvent
        public void onXp(PlayerPickupXpEvent event) {
            EntityPlayer player = event.entityPlayer;
            List<ItemStack> damaged = new ArrayList<ItemStack>();
            add(damaged, player.getCurrentEquippedItem());
            add(damaged, OffhandInventory.get(player).getStack());
            for (ItemStack armor : player.inventory.armorInventory) {
                add(damaged, armor);
            }
            if (damaged.isEmpty()) {
                return;
            }
            ItemStack stack = damaged.get(player.worldObj.rand.nextInt(damaged.size()));
            int repair = Math.min(event.orb.xpValue * 2, stack.getItemDamage());
            event.orb.xpValue -= repair / 2;
            stack.setItemDamage(stack.getItemDamage() - repair);
        }

        private void add(List<ItemStack> list, ItemStack stack) {
            if (stack != null && stack.isItemDamaged() && level(mending, stack) > 0) {
                list.add(stack);
            }
        }

        /** EntityPlayer.destroyVanishingCursedItems. */
        @SubscribeEvent
        public void onDrops(PlayerDropsEvent event) {
            for (Iterator<EntityItem> it = event.drops.iterator(); it.hasNext();) {
                if (level(vanishingCurse, it.next().getEntityItem()) > 0) {
                    it.remove();
                }
            }
        }

        /** EntityEnderPearl.onImpact: one pearl in twenty leaves an endermite behind. */
        @SubscribeEvent
        public void onPearl(EnderTeleportEvent event) {
            EntityLivingBase entity = event.entityLiving;
            if (entity instanceof EntityPlayer && !entity.worldObj.isRemote && entity.worldObj.rand.nextFloat() < 0.05F
                    && entity.worldObj.getGameRules().getGameRuleBooleanValue("doMobSpawning")) {
                EntityEndermite mite = new EntityEndermite(entity.worldObj);
                mite.setLocationAndAngles(entity.posX, entity.posY, entity.posZ, entity.rotationYaw, entity.rotationPitch);
                entity.worldObj.spawnEntityInWorld(mite);
            }
        }
    }

    /** SoundType.SLIME of 1.9. */
    private static final class SlimeSound extends Block.SoundType {
        SlimeSound() {
            super("mob.slime", 1.0F, 1.0F);
        }

        @Override
        public String getBreakSound() {
            return MC112Bridge.MOD_ID + ":block.slime.break";
        }

        @Override
        public String getStepResourcePath() {
            return MC112Bridge.MOD_ID + ":block.slime.step";
        }

        @Override
        public String func_150496_b() {
            return MC112Bridge.MOD_ID + ":block.slime.place";
        }
    }

    public static final class BlockSlime extends BlockBreakable {
        BlockSlime() {
            super(MC112Bridge.MOD_ID + ":slime", Material.clay, false);
            setCreativeTab(CreativeTabs.tabDecorations);
            setStepSound(new SlimeSound());
            slipperiness = 0.8F;
        }

        @Override
        public int getRenderBlockPass() {
            return 1;
        }

        /** No fall damage unless sneaking; the bounce itself is applied in {@link Items112.Bounce}. */
        @Override
        public void onFallenUpon(World world, int x, int y, int z, Entity entity, float distance) {
            if (!entity.isSneaking()) {
                entity.fallDistance = 0.0F;
            }
        }

        @Override
        public void onEntityWalking(World world, int x, int y, int z, Entity entity) {
            if (Math.abs(entity.motionY) < 0.1D && !entity.isSneaking()) {
                double slow = 0.4D + Math.abs(entity.motionY) * 0.2D;
                entity.motionX *= slow;
                entity.motionZ *= slow;
            }
        }
    }

    public static final class BlockBarrier extends Block {
        BlockBarrier() {
            super(Material.rock);
            setBlockUnbreakable();
            setResistance(6000001.0F);
            setCreativeTab(CreativeTabs.tabRedstone);
            disableStats();
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
        public void dropBlockAsItemWithChance(World world, int x, int y, int z, int meta, float chance, int fortune) {
        }
    }

    public static final class ObserverTile extends TileEntity {
        int watched = -1;

        @Override
        public boolean canUpdate() {
            return false;
        }

        @Override
        public void writeToNBT(NBTTagCompound tag) {
            super.writeToNBT(tag);
            tag.setInteger("Watched", watched);
        }

        @Override
        public void readFromNBT(NBTTagCompound tag) {
            super.readFromNBT(tag);
            watched = tag.getInteger("Watched");
        }
    }

    /** BlockObserver of 1.11. Metadata: bit 8 = powered, low bits = facing xor 3, so that the item shows the face. */
    public static final class BlockObserver extends BlockContainer {
        private IIcon front;
        private IIcon back;
        private IIcon backLit;
        private IIcon side;
        private IIcon top;

        BlockObserver() {
            super(Material.rock);
            setHardness(3.0F);
            setCreativeTab(CreativeTabs.tabRedstone);
            setStepSound(Block.soundTypeStone);
        }

        @Override
        public TileEntity createNewTileEntity(World world, int meta) {
            return new ObserverTile();
        }

        @Override
        public int getRenderType() {
            return 0;
        }

        private static int facing(int meta) {
            return (meta & 7) ^ 3;
        }

        @Override
        public void registerBlockIcons(IIconRegister register) {
            String p = MC112Bridge.MOD_ID + ":observer_";
            front = register.registerIcon(p + "front");
            back = register.registerIcon(p + "back");
            backLit = register.registerIcon(p + "back_lit");
            side = register.registerIcon(p + "side");
            top = register.registerIcon(p + "top");
        }

        @Override
        public IIcon getIcon(int face, int meta) {
            int f = facing(meta);
            if (face == f) {
                return front;
            }
            if (face == Facing.oppositeSide[f]) {
                return (meta & 8) != 0 ? backLit : back;
            }
            if (f < 2) {
                return face == 2 || face == 3 ? top : side;
            }
            return face < 2 ? top : side;
        }

        private int state(World world, int x, int y, int z, int f) {
            int fx = x + Facing.offsetsXForSide[f];
            int fy = y + Facing.offsetsYForSide[f];
            int fz = z + Facing.offsetsZForSide[f];
            return Block.getIdFromBlock(world.getBlock(fx, fy, fz)) << 4 | world.getBlockMetadata(fx, fy, fz);
        }

        @Override
        public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
            int f = Facing.oppositeSide[BlockPistonBase.determineOrientation(world, x, y, z, placer)];
            world.setBlockMetadataWithNotify(x, y, z, f ^ 3, 2);
            TileEntity tile = world.getTileEntity(x, y, z);
            if (tile instanceof ObserverTile) {
                ((ObserverTile) tile).watched = state(world, x, y, z, f);
            }
        }

        @Override
        public void onNeighborBlockChange(World world, int x, int y, int z, Block block) {
            int meta = world.getBlockMetadata(x, y, z);
            TileEntity tile = world.getTileEntity(x, y, z);
            if (!world.isRemote && (meta & 8) == 0 && tile instanceof ObserverTile && ((ObserverTile) tile).watched != state(world, x, y, z, facing(meta))) {
                world.scheduleBlockUpdate(x, y, z, this, 2);
            }
        }

        @Override
        public void updateTick(World world, int x, int y, int z, Random random) {
            int meta = world.getBlockMetadata(x, y, z);
            int f = facing(meta);
            TileEntity tile = world.getTileEntity(x, y, z);
            if (tile instanceof ObserverTile) {
                ((ObserverTile) tile).watched = state(world, x, y, z, f);
            }
            if ((meta & 8) != 0) {
                world.setBlockMetadataWithNotify(x, y, z, meta & 7, 2);
            } else {
                world.setBlockMetadataWithNotify(x, y, z, meta | 8, 2);
                world.scheduleBlockUpdate(x, y, z, this, 2);
            }
            notifyBack(world, x, y, z, f);
        }

        private void notifyBack(World world, int x, int y, int z, int f) {
            int b = Facing.oppositeSide[f];
            int bx = x + Facing.offsetsXForSide[b];
            int by = y + Facing.offsetsYForSide[b];
            int bz = z + Facing.offsetsZForSide[b];
            world.notifyBlockOfNeighborChange(bx, by, bz, this);
            world.notifyBlocksOfNeighborChange(bx, by, bz, this, f);
        }

        @Override
        public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
            super.breakBlock(world, x, y, z, block, meta);
            if ((meta & 8) != 0) {
                notifyBack(world, x, y, z, facing(meta));
            }
        }

        @Override
        public boolean canProvidePower() {
            return true;
        }

        @Override
        public int isProvidingWeakPower(IBlockAccess world, int x, int y, int z, int side) {
            int meta = world.getBlockMetadata(x, y, z);
            return (meta & 8) != 0 && facing(meta) == side ? 15 : 0;
        }

        @Override
        public int isProvidingStrongPower(IBlockAccess world, int x, int y, int z, int side) {
            return isProvidingWeakPower(world, x, y, z, side);
        }

        @Override
        public int damageDropped(int meta) {
            return 0;
        }
    }

    /** BlockFrostedIce: metadata is the age, melts faster at the edges. */
    public static final class BlockFrostedIce extends BlockIce {
        private IIcon[] ages;

        BlockFrostedIce() {
            setHardness(0.5F);
            setLightOpacity(3);
            setStepSound(Block.soundTypeGlass);
            setCreativeTab(null);
        }

        @Override
        public void registerBlockIcons(IIconRegister register) {
            ages = new IIcon[4];
            for (int i = 0; i < 4; i++) {
                ages[i] = register.registerIcon(MC112Bridge.MOD_ID + ":frosted_ice_" + i);
            }
        }

        @Override
        public IIcon getIcon(int side, int meta) {
            return ages[Math.min(meta, 3)];
        }

        private int neighbours(World world, int x, int y, int z) {
            int count = 0;
            for (int f = 0; f < 6; f++) {
                if (world.getBlock(x + Facing.offsetsXForSide[f], y + Facing.offsetsYForSide[f], z + Facing.offsetsZForSide[f]) == this) {
                    count++;
                }
            }
            return count;
        }

        @Override
        public void updateTick(World world, int x, int y, int z, Random random) {
            int age = world.getBlockMetadata(x, y, z);
            if ((random.nextInt(3) == 0 || neighbours(world, x, y, z) < 4) && world.getBlockLightValue(x, y, z) > 11 - age - getLightOpacity()) {
                slightlyMelt(world, x, y, z, random, true);
            } else {
                world.scheduleBlockUpdate(x, y, z, this, 20 + random.nextInt(21));
            }
        }

        private void slightlyMelt(World world, int x, int y, int z, Random random, boolean spread) {
            int age = world.getBlockMetadata(x, y, z);
            if (age < 3) {
                world.setBlockMetadataWithNotify(x, y, z, age + 1, 2);
                world.scheduleBlockUpdate(x, y, z, this, 20 + random.nextInt(21));
                return;
            }
            world.setBlock(x, y, z, Blocks.water, 0, 3);
            if (spread) {
                for (int f = 0; f < 6; f++) {
                    int a = x + Facing.offsetsXForSide[f];
                    int b = y + Facing.offsetsYForSide[f];
                    int c = z + Facing.offsetsZForSide[f];
                    if (world.getBlock(a, b, c) == this) {
                        slightlyMelt(world, a, b, c, random, false);
                    }
                }
            }
        }

        @Override
        public void onNeighborBlockChange(World world, int x, int y, int z, Block block) {
            if (block == this && neighbours(world, x, y, z) < 2) {
                world.setBlock(x, y, z, Blocks.water, 0, 3);
            }
        }

        @Override
        public int quantityDropped(Random random) {
            return 0;
        }

        @Override
        public void harvestBlock(World world, EntityPlayer player, int x, int y, int z, int meta) {
            world.setBlock(x, y, z, Blocks.water, 0, 3);
        }
    }

    public static final class ItemEndCrystal extends Item {
        @Override
        public boolean hasEffect(ItemStack stack, int pass) {
            return true;
        }

        @Override
        public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
            Block base = world.getBlock(x, y, z);
            if ((base != Blocks.obsidian && base != Blocks.bedrock) || !player.canPlayerEdit(x, y + 1, z, side, stack)) {
                return false;
            }
            if (!world.isAirBlock(x, y + 1, z) || !world.isAirBlock(x, y + 2, z)
                    || !world.getEntitiesWithinAABBExcludingEntity(null, AxisAlignedBB.getBoundingBox((double) x, (double) y + 1.0D, (double) z, (double) x + 1.0D, (double) y + 3.0D, (double) z + 1.0D)).isEmpty()) {
                return false;
            }
            if (!world.isRemote) {
                End112.Crystal crystal = new End112.Crystal(world);
                crystal.setPosition((double) x + 0.5D, (double) y + 1.0D, (double) z + 0.5D);
                world.spawnEntityInWorld(crystal);
            }
            if (!player.capabilities.isCreativeMode) {
                stack.stackSize--;
            }
            return true;
        }
    }

    public static class EntityEndermite extends EntityMob {
        private int lifetime;

        public EntityEndermite(World world) {
            super(world);
            experienceValue = 3;
            setSize(0.4F, 0.3F);
            tasks.addTask(1, new EntityAISwimming(this));
            tasks.addTask(2, new EntityAIAttackOnCollide(this, EntityPlayer.class, 1.0D, false));
            tasks.addTask(3, new EntityAIWander(this, 1.0D));
            tasks.addTask(7, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
            tasks.addTask(8, new EntityAILookIdle(this));
            targetTasks.addTask(1, new EntityAIHurtByTarget(this, true));
            targetTasks.addTask(2, new EntityAINearestAttackableTarget(this, EntityPlayer.class, 0, true));
        }

        @Override
        protected boolean isAIEnabled() {
            return true;
        }

        @Override
        protected void applyEntityAttributes() {
            super.applyEntityAttributes();
            getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(8.0D);
            getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.25D);
            getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(2.0D);
        }

        @Override
        protected boolean canTriggerWalking() {
            return false;
        }

        @Override
        protected String getLivingSound() {
            return MC112Bridge.MOD_ID + ":entity.endermite.ambient";
        }

        @Override
        protected String getHurtSound() {
            return MC112Bridge.MOD_ID + ":entity.endermite.hurt";
        }

        @Override
        protected String getDeathSound() {
            return MC112Bridge.MOD_ID + ":entity.endermite.death";
        }

        @Override
        protected void func_145780_a(int x, int y, int z, Block block) {
            playSound(MC112Bridge.MOD_ID + ":entity.endermite.step", 0.15F, 1.0F);
        }

        @Override
        public EnumCreatureAttribute getCreatureAttribute() {
            return EnumCreatureAttribute.ARTHROPOD;
        }

        @Override
        public void writeEntityToNBT(NBTTagCompound tag) {
            super.writeEntityToNBT(tag);
            tag.setInteger("Lifetime", lifetime);
        }

        @Override
        public void readEntityFromNBT(NBTTagCompound tag) {
            super.readEntityFromNBT(tag);
            lifetime = tag.getInteger("Lifetime");
        }

        @Override
        public void onLivingUpdate() {
            super.onLivingUpdate();
            if (worldObj.isRemote) {
                for (int i = 0; i < 2; i++) {
                    worldObj.spawnParticle("portal", posX + (rand.nextDouble() - 0.5D) * (double) width, posY + rand.nextDouble() * (double) height,
                            posZ + (rand.nextDouble() - 0.5D) * (double) width, (rand.nextDouble() - 0.5D) * 2.0D, -rand.nextDouble(), (rand.nextDouble() - 0.5D) * 2.0D);
                }
            } else if (!hasCustomNameTag() && ++lifetime >= 2400) {
                setDead();
            }
        }
    }

    public static final class CommandFill extends CommandBase {
        @Override
        public String getCommandName() {
            return "fill";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 2;
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "/fill <x1> <y1> <z1> <x2> <y2> <z2> <block> [meta] [replace|destroy|keep|hollow|outline]";
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length < 7) {
                throw new WrongUsageException(getCommandUsage(sender));
            }
            int[] c = coords(sender, args, 0);
            int[] d = coords(sender, args, 3);
            Block block = getBlockByText(sender, args[6]);
            int meta = args.length > 7 ? parseIntBounded(sender, args[7], 0, 15) : 0;
            String mode = args.length > 8 ? args[8] : "replace";
            int x0 = Math.min(c[0], d[0]);
            int x1 = Math.max(c[0], d[0]);
            int y0 = Math.max(0, Math.min(c[1], d[1]));
            int y1 = Math.min(255, Math.max(c[1], d[1]));
            int z0 = Math.min(c[2], d[2]);
            int z1 = Math.max(c[2], d[2]);
            long volume = (long) (x1 - x0 + 1) * (long) (y1 - y0 + 1) * (long) (z1 - z0 + 1);
            if (volume > 32768L) {
                throw new WrongUsageException("Too many blocks in the specified area (" + volume + " > 32768)");
            }
            World world = sender.getEntityWorld();
            int count = 0;
            for (int x = x0; x <= x1; x++) {
                for (int y = y0; y <= y1; y++) {
                    for (int z = z0; z <= z1; z++) {
                        boolean edge = x == x0 || x == x1 || y == y0 || y == y1 || z == z0 || z == z1;
                        Block put = block;
                        int putMeta = meta;
                        if ("outline".equals(mode) && !edge) {
                            continue;
                        }
                        if ("hollow".equals(mode) && !edge) {
                            put = Blocks.air;
                            putMeta = 0;
                        }
                        if ("keep".equals(mode) && !world.isAirBlock(x, y, z)) {
                            continue;
                        }
                        if ("destroy".equals(mode)) {
                            world.func_147480_a(x, y, z, true);
                        }
                        if (world.setBlock(x, y, z, put, putMeta, 2)) {
                            count++;
                        }
                    }
                }
            }
            world.markBlockRangeForRenderUpdate(x0, y0, z0, x1, y1, z1);
            sender.addChatMessage(new ChatComponentText(count + " blocks filled"));
        }
    }

    public static final class CommandClone extends CommandBase {
        @Override
        public String getCommandName() {
            return "clone";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 2;
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "/clone <x1> <y1> <z1> <x2> <y2> <z2> <x> <y> <z> [replace|masked]";
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length < 9) {
                throw new WrongUsageException(getCommandUsage(sender));
            }
            int[] c = coords(sender, args, 0);
            int[] d = coords(sender, args, 3);
            int[] to = coords(sender, args, 6);
            boolean masked = args.length > 9 && "masked".equals(args[9]);
            int x0 = Math.min(c[0], d[0]);
            int y0 = Math.min(c[1], d[1]);
            int z0 = Math.min(c[2], d[2]);
            int sx = Math.abs(c[0] - d[0]) + 1;
            int sy = Math.abs(c[1] - d[1]) + 1;
            int sz = Math.abs(c[2] - d[2]) + 1;
            if ((long) sx * (long) sy * (long) sz > 32768L) {
                throw new WrongUsageException("Too many blocks in the specified area");
            }
            World world = sender.getEntityWorld();
            Block[] blocks = new Block[sx * sy * sz];
            int[] metas = new int[blocks.length];
            NBTTagCompound[] tiles = new NBTTagCompound[blocks.length];
            for (int x = 0; x < sx; x++) {
                for (int y = 0; y < sy; y++) {
                    for (int z = 0; z < sz; z++) {
                        int i = (x * sy + y) * sz + z;
                        blocks[i] = world.getBlock(x0 + x, y0 + y, z0 + z);
                        metas[i] = world.getBlockMetadata(x0 + x, y0 + y, z0 + z);
                        TileEntity tile = world.getTileEntity(x0 + x, y0 + y, z0 + z);
                        if (tile != null) {
                            tiles[i] = new NBTTagCompound();
                            tile.writeToNBT(tiles[i]);
                        }
                    }
                }
            }
            int count = 0;
            for (int pass = 0; pass < 2; pass++) {
                for (int x = 0; x < sx; x++) {
                    for (int y = 0; y < sy; y++) {
                        for (int z = 0; z < sz; z++) {
                            int i = (x * sy + y) * sz + z;
                            if ((blocks[i].isOpaqueCube() ? 0 : 1) != pass || (masked && blocks[i] == Blocks.air)) {
                                continue;
                            }
                            int px = to[0] + x;
                            int py = to[1] + y;
                            int pz = to[2] + z;
                            if (world.setBlock(px, py, pz, blocks[i], metas[i], 2)) {
                                count++;
                            }
                            if (tiles[i] != null) {
                                tiles[i].setInteger("x", px);
                                tiles[i].setInteger("y", py);
                                tiles[i].setInteger("z", pz);
                                TileEntity tile = world.getTileEntity(px, py, pz);
                                if (tile != null) {
                                    tile.readFromNBT(tiles[i]);
                                    tile.markDirty();
                                }
                            }
                        }
                    }
                }
            }
            sender.addChatMessage(new ChatComponentText(count + " blocks cloned"));
        }
    }

    private static int[] coords(ICommandSender sender, String[] args, int from) {
        ChunkCoordinates base = sender.getPlayerCoordinates();
        return new int[] {
                MathHelper.floor_double(CommandBase.func_110666_a(sender, (double) base.posX, args[from])),
                MathHelper.floor_double(CommandBase.func_110666_a(sender, (double) base.posY, args[from + 1])),
                MathHelper.floor_double(CommandBase.func_110666_a(sender, (double) base.posZ, args[from + 2]))
        };
    }
}
