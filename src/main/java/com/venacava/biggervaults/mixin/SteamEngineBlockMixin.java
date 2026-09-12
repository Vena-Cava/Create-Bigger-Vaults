package com.venacava.biggervaults.mixin;

import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlock;
import com.venacava.biggervaults.block.TieredFluidTankBlock;
import com.venacava.biggervaults.blockentity.TieredFluidTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SteamEngineBlock.class)
public abstract class SteamEngineBlockMixin {

    /**
     * Allows a Create Steam Engine to be placed against a
     * Bigger Vaults tiered fluid tank.
     */
    @Inject(
            method = "canAttach",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void biggerVaults$allowTieredFluidTankAttachment(
            LevelReader level,
            BlockPos enginePos,
            Direction tankDirection,
            CallbackInfoReturnable<Boolean> cir
    ) {
        TieredFluidTankBlockEntity tank =
                biggerVaults$getTieredTank(
                        level,
                        enginePos,
                        tankDirection
                );

        if (tank != null) {
            cir.setReturnValue(true);
        }
    }

    /**
     * Re-evaluates the tiered boiler immediately after a Steam Engine
     * is placed.
     */
    @Inject(
            method = "onPlace",
            at = @At("TAIL")
    )
    private void biggerVaults$evaluateTieredBoilerAfterEnginePlaced(
            BlockState engineState,
            Level level,
            BlockPos enginePos,
            BlockState oldState,
            boolean isMoving,
            CallbackInfo ci
    ) {
        if (level.isClientSide) {
            return;
        }

        Direction tankDirection =
                SteamEngineBlock
                        .getFacing(engineState)
                        .getOpposite();

        TieredFluidTankBlockEntity tank =
                biggerVaults$getTieredTank(
                        level,
                        enginePos,
                        tankDirection
                );

        biggerVaults$evaluateController(tank);
    }

    /**
     * Re-evaluates the tiered boiler immediately after a Steam Engine
     * is removed.
     */
    @Inject(
            method = "onRemove",
            at = @At("TAIL")
    )
    private void biggerVaults$evaluateTieredBoilerAfterEngineRemoved(
            BlockState engineState,
            Level level,
            BlockPos enginePos,
            BlockState newState,
            boolean isMoving,
            CallbackInfo ci
    ) {
        if (level.isClientSide) {
            return;
        }

        Direction tankDirection =
                SteamEngineBlock
                        .getFacing(engineState)
                        .getOpposite();

        TieredFluidTankBlockEntity tank =
                biggerVaults$getTieredTank(
                        level,
                        enginePos,
                        tankDirection
                );

        biggerVaults$evaluateController(tank);
    }

    /**
     * Finds the tiered fluid tank in the supplied direction from the
     * Steam Engine.
     */
    @Unique
    @Nullable
    private static TieredFluidTankBlockEntity biggerVaults$getTieredTank(
            LevelReader level,
            BlockPos enginePos,
            Direction tankDirection
    ) {
        BlockPos tankPos =
                enginePos.relative(tankDirection);

        BlockState tankState =
                level.getBlockState(tankPos);

        if (!(tankState.getBlock()
                instanceof TieredFluidTankBlock)) {
            return null;
        }

        BlockEntity blockEntity =
                level.getBlockEntity(tankPos);

        if (blockEntity
                instanceof TieredFluidTankBlockEntity tank) {
            return tank;
        }

        return null;
    }

    /**
     * Resolves the multiblock controller and re-evaluates its boiler data.
     */
    @Unique
    private static void biggerVaults$evaluateController(
            @Nullable TieredFluidTankBlockEntity tank
    ) {
        if (tank == null) {
            return;
        }

        TieredFluidTankBlockEntity controller =
                tank.getControllerBE();

        if (controller == null) {
            return;
        }

        controller.evaluateBoiler();
    }
}