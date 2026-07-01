/*
 * Hunger Sucks — a Forge 1.20.1 port of MoriyaShiine's Hearty Meals.
 * Developer: greenmint20.
 */
package com.greenmint20.hungersucks;

import com.greenmint20.hungersucks.capability.FoodHealingCapability;
import com.greenmint20.hungersucks.client.ClientEvents;
import com.greenmint20.hungersucks.config.ModConfig;
import com.greenmint20.hungersucks.event.CommonEvents;
import com.greenmint20.hungersucks.network.HSNetwork;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Main entry point. Wires up the deferred registers, capability lifecycle, the
 * network channel, and the Forge-bus event handlers.
 *
 * <p>The heavy lifting lives elsewhere: {@link com.greenmint20.hungersucks.mixin.FoodDataMixin}
 * neutralises vanilla hunger, {@link FoodHealingCapability} stores the per-player
 * heal-over-time state, and {@link CommonEvents}/{@link ClientEvents} translate the
 * Fabric event hooks of the original mod into Forge ones.
 */
@Mod(HungerSucks.MOD_ID)
public final class HungerSucks {

    public static final String MOD_ID = "hungersucks";

    public HungerSucks() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Config (gameplay flags = COMMON, visual flags = CLIENT).
        ModLoadingContext ctx = ModLoadingContext.get();
        ctx.registerConfig(Type.COMMON, ModConfig.COMMON_SPEC);
        ctx.registerConfig(Type.CLIENT, ModConfig.CLIENT_SPEC);

        // Capability + network.
        FoodHealingCapability.register(modBus);
        HSNetwork.register();

        // Common (both-sides) Forge-bus handlers.
        CommonEvents.register();

        // Client-only Forge-bus handlers.
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientEvents.register();
        }
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
