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
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;

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

    private static final Logger LOGGER = LogUtils.getLogger();
    /** One warning per player object — see {@link #get}. */
    private static final Set<Player> WARNED = Collections.newSetFromMap(new WeakHashMap<>());

    /**
     * The player's heal state, with a revive-and-retry for the failure mode that
     * makes food silently stop healing.
     *
     * <p>Forge's {@code CapabilityProvider#getCapability} compiles to
     * {@code !valid || disp == null ? LazyOptional.empty() : disp.getCapability(...)}
     * (javap-verified against forge-1.20.1-47.4.10). That {@code valid} flag is
     * the whole problem: once {@code invalidateCaps()} has run on a player and
     * nothing has called {@code reviveCaps()}, this returns empty <em>without
     * ever reaching our provider</em> — so the recreate-on-access workaround
     * inside {@link FoodHealingProvider#getCapability} can never fire for it,
     * because that code is downstream of the flag.
     *
     * <p>From then on every hook in the mod no-ops in exactly the way that hides
     * the fault: {@code startHealing} is skipped while {@code FoodDataMixin}
     * cancels vanilla eating anyway (food consumed, nothing healed), the server
     * tick is skipped, and {@code canEat} falls back to {@code orElse(true)} so
     * you can keep eating into the void. That is the reported "sometimes food
     * stops healing", and it lasts until the player reconnects because nothing
     * ever puts {@code valid} back.
     *
     * <p>So: if a live (not-removed) player resolves empty, revive and retry
     * once. The {@link FoodHealing} data lives on the provider and is not
     * destroyed by an invalidation, so the retry returns the real state rather
     * than a blank one. The warning is logged once per player object, so the
     * next report arrives with proof in the log instead of a hypothesis.
     */
    public static Optional<FoodHealing> get(Player player) {
        if (player == null) {
            return Optional.empty();
        }
        Optional<FoodHealing> resolved = player.getCapability(FOOD_HEALING).resolve();
        if (resolved.isPresent() || player.isRemoved()) {
            return resolved;
        }
        player.reviveCaps();
        resolved = player.getCapability(FOOD_HEALING).resolve();
        if (resolved.isPresent() && WARNED.add(player)) {
            LOGGER.warn("[hungersucks] the food-healing capability was invalidated on live player {}"
                    + " — food would have stopped healing for them; revived it.",
                    player.getGameProfile().getName());
        }
        return resolved;
    }
}
