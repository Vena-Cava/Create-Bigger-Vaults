package com.venacava.biggervaults.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.EnumMap;
import java.util.Map;

import com.venacava.biggervaults.content.MaterialTier;

public final class BiggerVaultsConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue
            ITEM_VAULT_BASE_SLOTS_PER_BLOCK;

    public static final ModConfigSpec.IntValue
            FLUID_TANK_BASE_CAPACITY_PER_BLOCK;

    private static final Map<MaterialTier, ModConfigSpec.DoubleValue>
            MATERIAL_MULTIPLIERS =
            new EnumMap<>(MaterialTier.class);

    static {
        ModConfigSpec.Builder builder =
                new ModConfigSpec.Builder();

        builder.comment(
                "Common configuration for Create: Bigger Vaults."
        );

        builder.push("item_vaults");

        ITEM_VAULT_BASE_SLOTS_PER_BLOCK =
                builder
                        .comment(
                                "The base number of inventory slots "
                                        + "provided by one Item Vault block "
                                        + "before applying its material "
                                        + "multiplier.",
                                "",
                                "The default value of 20 matches "
                                        + "Create's normal Item Vault."
                        )
                        .worldRestart()
                        .defineInRange(
                                "base_slots_per_block",
                                20,
                                1,
                                1_000
                        );

        builder.pop();

        builder.push("fluid_tanks");

        FLUID_TANK_BASE_CAPACITY_PER_BLOCK =
                builder
                        .comment(
                                "The base fluid capacity, in millibuckets, "
                                        + "provided by one Fluid Tank block "
                                        + "before applying its material "
                                        + "multiplier.",
                                "",
                                "1000 millibuckets equals one bucket.",
                                "The default value is 8000 millibuckets."
                        )
                        .worldRestart()
                        .defineInRange(
                                "base_capacity_per_block",
                                8_000,
                                1,
                                10_000_000
                        );

        builder.pop();

        builder.push("materials");

        defineMaterial(
                builder,
                MaterialTier.WOODEN,
                0.5
        );

        defineMaterial(
                builder,
                MaterialTier.COPPER,
                1.5
        );

        defineMaterial(
                builder,
                MaterialTier.ZINC,
                1.5
        );

        defineMaterial(
                builder,
                MaterialTier.IRON,
                2.0
        );

        defineMaterial(
                builder,
                MaterialTier.EMERALD,
                2.5
        );

        defineMaterial(
                builder,
                MaterialTier.GOLD,
                3.0
        );

        defineMaterial(
                builder,
                MaterialTier.DIAMOND,
                3.5
        );

        defineMaterial(
                builder,
                MaterialTier.OBSIDIAN,
                4.0
        );

        defineMaterial(
                builder,
                MaterialTier.NETHERITE,
                5.0
        );

        builder.pop();

        SPEC = builder.build();
    }

    private BiggerVaultsConfig() {
    }

    private static void defineMaterial(
            ModConfigSpec.Builder builder,
            MaterialTier tier,
            double defaultMultiplier
    ) {
        builder.push(tier.serializedName());

        ModConfigSpec.DoubleValue multiplier =
                builder
                        .comment(
                                "Shared capacity and output multiplier "
                                        + "for the "
                                        + tier.serializedName()
                                        + " material tier.",
                                "",
                                "Changing this value affects all systems "
                                        + "which use this material tier."
                        )
                        .worldRestart()
                        .defineInRange(
                                "multiplier",
                                defaultMultiplier,
                                0.01,
                                1_000.0
                        );

        MATERIAL_MULTIPLIERS.put(
                tier,
                multiplier
        );

        builder.pop();
    }

    public static int itemVaultBaseSlotsPerBlock() {
        return ITEM_VAULT_BASE_SLOTS_PER_BLOCK.get();
    }

    public static int fluidTankBaseCapacityPerBlock() {
        return FLUID_TANK_BASE_CAPACITY_PER_BLOCK.get();
    }

    public static double materialMultiplier(
            MaterialTier tier
    ) {
        ModConfigSpec.DoubleValue value =
                MATERIAL_MULTIPLIERS.get(tier);

        if (value == null) {
            throw new IllegalArgumentException(
                    "No configured multiplier exists for material tier "
                            + tier.serializedName()
            );
        }

        return value.get();
    }
}