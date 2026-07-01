/*
 * Copyright (c) greenmint20.
 */
package com.greenmint20.hungersucks.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Attaches a {@link FoodHealing} instance to each player via
 * {@code AttachCapabilitiesEvent<Entity>}.
 */
public class FoodHealingProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {

    private final FoodHealing healing = new FoodHealing();
    private LazyOptional<FoodHealing> optional = LazyOptional.of(() -> healing);

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == FoodHealingCapability.FOOD_HEALING) {
            // Heal-over-time silently dies on death-heavy servers: a respawn/Clone
            // invalidates this LazyOptional, and reviveCaps() does NOT resurrect an
            // explicitly-invalidated optional. Every gameplay hook reads the
            // capability through resolve(), so once it's empty, startHealing and
            // the server tick are skipped (.ifPresent) while canEat still returns
            // true (orElse(true)) — i.e. food is eaten but never heals, until the
            // player reconnects. Recreate the wrapper on access (the underlying
            // FoodHealing data is preserved) so the capability can never get stuck
            // empty for a live player.
            if (!optional.isPresent()) {
                optional = LazyOptional.of(() -> healing);
            }
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    public void invalidate() {
        optional.invalidate();
    }

    @Override
    public CompoundTag serializeNBT() {
        return healing.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        healing.deserializeNBT(tag);
    }
}
