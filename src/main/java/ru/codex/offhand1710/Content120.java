package ru.codex.offhand1710;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDaylightDetector;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.EntitySilverfish;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntitySnowman;
import net.minecraft.entity.monster.EntityWitch;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.passive.EntityMooshroom;
import net.minecraft.entity.passive.EntityOcelot;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.event.entity.player.ArrowNockEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;

/** Values brought in line with 1.12.2, the effects of 1.9, wet sponge, inverted light sensor, spectral arrow, more commands. */
public final class Content120 {
    public static Block wetSponge;
    public static BlockInvertedSensor invertedSensor;
    public static Item spectralArrow;
    public static Potion glowing;
    public static Potion levitation;
    public static Potion luck;
    public static Potion unluck;
    private static final Map<Class<?>, float[]> SIZES = new HashMap<Class<?>, float[]>();

    private Content120() {
    }

    private static final class Effect extends Potion {
        Effect(int id, boolean bad, int color, String name, int iconX, int iconY) {
            super(id, bad, color);
            setPotionName(name);
            setIconIndex(iconX, iconY);
        }
    }

    public static void register(Object mod) {
        applyValues();
        glowing = new Effect(24, false, 9740385, "effect.glowing", 4, 2);
        levitation = new Effect(25, true, 13565951, "effect.levitation", 3, 2);
        luck = new Effect(26, false, 3381504, "effect.luck", 5, 2);
        unluck = new Effect(27, true, 12624973, "effect.unluck", 6, 2);

        wetSponge = new Block(Material.sponge) {
        }.setHardness(0.6F).setStepSound(Block.soundTypeGrass).setCreativeTab(CreativeTabs.tabBlock)
                .setBlockName(MC112Bridge.MOD_ID + ".wet_sponge").setBlockTextureName(MC112Bridge.MOD_ID + ":sponge_wet");
        GameRegistry.registerBlock(wetSponge, "wet_sponge");
        FurnaceRecipes.smelting().func_151393_a(wetSponge, new ItemStack(Blocks.sponge), 0.15F);

        invertedSensor = new BlockInvertedSensor();
        GameRegistry.registerBlock(invertedSensor, (Class<? extends net.minecraft.item.ItemBlock>) null, "daylight_detector_inverted");

        spectralArrow = new Item().setUnlocalizedName(MC112Bridge.MOD_ID + ".spectral_arrow").setTextureName(MC112Bridge.MOD_ID + ":spectral_arrow").setCreativeTab(CreativeTabs.tabCombat);
        GameRegistry.registerItem(spectralArrow, "spectral_arrow");
        GameRegistry.addShapedRecipe(new ItemStack(spectralArrow, 2), " # ", "#X#", " # ", '#', Items.glowstone_dust, 'X', Items.arrow);
        EntityRegistry.registerModEntity(EntitySpectralArrow.class, "SpectralArrow", 63, mod, 64, 20, false);

        CraftingManager.getInstance().getRecipeList().add(new BannerCopy());
        Events events = new Events();
        MinecraftForge.EVENT_BUS.register(events);
        FMLCommonHandler.instance().bus().register(events);
    }

    private static void size(Class<?> type, float width, float height) {
        SIZES.put(type, new float[] {width, height});
    }

