package com.venacava.biggervaults.client;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.venacava.biggervaults.block.TieredItemVaultBlock;
import com.venacava.biggervaults.content.MaterialTier;
import com.venacava.biggervaults.registry.ModSpriteShifts;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class TieredItemVaultCTBehaviour
        extends ConnectedTextureBehaviour.Base {

    private final MaterialTier tier;

    public TieredItemVaultCTBehaviour(
            MaterialTier tier
    ) {
        this.tier = tier;
    }

    @Nullable
    @Override
    public CTSpriteShiftEntry getShift(
            BlockState state,
            Direction direction,
            TextureAtlasSprite sprite
    ) {
        Direction.Axis vaultAxis =
                TieredItemVaultBlock.getVaultBlockAxis(
                        state
                );

        if (vaultAxis == null) {
            return null;
        }

        /*
         * Width 1 uses the original 16x16 texture because
         * there are no connected neighbours.
         *
         * Width 2 uses the medium texture sheet.
         * Width 3 uses the large texture sheet.
         */
        boolean useMedium =
                !TieredItemVaultBlock.isLarge(state);

        if (direction.getAxis() == vaultAxis) {
            return ModSpriteShifts.VAULT_FRONT
                    .get(tier)
                    .get(useMedium);
        }

        if (direction == Direction.UP) {
            return ModSpriteShifts.VAULT_TOP
                    .get(tier)
                    .get(useMedium);
        }

        if (direction == Direction.DOWN) {
            return ModSpriteShifts.VAULT_BOTTOM
                    .get(tier)
                    .get(useMedium);
        }

        return ModSpriteShifts.VAULT_SIDE
                .get(tier)
                .get(useMedium);
    }

    @Override
    protected Direction getUpDirection(
            BlockAndTintGetter world,
            BlockPos pos,
            BlockState state,
            Direction face
    ) {
        Direction.Axis vaultAxis =
                TieredItemVaultBlock.getVaultBlockAxis(
                        state
                );

        if (vaultAxis == null) {
            return super.getUpDirection(
                    world,
                    pos,
                    state,
                    face
            );
        }

        boolean xAxis =
                vaultAxis == Direction.Axis.X;

        if (face.getAxis().isVertical()
                && xAxis) {
            return super.getUpDirection(
                    world,
                    pos,
                    state,
                    face
            ).getClockWise();
        }

        if (face.getAxis() == vaultAxis
                || face.getAxis().isVertical()) {
            return super.getUpDirection(
                    world,
                    pos,
                    state,
                    face
            );
        }

        return Direction.fromAxisAndDirection(
                vaultAxis,
                xAxis
                        ? Direction.AxisDirection.POSITIVE
                        : Direction.AxisDirection.NEGATIVE
        );
    }

    @Override
    protected Direction getRightDirection(
            BlockAndTintGetter world,
            BlockPos pos,
            BlockState state,
            Direction face
    ) {
        Direction.Axis vaultAxis =
                TieredItemVaultBlock.getVaultBlockAxis(
                        state
                );

        if (vaultAxis == null) {
            return super.getRightDirection(
                    world,
                    pos,
                    state,
                    face
            );
        }

        if (face.getAxis().isVertical()
                && vaultAxis == Direction.Axis.X) {
            return super.getRightDirection(
                    world,
                    pos,
                    state,
                    face
            ).getClockWise();
        }

        if (face.getAxis() == vaultAxis
                || face.getAxis().isVertical()) {
            return super.getRightDirection(
                    world,
                    pos,
                    state,
                    face
            );
        }

        return Direction.fromAxisAndDirection(
                Direction.Axis.Y,
                face.getAxisDirection()
        );
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
        if (state.getBlock() != other.getBlock()) {
            return false;
        }

        return ConnectivityHandler.isConnected(
                world,
                pos,
                otherPos
        );
    }
}
