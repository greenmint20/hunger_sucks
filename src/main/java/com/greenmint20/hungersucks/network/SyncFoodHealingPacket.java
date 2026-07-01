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
 * Server&rarr;client. Mirrors the authoritative heal-over-time state so the HUD
 * can draw the "currently healing" preview.
 */
public final class SyncFoodHealingPacket {

    private final int healAmount;
    private final int amountHealed;

    public SyncFoodHealingPacket(int healAmount, int amountHealed) {
        this.healAmount = healAmount;
        this.amountHealed = amountHealed;
    }

    public static void encode(SyncFoodHealingPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.healAmount);
        buf.writeVarInt(msg.amountHealed);
    }

    public static SyncFoodHealingPacket decode(FriendlyByteBuf buf) {
        return new SyncFoodHealingPacket(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(SyncFoodHealingPacket msg, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () ->
                () -> ClientState.setHealing(msg.healAmount, msg.amountHealed)));
        ctx.setPacketHandled(true);
    }
}
