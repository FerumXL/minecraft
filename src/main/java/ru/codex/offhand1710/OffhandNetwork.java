package ru.codex.offhand1710;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.common.network.NetworkRegistry;

public final class OffhandNetwork {
    public final SimpleNetworkWrapper channel = NetworkRegistry.INSTANCE.newSimpleChannel(MC112Bridge.MOD_ID);

    /**
     * Client handlers are named, not referenced: a dedicated server must not load their classes (they reach
     * net.minecraft.client). There only the id is registered, so the server can still encode the message.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void client(String handler, Class message, int id) {
        if (cpw.mods.fml.common.FMLCommonHandler.instance().getSide().isClient()) {
            try {
                channel.registerMessage((Class) Class.forName(handler, false, OffhandNetwork.class.getClassLoader()), message, id, Side.CLIENT);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(handler, e);
            }
            return;
        }
        try {
            java.lang.reflect.Field f = SimpleNetworkWrapper.class.getDeclaredField("packetCodec");
            f.setAccessible(true);
            ((cpw.mods.fml.common.network.simpleimpl.SimpleIndexedCodec) f.get(channel)).addDiscriminator(id, message);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("SimpleNetworkWrapper.packetCodec", e);
        }
    }

    /** Registered on both sides so each side can encode what it sends; handlers run only on their side. */
    public void register() {
        channel.registerMessage(OffhandMessages.SwapHandler.class, OffhandMessages.SwapRequest.class, 0, Side.SERVER);
        channel.registerMessage(OffhandMessages.UseHandler.class, OffhandMessages.UseRequest.class, 1, Side.SERVER);
        client("ru.codex.offhand1710.client.ClientSyncHandler", OffhandMessages.Sync.class, 2);
        client("ru.codex.offhand1710.client.ClientUseResultHandler", OffhandMessages.UseResult.class, 3);
        channel.registerMessage(OffhandMessages.CreativeSetHandler.class, OffhandMessages.CreativeSet.class, 4,
                Side.SERVER);
        channel.registerMessage(OffhandMessages.ReleaseHandler.class, OffhandMessages.Release.class, 5, Side.SERVER);
        client("ru.codex.offhand1710.client.ClientSwingHandler", OffhandMessages.Swing.class, 6);
        client("ru.codex.offhand1710.client.ClientParticleHandler", OffhandMessages.Particle.class, 7);
        channel.registerMessage(OffhandMessages.ElytraStartHandler.class, OffhandMessages.ElytraStart.class, 8, Side.SERVER);
        client("ru.codex.offhand1710.client.ClientElytraHandler", OffhandMessages.ElytraState.class, 9);
        client("ru.codex.offhand1710.client.MoreMobsClient$LevitateHandler", OffhandMessages.Levitate.class, 10);
        client("ru.codex.offhand1710.client.TitleOverlay$Handler", Content121.TitleMessage.class, 11);
        channel.registerMessage(Content122.ModeHandler.class, Content122.ModeMessage.class, 12, Side.SERVER);
        channel.registerMessage(Content122.HandHandler.class, Content122.HandMessage.class, 13, Side.SERVER);
        channel.registerMessage(Data112.FillHandler.class, Data112.FillMessage.class, 14, Side.SERVER);
        client("ru.codex.offhand1710.client.DataClient$SyncHandler", Data112.Sync.class, 15);
        client("ru.codex.offhand1710.client.DataClient$ToastHandler", Data112.Toast.class, 16);
        client("ru.codex.offhand1710.client.MoreMobsClient$ShoulderHandler", Shoulder112.Msg.class, 17);
        client("ru.codex.offhand1710.client.SpectatorClient$Handler", Spectator112.Msg.class, 18);
        channel.registerMessage(Spectator112.SpectateHandler.class, Spectator112.SpectateMsg.class, 19, Side.SERVER);
        channel.registerMessage(StructureBlock112.Handler.class, StructureBlock112.Msg.class, 20, Side.SERVER);
        channel.registerMessage(Spectator112.TeleportHandler.class, Spectator112.TeleportMsg.class, 21, Side.SERVER);
        client("ru.codex.offhand1710.client.RulesClient$Handler", Rules112.Debug.class, 22);
        client("ru.codex.offhand1710.client.Border112Client$Handler", Border112.Msg.class, 23);
    }
}
