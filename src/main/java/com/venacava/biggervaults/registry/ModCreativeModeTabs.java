package com.venacava.biggervaults.registry;

import com.venacava.biggervaults.BiggerVaults;
import com.venacava.biggervaults.content.MaterialTier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeModeTabs {

    public static final DeferredRegister<CreativeModeTab>
            CREATIVE_MODE_TABS =
            DeferredRegister.create(
                    Registries.CREATIVE_MODE_TAB,
                    BiggerVaults.MOD_ID
            );

    public static final DeferredHolder<
            CreativeModeTab,
            CreativeModeTab
            > BIGGER_VAULTS =
            CREATIVE_MODE_TABS.register(
                    "bigger_vaults",
                    () -> CreativeModeTab.builder()
                            .title(
                                    Component.translatable(
                                            "itemGroup.biggervaults"
                                    )
                            )
                            .icon(
                                    () -> new ItemStack(
                                            ModBlocks.ITEM_VAULTS
                                                    .get(MaterialTier.IRON)
                                                    .get()
                                    )
                            )
                            .displayItems(
                                    (parameters, output) -> {
                                        /*
                                         * Item Vaults
                                         */
                                        ModBlocks.ITEM_VAULTS
                                                .values()
                                                .forEach(
                                                        vault ->
                                                                output.accept(
                                                                        vault.get()
                                                                )
                                                );

                                        /*
                                         * Fluid Tanks
                                         */
                                        ModBlocks.FLUID_TANKS
                                                .values()
                                                .forEach(
                                                        tank ->
                                                                output.accept(
                                                                        tank.get()
                                                                )
                                                );

                                        /*
                                         * Steam Engines
                                         */
                                        ModBlocks.STEAM_ENGINES
                                                .values()
                                                .forEach(
                                                        engine ->
                                                                output.accept(
                                                                        engine.get()
                                                                )
                                                );
                                    }
                            )
                            .build()
            );

    private ModCreativeModeTabs() {
    }
}