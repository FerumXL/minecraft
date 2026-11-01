package ru.codex.offhand1710;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
@Mod(modid=MC112Bridge.MOD_ID, name="Minecraft 1.12.2 Content for 1.7.10", version="0.1.0", acceptedMinecraftVersions="[1.7.10]")
public final class MC112Bridge {
 public static final String MOD_ID="mc112bridge";
 public static Item ironNugget;
 @EventHandler public void initialize(FMLInitializationEvent event) {
  ironNugget=new Item().setUnlocalizedName(MOD_ID+".iron_nugget").setTextureName(MOD_ID+":iron_nugget").setCreativeTab(CreativeTabs.tabMaterials);
  GameRegistry.registerItem(ironNugget,"iron_nugget");
  GameRegistry.addShapelessRecipe(new ItemStack(ironNugget,9),Items.iron_ingot);
  GameRegistry.addShapedRecipe(new ItemStack(Items.iron_ingot),"nnn","nnn","nnn",'n',ironNugget);
  addIronScrapSmelting();
 }
 private void addIronScrapSmelting() {
  ItemStack nugget=new ItemStack(ironNugget);
  GameRegistry.addSmelting(Items.iron_sword,nugget,0.1F);
  GameRegistry.addSmelting(Items.iron_pickaxe,nugget,0.1F);
  GameRegistry.addSmelting(Items.iron_shovel,nugget,0.1F);
  GameRegistry.addSmelting(Items.iron_axe,nugget,0.1F);
  GameRegistry.addSmelting(Items.iron_hoe,nugget,0.1F);
  GameRegistry.addSmelting(Items.iron_helmet,nugget,0.1F);
  GameRegistry.addSmelting(Items.iron_chestplate,nugget,0.1F);
  GameRegistry.addSmelting(Items.iron_leggings,nugget,0.1F);
  GameRegistry.addSmelting(Items.iron_boots,nugget,0.1F);
 }
}
