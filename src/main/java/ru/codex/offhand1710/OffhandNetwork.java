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
    }
}
