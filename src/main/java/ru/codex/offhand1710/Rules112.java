package ru.codex.offhand1710;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.server.CommandBlockLogic;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityHanging;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.rcon.RConConsoleSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.storage.DerivedWorldInfo;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.world.WorldEvent;

/**
 * The game rules that 1.8 - 1.12.2 added, with what each of them does. 1.7.10 keeps rules as plain strings, so the
 * rules are only added with the defaults of 1.12.2 (GameRules of the 1.12.2 jar); the behaviour is in the hooks below,
 * called from the transformer or from events. -Dmc112.gamerules17 leaves the rules of 1.7.10 alone.
 */
public final class Rules112 {
    public static final boolean OFF = System.getProperty("mc112.gamerules17") != null;

    /** Name and default, in the order of the 1.12.2 GameRules constructor. */
    public static final Map<String, String> ADDED = new LinkedHashMap<String, String>();
    /** Rules whose value is a number (ValueType.NUMERICAL_VALUE of 1.12.2); the rest of the built-in ones are booleans. */
    public static final java.util.Set<String> NUMBERS = new java.util.HashSet<String>();

    static {
        ADDED.put("doEntityDrops", "true");
        ADDED.put("logAdminCommands", "true");
        ADDED.put("showDeathMessages", "true");
        ADDED.put("randomTickSpeed", "3");
        ADDED.put("sendCommandFeedback", "true");
        ADDED.put("reducedDebugInfo", "false");
        ADDED.put("spectatorsGenerateChunks", "true");
        ADDED.put("spawnRadius", "10");
        ADDED.put("disableElytraMovementCheck", "false");
        ADDED.put("maxEntityCramming", "24");
        ADDED.put("doWeatherCycle", "true");
        ADDED.put("doLimitedCrafting", "false");
        ADDED.put("maxCommandChainLength", "65536");
        ADDED.put("announceAdvancements", "true");
        ADDED.put("gameLoopFunction", "-");
        NUMBERS.add("randomTickSpeed");
        NUMBERS.add("spawnRadius");
        NUMBERS.add("maxEntityCramming");
        NUMBERS.add("maxCommandChainLength");
    }

    /** DamageSource.CRAMMING of 1.12.2: "cramming", bypasses armour. */
    public static final DamageSource CRAMMING = new DamageSource("cramming") {
        {
            setDamageBypassesArmor();
        }
    };

    private Rules112() {
    }

    /** The rules of the server (the overworld's), like 1.12.2 reads them for most rules. */
    public static GameRules rules() {
        MinecraftServer s = MinecraftServer.getServer();
        return s == null || s.worldServers == null || s.worldServers.length == 0 || s.worldServers[0] == null ? null
            : s.worldServers[0].getGameRules();
    }

    public static boolean flag(World w, String name) {
        if (OFF) {
            return !"false".equals(ADDED.get(name)) && !"reducedDebugInfo".equals(name);
        }
        GameRules r = w == null ? rules() : w.getGameRules();
        if (r == null || !r.hasRule(name)) {
            return "true".equals(ADDED.get(name));
        }
        return r.getGameRuleBooleanValue(name);
    }

