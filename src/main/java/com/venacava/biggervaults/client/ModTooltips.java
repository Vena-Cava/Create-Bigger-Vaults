package com.venacava.biggervaults.client;

import com.simibubi.create.foundation.item.TooltipModifier;
import com.venacava.biggervaults.BiggerVaults;
import com.venacava.biggervaults.content.MaterialTier;
import com.venacava.biggervaults.content.SteamEngineProperties;
import com.venacava.biggervaults.registry.ModBlocks;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(
        modid = BiggerVaults.MOD_ID,
        value = Dist.CLIENT,
        bus = EventBusSubscriber.Bus.MOD
)
public final class ModTooltips {

    private ModTooltips() {
    }

    @SubscribeEvent
    public static void onClientSetup(
            FMLClientSetupEvent event
    ) {
        event.enqueueWork(
                ModTooltips::register
        );
    }

    private static void register() {
        for (MaterialTier tier
                : SteamEngineProperties.supportedTiers()) {

            Item steamEngineItem =
                    ModBlocks.STEAM_ENGINES
                            .get(tier)
                            .get()
                            .asItem();

            TooltipModifier.REGISTRY.register(
                    steamEngineItem,
                    new TieredSteamEngineStats(
                            tier
                    )
            );
        }
    }
}