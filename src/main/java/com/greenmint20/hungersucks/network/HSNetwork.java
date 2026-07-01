/*
 * Copyright (c) greenmint20.
 */
package com.greenmint20.hungersucks.network;

import com.greenmint20.hungersucks.HungerSucks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Server&rarr;client channel. Replaces the original's Fabric custom payloads.
 */
public final class HSNetwork {

    private static final String PROTOCOL = "1";
    private static SimpleChannel CHANNEL;

    private HSNetwork() {}

    public static void register() {
        CHANNEL = NetworkRegistry.newSimpleChannel(
                HungerSucks.id("main"),
                () -> PROTOCOL,
                PROTOCOL::equals,
                PROTOCOL::equals
        );

        int id = 0;
        CHANNEL.messageBuilder(SyncFoodHealingPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncFoodHealingPacket::encode)
                .decoder(SyncFoodHealingPacket::decode)
                .consumerMainThread(SyncFoodHealingPacket::handle)
                .add();

        CHANNEL.messageBuilder(SyncNaturalRegenPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncNaturalRegenPacket::encode)
                .decoder(SyncNaturalRegenPacket::decode)
                .consumerMainThread(SyncNaturalRegenPacket::handle)
                .add();

        CHANNEL.messageBuilder(ForceDisableSprintingPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ForceDisableSprintingPacket::encode)
                .decoder(ForceDisableSprintingPacket::decode)
                .consumerMainThread(ForceDisableSprintingPacket::handle)
                .add();
    }

    public static void toPlayer(ServerPlayer player, Object msg) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }
}
