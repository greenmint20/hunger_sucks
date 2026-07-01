/*
 * Copyright (c) greenmint20.
 */
package com.greenmint20.hungersucks.network;

import com.greenmint20.hungersucks.client.ClientState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server&rarr;client. Tells the client whether {@code naturalRegeneration} is on
 * so the HUD can hide the heal preview when food cannot heal.
 */
public final class SyncNaturalRegenPacket {

    private final boolean value;

    public SyncNaturalRegenPacket(boolean value) {
        this.value = value;
    }

    public static void encode(SyncNaturalRegenPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.value);
    }

    public static SyncNaturalRegenPacket decode(FriendlyByteBuf buf) {
        return new SyncNaturalRegenPacket(buf.readBoolean());
    }

    public static void handle(SyncNaturalRegenPacket msg, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () ->
                () -> ClientState.setNaturalRegen(msg.value)));
        ctx.setPacketHandled(true);
    }
}