    public static int number(World w, String name) {
        GameRules r = w == null ? rules() : w.getGameRules();
        String v = OFF || r == null || !r.hasRule(name) ? ADDED.get(name) : r.getGameRuleStringValue(name);
        try {
            return Integer.parseInt(v.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    /** True for a boolean rule (the built-in ones of 1.7.10 and 1.12.2 except the numbers and gameLoopFunction). */
    public static boolean isBoolean(GameRules rules, String name) {
        if (NUMBERS.contains(name) || "gameLoopFunction".equals(name)) {
            return false;
        }
        if (ADDED.containsKey(name)) {
            return true;
        }
        for (String vanilla : new GameRules().getRules()) {
            if (vanilla.equals(name)) {
                return true;
            }
        }
        return false;
    }

    // ---- events ----

    public static final class Events {
        private boolean lastReduced;
        private boolean lastLimited;
        private int tick;

        @SubscribeEvent
        public void onWorld(WorldEvent.Load event) {
            GameRules r = event.world.getGameRules();
            for (Map.Entry<String, String> e : ADDED.entrySet()) {
                if (!r.hasRule(e.getKey())) {
                    r.addGameRule(e.getKey(), e.getValue());
                }
            }
        }

        /**
         * doWeatherCycle false: the rain and thunder clocks stand still (World.updateWeather of 1.12.2 skips them).
         * The clock is moved up one before the tick, so the countdown of 1.7.10 brings it back and never switches.
         */
        @SubscribeEvent
        public void onWorldTick(TickEvent.WorldTickEvent event) {
            if (event.phase != TickEvent.Phase.START || event.world.isRemote || event.world.provider.hasNoSky) {
                return;
            }
            WorldInfo info = event.world.getWorldInfo();
            if (info instanceof DerivedWorldInfo || flag(event.world, "doWeatherCycle")) {
                return;
            }
            if (info.getRainTime() > 0) {
                info.setRainTime(info.getRainTime() + 1);
            }
            if (info.getThunderTime() > 0) {
                info.setThunderTime(info.getThunderTime() + 1);
            }
        }

        /** reducedDebugInfo reaches every client when it changes (SPacketEntityStatus 22/23 of 1.12.2). */
        @SubscribeEvent
        public void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END || ++tick % 10 != 0 || rules() == null) {
                return;
            }
            boolean now = flag(null, "reducedDebugInfo");
            boolean limited = flag(null, "doLimitedCrafting");
            if (now != lastReduced || limited != lastLimited) {
                lastReduced = now;
                lastLimited = limited;
                MC112Bridge.NETWORK.channel.sendToAll(new Debug(now, limited));
            }
        }

        @SubscribeEvent
        public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.player instanceof EntityPlayerMP) {
                MC112Bridge.NETWORK.channel.sendTo(new Debug(flag(null, "reducedDebugInfo"), flag(null, "doLimitedCrafting")),
                    (EntityPlayerMP) event.player);
            }
        }

