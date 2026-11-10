package ru.codex.offhand1710;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.MathHelper;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapData;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;

/** Explorer maps with a structure marker, the glowing flag and luck. */
public final class Maps112 {
    /** Icons of map_icons.png in 1.12.2 (MapDecoration.Type): 8 = mansion, 9 = monument. */
    private static final int ICON_MANSION = 8;
    private static final int ICON_MONUMENT = 9;

    private Maps112() {
    }

    public static void register() {
        Events events = new Events();
        MinecraftForge.EVENT_BUS.register(events);
        FMLCommonHandler.instance().bus().register(events);
    }

    /** ItemMap.setupNewMap with scale 2 and the marker kept in the item, as "Decorations" does in 1.12.2. */
    public static ItemStack explorerMap(World world, String structure, int fromX, int fromZ) {
        int[] found = Content121.locate(world, structure, fromX, fromZ);
        if (found == null) {
            return null;
        }
        int id = world.getUniqueDataId("map");
        ItemStack stack = new ItemStack(Items.filled_map, 1, id);
        MapData data = new MapData("map_" + id);
        world.setItemData(data.mapName, data);
        data.scale = 2;
        int size = 128 << data.scale;
        data.xCenter = (int) (Math.round((double) found[0] / (double) size) * (long) size);
        data.zCenter = (int) (Math.round((double) found[1] / (double) size) * (long) size);
        data.dimension = (byte) world.provider.dimensionId;
        data.markDirty();
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("Marker112Type", "Mansion".equals(structure) ? ICON_MANSION : ICON_MONUMENT);
        tag.setInteger("Marker112X", found[0]);
        tag.setInteger("Marker112Z", found[1]);
        stack.setTagCompound(tag);
        stack.setStackDisplayName(StatCollector.translateToLocal("filled_map." + structure.toLowerCase()));
        return stack;
    }

    private static void refresh(World world, ItemStack stack) {
        MapData data = Items.filled_map.getMapData(stack, world);
        if (data == null || data.playersVisibleOnMap.containsKey("+")) {
            return;
        }
        NBTTagCompound tag = stack.getTagCompound();
        int scale = 1 << data.scale;
        int x = MathHelper.clamp_int((int) ((double) ((float) (tag.getInteger("Marker112X") - data.xCenter) / (float) scale) * 2.0D + 0.5D), -128, 127);
        int z = MathHelper.clamp_int((int) ((double) ((float) (tag.getInteger("Marker112Z") - data.zCenter) / (float) scale) * 2.0D + 0.5D), -128, 127);
        data.playersVisibleOnMap.put("+", data.new MapCoord((byte) tag.getInteger("Marker112Type"), (byte) x, (byte) z, (byte) 8));
    }

    public static boolean hasMarker(World world, ItemStack stack) {
        MapData data = Items.filled_map.getMapData(stack, world);
        return data != null && data.playersVisibleOnMap.containsKey("+");
    }

    /** EntityLivingBase attribute generic.luck of 1.9, for the fishing roll: Luck of the Sea plus the two effects. */
    public static int fishingLuck(EntityLivingBase angler) {
        int luck = EnchantmentHelper.func_151386_g(angler);
        PotionEffect good = angler.getActivePotionEffect(Content120.luck);
        PotionEffect bad = angler.getActivePotionEffect(Content120.unluck);
        if (good != null) {
            luck += good.getAmplifier() + 1;
        }
        if (bad != null) {
            luck -= bad.getAmplifier() + 1;
        }
        return Math.max(luck, 0);
    }

    public static boolean isGlowing(Entity entity) {
        return (entity.getDataWatcher().getWatchableObjectByte(0) & 64) != 0;
    }

    public static final class Events {
        @SubscribeEvent
        public void onPlayer(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END || event.player.worldObj.isRemote || event.player.ticksExisted % 20 != 0) {
                return;
            }
            for (ItemStack stack : event.player.inventory.mainInventory) {
                if (stack != null && stack.getItem() == Items.filled_map && stack.hasTagCompound() && stack.getTagCompound().hasKey("Marker112Type")) {
                    refresh(event.player.worldObj, stack);
                }
            }
        }

        /** The glowing effect is mirrored into flag 6 of Entity, as in 1.9, so that every client can see it. */
        @SubscribeEvent
        public void onLiving(LivingUpdateEvent event) {
            EntityLivingBase entity = event.entityLiving;
            if (entity.worldObj.isRemote) {
                return;
            }
            byte flags = entity.getDataWatcher().getWatchableObjectByte(0);
            boolean active = entity.isPotionActive(Content120.glowing);
            if (active != ((flags & 64) != 0)) {
                entity.getDataWatcher().updateObject(0, Byte.valueOf((byte) (active ? flags | 64 : flags & ~64)));
            }
        }
    }

}
