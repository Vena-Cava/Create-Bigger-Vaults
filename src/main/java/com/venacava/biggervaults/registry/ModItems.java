package com.venacava.biggervaults.registry;

import com.venacava.biggervaults.BiggerVaults;
import com.venacava.biggervaults.content.FluidTankProperties;
import com.venacava.biggervaults.content.ItemVaultProperties;
import com.venacava.biggervaults.content.MaterialTier;
import com.venacava.biggervaults.content.SteamEngineProperties;
import com.venacava.biggervaults.item.TieredFluidTankItem;
import com.venacava.biggervaults.item.TieredItemVaultItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

public final class ModItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(
                    BiggerVaults.MOD_ID
            );

    public static final Map<
            MaterialTier,
            DeferredHolder<
                    Item,
                    TieredItemVaultItem
                    >
            > ITEM_VAULT_ITEMS =
            new EnumMap<>(MaterialTier.class);

    public static final Map<
            MaterialTier,
            DeferredHolder<
                    Item,
                    TieredFluidTankItem
                    >
            > FLUID_TANK_ITEMS =
            new EnumMap<>(MaterialTier.class);

    public static final Map<
            MaterialTier,
            DeferredHolder<
                    Item,
                    BlockItem
                    >
            > STEAM_ENGINE_ITEMS =
            new EnumMap<>(MaterialTier.class);

    static {
        for (MaterialTier tier
                : ItemVaultProperties.supportedTiers()) {

            ITEM_VAULT_ITEMS.put(
                    tier,
                    ITEMS.register(
                            tier.serializedName()
                                    + "_item_vault",
                            () -> new TieredItemVaultItem(
                                    ModBlocks.ITEM_VAULTS
                                            .get(tier)
                                            .get(),
                                    new Item.Properties()
                            )
                    )
            );
        }

        for (MaterialTier tier
                : FluidTankProperties.supportedTiers()) {

            FLUID_TANK_ITEMS.put(
                    tier,
                    ITEMS.register(
                            tier.serializedName()
                                    + "_fluid_tank",
                            () -> new TieredFluidTankItem(
                                    ModBlocks.FLUID_TANKS
                                            .get(tier)
                                            .get(),
                                    new Item.Properties()
                            )
                    )
            );
        }

        for (MaterialTier tier
                : SteamEngineProperties.supportedTiers()) {

            STEAM_ENGINE_ITEMS.put(
                    tier,
                    ITEMS.register(
                            tier.serializedName()
                                    + "_steam_engine",
                            () -> new BlockItem(
                                    ModBlocks.STEAM_ENGINES
                                            .get(tier)
                                            .get(),
                                    new Item.Properties()
                            )
                    )
            );
        }
    }

    private ModItems() {
    }
}