        /** EntityTameable.onDeath of 1.12.2: the owner hears how the pet died (showDeathMessages). */
        @SubscribeEvent
        public void onDeath(LivingDeathEvent event) {
            if (OFF || event.entity.worldObj.isRemote || !(event.entity instanceof EntityTameable)) {
                return;
            }
            EntityTameable pet = (EntityTameable) event.entity;
            if (flag(pet.worldObj, "showDeathMessages") && pet.getOwner() instanceof EntityPlayerMP) {
                ((EntityPlayerMP) pet.getOwner()).addChatMessage(pet.func_110142_aN().func_151521_b());
            }
        }
    }

    /** Server to client: the rules the client acts on (reducedDebugInfo, doLimitedCrafting). */
    public static final class Debug implements IMessage {
        public boolean reduced;
        public boolean limited;

        public Debug() {
        }

        public Debug(boolean reduced, boolean limited) {
            this.reduced = reduced;
            this.limited = limited;
        }

        @Override
        public void fromBytes(ByteBuf b) {
            reduced = b.readBoolean();
            limited = b.readBoolean();
        }

        @Override
        public void toBytes(ByteBuf b) {
            b.writeBoolean(reduced);
            b.writeBoolean(limited);
        }
    }

    // ---- NoAI (EntityLiving.isAIDisabled of 1.9 - 1.12.2) ----

    /** EntityLiving.readEntityFromNBT: NoAI kept beside the entity's data. */
    public static void readNoAI(net.minecraft.entity.EntityLiving e, net.minecraft.nbt.NBTTagCompound nbt) {
        if (nbt.hasKey("NoAI")) {
            e.getEntityData().setBoolean("NoAI", nbt.getBoolean("NoAI"));
        }
    }

    /** EntityLiving.writeEntityToNBT: written back under its 1.12.2 name. */
    public static void writeNoAI(net.minecraft.entity.EntityLiving e, net.minecraft.nbt.NBTTagCompound nbt) {
        if (e.getEntityData().getBoolean("NoAI")) {
            nbt.setBoolean("NoAI", true);
        }
    }

    /** updateAITasks / updateEntityActionState: a mob with NoAI thinks nothing (it still falls and burns). */
    public static boolean noAI(net.minecraft.entity.EntityLiving e) {
        return !OFF && e.getEntityData().getBoolean("NoAI");
    }

    // ---- hooks (Transformer) ----

    /** WorldServer.func_147456_g: blocks ticked at random in each 16-high section (randomTickSpeed, 3 in 1.7.10). */
    public static int randomTicks(WorldServer world) {
        return Math.max(0, number(world, "randomTickSpeed"));
    }

    /** EntityPlayerMP.onDeath: the death message goes to chat only with showDeathMessages. */
    public static void deathMessage(ServerConfigurationManager players, IChatComponent message, EntityPlayerMP dead) {
        if (!flag(dead.worldObj, "showDeathMessages")) {
            return;
        }
        net.minecraft.scoreboard.ScorePlayerTeam team = dead.worldObj.getScoreboard().getPlayersTeam(dead.getCommandSenderName());
        String vis = team == null ? "always" : TeamOptions.deathVisibility(team.getRegisteredName());
        if ("always".equals(vis)) {
            players.sendChatMsg(message);
        } else if ("hideForOtherTeams".equals(vis)) {
            // PlayerList.sendMessageToAllTeamMembers of 1.12.2: the team without the dead player
            for (Object o : team.getMembershipCollection()) {
                EntityPlayerMP p = players.func_152612_a((String) o);
                if (p != null && p != dead) {
                    p.addChatMessage(message);
                }
            }
        } else if ("hideForOwnTeam".equals(vis)) {
            // PlayerList.sendMessageToTeamOrAllPlayers of 1.12.2: everyone not on the team
            for (Object o : players.playerEntityList) {
                EntityPlayerMP p = (EntityPlayerMP) o;
                if (dead.worldObj.getScoreboard().getPlayersTeam(p.getCommandSenderName()) != team) {
                    p.addChatMessage(message);
                }
            }
        }
    }

    /**
     * Team options of 1.12.2 that ScorePlayerTeam of 1.7.10 has no field for, by team name, saved with the overworld:
     * deathMessageVisibility (-Dmc112.deathVis17 turns it off), nametagVisibility and collisionRule (-Dmc112.teams17).
     * The last two are sent to the clients (Teams112.Sync), as SPacketTeams of 1.12.2 carries them.
     */
    public static final class TeamOptions extends net.minecraft.world.WorldSavedData {
        public static final boolean OFF = System.getProperty("mc112.deathVis17") != null;
        public static final boolean TEAMS_OFF = System.getProperty("mc112.teams17") != null;
        public static final String KEY = "mc112teams";
        public static final String DEATH = "deathMessageVisibility";
        public static final String NAMETAG = "nametagVisibility";
        public static final String COLLISION = "collisionRule";
        /** Team.EnumVisible of 1.12.2. */
        public static final String[] VISIBLE = {"always", "never", "hideForOtherTeams", "hideForOwnTeam"};
        /** Team.CollisionRule of 1.12.2. */
        public static final String[] COLLISIONS = {"always", "never", "pushOtherTeams", "pushOwnTeam"};
        private static final String[] TAGS = {"DeathMessageVisibility", "NameTagVisibility", "CollisionRule"};
        public final Map<String, String> death = new java.util.HashMap<String, String>();
        public final Map<String, String> nametag = new java.util.HashMap<String, String>();
        public final Map<String, String> collision = new java.util.HashMap<String, String>();

        public TeamOptions(String key) {
            super(key);
        }

        private List<Map<String, String>> maps() {
            return java.util.Arrays.asList(death, nametag, collision);
        }

        @Override
        public void readFromNBT(net.minecraft.nbt.NBTTagCompound nbt) {
            for (int i = 0; i < 3; i++) {
                Map<String, String> m = maps().get(i);
                m.clear();
                net.minecraft.nbt.NBTTagCompound d = nbt.getCompoundTag(TAGS[i]);
                for (Object k : d.func_150296_c()) {
                    m.put((String) k, d.getString((String) k));
                }
            }
        }

        @Override
        public void writeToNBT(net.minecraft.nbt.NBTTagCompound nbt) {
            for (int i = 0; i < 3; i++) {
                net.minecraft.nbt.NBTTagCompound d = new net.minecraft.nbt.NBTTagCompound();
                for (Map.Entry<String, String> e : maps().get(i).entrySet()) {
                    d.setString(e.getKey(), e.getValue());
                }
                nbt.setTag(TAGS[i], d);
            }
        }

        public static TeamOptions get() {
            World w = MinecraftServer.getServer().worldServers[0];
            TeamOptions t = (TeamOptions) w.mapStorage.loadData(TeamOptions.class, KEY);
            if (t == null) {
                t = new TeamOptions(KEY);
                w.mapStorage.setData(KEY, t);
            }
            return t;
        }

        /** The option names "scoreboard teams option" takes here, with their values; null when it is not one of them. */
        public static String[] values(String option) {
            if (DEATH.equals(option)) {
                return OFF ? null : VISIBLE;
            }
            if (NAMETAG.equals(option)) {
                return TEAMS_OFF ? null : VISIBLE;
            }
            if (COLLISION.equals(option)) {
                return TEAMS_OFF ? null : COLLISIONS;
            }
            return null;
        }

        private Map<String, String> map(String option) {
            return DEATH.equals(option) ? death : NAMETAG.equals(option) ? nametag : collision;
        }

        public static String deathVisibility(String team) {
            if (OFF) {
                return "always";
            }
            String v = get().death.get(team);
            return v == null ? "always" : v;
        }

        public static void set(String team, String value) {
            set(DEATH, team, value);
        }

        public static void set(String option, String team, String value) {
            TeamOptions t = get();
            Map<String, String> m = t.map(option);
            if (value == null || "always".equals(value) ? m.remove(team) != null : !value.equals(m.put(team, value))) {
                t.markDirty();
                if (!DEATH.equals(option)) {
                    Teams112.broadcast();
                }
            }
        }

        /** A team added or removed: every option back to "always", as a new Team of 1.12.2. */
        public static void reset(String team) {
            for (String o : new String[] {DEATH, NAMETAG, COLLISION}) {
                set(o, team, null);
            }
        }
    }

    /**
     * EntityLivingBase.collideWithNearbyEntities of 1.12.2: more than maxEntityCramming - 1 pushable entities inside the
     * box (riders not counted) hurt it by 6 one tick in four. 1.7.10 found the list in a box 0.2 wider, so the list
     * is cut to the box itself here.
     */
    public static void cramming(List<?> near, EntityLivingBase self) {
        Teams112.filter(near, self);
        if (near == null || near.isEmpty() || self.worldObj.isRemote) {
            return;
        }
        int max = number(self.worldObj, "maxEntityCramming");
        if (max <= 0) {
            return;
        }
        int pushable = 0;
        int notRiding = 0;
        for (Object o : near) {
            Entity e = (Entity) o;
            if (e.canBePushed() && e.boundingBox != null && e.boundingBox.intersectsWith(self.boundingBox)) {
                pushable++;
                if (e.ridingEntity == null) {
                    notRiding++;
                }
            }
        }
        if (pushable > max - 1 && self.getRNG().nextInt(4) == 0 && notRiding > max - 1) {
            self.attackEntityFrom(CRAMMING, 6.0F);
        }
    }

    /** Entity.entityDropItem: boats, minecarts, item frames, paintings and falling blocks drop nothing without doEntityDrops. */
    public static boolean noDrop(Entity e) {
        return (e instanceof EntityBoat || e instanceof EntityMinecart || e instanceof EntityHanging || e instanceof EntityFallingBlock)
            && e.worldObj != null && !e.worldObj.isRemote && !flag(e.worldObj, "doEntityDrops");
    }

    /**
     * NetHandlerPlayServer.processPlayer: the "moved too quickly" limit. 1.12.2 allows 300 instead of 100 while gliding
     * and drops the check for gliders with disableElytraMovementCheck.
     */
    public static double speedLimit(NetHandlerPlayServer handler) {
        EntityPlayerMP p = handler.playerEntity;
        if (p == null || !OffhandInventory.get(p).elytraFlying) {
            return 100.0D;
        }
        return flag(p.worldObj, "disableElytraMovementCheck") ? Double.MAX_VALUE : 300.0D;
    }

    /**
     * WorldProvider.getRandomizedSpawnPoint: EntityPlayerMP of 1.12.2 spreads players spawning at the world spawn over
     * spawnRadius blocks each way, both ends counted (nextInt(r * 2 + 1) - r), outside adventure and in worlds with sky.
     * Null leaves the world type's own spread (other dimensions, mods).
     */
    public static ChunkCoordinates spawnPoint(WorldProvider provider) {
        World w = provider.worldObj;
        if (OFF || w == null || w.isRemote || provider.dimensionId != 0 || provider.getClass() != net.minecraft.world.WorldProviderSurface.class) {
            return null;
        }
        ChunkCoordinates c = new ChunkCoordinates(w.getSpawnPoint());
        if (!provider.hasNoSky && w.getWorldInfo().getGameType() != WorldSettings.GameType.ADVENTURE) {
            int r = Border112.spawnSpread(w, c, Math.max(0, number(w, "spawnRadius")));
            c.posX += w.rand.nextInt(r * 2 + 1) - r;
            c.posZ += w.rand.nextInt(r * 2 + 1) - r;
            c.posY = w.getTopSolidOrLiquidBlock(c.posX, c.posZ);
        }
        return c;
    }

    /** Content122: how many chain command blocks one chain may run. */
    public static int chainLength(World w) {
        return OFF ? 65536 : number(w, "maxCommandChainLength");
    }

    /** sendCommandFeedback of the sender (ICommandSender.sendCommandFeedback of 1.12.2). */
    private static boolean feedback(ICommandSender sender) {
        if (sender instanceof EntityPlayer || sender instanceof MinecraftServer) {
            return flag(null, "sendCommandFeedback");
        }
        if (sender instanceof CommandBlockLogic || sender != null && sender.getClass().getEnclosingClass() == Content120.CommandExecute.class) {
            // /execute of 1.12.2 wraps its sender withSendCommandFeedback(commandBlockOutput)
            return flag(null, "commandBlockOutput");
        }
        if (sender instanceof RConConsoleSource) {
            return true;
        }
        // entities and the other senders say nothing to the operators in 1.12.2
        return sender == null || !(sender instanceof Entity);
    }

    /**
     * ServerCommandManager.func_152372_a (notifyOperators) as CommandHandler.notifyListener of 1.12.2: the grey line
     * to the other operators goes out when the sender sends feedback, the server log gets it with logAdminCommands,
     * and the sender sees the result with sendCommandFeedback (commandBlockOutput for a command block).
     */
    public static boolean notifyOps(ICommandSender sender, ICommand command, int flags, String key, Object[] args) {
        if (OFF) {
            return false;
        }
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) {
            return false;
        }
        ChatComponentTranslation line = new ChatComponentTranslation("chat.type.admin", sender.getCommandSenderName(), new ChatComponentTranslation(key, args));
        line.getChatStyle().setColor(EnumChatFormatting.GRAY);
        line.getChatStyle().setItalic(Boolean.TRUE);
        if (feedback(sender)) {
            for (Object o : server.getConfigurationManager().playerEntityList) {
                EntityPlayer op = (EntityPlayer) o;
                if (op != sender && server.getConfigurationManager().func_152596_g(op.getGameProfile()) && command.canCommandSenderUseCommand(op)) {
                    boolean console = sender instanceof MinecraftServer && broadcastConsole(server);
                    boolean rcon = sender instanceof RConConsoleSource && server.func_152363_m();
                    if (console || rcon || !(sender instanceof RConConsoleSource) && !(sender instanceof MinecraftServer)) {
                        op.addChatMessage(line);
                    }
                }
            }
        }
        if (sender != server && flag(null, "logAdminCommands")) {
            server.addChatMessage(line);
        }
        boolean result = sender instanceof CommandBlockLogic ? flag(null, "commandBlockOutput") : flag(null, "sendCommandFeedback");
        if ((flags & 1) != 1 && result || sender instanceof MinecraftServer) {
            sender.addChatMessage(new ChatComponentTranslation(key, args));
        }
        return true;
    }

    /** broadcast-console-to-ops of 1.12.2 is on by default; 1.7.10 has no such setting. */
    private static boolean broadcastConsole(MinecraftServer server) {
        if (server instanceof net.minecraft.server.dedicated.DedicatedServer) {
            return ((net.minecraft.server.dedicated.DedicatedServer) server).getBooleanProperty("broadcast-console-to-ops", true);
        }
        return true;
    }
}
