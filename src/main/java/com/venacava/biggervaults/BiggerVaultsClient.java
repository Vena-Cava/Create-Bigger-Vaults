package com.venacava.biggervaults;

import com.simibubi.create.content.kinetics.steamEngine.SteamEngineRenderer;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.block.connected.CTModel;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineVisual;
import com.venacava.biggervaults.client.ModPartialModels;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import com.venacava.biggervaults.client.TieredFluidTankModel;
import com.venacava.biggervaults.client.TieredFluidTankRenderer;
import com.venacava.biggervaults.content.FluidTankProperties;
import com.venacava.biggervaults.content.ItemVaultProperties;
import com.venacava.biggervaults.content.SteamEngineProperties;
import com.venacava.biggervaults.registry.ModBlockEntities;
import com.venacava.biggervaults.client.TieredItemVaultCTBehaviour;
import com.venacava.biggervaults.content.MaterialTier;
import com.venacava.biggervaults.registry.ModSpriteShifts;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(
        value = BiggerVaults.MOD_ID,
        dist = Dist.CLIENT
)
public final class BiggerVaultsClient {

    public BiggerVaultsClient(
            ModContainer container
    ) {
        container.registerExtensionPoint(
                IConfigScreenFactory.class,
                ConfigurationScreen::new
        );

        ModSpriteShifts.register();
        ModPartialModels.register();

        for (MaterialTier tier
                : ItemVaultProperties.supportedTiers()) {

            CreateClient.MODEL_SWAPPER
                    .getCustomBlockModels()
                    .register(
                            BiggerVaults.asResource(
                                    tier.serializedName()
                                            + "_item_vault"
                            ),
                            originalModel ->
                                    new CTModel(
                                            originalModel,
                                            new TieredItemVaultCTBehaviour(
                                                    tier
                                            )
                                    )
                    );
        }
        for (MaterialTier tier
                : FluidTankProperties.supportedTiers()) {

            ModSpriteShifts.TankSpriteShifts shifts =
                    ModSpriteShifts.FLUID_TANK.get(tier);

            CreateClient.MODEL_SWAPPER
                    .getCustomBlockModels()
                    .register(
                            BiggerVaults.asResource(
                                    tier.serializedName()
                                            + "_fluid_tank"
                            ),
                            originalModel ->
                                    new TieredFluidTankModel(
                                            originalModel,
                                            shifts.side(),
                                            shifts.top(),
                                            shifts.inner()
                                    )
                    );
        }
    }

    @EventBusSubscriber(
            modid = BiggerVaults.MOD_ID,
            value = Dist.CLIENT,
            bus = EventBusSubscriber.Bus.MOD
    )
    public static final class ClientEvents {

        private ClientEvents() {
        }

        @SubscribeEvent
        public static void registerRenderers(
                EntityRenderersEvent.RegisterRenderers event
        ) {
            for (MaterialTier tier
                    : FluidTankProperties.supportedTiers()) {

                event.registerBlockEntityRenderer(
                        ModBlockEntities.FLUID_TANKS
                                .get(tier)
                                .get(),
                        TieredFluidTankRenderer::new
                );
            }
            for (MaterialTier tier
                    : SteamEngineProperties.supportedTiers()) {

                event.registerBlockEntityRenderer(
                        ModBlockEntities.STEAM_ENGINES
                                .get(tier)
                                .get(),
                        SteamEngineRenderer::new
                );

                SimpleBlockEntityVisualizer
                        .builder(
                                ModBlockEntities.STEAM_ENGINES
                                        .get(tier)
                                        .get()
                        )
                        .factory(
                                (
                                        visualizationContext,
                                        blockEntity,
                                        partialTick
                                ) ->
                                        new SteamEngineVisual(
                                                visualizationContext,
                                                blockEntity,
                                                partialTick
                                        )
                        )
                        .apply();
            }
        }
    }
}
