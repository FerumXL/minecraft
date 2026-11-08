package ru.codex.offhand1710;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemSoup;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

/** Registration of the mobs of 1.8-1.12.2, their spawn rules, drops and the totem of undying. */
public final class Mobs112 {
    public static Item rabbitHide;
    public static Item rabbitRaw;
    public static Item rabbitCooked;
    public static Item rabbitStew;
    public static Item rabbitFoot;
    public static Item totem;

    private static int nextId = 2;

    private Mobs112() {
    }

    private static Item item(Item item, String name) {
        item.setUnlocalizedName(MC112Bridge.MOD_ID + "." + name).setTextureName(MC112Bridge.MOD_ID + ":" + name);
        GameRegistry.registerItem(item, name);
        return item;
    }

    private static void mob(Object mod, Class<? extends EntityLiving> type, String name, int egg, int spots) {
        EntityRegistry.registerGlobalEntityID(type, name, EntityRegistry.findGlobalUniqueEntityId(), egg, spots);
        EntityRegistry.registerModEntity(type, name, nextId++, mod, 80, 3, true);
    }

    public static void register(Object mod) {
        rabbitHide = item(new Item().setCreativeTab(CreativeTabs.tabMaterials), "rabbit_hide");
        rabbitRaw = item(new ItemFood(3, 0.3F, true).setCreativeTab(CreativeTabs.tabFood), "rabbit_raw");
        rabbitCooked = item(new ItemFood(5, 0.6F, true).setCreativeTab(CreativeTabs.tabFood), "rabbit_cooked");
        rabbitStew = item(new ItemSoup(10).setCreativeTab(CreativeTabs.tabFood), "rabbit_stew");
        rabbitFoot = item(new Item().setCreativeTab(CreativeTabs.tabBrewing), "rabbit_foot");
        totem = item(new Item().setMaxStackSize(1).setCreativeTab(CreativeTabs.tabCombat), "totem");
        GameRegistry.addSmelting(rabbitRaw, new ItemStack(rabbitCooked), 0.35F);
        GameRegistry.addShapedRecipe(new ItemStack(Items.leather), "hh", "hh", 'h', rabbitHide);
        GameRegistry.addShapelessRecipe(new ItemStack(rabbitStew), rabbitCooked, Items.carrot, Items.baked_potato,
                net.minecraft.init.Blocks.brown_mushroom, Items.bowl);
        GameRegistry.addShapelessRecipe(new ItemStack(rabbitStew), rabbitCooked, Items.carrot, Items.baked_potato,
                net.minecraft.init.Blocks.red_mushroom, Items.bowl);

        mob(mod, EntityHusk.class, "Husk", 7958625, 15125652);
        mob(mod, EntityStray.class, "Stray", 6387319, 14543594);
        mob(mod, EntityPolarBear.class, "PolarBear", 15921906, 9803152);
        mob(mod, EntityRabbit.class, "Rabbit", 10051392, 7555121);
        mob(mod, Illagers.Vindicator.class, "VindicationIllager", 9804699, 2580065);
        mob(mod, Illagers.Evoker.class, "EvocationIllager", 9804699, 1973274);
        mob(mod, Illagers.Illusioner.class, "IllusionIllager", 1267616, 9804699);
        mob(mod, Illagers.Vex.class, "Vex", 7897998, 15265008);
        mob(mod, MoreMobs.Parrot.class, "Parrot", 894731, 16711680);
        mob(mod, MoreMobs.Llama.class, "Llama", 12623485, 10051392);
        mob(mod, MoreMobs.Shulker.class, "Shulker", 9725844, 5060690);
        EntityRegistry.registerModEntity(MoreMobs.Spit.class, "LlamaSpit", nextId++, mod, 64, 10, true);
        EntityRegistry.registerModEntity(MoreMobs.Bullet.class, "ShulkerBullet", nextId++, mod, 64, 3, true);
        EntityRegistry.registerModEntity(Illagers.Fangs.class, "EvocationFangs", nextId++, mod, 80, 3, false);

        BiomeGenBase[] desert = {BiomeGenBase.desert, BiomeGenBase.desertHills};
        BiomeGenBase[] snow = {BiomeGenBase.icePlains, BiomeGenBase.iceMountains};
        EntityRegistry.removeSpawn(EntityZombie.class, EnumCreatureType.monster, desert);
        EntityRegistry.addSpawn(EntityZombie.class, 19, 4, 4, EnumCreatureType.monster, desert);
        EntityRegistry.addSpawn(EntityHusk.class, 80, 4, 4, EnumCreatureType.monster, desert);
        EntityRegistry.removeSpawn(EntitySkeleton.class, EnumCreatureType.monster, snow);
        EntityRegistry.addSpawn(EntitySkeleton.class, 20, 4, 4, EnumCreatureType.monster, snow);
        EntityRegistry.addSpawn(EntityStray.class, 80, 4, 4, EnumCreatureType.monster, snow);
        EntityRegistry.addSpawn(EntityPolarBear.class, 1, 1, 2, EnumCreatureType.creature, snow);
        EntityRegistry.addSpawn(EntityRabbit.class, 10, 2, 3, EnumCreatureType.creature, snow);
        EntityRegistry.addSpawn(EntityRabbit.class, 4, 2, 3, EnumCreatureType.creature, BiomeGenBase.plains, BiomeGenBase.desert,
                BiomeGenBase.desertHills, BiomeGenBase.taiga, BiomeGenBase.taigaHills, BiomeGenBase.coldTaiga, BiomeGenBase.coldTaigaHills,
                BiomeGenBase.megaTaiga, BiomeGenBase.megaTaigaHills);

        EntityRegistry.addSpawn(MoreMobs.Parrot.class, 40, 1, 2, EnumCreatureType.creature, BiomeGenBase.jungle, BiomeGenBase.jungleHills, BiomeGenBase.jungleEdge);
        EntityRegistry.addSpawn(MoreMobs.Llama.class, 5, 4, 6, EnumCreatureType.creature, BiomeGenBase.extremeHills, BiomeGenBase.extremeHillsEdge, BiomeGenBase.extremeHillsPlus, BiomeGenBase.savanna, BiomeGenBase.savannaPlateau);
        MinecraftForge.EVENT_BUS.register(new EntityStray.Arrows());
        MinecraftForge.EVENT_BUS.register(new Mobs112Events());
    }

