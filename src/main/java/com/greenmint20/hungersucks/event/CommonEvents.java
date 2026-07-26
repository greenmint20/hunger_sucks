/*
 * Copyright (c) greenmint20.
 */
package com.greenmint20.hungersucks.event;

import com.greenmint20.hungersucks.capability.FoodHealingCapability;
import com.greenmint20.hungersucks.config.ModConfig;
import com.greenmint20.hungersucks.network.ForceDisableSprintingPacket;
import com.greenmint20.hungersucks.network.HSNetwork;
import com.greenmint20.hungersucks.network.SyncFoodHealingPacket;
import com.greenmint20.hungersucks.network.SyncNaturalRegenPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowlFoodItem;
import net.minecraft.world.item.HoneyBottleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.MilkBucketItem;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.SuspiciousStewItem;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Forge-bus handlers shared by both sides. Translates the original's Fabric
 * event hooks (server-join sync, stop-sleeping bed heal, item use-time) into
 * Forge equivalents.
 */
public final class CommonEvents {

    private CommonEvents() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.register(CommonEvents.class);
    }

    /** Drives the per-player heal-over-time on the server. */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        if (player.level().isClientSide) {
            return;
        }
        FoodHealingCapability.get(player).ifPresent(fh -> fh.serverTick(player));
    }

    /** Honey, stews, milk and potions are consumed twice as fast. */
    @SubscribeEvent
    public static void onUseItemStart(LivingEntityUseItemEvent.Start event) {
        if (!ModConfig.fasterFluidConsumption.get()) {
            return;
        }
        Item item = event.getItem().getItem();
        if (item instanceof HoneyBottleItem || item instanceof MilkBucketItem
                || item instanceof PotionItem || item instanceof BowlFoodItem
                || item instanceof SuspiciousStewItem) {
            event.setDuration(Math.max(1, event.getDuration() / 2));
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            syncAll(sp);
            if (ModConfig.disableSprinting.get() && !sp.server.isSingleplayer()) {
                HSNetwork.toPlayer(sp, new ForceDisableSprintingPacket());
            }
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            syncAll(sp);
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            syncAll(sp);
        }
    }

    private static void syncAll(ServerPlayer sp) {
        boolean naturalRegen = sp.level().getGameRules().getBoolean(GameRules.RULE_NATURAL_REGENERATION);
        HSNetwork.toPlayer(sp, new SyncNaturalRegenPacket(naturalRegen));
        FoodHealingCapability.get(sp).ifPresent(fh -> {
            HSNetwork.toPlayer(sp, new SyncFoodHealingPacket(fh.getHealAmount(), fh.getAmountHealed()));
            // Cooldowns are runtime-only state while the heal itself is persisted,
            // so a player who left mid-heal comes back able to click food that the
            // server will then refuse. Re-arm the remainder — see
            // FoodHealing#restoreCooldown.
            fh.restoreCooldown(sp);
        });
    }
}
