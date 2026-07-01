/*
 * Copyright (c) greenmint20.
 */
package com.greenmint20.hungersucks.init;

import com.greenmint20.hungersucks.HungerSucks;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class ModTags {

    /** Food whose saturation modifier is multiplied (faster heal). */
    public static final TagKey<Item> INCREASED_SATURATION =
            ItemTags.create(HungerSucks.id("increased_saturation"));

    private ModTags() {}
}
