/*
 * Copyright (c) greenmint20.
 */
package com.greenmint20.hungersucks.client;

import com.greenmint20.hungersucks.capability.FoodHealing;
import com.greenmint20.hungersucks.config.ModConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.text.DecimalFormat;

/**
 * Client-only Forge-bus handlers: hides the hunger bar, moves the armor bar,
 * draws the health-gain preview, adds the food heal tooltip, and enforces the
 * optional sprint lock.
 *
 * <p>Port note: the original achieved the HUD changes with deep {@code InGameHud}
 * mixins. On Forge these are expressed through {@code RenderGuiOverlayEvent}; the
 * health-gain preview is a faithful approximation of the original's pulsing
 * heart overlay.
 */
public final class ClientEvents {

    // 1.20.1 has no GUI sprite atlas (blitSprite); HUD icons are blitted from
    // icons.png with the classic hardcoded UVs.
    private static final ResourceLocation ICONS = new ResourceLocation("textures/gui/icons.png");
    private static final int ARMOR_EMPTY_U = 16, ARMOR_HALF_U = 25, ARMOR_FULL_U = 34, ARMOR_V = 9;

    private static int renderTicks = 0;

    private ClientEvents() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.register(ClientEvents.class);
    }

    // ---- HUD: hide hunger bar, move armor bar ----

    @SubscribeEvent
    public static void onRenderOverlayPre(RenderGuiOverlayEvent.Pre event) {
        ResourceLocation id = event.getOverlay().id();
        if (id.equals(VanillaGuiOverlay.FOOD_LEVEL.id())) {
            // Hunger is removed entirely.
            event.setCanceled(true);
        } else if (id.equals(VanillaGuiOverlay.ARMOR_LEVEL.id()) && ModConfig.moveArmorBar.get()) {
            event.setCanceled(true);
            drawArmorMoved(event.getGuiGraphics());
        } else if (id.equals(VanillaGuiOverlay.AIR_LEVEL.id())) {
            raiseAirAboveArmor();
        }
    }

    /**
     * Cancelling the hunger overlay frees its row, so Forge's air-bubble bar
     * drops to the bottom-right corner — exactly where the moved armor bar sits.
     * When armor is on screen, push the air bar up one row (10px, by reserving
     * the height the cancelled hunger bar would have) so the two don't overlap;
     * with no armor the bubbles stay at the bottom, as intended.
     */
    private static void raiseAirAboveArmor() {
        if (!ModConfig.moveArmorBar.get()) {
            return;
        }
        Player player = Minecraft.getInstance().player;
        if (player == null || player.isCreative() || player.isSpectator()
                || player.getArmorValue() <= 0) {
            return;
        }
        if (Minecraft.getInstance().gui instanceof ForgeGui forgeGui) {
            forgeGui.rightHeight += 10;
        }
    }

    @SubscribeEvent
    public static void onRenderOverlayPost(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay().id().equals(VanillaGuiOverlay.PLAYER_HEALTH.id())) {
            drawHealthGain(event.getGuiGraphics());
        }
    }

    private static void drawArmorMoved(GuiGraphics gui) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || player.isCreative() || player.isSpectator()) {
            return;
        }
        int armor = player.getArmorValue();
        if (armor <= 0) {
            return;
        }
        int width = gui.guiWidth();
        int height = gui.guiHeight();
        // +101px to the right of vanilla's armor anchor and pinned to the bottom
        // row — i.e. where the hunger bar used to be (matches the original).
        int left = width / 2 - 91 + 101;
        int top = height - 39;
        for (int j = 0; j < 10; j++) {
            int x = left + j * 8;
            if (j * 2 + 1 < armor) {
                gui.blit(ICONS, x, top, ARMOR_FULL_U, ARMOR_V, 9, 9);
            } else if (j * 2 + 1 == armor) {
                gui.blit(ICONS, x, top, ARMOR_HALF_U, ARMOR_V, 9, 9);
            } else {
                gui.blit(ICONS, x, top, ARMOR_EMPTY_U, ARMOR_V, 9, 9);
            }
        }
    }

    private static void drawHealthGain(GuiGraphics gui) {
        if (!ModConfig.displayHealthGained.get() || !ClientState.naturalRegen) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) {
            return;
        }
        // No survival health bar in creative/spectator, so don't paint a preview
        // over it (e.g. after taking damage in survival then switching to creative).
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        // Under the Hunger effect food cannot heal you (see FoodHealing), so
        // don't preview a heal that won't happen.
        if (player.hasEffect(MobEffects.HUNGER)) {
            return;
        }
        int health = Mth.ceil(player.getHealth());
        int maxHealth = Mth.ceil(player.getMaxHealth());
        if (health >= maxHealth) {
            return;
        }

        int toHeal;
        if (ClientState.healAmount > 0) {
            toHeal = ClientState.healAmount - ClientState.amountHealed;
        } else {
            toHeal = peekHeldFood(player);
        }
        if (toHeal <= 0) {
            renderTicks = 0;
            return;
        }

        if (!mc.isPaused()) {
            renderTicks++;
        }
        float alpha = (Mth.sin(renderTicks / 20F) + 1) / 3F;

        int width = gui.guiWidth();
        int height = gui.guiHeight();
        int left = width / 2 - 91;
        int top = height - 39;
        int rows = Mth.ceil(maxHealth / 2.0 / 10.0);
        int rowHeight = Math.max(10 - (rows - 2), 3);

        // Match whatever colour vanilla is currently tinting the hearts (Poison =
        // green, Wither = black, Frozen = cyan), so the preview doesn't stay red
        // over recoloured hearts. Replicates Gui.HeartType (package-private): the
        // full-heart U is 16 + index*2*9, V is 45 on hardcore worlds else 0.
        int heartU = heartTypeFullU(player);
        int heartV = player.level().getLevelData().isHardcore() ? 45 : 0;

        // Replicate vanilla's low-health heart jitter so the preview shakes in
        // sync with the real hearts. Vanilla (Gui#renderHearts) seeds its own
        // RandomSource with guiTicks*312871 and, when health+absorption <= 4,
        // nudges each heart's y by nextInt(2) — iterating hearts from the
        // highest index down to 0, absorption hearts first. We replay the same
        // draws into a per-heart offset table.
        int[] heartJitter = null;
        int absorption = Mth.ceil(player.getAbsorptionAmount());
        if (health + absorption <= 4) {
            int healthHearts = Mth.ceil(player.getMaxHealth() / 2.0F);
            int absorptionHearts = Mth.ceil(absorption / 2.0);
            int topHeart = healthHearts + absorptionHearts - 1;
            if (topHeart >= 0) {
                heartJitter = new int[topHeart + 1];
                RandomSource rng = RandomSource.create();
                rng.setSeed((long) (mc.gui.getGuiTicks() * 312871));
                for (int h = topHeart; h >= 0; h--) {
                    heartJitter[h] = rng.nextInt(2);
                }
            }
        }

        RenderSystem.enableBlend();
        gui.setColor(1, 1, 1, alpha);
        // Vanilla has already drawn the empty heart containers (this runs in the
        // PLAYER_HEALTH Post phase), so we only overlay the red fill the heal will
        // add. Each HP point is *half* a heart, composited from the full-heart
        // sprite: the left half is its [0,5) columns, the right half its [5,9)
        // columns shifted +5px (mirrors the original mod's xOffset trick). Drawing
        // per-half — rather than blitting a whole 9px half/full sprite per point —
        // avoids leaving a stray half-heart graphic (with the sprite's dark right
        // edge) sitting in an otherwise empty container when the preview ends on an
        // odd HP.
        for (int p = health; p < maxHealth && (p - health) < toHeal; p++) {
            int heartIndex = p / 2;
            int row = heartIndex / 10;
            int col = heartIndex % 10;
            int x = left + col * 8;
            int y = top - row * rowHeight;
            if (heartJitter != null && heartIndex < heartJitter.length) {
                y += heartJitter[heartIndex];
            }
            if (p % 2 == 0) {
                gui.blit(ICONS, x, y, heartU, heartV, 5, 9);
            } else {
                gui.blit(ICONS, x + 5, y, heartU + 5, heartV, 4, 9);
            }
        }
        gui.setColor(1, 1, 1, 1);
        RenderSystem.disableBlend();
    }

    /**
     * The icons.png U of the <em>full</em> heart sprite for the heart type vanilla
     * is currently using for this player — mirrors the package-private
     * {@code Gui.HeartType#forPlayer} + {@code getX(false, false)}.
     */
    private static int heartTypeFullU(Player player) {
        int index;
        if (player.hasEffect(MobEffects.POISON)) {
            index = 4;   // POISIONED (green)
        } else if (player.hasEffect(MobEffects.WITHER)) {
            index = 6;   // WITHERED (black)
        } else if (player.isFullyFrozen()) {
            index = 9;   // FROZEN (cyan)
        } else {
            index = 2;   // NORMAL (red)
        }
        return 16 + index * 2 * 9;
    }

    /**
     * Whether the vanilla item-cooldown sweep should be painted on food right now.
     * The heal cooldown is applied to every edible item on the player (so eating
     * is blocked no matter where the food comes from), but showing the grey clock
     * on <em>every</em> food on screen — creative inventory, JEI, etc. — looks
     * absurd. We keep the overlay only where the player's own food lives: the
     * hotbar (no screen open) and the survival inventory. See {@code
     * mixin.client.ItemCooldownsMixin}.
     */
    public static boolean shouldShowFoodCooldown() {
        Screen screen = Minecraft.getInstance().screen;
        return screen == null || screen instanceof InventoryScreen;
    }

    private static int peekHeldFood(Player player) {
        int n = foodNutrition(player.getMainHandItem(), player);
        if (n == 0) {
            n = foodNutrition(player.getOffhandItem(), player);
        }
        return n;
    }

    private static int foodNutrition(ItemStack stack, Player player) {
        FoodProperties food = stack.getFoodProperties(player);
        return food != null ? food.getNutrition() : 0;
    }

    // ---- Tooltip: how much / how fast a food heals ----

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!ModConfig.displayHealthGained.get() || !ClientState.naturalRegen) {
            return;
        }
        ItemStack stack = event.getItemStack();
        FoodProperties food = stack.getFoodProperties(event.getEntity());
        if (food == null || food.getNutrition() <= 0) {
            return;
        }
        DecimalFormat fmt = (DecimalFormat) DecimalFormat.getNumberInstance();
        fmt.setMaximumFractionDigits(1);
        float hearts = food.getNutrition() / 2F;
        float seconds = FoodHealing.getMaximumHealTicks(stack) / 20F;
        // "<amount> ❤ / <total>s" — how much it heals and over how long. The
        // explicit per-second rate was dropped (the saturation-driven number was
        // more confusing than useful); the heart keeps the dark-red tint the rate
        // figure used to carry.
        MutableComponent text = Component.literal(fmt.format(hearts) + " ")
                .withStyle(ChatFormatting.GRAY);
        text.append(Component.literal("❤ ").withStyle(ChatFormatting.DARK_RED));
        text.append(Component.translatable("hungersucks.tooltip.healing_time", fmt.format(seconds)).withStyle(ChatFormatting.GRAY));
        // Index 1 = right under the item name.
        if (event.getToolTip().size() > 1) {
            event.getToolTip().add(1, text);
        } else {
            event.getToolTip().add(text);
        }
    }

    // ---- Optional sprint lock ----

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        if ((ModConfig.disableSprinting.get() || ClientState.forceDisableSprinting) && player.isSprinting()) {
            player.setSprinting(false);
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientState.reset();
    }
}
