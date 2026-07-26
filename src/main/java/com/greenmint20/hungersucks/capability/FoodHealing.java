/*
 * Copyright (c) greenmint20.
 */
package com.greenmint20.hungersucks.capability;

import com.greenmint20.hungersucks.config.ModConfig;
import com.greenmint20.hungersucks.init.ModTags;
import com.greenmint20.hungersucks.network.HSNetwork;
import com.greenmint20.hungersucks.network.SyncFoodHealingPacket;
import com.greenmint20.hungersucks.network.SyncNaturalRegenPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.HoneyBottleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Per-player heal-over-time state. This is the Forge-capability replacement for
 * the original's Cardinal-Components {@code FoodHealingComponent}.
 *
 * <p>The data object is deliberately player-less: ticking is driven from
 * {@code CommonEvents#onPlayerTick} which passes the owner, mirroring how the
 * original cached the player inside {@code HungerManager}.
 */
public class FoodHealing implements INBTSerializable<CompoundTag> {

    private int healAmount = 0;
    private int ticksPerHeal = 0;
    private int healTicks = 0;
    private int amountHealed = 0;

    /** Set whenever a field that the client HUD cares about changes. */
    private boolean dirty = false;

    /**
     * Last {@code naturalRegeneration} value pushed to this player's client, so
     * a live {@code /gamerule} change re-syncs the heal preview without a relog.
     */
    private Boolean lastNaturalRegen = null;

    // ---- queries used by mixins / HUD ----

    public boolean canEat() {
        return healAmount == 0;
    }

    public int getHealAmount() {
        return healAmount;
    }

    public int getAmountHealed() {
        return amountHealed;
    }

    public int getMaximumHealTicks() {
        return healAmount * ticksPerHeal;
    }

    // ---- client-sync application ----

    public void setClientState(int healAmount, int amountHealed) {
        this.healAmount = healAmount;
        this.amountHealed = amountHealed;
    }

    // ---- start of a heal-over-time (called when a Player eats) ----

    public void startHealing(Player player, ItemStack stack) {
        FoodProperties food = stack.getFoodProperties(player);
        if (food == null) {
            return;
        }
        // Under the Hunger effect food no longer heals you (you may still eat it
        // for its other effects — e.g. suspicious stew). Don't start a heal at
        // all, which also keeps the HUD heal preview and the food cooldown off.
        if (player.hasEffect(MobEffects.HUNGER)) {
            return;
        }
        int nutrition = food.getNutrition();
        if (nutrition > 0) {
            healAmount = nutrition;
            ticksPerHeal = getTicksPerHeal(nutrition, getEffectiveSaturation(stack, food));
            healTicks = 0;
            amountHealed = 0;
            dirty = true;

            // Put every food item on cooldown for the whole heal so the player
            // cannot eat again mid-heal no matter where the food came from. The
            // grey overlay is then scoped to the inventory/hotbar client-side
            // (see ItemCooldownsMixin). The one-food-at-a-time rule itself is
            // also enforced by PlayerMixin#canEat, so this is opt-out via config.
            applyFoodCooldown(player, getMaximumHealTicks());
        }
    }

    /**
     * Puts every food item on cooldown for {@code ticks}. Split out of
     * {@link #startHealing} so a relog can restore it — see
     * {@link #restoreCooldown}.
     */
    private static void applyFoodCooldown(Player player, int ticks) {
        if (ticks <= 0 || !ModConfig.foodCooldown.get()) {
            return;
        }
        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            if (item.isEdible()) {
                player.getCooldowns().addCooldown(item, ticks);
            }
        }
    }

    /**
     * Re-arms the food cooldown for whatever is left of an in-progress heal.
     * Called on login/respawn/dimension change.
     *
     * <p>{@code ItemCooldowns} is pure runtime state — it is never written to
     * player NBT — while the heal itself IS persisted. So a player who logs out
     * mid-heal comes back with the heal still running on the server but no
     * cooldown on their food, which is its own flavour of "food does nothing":
     * nothing greys out, the client happily starts the eat animation, and the
     * server refuses it because {@code canEat} is false for the rest of the
     * heal. Restoring the remainder keeps the two sides telling the same story.
     */
    public void restoreCooldown(Player player) {
        if (healAmount > 0) {
            applyFoodCooldown(player, Math.max(0, getMaximumHealTicks() - healTicks));
        }
    }

    // ---- per-tick server logic ----

    public void serverTick(Player player) {
        tickFoodHealing(player);

        if (player instanceof ServerPlayer sp) {
            // Live-sync naturalRegeneration: if it changed (e.g. /gamerule mid
            // game), push it so the heal preview updates without a relog.
            boolean naturalRegen = player.level().getGameRules().getBoolean(GameRules.RULE_NATURAL_REGENERATION);
            if (lastNaturalRegen == null || lastNaturalRegen != naturalRegen) {
                lastNaturalRegen = naturalRegen;
                HSNetwork.toPlayer(sp, new SyncNaturalRegenPacket(naturalRegen));
            }
            if (dirty) {
                HSNetwork.toPlayer(sp, new SyncFoodHealingPacket(healAmount, amountHealed));
                dirty = false;
            }
        }
    }

    private void tickFoodHealing(Player player) {
        if (healAmount > 0) {
            int tph = Math.max(1, ticksPerHeal);
            healTicks++;
            if (healTicks % tph == 0) {
                boolean naturalRegen = player.level().getGameRules().getBoolean(GameRules.RULE_NATURAL_REGENERATION);
                if (!player.hasEffect(MobEffects.HUNGER) && naturalRegen) {
                    player.heal(1);
                }
                amountHealed++;
                dirty = true;
            }
            if (healTicks >= getMaximumHealTicks()) {
                healAmount = ticksPerHeal = healTicks = amountHealed = 0;
                dirty = true;
            }
        }
    }

    // ---- shared maths (also used by the tooltip) ----

    public static int getMaximumHealTicks(ItemStack stack) {
        FoodProperties food = stack.getFoodProperties(null);
        if (food == null) {
            return 0;
        }
        int nutrition = food.getNutrition();
        return nutrition * getTicksPerHeal(nutrition, getEffectiveSaturation(stack, food));
    }

    /**
     * The heal <em>speed</em> is driven by the food's saturation. The original
     * mod's {@code getTicksPerHeal} takes the <b>actual saturation value</b>
     * ({@code nutrition × modifier × 2}); 1.20.1's {@link FoodProperties} only
     * exposes the <em>modifier</em>, so we reconstruct the real value here.
     *
     * <p>Honey is buffed to a 0.8 modifier (the original's HoneyBottleItemMixin
     * set nutrition 6 / modifier 0.8) and {@code #increased_saturation} food has
     * its saturation multiplied by 2.6 — both mirror the original.
     */
    private static float getEffectiveSaturation(ItemStack stack, FoodProperties food) {
        float modifier = food.getSaturationModifier();
        if (ModConfig.increaseHoneySaturation.get() && stack.getItem() instanceof HoneyBottleItem) {
            modifier = 0.8F;
        }
        float saturation = food.getNutrition() * modifier * 2F;
        if (stack.is(ModTags.INCREASED_SATURATION)) {
            saturation *= 2.6F;
        }
        return saturation;
    }

    private static int getTicksPerHeal(int nutrition, float saturation) {
        return (int) Mth.clamp(20F / (saturation / nutrition / 2F), 0, 60);
    }

    // ---- persistence / copy ----

    public void copyFrom(FoodHealing other) {
        this.healAmount = other.healAmount;
        this.ticksPerHeal = other.ticksPerHeal;
        this.healTicks = other.healTicks;
        this.amountHealed = other.amountHealed;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("HealAmount", healAmount);
        tag.putInt("TicksPerHeal", ticksPerHeal);
        tag.putInt("HealTicks", healTicks);
        tag.putInt("AmountHealed", amountHealed);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        healAmount = tag.getInt("HealAmount");
        ticksPerHeal = tag.getInt("TicksPerHeal");
        healTicks = tag.getInt("HealTicks");
        amountHealed = tag.getInt("AmountHealed");
    }
}
