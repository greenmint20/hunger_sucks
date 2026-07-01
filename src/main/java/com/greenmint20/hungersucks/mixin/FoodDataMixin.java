/*
 * Copyright (c) greenmint20.
 */
package com.greenmint20.hungersucks.mixin;

import com.greenmint20.hungersucks.capability.FoodHealingCapability;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Neutralises the vanilla hunger system and reroutes eating into the
 * {@link com.greenmint20.hungersucks.capability.FoodHealing} heal-over-time.
 *
 * <p>Three injection points, all verified against 1.20.1 Forge official mappings:
 * <ul>
 *   <li>{@code tick(Player)} — peg food/saturation full and exhaustion to zero,
 *       then cancel so vanilla regen/starvation never runs.</li>
 *   <li>{@code eat(Item, ItemStack, LivingEntity)} — the 3-arg Forge overload that
 *       {@code Player#eat} actually calls. Start the heal-over-time and cancel,
 *       which also prevents the internal {@code eat(int,float)} call.</li>
 *   <li>{@code eat(int, float)} — only the Saturation effect still reaches this
 *       directly; turn it into an instant heal.</li>
 * </ul>
 */
@Mixin(FoodData.class)
public abstract class FoodDataMixin {

    // Shadow the raw vanilla fields rather than the setters: setSaturation /
    // setExhaustion may be Forge-added (not in searge mappings), which would
    // break refmap remapping. Private vanilla fields are always SRG-mapped.
    @Shadow private int foodLevel;
    @Shadow private float saturationLevel;
    @Shadow private float exhaustionLevel;

    /** Cached owner; {@code eat(int,float)} has no player parameter. */
    @Unique
    private Player hungersucks$player;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void hungersucks$replaceHunger(Player player, CallbackInfo ci) {
        hungersucks$player = player;
        this.foodLevel = 20;
        this.saturationLevel = 20F;
        this.exhaustionLevel = 0F;
        ci.cancel();
    }

    // remap = false: this 3-arg overload is Forge-added, so it is NOT in the
    // searge mappings. Forge keeps its name ("eat") at runtime and modern Forge
    // runtime class names are already the official names, so the literal target
    // matches without remapping. (The AP errors with the default remap = true:
    // "Unable to locate obfuscation mapping for @Inject target eat".)
    @Inject(
            method = "eat(Lnet/minecraft/world/item/Item;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false)
    private void hungersucks$startHealing(Item item, ItemStack stack, LivingEntity entity, CallbackInfo ci) {
        if (entity instanceof Player player && !player.level().isClientSide && item.isEdible()) {
            FoodHealingCapability.get(player).ifPresent(fh -> fh.startHealing(player, stack));
        }
        ci.cancel();
    }

    @Inject(method = "eat(IF)V", at = @At("HEAD"), cancellable = true)
    private void hungersucks$saturationInstantHeal(int nutrition, float saturationModifier, CallbackInfo ci) {
        Player player = hungersucks$player;
        if (player != null && !player.level().isClientSide) {
            player.heal(nutrition);
        }
        ci.cancel();
    }
}
