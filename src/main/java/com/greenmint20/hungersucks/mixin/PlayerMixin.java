/*
 * Copyright (c) greenmint20.
 */
package com.greenmint20.hungersucks.mixin;

import com.greenmint20.hungersucks.capability.FoodHealingCapability;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * With hunger removed, {@code needsFood()} is always false, so vanilla would
 * never let you eat. Re-base "can I eat?" on missing health instead — and block
 * eating while a heal is already in progress.
 *
 * <p>Always-edible foods (the {@code ignoreHunger} path, e.g. golden apples)
 * stay eatable, matching the original. While the Hunger effect is active you
 * may also eat at any time (full health, mid-heal) — food won't heal you then,
 * but you can still consume it for its other effects (e.g. suspicious stew).
 */
@Mixin(Player.class)
public abstract class PlayerMixin {

    @Inject(method = "canEat", at = @At("HEAD"), cancellable = true)
    private void hungersucks$canEat(boolean ignoreHunger, CallbackInfoReturnable<Boolean> cir) {
        Player self = (Player) (Object) this;
        boolean canHealFromFood =
                Mth.ceil(self.getHealth()) < self.getMaxHealth()
                        && FoodHealingCapability.get(self).map(fh -> fh.canEat()).orElse(true);
        boolean result = self.getAbilities().invulnerable
                || ignoreHunger
                || self.hasEffect(MobEffects.HUNGER)
                || canHealFromFood;
        cir.setReturnValue(result);
    }
}
