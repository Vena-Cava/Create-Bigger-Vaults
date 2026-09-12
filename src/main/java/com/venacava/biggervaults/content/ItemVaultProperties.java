package com.venacava.biggervaults.content;

import com.venacava.biggervaults.config.BiggerVaultsConfig;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class ItemVaultProperties {
    public static final int MINIMUM_WIDTH = 1;
    public static final int MAXIMUM_WIDTH = 3;

    private static final Map<MaterialTier, ItemVaultProperties>
            PROPERTIES_BY_TIER;

    static {
        EnumMap<MaterialTier, ItemVaultProperties> properties =
                new EnumMap<>(MaterialTier.class);

        register(
                properties,
                MaterialTier.WOODEN,
                2
        );

        register(
                properties,
                MaterialTier.COPPER,
                3
        );

        register(
                properties,
                MaterialTier.IRON,
                4
        );

        register(
                properties,
                MaterialTier.EMERALD,
                4
        );

        register(
                properties,
                MaterialTier.GOLD,
                5
        );

        register(
                properties,
                MaterialTier.DIAMOND,
                6
        );

        register(
                properties,
                MaterialTier.OBSIDIAN,
                6
        );

        register(
                properties,
                MaterialTier.NETHERITE,
                7
        );

        PROPERTIES_BY_TIER =
                Collections.unmodifiableMap(properties);
    }

    private final MaterialTier tier;
    private final int baseMaximumHorizontalLength;

    private ItemVaultProperties(
            MaterialTier tier,
            int baseMaximumHorizontalLength
    ) {
        this.tier = tier;
        this.baseMaximumHorizontalLength =
                baseMaximumHorizontalLength;
    }

    private static void register(
            EnumMap<MaterialTier, ItemVaultProperties> properties,
            MaterialTier tier,
            int baseMaximumHorizontalLength
    ) {
        properties.put(
                tier,
                new ItemVaultProperties(
                        tier,
                        baseMaximumHorizontalLength
                )
        );
    }

    public static ItemVaultProperties get(
            MaterialTier tier
    ) {
        ItemVaultProperties properties =
                PROPERTIES_BY_TIER.get(tier);

        if (properties == null) {
            throw new IllegalArgumentException(
                    tier.serializedName()
                            + " is not an Item Vault material"
            );
        }

        return properties;
    }

    public static boolean supports(
            MaterialTier tier
    ) {
        return PROPERTIES_BY_TIER.containsKey(tier);
    }

    public static MaterialTier[] supportedTiers() {
        return PROPERTIES_BY_TIER
                .keySet()
                .toArray(MaterialTier[]::new);
    }

    public MaterialTier tier() {
        return tier;
    }

    public int itemSlotsPerBlock() {
        int baseSlots =
                BiggerVaultsConfig.itemVaultBaseSlotsPerBlock();

        return tier.scaleInt(baseSlots);
    }

    public int itemCapacityPerBlock() {
        return itemSlotsPerBlock() * 64;
    }

    public int baseMaximumHorizontalLength() {
        return baseMaximumHorizontalLength;
    }

    public int maximumLength(
            int width
    ) {
        if (width < MINIMUM_WIDTH
                || width > MAXIMUM_WIDTH) {
            throw new IllegalArgumentException(
                    "Item Vault cross-section size must be between "
                            + MINIMUM_WIDTH
                            + " and "
                            + MAXIMUM_WIDTH
                            + ", received "
                            + width
            );
        }

        return baseMaximumHorizontalLength * width;
    }
}