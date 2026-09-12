package com.venacava.biggervaults.content;

import com.venacava.biggervaults.config.BiggerVaultsConfig;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class FluidTankProperties {
    public static final int MINIMUM_BASE_SIZE = 1;
    public static final int MAXIMUM_BASE_SIZE = 3;
    public static final int MAXIMUM_HEIGHT = 32;

    private static final Map<MaterialTier, FluidTankProperties>
            PROPERTIES_BY_TIER;

    static {
        EnumMap<MaterialTier, FluidTankProperties> properties =
                new EnumMap<>(MaterialTier.class);

        register(properties, MaterialTier.WOODEN);
        register(properties, MaterialTier.ZINC);
        register(properties, MaterialTier.IRON);
        register(properties, MaterialTier.EMERALD);
        register(properties, MaterialTier.GOLD);
        register(properties, MaterialTier.DIAMOND);
        register(properties, MaterialTier.OBSIDIAN);
        register(properties, MaterialTier.NETHERITE);

        PROPERTIES_BY_TIER =
                Collections.unmodifiableMap(properties);
    }

    private final MaterialTier tier;

    private FluidTankProperties(MaterialTier tier) {
        this.tier = tier;
    }

    private static void register(
            EnumMap<MaterialTier, FluidTankProperties> properties,
            MaterialTier tier
    ) {
        properties.put(
                tier,
                new FluidTankProperties(tier)
        );
    }

    public static FluidTankProperties get(MaterialTier tier) {
        FluidTankProperties properties =
                PROPERTIES_BY_TIER.get(tier);

        if (properties == null) {
            throw new IllegalArgumentException(
                    tier.serializedName()
                            + " is not a Fluid Tank material"
            );
        }

        return properties;
    }

    public static boolean supports(MaterialTier tier) {
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

    public int fluidCapacityPerBlock() {
        return tier.scaleInt(
                BiggerVaultsConfig.fluidTankBaseCapacityPerBlock()
        );
    }

    /**
     * Multiplies the amount of boiler power this material can provide.
     *
     * Copper represents Create's standard fluid-based tier and therefore
     * has a multiplier of 1.0.
     */
    public float boilerOutputMultiplier() {
        return (float) BiggerVaultsConfig.materialMultiplier(tier);
    }

    public int minimumBoilerOutput() {
        return tier.scaleInt(2_048);
    }

    public int maximumBoilerOutput() {
        return minimumBoilerOutput() * 144;
    }

    public long capacityForMultiblock(
            int width,
            int depth,
            int height
    ) {
        validateDimensions(width, depth, height);

        long blockCount =
                (long) width * depth * height;

        return blockCount * fluidCapacityPerBlock();
    }

    public static void validateDimensions(
            int width,
            int depth,
            int height
    ) {
        if (width < MINIMUM_BASE_SIZE
                || width > MAXIMUM_BASE_SIZE) {
            throw new IllegalArgumentException(
                    "Fluid Tank width must be between "
                            + MINIMUM_BASE_SIZE
                            + " and "
                            + MAXIMUM_BASE_SIZE
                            + ", received "
                            + width
            );
        }

        if (depth < MINIMUM_BASE_SIZE
                || depth > MAXIMUM_BASE_SIZE) {
            throw new IllegalArgumentException(
                    "Fluid Tank depth must be between "
                            + MINIMUM_BASE_SIZE
                            + " and "
                            + MAXIMUM_BASE_SIZE
                            + ", received "
                            + depth
            );
        }

        if (height < 1 || height > MAXIMUM_HEIGHT) {
            throw new IllegalArgumentException(
                    "Fluid Tank height must be between 1 and "
                            + MAXIMUM_HEIGHT
                            + ", received "
                            + height
            );
        }
    }
}