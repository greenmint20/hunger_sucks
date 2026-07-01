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
 * Server&rarr;client marker. Sent on login when a dedicated server has
 * {@code disableSprinting} enabled, so the client honours it regardless of its
 * own local config.
 */
public final class ForceDisableSprintingPacket {

    public ForceDisableSprintingPacket() {}

    public static void encode(ForceDisableSprintingPacket msg, FriendlyByteBuf buf) {}

    public static ForceDisableSprintingPacket decode(FriendlyByteBuf buf) {
        return new ForceDisableSprintingPacket();
    }

    public static void handle(ForceDisableSprintingPacket msg, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () ->
                () -> ClientState.setForceDisableSprinting(true)));
        ctx.setPacketHandled(true);
    }
}
