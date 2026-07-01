/*
 * Copyright (c) greenmint20.
 */
package com.greenmint20.hungersucks.client;

/**
 * Client-only mirror of the server-authoritative values the HUD needs. Updated
 * by the sync packets; reset on disconnect.
 */
public final class ClientState {

    public static int healAmount = 0;
    public static int amountHealed = 0;
    public static boolean naturalRegen = true;
    public static boolean forceDisableSprinting = false;

    private ClientState() {}

    public static void setHealing(int healAmount, int amountHealed) {
        ClientState.healAmount = healAmount;
        ClientState.amountHealed = amountHealed;
    }

    public static void setNaturalRegen(boolean value) {
        naturalRegen = value;
    }

    public static void setForceDisableSprinting(boolean value) {
        forceDisableSprinting = value;
    }

    public static void reset() {
        healAmount = 0;
        amountHealed = 0;
        naturalRegen = true;
        forceDisableSprinting = false;
    }
}
