package com.venacava.biggervaults.registry;

import com.venacava.biggervaults.blockentity.TieredSteamEngineBlockEntity;
import com.venacava.biggervaults.BiggerVaults;
import com.venacava.biggervaults.blockentity.TieredFluidTankBlockEntity;
import com.venacava.biggervaults.blockentity.TieredItemVaultBlockEntity;
import com.venacava.biggervaults.content.FluidTankProperties;
import com.venacava.biggervaults.content.ItemVaultProperties;
import com.venacava.biggervaults.content.MaterialTier;
import com.venacava.biggervaults.content.SteamEngineProperties;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

public final class ModBlockEntities {

    public static final DeferredRegister<
            BlockEntityType<?>
            > BLOCK_ENTITIES =
            DeferredRegister.create(
                    Registries.BLOCK_ENTITY_TYPE,
                    BiggerVaults.MOD_ID
            );

    public static final Map<
            MaterialTier,
            DeferredHolder<
                    BlockEntityType<?>,
                    BlockEntityType<
                            TieredItemVaultBlockEntity
                            >
                    >
            > ITEM_VAULTS =
            new EnumMap<>(MaterialTier.class);

    public static final Map<
            MaterialTier,
            DeferredHolder<
                    BlockEntityType<?>,
                    BlockEntityType<
                            TieredFluidTankBlockEntity
                            >
                    >
            > FLUID_TANKS =
            new EnumMap<>(MaterialTier.class);

    public static final Map<
            MaterialTier,
            DeferredHolder<
                    BlockEntityType<?>,
                    BlockEntityType<TieredSteamEngineBlockEntity>
                    >
            > STEAM_ENGINES =
            new EnumMap<>(MaterialTier.class);

    static {
        for (MaterialTier tier
                : ItemVaultProperties.supportedTiers()) {

            DeferredHolder<
                    BlockEntityType<?>,
                    BlockEntityType<
                            TieredItemVaultBlockEntity
                            >
                    > holder =
                    BLOCK_ENTITIES.register(
                            tier.serializedName()
                                    + "_item_vault",
                            () -> BlockEntityType.Builder.of(
                                    (
                                            blockPos,
                                            blockState
                                    ) ->
                                            new TieredItemVaultBlockEntity(
                                                    ITEM_VAULTS
                                                            .get(tier)
                                                            .get(),
                                                    blockPos,
                                                    blockState
                                            ),
                                    ModBlocks.ITEM_VAULTS
                                            .get(tier)
                                            .get()
                            ).build(null)
                    );

            ITEM_VAULTS.put(
                    tier,
                    holder
            );
        }

        for (MaterialTier tier
                : FluidTankProperties.supportedTiers()) {

            DeferredHolder<
                    BlockEntityType<?>,
                    BlockEntityType<
                            TieredFluidTankBlockEntity
                            >
                    > holder =
                    BLOCK_ENTITIES.register(
                            tier.serializedName()
                                    + "_fluid_tank",
                            () -> BlockEntityType.Builder.of(
                                    (
                                            blockPos,
                                            blockState
                                    ) ->
                                            new TieredFluidTankBlockEntity(
                                                    FLUID_TANKS
                                                            .get(tier)
                                                            .get(),
                                                    blockPos,
                                                    blockState
                                            ),
                                    ModBlocks.FLUID_TANKS
                                            .get(tier)
                                            .get()
                            ).build(null)
                    );

            FLUID_TANKS.put(
                    tier,
                    holder
            );
        }

        for (MaterialTier tier
                : SteamEngineProperties.supportedTiers()) {

            DeferredHolder<
                    BlockEntityType<?>,
                    BlockEntityType<TieredSteamEngineBlockEntity>
                    > holder =
                    BLOCK_ENTITIES.register(
                            tier.serializedName()
                                    + "_steam_engine",
                            () ->
                                    BlockEntityType.Builder.of(
                                            (blockPos, blockState) ->
                                                    new TieredSteamEngineBlockEntity(
                                                            STEAM_ENGINES
                                                                    .get(tier)
                                                                    .get(),
                                                            blockPos,
                                                            blockState
                                                    ),
                                            ModBlocks.STEAM_ENGINES
                                                    .get(tier)
                                                    .get()
                                    ).build(null)
                    );

            STEAM_ENGINES.put(
                    tier,
                    holder
            );
        }
    }

    private ModBlockEntities() {
    }
}