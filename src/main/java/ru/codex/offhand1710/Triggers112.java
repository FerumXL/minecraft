package ru.codex.offhand1710;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonObject;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.AchievementList;
import net.minecraft.tileentity.TileEntityBeacon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.AchievementEvent;
import net.minecraftforge.event.entity.player.EntityInteractEvent;

/**
 * The advancement triggers of 1.12.2 that need a hook into vanilla code (asm.Entities) or a check of their own:
 * enter_block, cured_zombie_villager, enchanted_item, item_durability_changed, tame_animal, brewed_potion,
 * construct_beacon and nether_travel; and Block.onLanded of the slime block and the bed for every entity.
 */
public final class Triggers112 {
    private static final String CURE_TAG = "mc112CureBy";
    private static Set<Block> enterBlocks;

    private Triggers112() {
    }

    static void reset() {
        enterBlocks = null;
    }

    // ---- Block.onLanded ----

    /**
     * Entity.moveEntity after a vertical collision, in place of "motionY = 0": BlockSlime.onLanded and
     * BlockBed.onLanded of 1.12.2 (the block 0.2 under the feet; not when sneaking; anything not living keeps 0.8 of
     * the bounce).
     */
    public static void landed(Entity e) {
        if (e.motionY < 0.0D && !e.isSneaking()) {
            Block below = e.worldObj.getBlock(MathHelper.floor_double(e.posX), MathHelper.floor_double(e.boundingBox.minY - 0.2D), MathHelper.floor_double(e.posZ));
            if (below == Content119.slime || below instanceof BlockBed) {
                e.motionY = below instanceof BlockBed ? -e.motionY * 0.6600000262260437D : -e.motionY;
                if (!(e instanceof EntityLivingBase)) {
                    e.motionY *= 0.8D;
                }
                return;
            }
        }
        e.motionY = 0.0D;
    }

    // ---- enter_block ----

    private static Block block(String name) {
        String full = name.contains(":") ? name : "minecraft:" + name;
        if (Block.blockRegistry.containsKey(full)) {
            return (Block) Block.blockRegistry.getObject(full);
        }
        String mod = MC112Bridge.MOD_ID + ":" + full.substring(full.indexOf(':') + 1);
        return Block.blockRegistry.containsKey(mod) ? (Block) Block.blockRegistry.getObject(mod) : null;
    }

    private static Set<Block> enterBlocks() {
        Set<Block> set = enterBlocks;
        if (set == null) {
            set = new HashSet<Block>();
            List<Data112.Criterion> list = Data112.BY_TRIGGER.get("enter_block");
            if (list != null) {
                for (Data112.Criterion c : list) {
                    Block b = c.conditions != null && c.conditions.has("block") ? block(c.conditions.get("block").getAsString()) : null;
                    if (b != null) {
                        set.add(b);
                    }
                }
            }
            enterBlocks = set;
        }
        return set;
    }

    /** Entity.func_145775_I (doBlockCollisions): the block's own collision, then EntityPlayerMP.onInsideBlock of 1.12. */
    public static void collide(final Block block, World world, int x, int y, int z, Entity entity) {
        block.onEntityCollidedWithBlock(world, x, y, z, entity);
        if (!world.isRemote && entity instanceof EntityPlayerMP && enterBlocks().contains(block)) {
            Data112.fire((EntityPlayerMP) entity, "enter_block", new Data112.Check() {
                @Override
                public boolean matches(JsonObject c) {
                    return !c.has("block") || block(c.get("block").getAsString()) == block;
                }
            });
        }
    }

    // ---- cured_zombie_villager ----

    /** EntityZombie.convertToVillager: the player who gave the golden apple (EntityZombieVillager.conversionStarter). */
    public static void cured(EntityZombie zombie) {
        if (zombie.worldObj.isRemote || MinecraftServer.getServer() == null) {
            return;
        }
        String by = zombie.getEntityData().getString(CURE_TAG);
        if (by.isEmpty()) {
            return;
        }
        for (Object o : MinecraftServer.getServer().getConfigurationManager().playerEntityList) {
            EntityPlayerMP p = (EntityPlayerMP) o;
            if (by.equals(p.getUniqueID().toString())) {
                Data112.fire(p, "cured_zombie_villager", null);
            }
        }
    }

    // ---- enchanted_item ----

    /** ContainerEnchantment.enchantItem returned. */
    public static void enchanted(boolean done, EntityPlayer player) {
        if (done && player instanceof EntityPlayerMP && !player.worldObj.isRemote) {
            Data112.fire((EntityPlayerMP) player, "enchanted_item", null);
        }
    }

    // ---- item_durability_changed ----

    public static int damageBefore(ItemStack stack) {
        return stack.getItemDamage();
    }

    /** ItemStack.damageItem returned: a broken item has durability -1 as in ItemDurabilityTrigger of 1.12. */
    public static void damaged(final ItemStack stack, EntityLivingBase entity, int before) {
        if (!(entity instanceof EntityPlayerMP) || entity.worldObj.isRemote || !stack.isItemStackDamageable()) {
            return;
        }
        boolean broken = stack.stackSize <= 0;
        if (!broken && stack.getItemDamage() == before) {
            return;
        }
        final int durability = broken ? -1 : stack.getMaxDamage() - stack.getItemDamage();
        final int delta = broken ? before - stack.getMaxDamage() - 1 : before - stack.getItemDamage();
        Data112.fire((EntityPlayerMP) entity, "item_durability_changed", new Data112.Check() {
            @Override
            public boolean matches(JsonObject c) {
                return Data112.itemCond(c, stack) && Data112.range(c, "durability", durability) && Data112.range(c, "delta", delta);
            }
        });
    }

    // ---- events: tame_animal, brewed_potion, construct_beacon, nether_travel, the cure starter ----

