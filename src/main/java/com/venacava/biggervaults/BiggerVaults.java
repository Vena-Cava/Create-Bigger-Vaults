package com.venacava.biggervaults;

import com.mojang.logging.LogUtils;
import com.simibubi.create.api.stress.BlockStressValues;

import com.venacava.biggervaults.config.BiggerVaultsConfig;
import com.venacava.biggervaults.registry.ModBlockEntities;
import com.venacava.biggervaults.registry.ModBlocks;
import com.venacava.biggervaults.registry.ModCapabilities;
import com.venacava.biggervaults.registry.ModCreativeModeTabs;
import com.venacava.biggervaults.registry.ModItems;
import com.venacava.biggervaults.registry.ModMountedStorageTypes;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

@Mod(BiggerVaults.MOD_ID)
public final class BiggerVaults {
    public static final String MOD_ID = "biggervaults";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static ResourceLocation asResource(
            String path
    ) {
        return ResourceLocation.fromNamespaceAndPath(
                MOD_ID,
                path
        );
    }

    public BiggerVaults(
            IEventBus modBus,
            ModContainer modContainer
    ) {
        modContainer.registerConfig(
                ModConfig.Type.COMMON,
                BiggerVaultsConfig.SPEC
        );

        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModCreativeModeTabs.CREATIVE_MODE_TABS.register(modBus);
        ModMountedStorageTypes.MOUNTED_ITEM_STORAGE_TYPES.register(
                modBus
        );

        modBus.addListener(ModCapabilities::register);
        modBus.addListener(
                ModMountedStorageTypes::registerBlockAssociations
        );
        modBus.addListener(BiggerVaults::commonSetup);

        LOGGER.info(
                "Create: Bigger Vaults registration complete"
        );
    }

    private static void commonSetup(
            FMLCommonSetupEvent event
    ) {
        event.enqueueWork(() -> {
            ModBlocks.STEAM_ENGINES.values().forEach(
                    engineEntry -> {
                        BlockStressValues.CAPACITIES.register(
                                engineEntry.get(),
                                () -> 1024.0
                        );

                        BlockStressValues.RPM.register(
                                engineEntry.get(),
                                new BlockStressValues.GeneratedRpm(
                                        64,
                                        true
                                )
                        );
                    }
            );

            LOGGER.info(
                    "Registered Create stress values for {} Bigger Vaults Steam Engines",
                    ModBlocks.STEAM_ENGINES.size()
            );
        });
    }
}