    /** Numbers read from 1.12.2.jar that differ from vanilla 1.7.10. */
    private static void applyValues() {
        Blocks.bed.setStepSound(Block.soundTypeWood);
        Blocks.noteblock.setStepSound(Block.soundTypeWood);
        Blocks.hopper.setStepSound(Block.soundTypeMetal);
        ReflectionHelper.setPrivateValue(ItemFood.class, (ItemFood) Items.carrot, Integer.valueOf(3), "healAmount", "field_77853_b");
        ReflectionHelper.setPrivateValue(ItemFood.class, (ItemFood) Items.baked_potato, Integer.valueOf(5), "healAmount", "field_77853_b");
        Items.wooden_door.setMaxStackSize(64);
        Items.iron_door.setMaxStackSize(64);

        for (Iterator<?> it = CraftingManager.getInstance().getRecipeList().iterator(); it.hasNext();) {
            ItemStack output = ((IRecipe) it.next()).getRecipeOutput();
            if (output != null && (output.getItem() == Items.wooden_door || output.getItem() == Items.iron_door || output.getItem() == Item.getItemFromBlock(Blocks.fence))) {
                it.remove();
            }
        }
        ItemStack oak = new ItemStack(Blocks.planks, 1, 0);
        GameRegistry.addShapedRecipe(new ItemStack(Blocks.fence, 3), "W#W", "W#W", 'W', oak, '#', Items.stick);
        GameRegistry.addShapedRecipe(new ItemStack(Items.wooden_door, 3), "##", "##", "##", '#', oak);
        GameRegistry.addShapedRecipe(new ItemStack(Items.iron_door, 3), "##", "##", "##", '#', Items.iron_ingot);
        GameRegistry.addShapedRecipe(new ItemStack(Blocks.stonebrick, 1, 3), "#", "#", '#', new ItemStack(Blocks.stone_slab, 1, 5));
        GameRegistry.addShapedRecipe(new ItemStack(Blocks.dirt, 4, 1), "DG", "GD", 'D', new ItemStack(Blocks.dirt, 1, 0), 'G', Blocks.gravel);
        GameRegistry.addShapelessRecipe(new ItemStack(Blocks.mossy_cobblestone), Blocks.cobblestone, Blocks.vine);
        GameRegistry.addShapelessRecipe(new ItemStack(Blocks.stonebrick, 1, 1), new ItemStack(Blocks.stonebrick, 1, 0), Blocks.vine);

        size(EntityZombie.class, 0.6F, 1.95F);
        size(EntityPigZombie.class, 0.6F, 1.95F);
        size(EntityHusk.class, 0.6F, 1.95F);
        size(EntitySkeleton.class, 0.6F, 1.99F);
        size(EntityStray.class, 0.6F, 1.99F);
        size(EntityCreeper.class, 0.6F, 1.7F);
        size(EntityWitch.class, 0.6F, 1.95F);
        size(EntityCow.class, 0.9F, 1.4F);
        size(EntityMooshroom.class, 0.9F, 1.4F);
        size(EntityChicken.class, 0.4F, 0.7F);
        size(EntitySquid.class, 0.8F, 0.8F);
        size(EntityWolf.class, 0.6F, 0.85F);
        size(EntitySnowman.class, 0.7F, 1.9F);
        size(EntityOcelot.class, 0.6F, 0.7F);
        size(EntityIronGolem.class, 1.4F, 2.7F);
        size(EntityWither.class, 0.9F, 3.5F);
        size(EntityVillager.class, 0.6F, 1.95F);
        size(EntitySilverfish.class, 0.4F, 0.3F);
    }

    /** RecipesBanners.RecipeDuplicatePattern: the patterned banner stays in the grid. */
    public static final class BannerCopy implements IRecipe {
        private static boolean patterned(ItemStack stack) {
            return stack.hasTagCompound() && stack.getTagCompound().getCompoundTag("BlockEntityTag").getTagList("Patterns", 10).tagCount() > 0;
        }

        private ItemStack pick(InventoryCrafting grid, boolean wantPattern) {
            ItemStack found = null;
            for (int i = 0; i < grid.getSizeInventory(); i++) {
                ItemStack stack = grid.getStackInSlot(i);
                if (stack != null) {
                    if (stack.getItem() != Banner112.banner) {
                        return null;
                    }
                    if (patterned(stack) == wantPattern) {
                        if (found != null) {
                            return null;
                        }
                        found = stack;
                    }
                }
            }
            return found;
        }

        @Override
        public boolean matches(InventoryCrafting grid, World world) {
            ItemStack a = pick(grid, true);
            ItemStack b = pick(grid, false);
            return a != null && b != null && a.getItemDamage() == b.getItemDamage();
        }

        @Override
        public ItemStack getCraftingResult(InventoryCrafting grid) {
            ItemStack copy = pick(grid, true).copy();
            copy.stackSize = 1;
            return copy;
        }

        @Override
        public int getRecipeSize() {
            return 2;
        }

        @Override
        public ItemStack getRecipeOutput() {
            return null;
        }
    }

    public static final class BlockInvertedSensor extends BlockDaylightDetector {
        private IIcon top;
        private IIcon side;

        BlockInvertedSensor() {
            setHardness(0.2F);
            setStepSound(Block.soundTypeWood);
            setBlockName(MC112Bridge.MOD_ID + ".daylight_detector_inverted");
        }

        @Override
        public void registerBlockIcons(IIconRegister register) {
            top = register.registerIcon(MC112Bridge.MOD_ID + ":daylight_detector_inverted_top");
            side = register.registerIcon("daylight_detector_side");
        }

        @Override
        public IIcon getIcon(int face, int meta) {
            return face == 1 ? top : side;
        }

