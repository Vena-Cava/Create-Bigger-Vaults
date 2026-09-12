package com.venacava.biggervaults.registry;

import com.venacava.biggervaults.BiggerVaults;
import com.venacava.biggervaults.block.TieredFluidTankBlock;
import com.venacava.biggervaults.block.TieredItemVaultBlock;
import com.venacava.biggervaults.block.TieredSteamEngineBlock;
import com.venacava.biggervaults.content.FluidTankProperties;
import com.venacava.biggervaults.content.ItemVaultProperties;
import com.venacava.biggervaults.content.MaterialTier;
import com.venacava.biggervaults.content.SteamEngineProperties;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(
                    BiggerVaults.MOD_ID
            );

    public static final Map<
            MaterialTier,
            DeferredBlock<TieredItemVaultBlock>
            > ITEM_VAULTS =
            new EnumMap<>(MaterialTier.class);

    public static final Map<
            MaterialTier,
            DeferredBlock<TieredFluidTankBlock>
            > FLUID_TANKS =
            new EnumMap<>(MaterialTier.class);

    public static final Map<
            MaterialTier,
            DeferredBlock<TieredSteamEngineBlock>
            > STEAM_ENGINES =
            new EnumMap<>(MaterialTier.class);

    static {
        for (MaterialTier tier
                : ItemVaultProperties.supportedTiers()) {

            ITEM_VAULTS.put(
                    tier,
                    BLOCKS.register(
                            tier.serializedName()
                                    + "_item_vault",
                            () -> new TieredItemVaultBlock(
                                    tier,
                                    itemVaultProperties(
                                            tier
                                    )
                            )
                    )
            );
        }

        for (MaterialTier tier
                : FluidTankProperties.supportedTiers()) {

            FLUID_TANKS.put(
                    tier,
                    BLOCKS.register(
                            tier.serializedName()
                                    + "_fluid_tank",
                            () -> new TieredFluidTankBlock(
                                    tier,
                                    fluidTankProperties(
                                            tier
                                    )
                            )
                    )
            );
        }

        for (MaterialTier tier
                : SteamEngineProperties.supportedTiers()) {

            STEAM_ENGINES.put(
                    tier,
                    BLOCKS.register(
                            tier.serializedName()
                                    + "_steam_engine",
                            () -> new TieredSteamEngineBlock(
                                    tier,
                                    steamEngineProperties(
                                            tier
                                    )
                            )
                    )
            );
        }
    }

    private ModBlocks() {
    }

    private static BlockBehaviour.Properties
    itemVaultProperties(
            MaterialTier tier
    ) {
        return materialProperties(tier);
    }

    private static BlockBehaviour.Properties
    fluidTankProperties(
            MaterialTier tier
    ) {
        return materialProperties(tier)
                .noOcclusion();
    }

    private static BlockBehaviour.Properties
    steamEngineProperties(
            MaterialTier tier
    ) {
        return materialProperties(tier)
                .noOcclusion();
    }

    private static BlockBehaviour.Properties
    materialProperties(
            MaterialTier tier
    ) {
        return switch (tier) {
            case WOODEN ->
                    BlockBehaviour.Properties.ofFullCopy(
                            Blocks.OAK_PLANKS
                    );

            case COPPER ->
                    BlockBehaviour.Properties.ofFullCopy(
                            Blocks.COPPER_BLOCK
                    );

            case ZINC ->
                    zincProperties();

            case IRON ->
                    BlockBehaviour.Properties.ofFullCopy(
                            Blocks.IRON_BLOCK
                    );

            case EMERALD ->
                    BlockBehaviour.Properties.ofFullCopy(
                            Blocks.EMERALD_BLOCK
                    );

            case GOLD ->
                    BlockBehaviour.Properties.ofFullCopy(
                            Blocks.GOLD_BLOCK
                    );

            case DIAMOND ->
                    BlockBehaviour.Properties.ofFullCopy(
                            Blocks.DIAMOND_BLOCK
                    );

            case OBSIDIAN ->
                    BlockBehaviour.Properties.ofFullCopy(
                            Blocks.OBSIDIAN
                    );

            case NETHERITE ->
                    BlockBehaviour.Properties.ofFullCopy(
                            Blocks.NETHERITE_BLOCK
                    );
        };
    }

    private static BlockBehaviour.Properties
    zincProperties() {
        return BlockBehaviour.Properties.of()
                .strength(
                        5.0F,
                        6.0F
                )
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops();
    }
}