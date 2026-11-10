package ru.codex.offhand1710;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.common.network.NetworkRegistry;
import ru.codex.offhand1710.client.ClientParticleHandler;
import ru.codex.offhand1710.client.ClientSwingHandler;
import ru.codex.offhand1710.client.ClientSyncHandler;
import ru.codex.offhand1710.client.ClientUseResultHandler;

public final class OffhandNetwork {
    public final SimpleNetworkWrapper channel = NetworkRegistry.INSTANCE.newSimpleChannel(MC112Bridge.MOD_ID);

    /** Registered on both sides so each side can encode what it sends; handlers run only on their side. */
    public void register() {
        channel.registerMessage(OffhandMessages.SwapHandler.class, OffhandMessages.SwapRequest.class, 0, Side.SERVER);
        channel.registerMessage(OffhandMessages.UseHandler.class, OffhandMessages.UseRequest.class, 1, Side.SERVER);
        channel.registerMessage(ClientSyncHandler.class, OffhandMessages.Sync.class, 2, Side.CLIENT);
        channel.registerMessage(ClientUseResultHandler.class, OffhandMessages.UseResult.class, 3, Side.CLIENT);
        channel.registerMessage(OffhandMessages.CreativeSetHandler.class, OffhandMessages.CreativeSet.class, 4,
                Side.SERVER);
        channel.registerMessage(OffhandMessages.ReleaseHandler.class, OffhandMessages.Release.class, 5, Side.SERVER);
        channel.registerMessage(ClientSwingHandler.class, OffhandMessages.Swing.class, 6, Side.CLIENT);
        channel.registerMessage(ClientParticleHandler.class, OffhandMessages.Particle.class, 7, Side.CLIENT);
        channel.registerMessage(OffhandMessages.ElytraStartHandler.class, OffhandMessages.ElytraStart.class, 8, Side.SERVER);
        channel.registerMessage(ru.codex.offhand1710.client.ClientElytraHandler.class, OffhandMessages.ElytraState.class, 9,
                Side.CLIENT);
        channel.registerMessage(ru.codex.offhand1710.client.MoreMobsClient.LevitateHandler.class, OffhandMessages.Levitate.class, 10, Side.CLIENT);
        channel.registerMessage(ru.codex.offhand1710.client.TitleOverlay.Handler.class, Content121.TitleMessage.class, 11, Side.CLIENT);
        channel.registerMessage(Content122.ModeHandler.class, Content122.ModeMessage.class, 12, Side.SERVER);
        channel.registerMessage(Content122.HandHandler.class, Content122.HandMessage.class, 13, Side.SERVER);
        channel.registerMessage(Data112.FillHandler.class, Data112.FillMessage.class, 14, Side.SERVER);
        channel.registerMessage(ru.codex.offhand1710.client.DataClient.SyncHandler.class, Data112.Sync.class, 15, Side.CLIENT);
        channel.registerMessage(ru.codex.offhand1710.client.DataClient.ToastHandler.class, Data112.Toast.class, 16, Side.CLIENT);
    }
}
