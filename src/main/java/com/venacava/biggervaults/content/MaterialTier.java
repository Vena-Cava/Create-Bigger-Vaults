package com.venacava.biggervaults.content;

import com.venacava.biggervaults.config.BiggerVaultsConfig;

import java.util.Locale;

public enum MaterialTier {
    WOODEN,
    COPPER,
    ZINC,
    IRON,
    EMERALD,
    GOLD,
    DIAMOND,
    OBSIDIAN,
    NETHERITE;

    public float multiplier() {
        return (float) BiggerVaultsConfig.materialMultiplier(this);
    }

    public int scaleInt(int baseValue) {
        return Math.round(baseValue * multiplier());
    }

    public long scaleLong(long baseValue) {
        return Math.round(baseValue * multiplier());
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}