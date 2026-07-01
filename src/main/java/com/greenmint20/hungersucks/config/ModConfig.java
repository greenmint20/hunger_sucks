/*
 * Copyright (c) greenmint20.
 */
package com.greenmint20.hungersucks.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Forge replacement for the original's MidnightConfig.
 *
 * <p>Gameplay-affecting toggles must agree between client and server, so they
 * live in the COMMON spec. Purely visual toggles live in the CLIENT spec so each
 * player can set them independently.
 */
public final class ModConfig {

    // ---- COMMON (gameplay) ----
    public static final ForgeConfigSpec COMMON_SPEC;
    public static ForgeConfigSpec.BooleanValue foodCooldown;
    public static ForgeConfigSpec.BooleanValue disableSprinting;
    public static ForgeConfigSpec.BooleanValue fasterFluidConsumption;
    public static ForgeConfigSpec.BooleanValue increaseHoneySaturation;

    // ---- CLIENT (visual) ----
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static ForgeConfigSpec.BooleanValue displayHealthGained;
    public static ForgeConfigSpec.BooleanValue moveArmorBar;

    static {
        Pair<Common, ForgeConfigSpec> common = new ForgeConfigSpec.Builder().configure(Common::new);
        COMMON_SPEC = common.getRight();

        Pair<Client, ForgeConfigSpec> client = new ForgeConfigSpec.Builder().configure(Client::new);
        CLIENT_SPEC = client.getRight();
    }

    private ModConfig() {}

    private static final class Common {
        Common(ForgeConfigSpec.Builder b) {
            b.push("gameplay");
            foodCooldown = b
                    .comment("While a food heal is in progress, put every edible item on cooldown (overriding the vanilla cooldown) so you cannot eat again until it finishes. The grey cooldown sweep is only shown in your inventory and hotbar.")
                    .define("foodCooldown", true);
            disableSprinting = b
                    .comment("Prevents the player from sprinting (off by default).")
                    .define("disableSprinting", false);
            fasterFluidConsumption = b
                    .comment("Honey, stews, milk and potions are consumed twice as fast.")
                    .define("fasterFluidConsumption", true);
            increaseHoneySaturation = b
                    .comment("Gives Honey Bottles a much greater saturation modifier (heals faster).")
                    .define("increaseHoneySaturation", true);
            b.pop();
        }
    }

    private static final class Client {
        Client(ForgeConfigSpec.Builder b) {
            b.push("client");
            displayHealthGained = b
                    .comment("Show the health-gain overlay on the health bar and the heal tooltip on food items.")
                    .define("displayHealthGained", true);
            moveArmorBar = b
                    .comment("Move the armor bar to where the hunger bar used to be (bottom-right).")
                    .define("moveArmorBar", true);
            b.pop();
        }
    }
}
