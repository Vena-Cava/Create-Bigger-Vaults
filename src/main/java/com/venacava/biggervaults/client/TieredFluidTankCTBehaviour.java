package com.venacava.biggervaults.client;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.HorizontalCTBehaviour;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

public class TieredFluidTankCTBehaviour
        extends HorizontalCTBehaviour {

    private final CTSpriteShiftEntry innerShift;

    public TieredFluidTankCTBehaviour(
            CTSpriteShiftEntry sideShift,
            CTSpriteShiftEntry topShift,
            CTSpriteShiftEntry innerShift
    ) {
        super(sideShift, topShift);
        this.innerShift = innerShift;
    }

    @Override
    public CTSpriteShiftEntry getShift(
            BlockState state,
            Direction direction,
            TextureAtlasSprite sprite
    ) {
        if (sprite != null
                && direction.getAxis() == Direction.Axis.Y
                && innerShift.getOriginal() == sprite) {
            return innerShift;
        }

        return super.getShift(
                state,
                direction,
                sprite
        );
    }

    @Override
    public boolean buildContextForOccludedDirections() {
        return true;
    }

    @Override
    public boolean connectsTo(
            BlockState state,
            BlockState other,
            BlockAndTintGetter world,
            BlockPos pos,
            BlockPos otherPos,
            Direction face
    ) {
        return state.getBlock() == other.getBlock()
                && ConnectivityHandler.isConnected(
                world,
                pos,
                otherPos
        );
    }
}