        @Override
        public void func_149957_e(World world, int x, int y, int z) {
            if (world.provider.hasNoSky) {
                return;
            }
            int light = world.getSavedLightValue(EnumSkyBlock.Sky, x, y, z) - world.skylightSubtracted;
            float angle = world.getCelestialAngleRadians(1.0F);
            angle += ((angle < (float) Math.PI ? 0.0F : (float) Math.PI * 2.0F) - angle) * 0.2F;
            int power = 15 - MathHelper.clamp_int(Math.round((float) light * MathHelper.cos(angle)), 0, 15);
            if (world.getBlockMetadata(x, y, z) != power) {
                world.setBlockMetadataWithNotify(x, y, z, power, 3);
            }
        }

        @Override
        public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hx, float hy, float hz) {
            if (!world.isRemote) {
                world.setBlock(x, y, z, Blocks.daylight_detector, world.getBlockMetadata(x, y, z), 3);
            }
            return true;
        }

        @Override
        public Item getItemDropped(int meta, Random random, int fortune) {
            return Item.getItemFromBlock(Blocks.daylight_detector);
        }

        @Override
        public Item getItem(World world, int x, int y, int z) {
            return Item.getItemFromBlock(Blocks.daylight_detector);
        }
    }

    public static final class EntitySpectralArrow extends EntityArrow {
        public EntitySpectralArrow(World world) {
            super(world);
        }

        EntitySpectralArrow(World world, EntityLivingBase shooter, float power) {
            super(world, shooter, power);
        }

        @Override
        public void onCollideWithPlayer(EntityPlayer player) {
            boolean grounded = ReflectionHelper.<Boolean, EntityArrow>getPrivateValue(EntityArrow.class, this, "inGround", "field_70254_i");
            if (worldObj.isRemote || !grounded || arrowShake > 0) {
                return;
            }
            boolean take = canBePickedUp == 1 || (canBePickedUp == 2 && player.capabilities.isCreativeMode);
            if (canBePickedUp == 1 && !player.inventory.addItemStackToInventory(new ItemStack(spectralArrow))) {
                take = false;
            }
            if (take) {
                playSound("random.pop", 0.2F, ((rand.nextFloat() - rand.nextFloat()) * 0.7F + 1.0F) * 2.0F);
                player.onItemPickup(this, 1);
                setDead();
            }
        }
    }

    public static final class Events {
        private static ItemStack findAmmo(EntityPlayer player) {
            ItemStack off = OffhandInventory.get(player).getStack();
            if (off != null && (off.getItem() == spectralArrow || off.getItem() == Items.arrow)) {
                return off;
            }
            for (ItemStack stack : player.inventory.mainInventory) {
                if (stack != null && (stack.getItem() == spectralArrow || stack.getItem() == Items.arrow)) {
                    return stack;
                }
            }
            return null;
        }

        @SubscribeEvent
        public void onNock(ArrowNockEvent event) {
            ItemStack ammo = findAmmo(event.entityPlayer);
            if (ammo != null && ammo.getItem() == spectralArrow && !event.entityPlayer.inventory.hasItem(Items.arrow)) {
                event.entityPlayer.setItemInUse(event.result, event.result.getMaxItemUseDuration());
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public void onLoose(ArrowLooseEvent event) {
            EntityPlayer player = event.entityPlayer;
            ItemStack ammo = findAmmo(player);
            if (ammo == null || ammo.getItem() != spectralArrow) {
                return;
            }
            event.setCanceled(true);
            float power = (float) event.charge / 20.0F;
            power = (power * power + power * 2.0F) / 3.0F;
            if ((double) power < 0.1D) {
                return;
            }
            power = Math.min(power, 1.0F);
            EntitySpectralArrow arrow = new EntitySpectralArrow(player.worldObj, player, power * 2.0F);
            arrow.setIsCritical(power == 1.0F);
            int strength = EnchantmentHelper.getEnchantmentLevel(Enchantment.power.effectId, event.bow);
            if (strength > 0) {
                arrow.setDamage(arrow.getDamage() + (double) strength * 0.5D + 0.5D);
            }
            int punch = EnchantmentHelper.getEnchantmentLevel(Enchantment.punch.effectId, event.bow);
            if (punch > 0) {
                arrow.setKnockbackStrength(punch);
            }
            if (EnchantmentHelper.getEnchantmentLevel(Enchantment.flame.effectId, event.bow) > 0) {
                arrow.setFire(100);
            }
            event.bow.damageItem(1, player);
            player.worldObj.playSoundAtEntity(player, "random.bow", 1.0F, 1.0F / (player.worldObj.rand.nextFloat() * 0.4F + 1.2F) + power * 0.5F);
            if (player.capabilities.isCreativeMode) {
                arrow.canBePickedUp = 2;
            } else {
                arrow.canBePickedUp = 1;
                ammo.stackSize--;
                if (ammo.stackSize <= 0) {
                    if (ammo == OffhandInventory.get(player).getStack()) {
                        OffhandInventory.get(player).setStack(null);
                    } else {
                        for (int i = 0; i < player.inventory.mainInventory.length; i++) {
                            if (player.inventory.mainInventory[i] == ammo) {
                                player.inventory.mainInventory[i] = null;
                            }
                        }
                    }
                }
            }
            if (!player.worldObj.isRemote) {
                player.worldObj.spawnEntityInWorld(arrow);
            }
        }

        @SubscribeEvent
        public void onHurt(LivingHurtEvent event) {
            if (event.source.getSourceOfDamage() instanceof EntitySpectralArrow) {
                event.entityLiving.addPotionEffect(new PotionEffect(glowing.id, 200, 0));
            }
        }

        @SubscribeEvent
        public void onUpdate(LivingUpdateEvent event) {
            EntityLivingBase entity = event.entityLiving;
            PotionEffect effect = entity.getActivePotionEffect(levitation);
            if (effect != null) {
                entity.motionY += (0.05D * (double) (effect.getAmplifier() + 1) - entity.motionY) * 0.2D;
                entity.fallDistance = 0.0F;
            }
        }

        @SubscribeEvent
        public void onJoin(EntityJoinWorldEvent event) {
            Entity entity = event.entity;
            float[] size = SIZES.get(entity.getClass());
            if (size == null || (entity instanceof EntityAgeable && ((EntityAgeable) entity).isChild())
                    || (entity instanceof EntityZombie && ((EntityZombie) entity).isChild())
                    || (entity instanceof EntitySkeleton && ((EntitySkeleton) entity).getSkeletonType() == 1)) {
                return;
            }
            entity.width = size[0];
            entity.height = size[1];
            entity.setPosition(entity.posX, entity.posY, entity.posZ);
            if (entity instanceof EntityZombie && !(entity instanceof EntityPigZombie)) {
                ((EntityZombie) entity).getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(35.0D);
            }
        }

        /** BlockSponge.absorb: a sponge removes up to 65 water blocks within 7 steps. */
        @SubscribeEvent
        public void onPlace(BlockEvent.PlaceEvent event) {
            if (event.block != Blocks.sponge || event.blockMetadata != 0 || event.world.isRemote) {
                return;
            }
            World world = event.world;
            ArrayDeque<int[]> queue = new ArrayDeque<int[]>();
            queue.add(new int[] {event.x, event.y, event.z, 0});
            int removed = 0;
            int[][] sides = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
            while (!queue.isEmpty() && removed <= 64) {
                int[] p = queue.poll();
                for (int[] s : sides) {
                    int x = p[0] + s[0];
                    int y = p[1] + s[1];
                    int z = p[2] + s[2];
                    if (world.getBlock(x, y, z).getMaterial() == Material.water) {
                        world.setBlock(x, y, z, Blocks.air, 0, 2);
                        removed++;
                        if (p[3] < 6) {
                            queue.add(new int[] {x, y, z, p[3] + 1});
                        }
                    }
                }
            }
            if (removed > 0) {
                world.setBlock(event.x, event.y, event.z, wetSponge, 0, 2);
            }
        }

        @SubscribeEvent
        public void onInteract(PlayerInteractEvent event) {
            if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK || event.world.isRemote) {
                return;
            }
            Block block = event.world.getBlock(event.x, event.y, event.z);
            if (block == Blocks.daylight_detector) {
                event.world.setBlock(event.x, event.y, event.z, invertedSensor, event.world.getBlockMetadata(event.x, event.y, event.z), 3);
                event.setCanceled(true);
                return;
            }
            ItemStack held = event.entityPlayer.getCurrentEquippedItem();
            if (block == Blocks.cauldron && held != null && held.getItem() == Banner112.banner && held.hasTagCompound()
                    && event.world.getBlockMetadata(event.x, event.y, event.z) > 0) {
                NBTTagCompound tag = held.getTagCompound().getCompoundTag("BlockEntityTag");
                NBTTagList patterns = tag.getTagList("Patterns", 10);
                if (patterns.tagCount() > 0) {
                    patterns.removeTag(patterns.tagCount() - 1);
                    if (patterns.tagCount() == 0) {
                        held.setTagCompound(null);
                    }
                    event.world.setBlockMetadataWithNotify(event.x, event.y, event.z, event.world.getBlockMetadata(event.x, event.y, event.z) - 1, 2);
                    event.entityPlayer.inventoryContainer.detectAndSendChanges();
                    event.setCanceled(true);
                }
            }
        }

        @SubscribeEvent
        public void onCrafted(PlayerEvent.ItemCraftedEvent event) {
            if (event.crafting.getItem() != Banner112.banner || !event.crafting.hasTagCompound()) {
                return;
            }
            int banners = 0;
            ItemStack patterned = null;
            for (int i = 0; i < event.craftMatrix.getSizeInventory(); i++) {
                ItemStack stack = event.craftMatrix.getStackInSlot(i);
                if (stack != null) {
                    banners = stack.getItem() == Banner112.banner ? banners + 1 : banners + 10;
                    if (stack.getItem() == Banner112.banner && stack.hasTagCompound()) {
                        patterned = stack;
                    }
                }
            }
            if (banners == 2 && patterned != null) {
                patterned.stackSize++;
            }
        }
    }

    public static final class CommandTeleport extends CommandBase {
        @Override
        public String getCommandName() {
            return "teleport";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 2;
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "/teleport [player] <x> <y> <z> [yaw] [pitch]";
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length < 3) {
                throw new WrongUsageException(getCommandUsage(sender));
            }
            int shift = args.length == 3 || args.length == 5 ? 0 : 1;
            EntityPlayerMP player = shift == 0 ? getCommandSenderAsPlayer(sender) : getPlayer(sender, args[0]);
            double x = func_110666_a(sender, player.posX, args[shift]);
            double y = func_110666_a(sender, player.posY, args[shift + 1]);
            double z = func_110666_a(sender, player.posZ, args[shift + 2]);
            float yaw = args.length > shift + 3 ? (float) func_110666_a(sender, player.rotationYaw, args[shift + 3]) : player.rotationYaw;
            float pitch = args.length > shift + 4 ? (float) func_110666_a(sender, player.rotationPitch, args[shift + 4]) : player.rotationPitch;
            player.mountEntity(null);
            player.playerNetServerHandler.setPlayerLocation(x, y, z, yaw, pitch);
            sender.addChatMessage(new ChatComponentText("Teleported " + player.getCommandSenderName()));
        }
    }

    public static final class CommandReplaceItem extends CommandBase {
        @Override
        public String getCommandName() {
            return "replaceitem";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 2;
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "/replaceitem block <x> <y> <z> <slot> <item> [count] [data] | /replaceitem entity <player> <hotbar 0-8 or inventory 9-35> <item> [count] [data]";
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length < 4) {
                throw new WrongUsageException(getCommandUsage(sender));
            }
            boolean block = "block".equals(args[0]);
            int base = block ? 5 : 3;
            if (args.length <= base) {
                throw new WrongUsageException(getCommandUsage(sender));
            }
            Item item = getItemByText(sender, args[base]);
            int count = args.length > base + 1 ? parseIntBounded(sender, args[base + 1], 1, 64) : 1;
            int data = args.length > base + 2 ? parseInt(sender, args[base + 2]) : 0;
            ItemStack stack = new ItemStack(item, count, data);
            if (block) {
                ChunkCoordinates c = sender.getPlayerCoordinates();
                int x = MathHelper.floor_double(func_110666_a(sender, c.posX, args[1]));
                int y = MathHelper.floor_double(func_110666_a(sender, c.posY, args[2]));
                int z = MathHelper.floor_double(func_110666_a(sender, c.posZ, args[3]));
                TileEntity tile = sender.getEntityWorld().getTileEntity(x, y, z);
                if (!(tile instanceof IInventory)) {
                    throw new WrongUsageException("Block has no inventory");
                }
                ((IInventory) tile).setInventorySlotContents(parseIntBounded(sender, args[4], 0, ((IInventory) tile).getSizeInventory() - 1), stack);
                tile.markDirty();
            } else {
                EntityPlayerMP player = getPlayer(sender, args[1]);
                player.inventory.setInventorySlotContents(parseIntBounded(sender, args[2], 0, 35), stack);
                player.inventoryContainer.detectAndSendChanges();
            }
            sender.addChatMessage(new ChatComponentText("Replaced with " + stack.getDisplayName()));
        }
    }

    public static final class CommandBlockData extends CommandBase {
        @Override
        public String getCommandName() {
            return "blockdata";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 2;
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "/blockdata <x> <y> <z> <nbt>";
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length < 4) {
                throw new WrongUsageException(getCommandUsage(sender));
            }
            ChunkCoordinates c = sender.getPlayerCoordinates();
            int x = MathHelper.floor_double(func_110666_a(sender, c.posX, args[0]));
            int y = MathHelper.floor_double(func_110666_a(sender, c.posY, args[1]));
            int z = MathHelper.floor_double(func_110666_a(sender, c.posZ, args[2]));
            World world = sender.getEntityWorld();
            TileEntity tile = world.getTileEntity(x, y, z);
            if (tile == null) {
                throw new WrongUsageException("Block has no data");
            }
            NBTBase parsed;
            try {
                parsed = JsonToNBT.func_150315_a(func_147178_a(sender, args, 3).getUnformattedText());
            } catch (NBTException e) {
                throw new WrongUsageException(e.getMessage());
            }
            if (!(parsed instanceof NBTTagCompound)) {
                throw new WrongUsageException("Not a compound");
            }
            NBTTagCompound tag = new NBTTagCompound();
            tile.writeToNBT(tag);
            for (Object key : ((NBTTagCompound) parsed).func_150296_c()) {
                tag.setTag((String) key, ((NBTTagCompound) parsed).getTag((String) key).copy());
            }
            tag.setInteger("x", x);
            tag.setInteger("y", y);
            tag.setInteger("z", z);
            tile.readFromNBT(tag);
            tile.markDirty();
            world.markBlockForUpdate(x, y, z);
            sender.addChatMessage(new ChatComponentText("Block data updated"));
        }
    }

    public static final class CommandParticle extends CommandBase {
        @Override
        public String getCommandName() {
            return "particle";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 2;
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "/particle <name> <x> <y> <z> <dx> <dy> <dz> <speed> [count]";
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length < 8) {
                throw new WrongUsageException(getCommandUsage(sender));
            }
            ChunkCoordinates c = sender.getPlayerCoordinates();
            double x = func_110666_a(sender, c.posX, args[1]);
            double y = func_110666_a(sender, c.posY, args[2]);
            double z = func_110666_a(sender, c.posZ, args[3]);
            int count = args.length > 8 ? parseIntBounded(sender, args[8], 0, 100000) : 0;
            ((WorldServer) sender.getEntityWorld()).func_147487_a(args[0], x, y, z, count, parseDouble(sender, args[4]), parseDouble(sender, args[5]), parseDouble(sender, args[6]), parseDouble(sender, args[7]));
        }
    }

    public static final class CommandExecute extends CommandBase {
        @Override
        public String getCommandName() {
            return "execute";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 2;
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "/execute <player> <x> <y> <z> <command>";
        }

        @Override
        public void processCommand(final ICommandSender sender, String[] args) {
            if (args.length < 5) {
                throw new WrongUsageException(getCommandUsage(sender));
            }
            final EntityPlayerMP player = getPlayer(sender, args[0]);
            final ChunkCoordinates at = new ChunkCoordinates(
                    MathHelper.floor_double(func_110666_a(sender, player.posX, args[1])),
                    MathHelper.floor_double(func_110666_a(sender, player.posY, args[2])),
                    MathHelper.floor_double(func_110666_a(sender, player.posZ, args[3])));
            ICommandSender wrapped = new ICommandSender() {
                @Override
                public String getCommandSenderName() {
                    return player.getCommandSenderName();
                }

                @Override
                public IChatComponent func_145748_c_() {
                    return player.func_145748_c_();
                }

                @Override
                public void addChatMessage(IChatComponent message) {
                    sender.addChatMessage(message);
                }

                @Override
                public boolean canCommandSenderUseCommand(int level, String command) {
                    return sender.canCommandSenderUseCommand(level, command);
                }

                @Override
                public ChunkCoordinates getPlayerCoordinates() {
                    return at;
                }

                @Override
                public World getEntityWorld() {
                    return player.worldObj;
                }
            };
            StringBuilder command = new StringBuilder();
            for (int i = 4; i < args.length; i++) {
                command.append(i > 4 ? " " : "").append(args[i]);
            }
            net.minecraft.server.MinecraftServer.getServer().getCommandManager().executeCommand(wrapped, command.toString());
        }
    }

}
