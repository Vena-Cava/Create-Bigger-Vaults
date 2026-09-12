package com.venacava.biggervaults.client.ponder;

import com.simibubi.create.infrastructure.ponder.AllCreatePonderTags;
import com.simibubi.create.infrastructure.ponder.scenes.ItemVaultScenes;
import com.simibubi.create.infrastructure.ponder.scenes.SteamScenes;
import com.simibubi.create.infrastructure.ponder.scenes.fluid.FluidTankScenes;
import com.venacava.biggervaults.BiggerVaults;
import com.venacava.biggervaults.content.FluidTankProperties;
import com.venacava.biggervaults.content.ItemVaultProperties;
import com.venacava.biggervaults.content.MaterialTier;
import com.venacava.biggervaults.content.SteamEngineProperties;
import com.venacava.biggervaults.registry.ModBlocks;
import com.venacava.biggervaults.client.ponder.scenes.BoilerScenes;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

public final class BiggerVaultsPonderPlugin implements PonderPlugin {

    private static final ResourceLocation CREATE_ITEM_VAULT_STORAGE =
            ResourceLocation.fromNamespaceAndPath(
                    "create",
                    "item_vault/storage"
            );

    private static final ResourceLocation CREATE_FLUID_TANK_STORAGE =
            ResourceLocation.fromNamespaceAndPath(
                    "create",
                    "fluid_tank/storage"
            );

    private static final ResourceLocation CREATE_STEAM_ENGINE =
            ResourceLocation.fromNamespaceAndPath(
                    "create",
                    "steam_engine"
            );

    @Override
    public String getModId() {
        return BiggerVaults.MOD_ID;
    }

    @Override
    public void registerScenes(
            PonderSceneRegistrationHelper<ResourceLocation> helper
    ) {
        registerItemVaults(helper);
        registerFluidTanks(helper);
        registerSteamEngines(helper);
    }

    @Override
    public void registerTags(
            PonderTagRegistrationHelper<ResourceLocation> helper
    ) {
        BiggerVaultsPonderTags.registerItemVaultTags(helper);
        BiggerVaultsPonderTags.registerFluidTankTags(helper);
        BiggerVaultsPonderTags.registerSteamEngineTags(helper);
    }

    private static void registerItemVaults(
            PonderSceneRegistrationHelper<ResourceLocation> helper
    ) {
        for (MaterialTier tier : ItemVaultProperties.supportedTiers()) {
            ResourceLocation blockId =
                    ModBlocks.ITEM_VAULTS
                            .get(tier)
                            .getId();

            helper.forComponents(blockId)
                    .addStoryBoard(
                            CREATE_ITEM_VAULT_STORAGE,
                            ItemVaultScenes::storage,
                            AllCreatePonderTags.LOGISTICS
                    );
        }
    }

    private static void registerFluidTanks(
            PonderSceneRegistrationHelper<ResourceLocation> helper
    ) {
        for (MaterialTier tier
                : FluidTankProperties.supportedTiers()) {

            ResourceLocation blockId =
                    ModBlocks.FLUID_TANKS
                            .get(tier)
                            .getId();

            helper.forComponents(blockId)
                    .addStoryBoard(
                            CREATE_FLUID_TANK_STORAGE,
                            FluidTankScenes::storage,
                            AllCreatePonderTags.FLUIDS
                    )
                    .addStoryBoard(
                            "fluid_tank/boiler",
                            BoilerScenes::buildingBetterBoilers,
                            AllCreatePonderTags.FLUIDS
                    );
        }
    }

    private static void registerSteamEngines(
            PonderSceneRegistrationHelper<ResourceLocation> helper
    ) {
        for (MaterialTier tier : SteamEngineProperties.supportedTiers()) {
            ResourceLocation blockId =
                    ModBlocks.STEAM_ENGINES
                            .get(tier)
                            .getId();

            helper.forComponents(blockId)
                    .addStoryBoard(
                            CREATE_STEAM_ENGINE,
                            SteamScenes::engine,
                            AllCreatePonderTags.KINETIC_SOURCES
                    );
        }
    }
}