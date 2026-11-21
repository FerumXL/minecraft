package ru.codex.offhand1710;

import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.item.EntityPainting;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntityWitch;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S29PacketSoundEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.PlaySoundAtEntityEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.EntityInteractEvent;

/**
 * Sounds that 1.12.2 has and 1.7.10 does not play: the hooks the coremod (asm.SoundPatches) calls, and the renaming
 * of entity sounds that 1.7.10 plays under an older or a shared name (witch, wither skeleton, zombie villager,
 * thorns, the player hurt by drowning or burning, the bobber and the eye of ender thrown). Volumes and
 * pitches are those of the 1.12.2 calls. Client-side block sounds (chests, doors, note blocks, the hoe) are renamed
 * in client.Sounds112Client. Everything off with -Dmc112.sounds17=1.
 */
public final class Sounds112 {
    public static final boolean OFF = System.getProperty("mc112.sounds17") != null;
    private static final String P = MC112Bridge.MOD_ID + ":";
    private static final Random RAND = new Random();
    /** The last damage source per entity, for the player's hurt sound (1.12 EntityPlayer.getHurtSound(DamageSource)). */
    private static final Map<Entity, DamageSource> LAST = new WeakHashMap<Entity, DamageSource>();
    private static final Map<Entity, Long> LAST_TICK = new WeakHashMap<Entity, Long>();
    /** One bottle sound per player and tick: the bottle method may reach two potion stacks on one use. */
    private static final Map<Entity, Long> BOTTLE = new WeakHashMap<Entity, Long>();
    private static long portalTick = -1L;
    /** For the autotest: the last sound name played through these hooks or renamed by the handler. */
    public static volatile String last = "";
    /** For the autotest: toast sounds played (ui.toast.in and ui.toast.out). */
    public static volatile int toastSounds;

    private Sounds112() {
    }

