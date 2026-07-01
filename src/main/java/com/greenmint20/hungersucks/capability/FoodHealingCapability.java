/*
 * Copyright (c) greenmint20.
 */
package com.greenmint20.hungersucks.capability;

import com.greenmint20.hungersucks.HungerSucks;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;

/**
 * Capability glue for {@link FoodHealing}. Mirrors the working pattern used by
 * the RAI mod: {@code RegisterCapabilitiesEvent} on the mod bus,
 * {@code AttachCapabilitiesEvent}/{@code PlayerEvent.Clone} on the Forge bus.
 *
 * <p>Annotation-based {@code @SubscribeEvent} handles the generic
 * {@code AttachCapabilitiesEvent<Entity>} fine; only programmatic
 * {@code addListener} would need {@code addGenericListener}.
 */
@Mod.EventBusSubscriber(modid = HungerSucks.MOD_ID)
public final class FoodHealingCapability {

    public static final Capability<FoodHealing> FOOD_HEALING =
            CapabilityManager.get(new CapabilityToken<>() {});

    private static final ResourceLocation CAP_ID = HungerSucks.id("food_healing");

    private FoodHealingCapability() {}

    /** Registers the mod-bus listener from the mod constructor. */
    public static void register(IEventBus modBus) {
        modBus.addListener(FoodHealingCapability::onRegister);
    }

    private static void onRegister(RegisterCapabilitiesEvent event) {
        event.register(FoodHealing.class);
    }

    @SubscribeEvent
    public static void onAttach(AttachCapabilitiesEvent<Entity> event) {
        if (!(event.getObject() instanceof Player player) || player instanceof FakePlayer) {
            return;
        }
        FoodHealingProvider provider = new FoodHealingProvider();
        event.addCapability(CAP_ID, provider);
        event.addListener(provider::invalidate);
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        // Respawn drops the capability instance; copy it across. Idempotent for
        // dimension changes (where it survives but Clone still fires).
        Player oldP = event.getOriginal();
        Player newP = event.getEntity();
        oldP.reviveCaps();
        get(oldP).ifPresent(oldH -> get(newP).ifPresent(newH -> newH.copyFrom(oldH)));
        oldP.invalidateCaps();
    }

    public static Optional<FoodHealing> get(Player player) {
        if (player == null) {
            return Optional.empty();
        }
        return player.getCapability(FOOD_HEALING).resolve();
    }
}
