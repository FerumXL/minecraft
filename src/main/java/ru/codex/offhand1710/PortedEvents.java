package ru.codex.offhand1710;

import java.util.Random;

import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.UseHoeEvent;

/** Behaviour of 1.12.2 that is not part of a single block or item. */
public final class PortedEvents {
    /**
     * Shovel on grass makes a grass path (1.12.2 ItemSpade.onItemUse): any face but the bottom, air above,
     * flatten sound, 1 durability. The client only swings; the block changes on the server.
     */
    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent event) {
        if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK || event.face == 0) {
            return;
        }
        EntityPlayer player = event.entityPlayer;
        ItemStack held = player.getCurrentEquippedItem();
        World world = event.world;
        int x = event.x;
        int y = event.y;
        int z = event.z;
        if (held == null || !(held.getItem() instanceof ItemSpade) || world.getBlock(x, y, z) != Blocks.grass
                || !world.isAirBlock(x, y + 1, z) || !player.canPlayerEdit(x, y, z, event.face, held)) {
            return;
        }
        if (world.isRemote) {
            player.swingItem();
            return;
        }
        world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, MC112Bridge.MOD_ID + ":item.shovel.flatten", 1.0F, 1.0F);
        world.setBlock(x, y, z, PortedBlocks.grassPath);
        held.damageItem(1, player);
    }

    /** A hoe turns a grass path into farmland, as on grass (1.12.2 ItemHoe). */
    @SubscribeEvent
    public void onHoe(UseHoeEvent event) {
        World world = event.world;
        if (world.getBlock(event.x, event.y, event.z) != PortedBlocks.grassPath
                || !world.isAirBlock(event.x, event.y + 1, event.z)) {
            return;
        }
        Block farmland = Blocks.farmland;
        world.playSoundEffect(event.x + 0.5D, event.y + 0.5D, event.z + 0.5D, farmland.stepSound.getStepResourcePath(),
                (farmland.stepSound.getVolume() + 1.0F) / 2.0F, farmland.stepSound.getPitch() * 0.8F);
        if (!world.isRemote) {
            world.setBlock(event.x, event.y, event.z, farmland);
        }
        event.setResult(Event.Result.ALLOW); // ItemHoe then takes 1 durability
    }

    /**
     * Sheep drop mutton (1.12.2 loot table): 1-2, plus looting * random(0..1) rounded; cooked when the sheep
     * was burning; lambs drop none.
     */
    @SubscribeEvent
    public void onDrops(LivingDropsEvent event) {
        if (!(event.entityLiving instanceof EntitySheep) || event.entityLiving.isChild()) {
            return;
        }
        EntitySheep sheep = (EntitySheep) event.entityLiving;
        Random random = sheep.getRNG();
        int count = 1 + random.nextInt(2) + Math.round(event.lootingLevel * random.nextFloat());
        ItemStack stack = new ItemStack(sheep.isBurning() ? PortedBlocks.muttonCooked : PortedBlocks.muttonRaw, count);
        EntityItem item = new EntityItem(sheep.worldObj, sheep.posX, sheep.posY, sheep.posZ, stack);
        item.delayBeforeCanPickup = 10;
        event.drops.add(item);
    }
}