    static void register() {
        if (!OFF) {
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new Sounds112.Events());
        }
    }

    public static String name(String event) {
        return P + event;
    }

    private static void at(World world, double x, double y, double z, String event, float volume, float pitch) {
        last = event;
        world.playSoundEffect(x, y, z, P + event, volume, pitch);
    }

    private static void on(Entity e, String event, float volume, float pitch) {
        last = event;
        e.worldObj.playSoundAtEntity(e, P + event, volume, pitch);
    }

    // ---- hooks called by the coremod -------------------------------------------------------------------------

    public static void witchThrow(Entity witch) {
        if (!OFF && !witch.worldObj.isRemote) {
            on(witch, "entity.witch.throw", 1.0F, 0.8F + RAND.nextFloat() * 0.4F);
        }
    }

    public static void witchDrink(Entity witch) {
        if (!OFF && !witch.worldObj.isRemote) {
            on(witch, "entity.witch.drink", 1.0F, 0.8F + RAND.nextFloat() * 0.4F);
        }
    }

    /** ItemBucket.onItemRightClick, just before the full bucket is handed out. */
    public static void bucketFill(Item full, EntityPlayer player) {
        if (!OFF && !player.worldObj.isRemote) {
            on(player, full == Items.lava_bucket ? "item.bucket.fill_lava" : "item.bucket.fill", 1.0F, 1.0F);
        }
    }

    /** ItemBucket.tryPlaceContainedLiquid returning true; water in the Nether fizzes instead, as in both versions. */
    public static void bucketEmpty(Item bucket, World world, int x, int y, int z) {
        if (OFF || world.isRemote || bucket == Items.bucket) {
            return;
        }
        boolean lava = bucket == Items.lava_bucket;
        if (world.provider.isHellWorld && !lava) {
            return;
        }
        at(world, x + 0.5D, y + 0.5D, z + 0.5D, lava ? "item.bucket.empty_lava" : "item.bucket.empty", 1.0F, 1.0F);
    }

    public static void cauldronBucket(World world, int x, int y, int z) {
        if (!OFF && !world.isRemote) {
            at(world, x + 0.5D, y + 0.5D, z + 0.5D, "item.bucket.empty", 1.0F, 1.0F);
        }
    }

    public static void bottleFill(EntityPlayer player) {
        if (OFF || player.worldObj.isRemote) {
            return;
        }
        long now = player.worldObj.getTotalWorldTime();
        Long before = BOTTLE.get(player);
        if (before != null && before.longValue() == now) {
            return;
        }
        BOTTLE.put(player, now);
        at(player.worldObj, player.posX, player.posY, player.posZ, "item.bottle.fill", 1.0F, 1.0F);
    }

    /** ItemArmor.onItemRightClick: the piece goes on when its slot is empty (EntityLivingBase.playEquipSound). */
    public static void armorRightClick(ItemStack stack, EntityPlayer player) {
        if (OFF || player.worldObj.isRemote || stack == null || !(stack.getItem() instanceof ItemArmor)) {
            return;
        }
        int slot = EntityLiving.getArmorPosition(stack) - 1;
        if (slot < 0 || player.getCurrentArmor(slot) != null) {
            return;
        }
        on(player, equipSound(stack), 1.0F, 1.0F);
    }

    public static String equipSound(ItemStack stack) {
        if (stack.getItem() == Elytra112.elytra) {
            return "item.armor.equip_elytra";
        }
        if (!(stack.getItem() instanceof ItemArmor)) {
            return "item.armor.equip_generic";
        }
        switch (((ItemArmor) stack.getItem()).getArmorMaterial()) {
            case CLOTH:
                return "item.armor.equip_leather";
            case CHAIN:
                return "item.armor.equip_chain";
            case IRON:
                return "item.armor.equip_iron";
            case GOLD:
                return "item.armor.equip_gold";
            case DIAMOND:
                return "item.armor.equip_diamond";
            default:
                return "item.armor.equip_generic";
        }
    }

    public static void hangingBroken(Entity e) {
        if (!OFF && !e.worldObj.isRemote) {
            on(e, e instanceof EntityItemFrame ? "entity.itemframe.break" : e instanceof EntityPainting ? "entity.painting.break" : "entity.leashknot.break", 1.0F, 1.0F);
        }
    }

    public static void hangingPlaced(Entity e) {
        if (!OFF && e != null && !e.worldObj.isRemote) {
            on(e, e instanceof EntityItemFrame ? "entity.itemframe.place" : e instanceof EntityPainting ? "entity.painting.place" : "entity.leashknot.place", 1.0F, 1.0F);
        }
    }

    public static void frameAdd(Entity frame) {
        if (!OFF && !frame.worldObj.isRemote) {
            on(frame, "entity.itemframe.add_item", 1.0F, 1.0F);
        }
    }

    public static void frameRotate(Entity frame) {
        if (!OFF && !frame.worldObj.isRemote) {
            on(frame, "entity.itemframe.rotate_item", 1.0F, 1.0F);
        }
    }

    public static void frameRemove(Entity frame) {
        if (!OFF && !frame.worldObj.isRemote) {
            on(frame, "entity.itemframe.remove_item", 1.0F, 1.0F);
        }
    }

    /** TileEntityBrewingStand.brewPotions (1.12: playEvent 1035). */
    public static void brewed(TileEntity te) {
        if (!OFF && te.getWorldObj() != null && !te.getWorldObj().isRemote) {
            at(te.getWorldObj(), te.xCoord + 0.5D, te.yCoord + 0.5D, te.zCoord + 0.5D, "block.brewing_stand.brew", 1.0F, 1.0F);
        }
    }

    /** BlockFurnace.randomDisplayTick of a lit furnace: one time in ten, at the bottom of the block. */
    public static void furnaceTick(Block block, World world, int x, int y, int z, Random rand) {
        if (!OFF && block == Blocks.lit_furnace && rand.nextDouble() < 0.1D) {
            last = "block.furnace.fire_crackle";
            world.playSound(x + 0.5D, y, z + 0.5D, P + "block.furnace.fire_crackle", 1.0F, 1.0F, false);
        }
    }

    /** ContainerEnchantment.enchantItem returning true. */
    public static void enchanted(World world, int x, int y, int z) {
        if (!OFF && world != null && !world.isRemote) {
            at(world, x + 0.5D, y + 0.5D, z + 0.5D, "block.enchantment_table.use", 1.0F, world.rand.nextFloat() * 0.1F + 0.9F);
        }
    }

    /** ItemEnderEye.onItemUse: the eye goes into the frame. */
    public static void eyePlaced(World world, int x, int y, int z) {
        if (!OFF && !world.isRemote) {
            at(world, x + 0.5D, y + 0.5D, z + 0.5D, "block.end_portal_frame.fill", 1.0F, 1.0F);
        }
    }

    /**
     * The portal opens: 1.12 broadcasts event 1038 to every player, each hears it from two blocks away in the
     * direction of the portal (RenderGlobal.broadcastSound).
     */
    public static void portalOpened(World world, int x, int y, int z) {
        if (OFF || world.isRemote || world.getTotalWorldTime() == portalTick) {
            return;
        }
        portalTick = world.getTotalWorldTime();
        last = "block.end_portal.spawn";
        for (Object o : MinecraftServer.getServer().getConfigurationManager().playerEntityList) {
            EntityPlayerMP p = (EntityPlayerMP) o;
            double dx = x + 0.5D - p.posX;
            double dy = y + 0.5D - p.posY;
            double dz = z + 0.5D - p.posZ;
            double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double sx = p.posX;
            double sy = p.posY;
            double sz = p.posZ;
            if (d > 0.0D) {
                sx += dx / d * 2.0D;
                sy += dy / d * 2.0D;
                sz += dz / d * 2.0D;
            }
            p.playerNetServerHandler.sendPacket(new S29PacketSoundEffect(P + "block.end_portal.spawn", sx, sy, sz, 1.0F, 1.0F));
        }
    }

    public static void eyeDeath(Entity eye) {
        if (!OFF && !eye.worldObj.isRemote) {
            on(eye, "entity.endereye.death", 1.0F, 1.0F);
        }
    }

    public static void bobberRetrieve(EntityPlayer player) {
        if (!OFF && !player.worldObj.isRemote) {
            at(player.worldObj, player.posX, player.posY, player.posZ, "entity.bobber.retrieve", 1.0F, 0.4F / (RAND.nextFloat() * 0.4F + 0.8F));
        }
    }

    /** End112.BlockChorusFlower: 1.12 playEvent 1033 (grown) and 1034 (dead). */
    public static void chorus(World world, int x, int y, int z, boolean grown) {
        if (!OFF && !world.isRemote) {
            at(world, x + 0.5D, y + 0.5D, z + 0.5D, grown ? "block.chorus_flower.grow" : "block.chorus_flower.death", 1.0F, 1.0F);
        }
    }

    /** The 1.12 event for a 1.7.10 entity sound, or null to keep it. */
    public static String rename(Entity e, String name) {
        if (name == null || name.indexOf(':') >= 0) {
            return null;
        }
        if (name.equals("damage.thorns")) {
            return "enchant.thorns.hit";
        }
        if (e instanceof EntityWitch && name.startsWith("mob.witch.")) {
            String s = name.substring(10);
            return "idle".equals(s) ? "entity.witch.ambient" : "hurt".equals(s) ? "entity.witch.hurt" : "death".equals(s) ? "entity.witch.death" : null;
        }
        if (e instanceof EntitySkeleton && ((EntitySkeleton) e).getSkeletonType() == 1 && name.startsWith("mob.skeleton.")) {
            String s = name.substring(13);
            return "say".equals(s) ? "entity.wither_skeleton.ambient" : "hurt".equals(s) ? "entity.wither_skeleton.hurt"
                : "death".equals(s) ? "entity.wither_skeleton.death" : "step".equals(s) ? "entity.wither_skeleton.step" : null;
        }
        if (e instanceof EntityZombie && !(e instanceof EntityHusk) && ((EntityZombie) e).isVillager() && name.startsWith("mob.zombie.")) {
            String s = name.substring(11);
            return "say".equals(s) ? "entity.zombie_villager.ambient" : "hurt".equals(s) ? "entity.zombie_villager.hurt" : "death".equals(s) ? "entity.zombie_villager.death" : null;
        }
        if (e instanceof EntityPlayer) {
            EntityPlayer p = (EntityPlayer) e;
            if ("game.player.hurt".equals(name)) {
                DamageSource source = LAST.remove(e);
                Long tick = LAST_TICK.remove(e);
                if (tick == null || tick.longValue() != p.worldObj.getTotalWorldTime()) {
                    source = null;
                }
                if (source == null && p.worldObj.isRemote) {
                    // the client is not told the source: the state that causes it
                    source = p.isBurning() && !p.isImmuneToFire() ? DamageSource.onFire : p.getAir() <= -20 ? DamageSource.drown : null;
                }
                return source == DamageSource.onFire ? "entity.player.hurt_on_fire" : source == DamageSource.drown ? "entity.player.hurt_drown" : null;
            }
            if ("random.bow".equals(name) && p.getHeldItem() != null) {
                Item held = p.getHeldItem().getItem();
                return held == Items.fishing_rod ? "entity.bobber.throw" : held == Items.ender_eye ? "entity.endereye.launch" : null;
            }
        }
        return null;
    }

    public static final class Events {
        @SubscribeEvent
        public void onSound(PlaySoundAtEntityEvent event) {
            String to = rename(event.entity, event.name);
            if (to != null) {
                last = to;
                event.name = P + to;
            }
        }

        @SubscribeEvent
        public void onAttack(LivingAttackEvent event) {
            if (event.entityLiving instanceof EntityPlayer) {
                LAST.put(event.entityLiving, event.source);
                LAST_TICK.put(event.entityLiving, event.entityLiving.worldObj.getTotalWorldTime());
            }
        }

        /** EntityCow.interact of 1.12: a bucket on a grown cow, creative too. */
        @SubscribeEvent
        public void onInteract(EntityInteractEvent event) {
            if (event.target instanceof EntityCow && !((EntityCow) event.target).isChild() && !event.entityPlayer.worldObj.isRemote) {
                ItemStack held = event.entityPlayer.getHeldItem();
                if (held != null && held.getItem() == Items.bucket) {
                    on(event.entityPlayer, "entity.cow.milk", 1.0F, 1.0F);
                }
            }
        }
    }
}
