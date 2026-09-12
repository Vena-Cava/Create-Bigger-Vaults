package com.venacava.biggervaults.client.ponder;

import com.simibubi.create.infrastructure.ponder.AllCreatePonderTags;
import com.venacava.biggervaults.content.FluidTankProperties;
import com.venacava.biggervaults.content.ItemVaultProperties;
import com.venacava.biggervaults.content.MaterialTier;
import com.venacava.biggervaults.content.SteamEngineProperties;
import com.venacava.biggervaults.registry.ModBlocks;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

public final class BiggerVaultsPonderTags {

    private BiggerVaultsPonderTags() {
    }

    public static void registerItemVaultTags(
            PonderTagRegistrationHelper<ResourceLocation> helper
    ) {
        for (MaterialTier tier : ItemVaultProperties.supportedTiers()) {
            helper.addToTag(AllCreatePonderTags.LOGISTICS)
                    .add(
                            ModBlocks.ITEM_VAULTS
                                    .get(tier)
                                    .getId()
                    );
        }
    }

    public static void registerFluidTankTags(
            PonderTagRegistrationHelper<ResourceLocation> helper
    ) {
        for (MaterialTier tier : FluidTankProperties.supportedTiers()) {
            helper.addToTag(AllCreatePonderTags.FLUIDS)
                    .add(
                            ModBlocks.FLUID_TANKS
                                    .get(tier)
                                    .getId()
                    );
        }
    }

    public static void registerSteamEngineTags(
            PonderTagRegistrationHelper<ResourceLocation> helper
    ) {
        for (MaterialTier tier : SteamEngineProperties.supportedTiers()) {
            helper.addToTag(AllCreatePonderTags.KINETIC_SOURCES)
                    .add(
                            ModBlocks.STEAM_ENGINES
                                    .get(tier)
                                    .getId()
                    );
        }
    }
}