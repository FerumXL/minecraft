package ru.codex.offhand1710;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.common.network.NetworkRegistry;

public final class OffhandNetwork {
    public final SimpleNetworkWrapper channel = NetworkRegistry.INSTANCE.newSimpleChannel(MC112Bridge.MOD_ID);

    public void register() {
        channel.registerMessage(OffhandMessages.SwapHandler.class, OffhandMessages.SwapRequest.class, 0, Side.SERVER);
        channel.registerMessage(OffhandMessages.UseHandler.class, OffhandMessages.UseRequest.class, 1, Side.SERVER);
    }
}