    public static final class Events {
        private final List<Object[]> taming = new ArrayList<Object[]>();
        private final Map<String, Integer> riddenHorse = new HashMap<String, Integer>();
        private final Map<String, double[]> overworld = new HashMap<String, double[]>();
        private final Map<String, double[]> enteredNether = new HashMap<String, double[]>();

        @SubscribeEvent
        public void onInteract(EntityInteractEvent event) {
            if (event.entityPlayer.worldObj.isRemote || !(event.entityPlayer instanceof EntityPlayerMP)) {
                return;
            }
            if (event.target instanceof EntityTameable && !((EntityTameable) event.target).isTamed()) {
                taming.add(new Object[] {event.entityPlayer, event.target});
            }
            ItemStack held = event.entityPlayer.getCurrentEquippedItem();
            if (event.target instanceof EntityZombie && held != null && held.getItem() == Items.golden_apple && held.getItemDamage() == 0) {
                EntityZombie z = (EntityZombie) event.target;
                if (z.isVillager() && z.isPotionActive(Potion.weakness) && !z.isConverting()) {
                    z.getEntityData().setString(CURE_TAG, event.entityPlayer.getUniqueID().toString());
                }
            }
        }

        private static void tamed(EntityPlayerMP p, final Entity animal) {
            Data112.fire(p, "tame_animal", new Data112.Check() {
                @Override
                public boolean matches(JsonObject c) {
                    return Data112.entityCond(c, "entity", animal);
                }
            });
        }

        @SubscribeEvent
        public void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END || taming.isEmpty()) {
                return;
            }
            for (Object[] t : taming) {
                if (((EntityTameable) t[1]).isTamed()) {
                    tamed((EntityPlayerMP) t[0], (Entity) t[1]);
                }
            }
            taming.clear();
        }

        @SubscribeEvent
        public void onAchievement(AchievementEvent event) {
            // ContainerBrewingStand.Potion.onPickupFromSlot: a potion taken out of the stand (SlotBrewingStand.Potion.onTake of 1.12)
            if (event.achievement == AchievementList.potion && event.entityPlayer instanceof EntityPlayerMP && !event.entityPlayer.worldObj.isRemote) {
                Data112.fire((EntityPlayerMP) event.entityPlayer, "brewed_potion", null);
            }
        }

        @SubscribeEvent
        public void onTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END || event.player.worldObj.isRemote || !(event.player instanceof EntityPlayerMP)) {
                return;
            }
            final EntityPlayerMP p = (EntityPlayerMP) event.player;
            String name = p.getCommandSenderName();
            if (p.dimension == 0) {
                overworld.put(name, new double[] {p.posX, p.posY, p.posZ});
            }
            // a horse is tamed by riding it (EntityHorse.setTamedBy)
            if (p.ridingEntity instanceof EntityHorse) {
                EntityHorse horse = (EntityHorse) p.ridingEntity;
                Integer id = riddenHorse.get(name);
                if (!horse.isTame()) {
                    riddenHorse.put(name, Integer.valueOf(horse.getEntityId()));
                } else if (id != null && id.intValue() == horse.getEntityId()) {
                    riddenHorse.remove(name);
                    tamed(p, horse);
                }
            }
            // TileEntityBeacon.updateBeacon of 1.12: players in the box around a beacon with a pyramid
            if (p.ticksExisted % 20 == 0) {
                int levels = 0;
                for (Object o : p.worldObj.loadedTileEntityList) {
                    if (o instanceof TileEntityBeacon) {
                        TileEntityBeacon b = (TileEntityBeacon) o;
                        if (Math.abs(b.xCoord + 0.5D - p.posX) <= 10.5D && Math.abs(b.zCoord + 0.5D - p.posZ) <= 10.5D && p.posY >= b.yCoord - 9 && p.posY <= b.yCoord + 6) {
                            levels = Math.max(levels, b.getLevels());
                        }
                    }
                }
                if (levels > 0) {
                    final int level = levels;
                    Data112.fire(p, "construct_beacon", new Data112.Check() {
                        @Override
                        public boolean matches(JsonObject c) {
                            return Data112.range(c, "level", level);
                        }
                    });
                }
            }
        }

        /** EntityPlayerMP.changeDimension and PlayerList.changePlayerDimension of 1.12 (enteredNetherPosition). */
        @SubscribeEvent
        public void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
            if (!(event.player instanceof EntityPlayerMP)) {
                return;
            }
            final EntityPlayerMP p = (EntityPlayerMP) event.player;
            String name = p.getCommandSenderName();
            if (event.fromDim == 0 && event.toDim == -1) {
                double[] pos = overworld.get(name);
                if (pos != null) {
                    enteredNether.put(name, pos);
                }
            } else if (event.fromDim == -1 && event.toDim == 0) {
                final double[] entered = enteredNether.get(name);
                if (entered != null) {
                    Data112.fire(p, "nether_travel", new Data112.Check() {
                        @Override
                        public boolean matches(JsonObject c) {
                            if (!c.has("distance")) {
                                return true;
                            }
                            JsonObject d = c.getAsJsonObject("distance");
                            double dx = p.posX - entered[0];
                            double dy = p.posY - entered[1];
                            double dz = p.posZ - entered[2];
                            return Data112.range(d, "x", Math.abs(dx)) && Data112.range(d, "y", Math.abs(dy)) && Data112.range(d, "z", Math.abs(dz))
                                && Data112.range(d, "horizontal", Math.sqrt(dx * dx + dz * dz)) && Data112.range(d, "absolute", Math.sqrt(dx * dx + dy * dy + dz * dz));
                        }
                    });
                }
            } else if (event.fromDim != -1 && event.toDim != 0) {
                enteredNether.remove(name);
            }
        }
    }
}
