package com.venacava.biggervaults.mixin;

import com.simibubi.create.content.contraptions.bearing.WindmillBearingBlockEntity.RotationDirection;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.steamEngine.PoweredShaftBlockEntity;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlock;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlockEntity;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.venacava.biggervaults.block.TieredSteamEngineBlock;
import net.minecraft.core.Direction.AxisDirection;
import com.venacava.biggervaults.block.TieredFluidTankBlock;
import com.venacava.biggervaults.blockentity.TieredFluidTankBlockEntity;
import com.venacava.biggervaults.content.FluidTankProperties;
import com.venacava.biggervaults.content.SteamEngineProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SteamEngineBlockEntity.class)
public abstract class SteamEngineBlockEntityMixin
        extends SmartBlockEntity {

    @Shadow
    protected ScrollOptionBehaviour<RotationDirection>
            movementDirection;

    protected SteamEngineBlockEntityMixin(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state
    ) {
        super(type, pos, state);
    }

    /**
     * Runs Create's normal Steam Engine behaviour against a
     * TieredFluidTankBlockEntity.
     *
     * Vanilla Create tanks are left completely untouched and continue through
     * SteamEngineBlockEntity.tick().
     */
    @Inject(
            method = "tick",
            at = @At("HEAD"),
            cancellable = true
    )
    private void biggerVaults$tickTieredBoiler(
            CallbackInfo ci
    ) {
        TieredFluidTankBlockEntity tank =
                biggerVaults$getTieredTank();

        if (tank == null) {
            return;
        }

        /*
         * We are replacing SteamEngineBlockEntity.tick() for this engine, so
         * its normal SmartBlockEntity tick still needs to happen once.
         */
        super.tick();

        SteamEngineBlockEntity engine =
                (SteamEngineBlockEntity) (Object) this;

        PoweredShaftBlockEntity shaft =
                engine.getShaft();

        TieredFluidTankBlockEntity controller =
                tank.getControllerBE();

        if (controller == null || shaft == null) {
            if (!level.isClientSide && shaft != null) {
                shaft.update(
                        worldPosition,
                        0,
                        0
                );
            }

            ci.cancel();
            return;
        }

        /*
         * Keep the controller's engine count correct after engines are placed
         * or removed.
         *
         * We will replace this polling with direct block update hooks later.
         */
        if (!level.isClientSide
                && level.getGameTime() % 20 == 0) {
            controller.evaluateBoiler();
        }

        BlockState shaftState =
                shaft.getBlockState();

        Axis targetAxis = Axis.X;

        if (shaftState.getBlock()
                instanceof IRotate rotatingBlock) {
            targetAxis =
                    rotatingBlock.getRotationAxis(
                            shaftState
                    );
        }

        boolean verticalTarget =
                targetAxis == Axis.Y;

        BlockState engineState =
                getBlockState();

        if (!(engineState.getBlock()
                instanceof SteamEngineBlock)) {
            ci.cancel();
            return;
        }

        Direction facing =
                SteamEngineBlock.getFacing(
                        engineState
                );

        if (facing.getAxis() == Axis.Y) {
            facing =
                    engineState.getValue(
                            SteamEngineBlock.FACING
                    );
        }

        float boilerOutputPerEngine = Math.max(
                0.0f,
                controller
                        .getBoilerData()
                        .getEngineEfficiency(
                                controller.getTotalTankSize(),
                                FluidTankProperties
                                        .get(controller.getTier())
                                        .boilerOutputMultiplier()
                        )
        );

        float engineMaximumOutput = 1.0f;

        if (engineState.getBlock()
                instanceof TieredSteamEngineBlock tieredEngine) {
            engineMaximumOutput =
                    SteamEngineProperties
                            .get(tieredEngine.getTier())
                            .outputMultiplier();
        }

        float efficiency = Math.min(
                boilerOutputPerEngine,
                engineMaximumOutput
        );

        if (efficiency > 0) {
            award(AllAdvancements.STEAM_ENGINE);
        }

        int conveyedSpeedLevel =
                efficiency == 0
                        ? 1
                        : verticalTarget
                        ? 1
                        : (int) GeneratingKineticBlockEntity
                        .convertToDirection(
                                1,
                                facing
                        );

        if (targetAxis == Axis.Z) {
            conveyedSpeedLevel *= -1;
        }

        if (movementDirection.get()
                == RotationDirection.COUNTER_CLOCKWISE) {
            conveyedSpeedLevel *= -1;
        }

        float shaftSpeed =
                shaft.getTheoreticalSpeed();

        if (shaft.hasSource()
                && shaftSpeed != 0
                && conveyedSpeedLevel != 0
                && (shaftSpeed > 0)
                != (conveyedSpeedLevel > 0)) {

            movementDirection.setValue(
                    1 - movementDirection
                            .get()
                            .ordinal()
            );

            conveyedSpeedLevel *= -1;
        }

        shaft.update(
                worldPosition,
                conveyedSpeedLevel,
                efficiency
        );

        ci.cancel();
    }

    /**
     * Allows Create and any other callers of SteamEngineBlockEntity.isValid()
     * to regard a correctly attached tiered tank as valid.
     */
    @Inject(
            method = "isValid",
            at = @At("HEAD"),
            cancellable = true
    )
    private void biggerVaults$isTieredTankValid(
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (biggerVaults$getTieredTank() != null) {
            cir.setReturnValue(true);
        }
    }

    /**
     * Allows tiered Steam Engine blocks to pass Create's exact
     * AllBlocks.STEAM_ENGINE check during the normal engine tick.
     *
     * This is needed when a Bigger Vaults Steam Engine is attached
     * to a normal Create Fluid Tank.
     */
    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target =
                            "Lcom/tterrag/registrate/util/entry/BlockEntry;"
                                    + "has("
                                    + "Lnet/minecraft/world/level/block/state/BlockState;"
                                    + ")Z"
            )
    )
    private boolean biggerVaults$recogniseTieredEngineDuringTick(
            BlockEntry<?> createSteamEngine,
            BlockState blockState
    ) {
        return createSteamEngine.has(blockState)
                || blockState.getBlock()
                instanceof SteamEngineBlock;
    }

    /**
     * Calculates the moving-part angle for Bigger Vaults Steam Engines.
     *
     * Create's original method rejects every block except its own registered
     * Steam Engine before calculating this angle.
     */
    @Inject(
            method = "getTargetAngle",
            at = @At("HEAD"),
            cancellable = true
    )
    private void biggerVaults$getTieredEngineTargetAngle(
            CallbackInfoReturnable<Float> cir
    ) {
        BlockState engineState = getBlockState();

        if (!(engineState.getBlock()
                instanceof TieredSteamEngineBlock)) {
            return;
        }

        SteamEngineBlockEntity engine =
                (SteamEngineBlockEntity) (Object) this;

        PoweredShaftBlockEntity shaft =
                engine.getShaft();

        if (shaft == null) {
            cir.setReturnValue(null);
            return;
        }

        Direction facing =
                SteamEngineBlock.getFacing(engineState);

        Axis facingAxis =
                facing.getAxis();

        Axis shaftAxis =
                KineticBlockEntityRenderer
                        .getRotationAxisOf(shaft);

        if (shaftAxis == facingAxis) {
            cir.setReturnValue(null);
            return;
        }

        float angle =
                KineticBlockEntityRenderer
                        .getAngleForBe(
                                shaft,
                                shaft.getBlockPos(),
                                shaftAxis
                        );

        if (shaftAxis.isHorizontal()
                && (facingAxis == Axis.X
                ^ facing.getAxisDirection()
                == AxisDirection.POSITIVE)) {
            angle *= -1;
        }

        if (shaftAxis == Axis.X
                && facing == Direction.DOWN) {
            angle *= -1;
        }

        cir.setReturnValue(angle);
    }

    @Unique
    private TieredFluidTankBlockEntity
    biggerVaults$getTieredTank() {
        if (level == null) {
            return null;
        }

        BlockState engineState =
                getBlockState();

        if (!(engineState.getBlock()
                instanceof SteamEngineBlock)) {
            return null;
        }

        Direction facing =
                SteamEngineBlock.getFacing(
                        engineState
                );

        BlockPos tankPos =
                worldPosition.relative(
                        facing.getOpposite()
                );

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
}