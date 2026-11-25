package com.company.GymVerse.models.tenant.enums;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Deterministic plan tier theme keys shared between backend persistence and frontend styling.
 * ELITE -> GOLDEN, LEGENDARY -> PURPLE, and 20 custom theme keys for admin-created tiers.
 */
public enum PlanTheme {

    // Default Tier Themes
    GOLDEN(false),
    PURPLE(false),

    // 20 Custom UI-Aligned Themes
    EMERALD_GREEN(true),
    CRIMSON_RED(true),
    OCEAN_BLUE(true),
    MIDNIGHT_BLACK(true),
    SUNSET_ORANGE(true),
    CYAN_WAVE(true),
    ROSE_PINK(true),
    AMBER_GOLD(true),
    INDIGO_NIGHT(true),
    TEAL_MINT(true),
    SLATE_SILVER(true),
    VIOLET_SPARK(true),
    CORAL_BLUSH(true),
    LIME_PULSE(true),
    COPPER_BRONZE(true),
    ARCTIC_ICE(true),
    FOREST_PINE(true),
    MAGENTA_GLOW(true),
    CHARCOAL_STEEL(true),
    ROYAL_COBALT(true);

    private final boolean customTheme;

    PlanTheme(final boolean customTheme) {
        this.customTheme = customTheme;
    }

    public boolean isCustomTheme() {
        return customTheme;
    }

    public static List<String> getCustomThemeKeys() {
        return Arrays.stream(values())
                .filter(PlanTheme::isCustomTheme)
                .map(Enum::name)
                .toList();
    }

    public static Optional<PlanTheme> resolveDefaultTierTheme(final String tierName) {
        if (tierName == null) {
            return Optional.empty();
        }
        String normalized = tierName.trim().toUpperCase();
        if ("ELITE".equals(normalized)) {
            return Optional.of(GOLDEN);
        }
        if ("LEGENDARY".equals(normalized)) {
            return Optional.of(PURPLE);
        }
        return Optional.empty();
    }
}
