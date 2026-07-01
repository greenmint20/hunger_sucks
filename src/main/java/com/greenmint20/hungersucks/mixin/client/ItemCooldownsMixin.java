/*
 * Copyright (c) greenmint20.
 */
package com.greenmint20.hungersucks.mixin.client;

import com.greenmint20.hungersucks.client.ClientEvents;
import com.greenmint20.hungersucks.client.ClientState;
import com.greenmint20.hungersucks.config.ModConfig;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Client-only: limits where the food heal cooldown's grey "clock" sweep is drawn.
 *
 * <p>While a heal is in progress we put every edible item on the player's vanilla
 * cooldown so eating is blocked regardless of which stack they grab. But {@code
 * getCooldownPercent} is queried by item-decoration rendering <em>everywhere</em>
 * — the creative inventory, JEI, other mods' ingredient lists — which made all the
 * food on screen appear to recharge. Here we report "no cooldown" for edible items
 * unless we're looking at the hotbar or the survival inventory, so the gameplay
 * lock stays intact while the visual is confined to the player's own food.
 *
 * <p>The suppression is gated on an <em>active heal</em> ({@code
 * ClientState.healAmount > 0}) and on the feature being enabled, so that when no
 * heal is running we don't touch anything — an item's own vanilla cooldown (e.g.
 * a chorus fruit's teleport cooldown) then renders normally everywhere.
 *
 * @see ClientEvents#shouldShowFoodCooldown()
 */
@Mixin(ItemCooldowns.class)
public class ItemCooldownsMixin {

    @Inject(method = "getCooldownPercent", at = @At("HEAD"), cancellable = true)
    private void hungersucks$hideFoodCooldownOutsideInventory(Item item, float partialTick,
                                                              CallbackInfoReturnable<Float> cir) {
        if (item.isEdible()
                && ModConfig.foodCooldown.get()
                && ClientState.healAmount > 0
                && !ClientEvents.shouldShowFoodCooldown()) {
            cir.setReturnValue(0F);
        }
    }
}
