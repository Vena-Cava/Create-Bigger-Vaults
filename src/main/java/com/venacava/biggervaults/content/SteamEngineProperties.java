package com.venacava.biggervaults.content;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class SteamEngineProperties {

    private static final Map<
            MaterialTier,
            SteamEngineProperties
            > PROPERTIES;

    static {
        EnumMap<
                MaterialTier,
                SteamEngineProperties
                > properties =
                new EnumMap<>(MaterialTier.class);

        register(
                properties,
                MaterialTier.WOODEN
        );

        register(
                properties,
                MaterialTier.ZINC
        );

        register(
                properties,
                MaterialTier.IRON
        );

        register(
                properties,
                MaterialTier.EMERALD
        );

        register(
                properties,
                MaterialTier.GOLD
        );

        register(
                properties,
                MaterialTier.DIAMOND
        );

        register(
                properties,
                MaterialTier.OBSIDIAN
        );

        register(
                properties,
                MaterialTier.NETHERITE
        );

        PROPERTIES =
                Collections.unmodifiableMap(
                        properties
                );
    }

    private final MaterialTier tier;

    private SteamEngineProperties(
            MaterialTier tier
    ) {
        this.tier = tier;
    }

    private static void register(
            EnumMap<
                    MaterialTier,
                    SteamEngineProperties
                    > properties,
            MaterialTier tier
    ) {
        properties.put(
                tier,
                new SteamEngineProperties(
                        tier
                )
        );
    }

    public static SteamEngineProperties get(
            MaterialTier tier
    ) {
        SteamEngineProperties properties =
                PROPERTIES.get(tier);

        if (properties == null) {
            throw new IllegalArgumentException(
                    "Material tier "
                            + tier
                            + " does not support Steam Engines."
            );
        }

        return properties;
    }

    public static boolean supports(
            MaterialTier tier
    ) {
        return PROPERTIES.containsKey(tier);
    }

    public static MaterialTier[] supportedTiers() {
        return PROPERTIES.keySet()
                .toArray(MaterialTier[]::new);
    }

    public MaterialTier tier() {
        return tier;
    }

    public float outputMultiplier() {
        return tier.multiplier();
    }
}