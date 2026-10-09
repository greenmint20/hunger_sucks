# Hunger Sucks — gameplay and configuration

[Русское описание](FEATURES.md)

Hunger Sucks replaces hunger with gradual healing from food. This document
describes release **0.1.5** for **Forge 1.20.1** on the `main` branch.
The NeoForge 1.21.1 port lives on `neoforge-1.21.1`.

## Food heals you

- The hunger bar is hidden. Hunger does not drain, cause starvation, or prevent sprinting.
- Vanilla hunger-based regeneration is replaced by food healing over time.
- A food's nutrition determines how many health points it restores; two health points equal one heart.
- Its saturation determines how quickly it heals.
- Ordinary food can be eaten when you are missing health and no food healing is running.
- By default, all edible items receive a cooldown for the duration of healing.
  The grey cooldown sweep appears in your inventory and hotbar, rather than in
  the creative inventory or other mods' item lists such as JEI.
- Disabling `foodCooldown` removes the item cooldown. Ordinary food is still
  blocked during healing; always-edible food, such as golden apples, follows
  Minecraft's exception and can be eaten without missing health.
- Healing progress is saved when you leave the world. Its remaining cooldown
  is restored on login, respawn, or dimension changes when applicable.

## Healing tooltips and previews

- Food tooltips show the healing amount in hearts and the total duration.
  For example, `2 ❤ / 4s` means two hearts over four seconds.
- Holding food or healing from it displays a pulsing outline of the expected
  health gain on the health bar.
- The outline matches the heart color during Poison, Wither, and freezing,
  and follows the hearts' low-health shaking.
- The preview is hidden in Creative and Spectator modes, while Hunger is
  active, or when `naturalRegeneration` is disabled.
- `displayHealthGained` controls both the tooltip and the health-bar preview.
- English and Russian translations are available for the tooltip's time unit.

## Armor and air bars

- The armor bar moves to the bottom right, where the hunger bar used to be.
  Disable `moveArmorBar` to keep its original position.
- With the relocated armor bar, air bubbles stay on the lower row without
  armor and move up one row when armor is equipped.

## Faster drinks and saturation bonuses

- Honey bottles, stews, milk, and potions are consumed twice as fast when
  `fasterFluidConsumption` is enabled.
- `increaseHoneySaturation` raises honey's saturation modifier to `0.8`,
  making its food healing faster.
- Items in the `hungersucks:increased_saturation` tag receive a `2.6` multiplier
  to their effective saturation. The tag currently contains pumpkin pie.
  Data packs can extend it to other foods.

## Status effects and game rules

- **Saturation** restores health instantly instead of filling hunger.
  The amount depends on the effect's level.
- **Hunger** prevents food healing. You can still eat at full health to gain
  other food effects, such as those from suspicious stew, subject to an existing
  item cooldown. The healing preview is hidden while Hunger is active.
- Starvation damage is removed. Other harmful food effects, such as Poison,
  still apply.
- Setting `naturalRegeneration` to `false` stops food from restoring health
  and hides healing tooltips and previews. Changes take effect without rejoining.
  This rule does not disable the separate instant healing from Saturation.
- If Hunger or disabled natural regeneration interrupts a running food heal,
  its timer continues; the missed health is not restored afterward.

## Configuration

Forge creates `config/hungersucks-common.toml` for gameplay settings and
`config/hungersucks-client.toml` for visual settings. Keep common settings
consistent between the client and server.

| Setting | Default | Effect | Scope |
| --- | --- | --- | --- |
| `foodCooldown` | `true` | Applies a cooldown to all edible items during food healing | Common |
| `disableSprinting` | `false` | Disables sprinting | Common |
| `fasterFluidConsumption` | `true` | Halves the consumption time of honey, stews, milk, and potions | Common |
| `increaseHoneySaturation` | `true` | Increases honey's saturation for faster healing | Common |
| `displayHealthGained` | `true` | Shows food healing tooltips and the health-bar preview | Client |
| `moveArmorBar` | `true` | Moves the armor bar into the former hunger-bar position | Client |
