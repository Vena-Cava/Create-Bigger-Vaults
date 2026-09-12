package com.venacava.biggervaults.block;

import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlock;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlockEntity;
import com.venacava.biggervaults.content.MaterialTier;
import com.venacava.biggervaults.registry.ModBlockEntities;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class TieredSteamEngineBlock
        extends SteamEngineBlock {

    private final MaterialTier tier;

    public TieredSteamEngineBlock(
            MaterialTier tier,
            BlockBehaviour.Properties properties
    ) {
        super(properties);

        this.tier = tier;
    }

    public MaterialTier getTier() {
        return tier;
    }

    @Override
    public BlockEntityType<
            ? extends SteamEngineBlockEntity
            > getBlockEntityType() {
        return ModBlockEntities.STEAM_ENGINES
                .get(tier)
                .get();
    }
}