    /** EntityLivingBase.checkTotemDeathProtection of 1.12.2; any hand counts, the void does not. */
    public static final class Mobs112Events {
        @SubscribeEvent
        public void onDeath(LivingDeathEvent event) {
            if (event.source.canHarmInCreative() || !(event.entityLiving instanceof EntityPlayer)) {
                return;
            }
            EntityPlayer player = (EntityPlayer) event.entityLiving;
            OffhandInventory offhand = OffhandInventory.get(player);
            ItemStack main = player.getCurrentEquippedItem();
            if (main != null && main.getItem() == totem) {
                player.inventory.mainInventory[player.inventory.currentItem] = null;
            } else if (offhand.getStack() != null && offhand.getStack().getItem() == totem) {
                offhand.setStack(null);
                if (player instanceof net.minecraft.entity.player.EntityPlayerMP) {
                    OffhandMessages.synchronize((net.minecraft.entity.player.EntityPlayerMP) player);
                }
            } else {
                return;
            }
            event.setCanceled(true);
            player.setHealth(1.0F);
            player.clearActivePotions();
            player.addPotionEffect(new PotionEffect(Potion.regeneration.id, 900, 1));
            player.addPotionEffect(new PotionEffect(Potion.field_76444_x.id, 100, 1));
            player.worldObj.playSoundAtEntity(player, MC112Bridge.MOD_ID + ":item.totem.use", 1.0F, 1.0F);
        }
    }

}
