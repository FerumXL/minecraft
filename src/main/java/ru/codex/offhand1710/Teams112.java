package ru.codex.offhand1710;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;

/**
 * nametagVisibility and collisionRule of 1.8 / 1.9 teams. The server keeps them in Rules112.TeamOptions and sends
 * them to every client (SPacketTeams of 1.12.2 carries them; S3EPacketTeams of 1.7.10 does not). Collisions follow
 * EntitySelectors.getTeamCollisionPredicate of 1.12.2, the name tag RenderLivingBase.canRenderName.
 * -Dmc112.teams17 turns both off.
 */
public final class Teams112 {
    /** What this client was told (team -> value); on a server the saved data is read instead. */
    public static final Map<String, String> CLIENT_NAMETAG = new java.util.concurrent.ConcurrentHashMap<String, String>();
    public static final Map<String, String> CLIENT_COLLISION = new java.util.concurrent.ConcurrentHashMap<String, String>();

    public Teams112() {
    }

    /** The option of the team of an entity, from the side the world is on; "always" without a team. */
    public static String option(World world, ScorePlayerTeam team, String option) {
        if (team == null || Rules112.TeamOptions.TEAMS_OFF) {
            return "always";
        }
        String v;
        if (world.isRemote) {
            v = (Rules112.TeamOptions.NAMETAG.equals(option) ? CLIENT_NAMETAG : CLIENT_COLLISION).get(team.getRegisteredName());
        } else {
            Rules112.TeamOptions t = Rules112.TeamOptions.get();
            v = (Rules112.TeamOptions.NAMETAG.equals(option) ? t.nametag : t.collision).get(team.getRegisteredName());
        }
        return v == null ? "always" : v;
    }

    /** The team of an entity: in 1.7.10 only players belong to one (by name). */
    public static ScorePlayerTeam team(Entity e) {
        return e instanceof EntityPlayer ? e.worldObj.getScoreboard().getPlayersTeam(e.getCommandSenderName()) : null;
    }

    /** EntitySelectors.getTeamCollisionPredicate of 1.12.2, applied to the list collideWithNearbyEntities found. */
    public static void filter(List<?> near, EntityLivingBase self) {
        if (Rules112.TeamOptions.TEAMS_OFF || near == null || near.isEmpty()) {
            return;
        }
        ScorePlayerTeam team = team(self);
        String rule = option(self.worldObj, team, Rules112.TeamOptions.COLLISION);
        for (Iterator<?> it = near.iterator(); it.hasNext();) {
            Entity e = (Entity) it.next();
            if (!collides(self, team, rule, e)) {
                it.remove();
            }
        }
    }

    static boolean collides(EntityLivingBase self, ScorePlayerTeam team, String rule, Entity e) {
        if ("never".equals(rule)) {
            return false;
        }
        ScorePlayerTeam other = team(e);
        String rule1 = option(self.worldObj, other, Rules112.TeamOptions.COLLISION);
        if ("never".equals(rule1)) {
            return false;
        }
        boolean same = team != null && team == other;
        // the names of 1.12.2: pushOwnTeam is HIDE_FOR_OWN_TEAM (no pushing inside the team), pushOtherTeams the reverse
        if (("pushOwnTeam".equals(rule) || "pushOwnTeam".equals(rule1)) && same) {
            return false;
        }
        return !"pushOtherTeams".equals(rule) && !"pushOtherTeams".equals(rule1) || same;
    }

    /** RenderLivingBase.canRenderName of 1.12.2, the team part: false hides the name tag of target for viewer. */
    public static boolean nameShown(EntityPlayer viewer, Entity target) {
        if (Rules112.TeamOptions.TEAMS_OFF || target == viewer) {
            return true;
        }
        ScorePlayerTeam team = team(target);
        if (team == null) {
            return true;
        }
        ScorePlayerTeam own = team(viewer);
        String v = option(target.worldObj, team, Rules112.TeamOptions.NAMETAG);
        if ("never".equals(v)) {
            return false;
        }
        if ("hideForOtherTeams".equals(v)) {
            return own == null || own == team;
        }
        if ("hideForOwnTeam".equals(v)) {
            return own == null || own != team;
        }
        return true;
    }

    // ---- sending ----

    public static Sync message() {
        Rules112.TeamOptions t = Rules112.TeamOptions.get();
        Sync s = new Sync();
        s.nametag.putAll(t.nametag);
        s.collision.putAll(t.collision);
        return s;
    }

    /** After a change: every player gets the options. */
    public static void broadcast() {
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null || server.getConfigurationManager() == null || Rules112.TeamOptions.TEAMS_OFF) {
            return;
        }
        MC112Bridge.NETWORK.channel.sendToAll(message());
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!Rules112.TeamOptions.TEAMS_OFF && event.player instanceof EntityPlayerMP) {
            MC112Bridge.NETWORK.channel.sendTo(message(), (EntityPlayerMP) event.player);
        }
    }

    /** Server to client: nametagVisibility and collisionRule of every team that is not "always". */
    public static final class Sync implements IMessage {
        public final Map<String, String> nametag = new HashMap<String, String>();
        public final Map<String, String> collision = new HashMap<String, String>();

        @Override
        public void fromBytes(ByteBuf buf) {
            read(buf, nametag);
            read(buf, collision);
        }

        @Override
        public void toBytes(ByteBuf buf) {
            write(buf, nametag);
            write(buf, collision);
        }

        private static void read(ByteBuf buf, Map<String, String> m) {
            int n = buf.readShort();
            for (int i = 0; i < n; i++) {
                m.put(ByteBufUtils.readUTF8String(buf), ByteBufUtils.readUTF8String(buf));
            }
        }

        private static void write(ByteBuf buf, Map<String, String> m) {
            buf.writeShort(m.size());
            for (Map.Entry<String, String> e : m.entrySet()) {
                ByteBufUtils.writeUTF8String(buf, e.getKey());
                ByteBufUtils.writeUTF8String(buf, e.getValue());
            }
        }

        /** On the client: replace what it knew. */
        public void apply() {
            synchronized (CLIENT_NAMETAG) {
                CLIENT_NAMETAG.clear();
                CLIENT_NAMETAG.putAll(nametag);
                CLIENT_COLLISION.clear();
                CLIENT_COLLISION.putAll(collision);
            }
        }
    }
}
