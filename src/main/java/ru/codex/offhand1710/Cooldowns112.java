package ru.codex.offhand1710;

import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

/**
 * CooldownTracker of 1.9 - 1.12.2: an ender pearl or a chorus fruit cannot be used again for 20 ticks, shown as a
 * white bar over the item in every slot (RenderItem.renderItemOverlayIntoGUI). The shield after an axe hit keeps the
 * cooldown it had before (ShieldMechanics). -Dmc112.cooldowns17 turns it off.
 */
public final class Cooldowns112 {
    public static final boolean OFF = System.getProperty("mc112.cooldowns17") != null;
    /**
     * Per side: Entity.equals of 1.7.10 compares entity ids, and in single player the client player has the id of the
     * server player, so one map would give both the same tracker.
     */
    private static final Map<EntityPlayer, Tracker> SERVER = new WeakHashMap<EntityPlayer, Tracker>();
    private static final Map<EntityPlayer, Tracker> CLIENT = new WeakHashMap<EntityPlayer, Tracker>();

    public Cooldowns112() {
    }

    /** CooldownTracker: a tick count and, per item, the ticks it was set and expires at. */
    public static final class Tracker {
        int ticks;
        final Map<Item, int[]> cooldowns = new HashMap<Item, int[]>();

        public synchronized float get(Item item, float partialTicks) {
            int[] c = cooldowns.get(item);
            if (c == null) {
                return 0.0F;
            }
            float total = (float) (c[1] - c[0]);
            float left = (float) c[1] - ((float) ticks + partialTicks);
            return Math.max(0.0F, Math.min(1.0F, left / total));
        }
    }

    public static synchronized Tracker tracker(EntityPlayer player) {
        Map<EntityPlayer, Tracker> map = player.worldObj != null && player.worldObj.isRemote ? CLIENT : SERVER;
        Tracker t = map.get(player);
        if (t == null) {
            t = new Tracker();
            map.put(player, t);
        }
        return t;
    }

    public static boolean has(EntityPlayer player, Item item) {
        return !OFF && player != null && item != null && tracker(player).get(item, 0.0F) > 0.0F;
    }

    public static float get(EntityPlayer player, Item item, float partialTicks) {
        return OFF || player == null || item == null ? 0.0F : tracker(player).get(item, partialTicks);
    }

    /** CooldownTracker.setCooldown; the server tells the client (SPacketCooldown). */
    public static void set(EntityPlayer player, Item item, int ticks) {
        if (OFF || player == null) {
            return;
        }
        Tracker t = tracker(player);
        synchronized (t) {
            if (ticks <= 0) {
                t.cooldowns.remove(item);
            } else {
                t.cooldowns.put(item, new int[] {t.ticks, t.ticks + ticks});
            }
        }
        if (player instanceof EntityPlayerMP && !player.worldObj.isRemote) {
            MC112Bridge.NETWORK.channel.sendTo(new Msg(Item.getIdFromItem(item), ticks), (EntityPlayerMP) player);
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.PlayerTickEvent event) {
        if (OFF || event.phase != TickEvent.Phase.END) {
            return;
        }
        Tracker t = tracker(event.player);
        synchronized (t) {
            t.ticks++;
            for (Iterator<int[]> it = t.cooldowns.values().iterator(); it.hasNext();) {
                if (it.next()[1] <= t.ticks) {
                    it.remove();
                }
            }
        }
    }

    /** PlayerInteractionManager.processRightClick / processRightClickBlock: an item on cooldown is not used. */
    @SubscribeEvent
    public void onInteract(PlayerInteractEvent event) {
        if (OFF || event.action == PlayerInteractEvent.Action.LEFT_CLICK_BLOCK) {
            return;
        }
        ItemStack held = event.entityPlayer.getHeldItem();
        if (held != null && has(event.entityPlayer, held.getItem())) {
            if (event.action == PlayerInteractEvent.Action.RIGHT_CLICK_AIR) {
                event.setCanceled(true);
            } else {
                event.useItem = Event.Result.DENY;
            }
        }
    }

    /** ItemEnderPearl.onItemRightClick of 1.12.2: 20 ticks after a throw. */
    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent event) {
        if (!OFF && !event.world.isRemote && event.entity instanceof EntityEnderPearl
            && ((EntityEnderPearl) event.entity).getThrower() instanceof EntityPlayer && event.entity.ticksExisted == 0) {
            set((EntityPlayer) ((EntityEnderPearl) event.entity).getThrower(), Items.ender_pearl, 20);
        }
    }

    /** SPacketCooldown: the item id and the ticks (0 clears it). */
    public static final class Msg implements IMessage {
        public int item;
        public int ticks;

        public Msg() {
        }

        Msg(int item, int ticks) {
            this.item = item;
            this.ticks = ticks;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            item = buf.readInt();
            ticks = buf.readInt();
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeInt(item);
            buf.writeInt(ticks);
        }
    }
}
