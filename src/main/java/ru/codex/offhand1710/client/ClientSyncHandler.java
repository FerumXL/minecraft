package ru.codex.offhand1710.client;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import ru.codex.offhand1710.OffhandMessages;

public final class ClientSyncHandler implements IMessageHandler<OffhandMessages.Sync, IMessage> {
    @Override
    public IMessage onMessage(OffhandMessages.Sync message, MessageContext context) {
        ClientProxy.scheduleOffhandSync(message.entityId, message.stack);
        return null;
    }
}

