package com.venacava.biggervaults.block;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
import com.simibubi.create.content.fluids.transfer.GenericItemFilling;
import com.simibubi.create.foundation.fluid.FluidHelper;
import com.venacava.biggervaults.blockentity.TieredFluidTankBlockEntity;
import com.venacava.biggervaults.content.MaterialTier;
import com.venacava.biggervaults.registry.ModBlockEntities;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

public class TieredFluidTankBlock
        extends BaseEntityBlock
        implements IWrenchable {
    public static final BooleanProperty TOP =
            FluidTankBlock.TOP;

    public static final BooleanProperty BOTTOM =
            FluidTankBlock.BOTTOM;

    public static final EnumProperty<FluidTankBlock.Shape> SHAPE =
            FluidTankBlock.SHAPE;

    private final MaterialTier tier;

    public TieredFluidTankBlock(
            MaterialTier tier,
            Properties properties
    ) {
        super(properties);
        this.tier = tier;

        registerDefaultState(
                defaultBlockState()
                        .setValue(TOP, true)
                        .setValue(BOTTOM, true)
                        .setValue(
                                SHAPE,
                                FluidTankBlock.Shape.PLAIN
                        )
        );
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<
                    net.minecraft.world.level.block.Block,
                    BlockState
                    > builder
    ) {
        super.createBlockStateDefinition(builder);

        builder.add(
                TOP,
                BOTTOM,
                SHAPE
        );
    }

    public MaterialTier tier() {
        return tier;
    }

    public BlockEntityType<TieredFluidTankBlockEntity>
    getTankBlockEntityType() {
        return ModBlockEntities.FLUID_TANKS
                .get(tier)
                .get();
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        throw new UnsupportedOperationException(
                "Tiered fluid tank blocks are registry-created"
        );
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected boolean hasAnalogOutputSignal(
            BlockState state
    ) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(
            BlockState state,
            Level level,
            BlockPos pos
    ) {
        BlockEntity blockEntity =
                level.getBlockEntity(pos);

        if (!(blockEntity
                instanceof TieredFluidTankBlockEntity tank)) {
            return 0;
        }

        IFluidHandler inventory =
                tank.getTankInventory();

        int capacity =
                inventory.getTankCapacity(0);

        int fluidAmount =
                inventory.getFluidInTank(0)
                        .getAmount();

        if (capacity <= 0 || fluidAmount <= 0) {
            return 0;
        }

        /*
         * Match normal container-style comparator behaviour:
         *
         * Empty       = 0
         * Part-filled = 1 through 14
         * Full        = 15
         */
        return Math.min(
                15,
                1 + (int) (
                        (long) fluidAmount * 14L
                                / capacity
                )
        );
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(
            BlockPos blockPos,
            BlockState blockState
    ) {
        return new TieredFluidTankBlockEntity(
                getTankBlockEntityType(),
                blockPos,
                blockState
        );
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState blockState,
            BlockEntityType<T> blockEntityType
    ) {
        return createTickerHelper(
                blockEntityType,
                getTankBlockEntityType(),
                TieredFluidTankBlockEntity::tankTick
        );
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack heldStack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        if (heldStack.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // Match Create's normal Fluid Tank behaviour:
        // direct fluid-container interaction is Creative-only.
        if (!player.isCreative()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        TieredFluidTankBlockEntity tank =
                ConnectivityHandler.partAt(
                        getTankBlockEntityType(),
                        level,
                        pos
                );

        if (tank == null) {
            return ItemInteractionResult.FAIL;
        }

        IFluidHandler fluidHandler =
                level.getCapability(
                        Capabilities.FluidHandler.BLOCK,
                        tank.getBlockPos(),
                        null
                );

        if (fluidHandler == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        FluidStack fluidBefore =
                fluidHandler.getFluidInTank(0).copy();

        FluidHelper.FluidExchange exchange = null;

        if (FluidHelper.tryEmptyItemIntoBE(
                level,
                player,
                hand,
                heldStack,
                tank
        )) {
            exchange = FluidHelper.FluidExchange.ITEM_TO_TANK;
        } else if (FluidHelper.tryFillItemFromBE(
                level,
                player,
                hand,
                heldStack,
                tank
        )) {
            exchange = FluidHelper.FluidExchange.TANK_TO_ITEM;
        }

        if (exchange == null) {
            /*
             * Consume the interaction when the held item is recognised as a
             * fluid container, even when the complete transfer cannot occur.
             *
             * This stops a water or lava bucket from placing its fluid into
             * the world when the tank lacks a full bucket of available space.
             */
            if (GenericItemEmptying.canItemBeEmptied(level, heldStack)
                    || GenericItemFilling.canItemBeFilled(level, heldStack)) {

                return ItemInteractionResult.SUCCESS;
            }

            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        FluidStack fluidAfter =
                fluidHandler.getFluidInTank(0).copy();

        SoundEvent sound = null;

        if (exchange == FluidHelper.FluidExchange.ITEM_TO_TANK
                && !fluidAfter.isEmpty()) {

            sound = FluidHelper.getEmptySound(fluidAfter);
        }

        if (exchange == FluidHelper.FluidExchange.TANK_TO_ITEM
                && !fluidBefore.isEmpty()) {

            sound = FluidHelper.getFillSound(fluidBefore);
        }

        if (sound != null && !level.isClientSide) {
            level.playSound(
                    null,
                    pos,
                    sound,
                    SoundSource.BLOCKS,
                    0.5f,
                    1.0f
            );
        }

        if (!FluidStack.isSameFluidSameComponents(
                fluidAfter,
                fluidBefore
        )) {
            TieredFluidTankBlockEntity controller =
                    tank.getControllerBE();

            if (controller != null && !level.isClientSide) {
                controller.setChanged();
                level.sendBlockUpdated(
                        controller.getBlockPos(),
                        controller.getBlockState(),
                        controller.getBlockState(),
                        3
                );
            }
        }

        return ItemInteractionResult.SUCCESS;
    }

    @Override
    public net.minecraft.world.InteractionResult onWrenched(
            BlockState state,
            net.minecraft.world.item.context.UseOnContext context
    ) {
        BlockEntity blockEntity =
                context.getLevel()
                        .getBlockEntity(
                                context.getClickedPos()
                        );

        if (blockEntity instanceof TieredFluidTankBlockEntity tank) {
            tank.toggleWindows();
        }

        return net.minecraft.world.InteractionResult.SUCCESS;
    }

    @Override
    public net.minecraft.world.InteractionResult onSneakWrenched(
            BlockState state,
            net.minecraft.world.item.context.UseOnContext context
    ) {
        BlockEntity blockEntity =
                context.getLevel()
                        .getBlockEntity(
                                context.getClickedPos()
                        );

        if (blockEntity instanceof TieredFluidTankBlockEntity tank) {

            ConnectivityHandler.splitMulti(tank);
            tank.removeController(true);
        }

        return IWrenchable.super.onSneakWrenched(
                state,
                context
        );
    }

    @Override
    public SoundType getSoundType(
            BlockState state,
            LevelReader level,
            BlockPos pos,
            @Nullable Entity entity
    ) {
        SoundType soundType =
                super.getSoundType(
                        state,
                        level,
                        pos,
                        entity
                );

        if (entity != null
                && entity.getPersistentData()
                .contains("SilenceTankSound")) {

            return FluidTankBlock.SILENCED_METAL;
        }

        return soundType;
    }

    @Override
    protected void onRemove(
            BlockState oldState,
            Level level,
            BlockPos blockPos,
            BlockState newState,
            boolean movedByPiston
    ) {
        if (oldState.hasBlockEntity()
                && (
                oldState.getBlock() != newState.getBlock()
                        || !newState.hasBlockEntity()
        )) {

            BlockEntity blockEntity =
                    level.getBlockEntity(blockPos);

            if (blockEntity
                    instanceof TieredFluidTankBlockEntity tank) {
                level.removeBlockEntity(blockPos);
                ConnectivityHandler.splitMulti(tank);
            }
        }

        super.onRemove(
                oldState,
                level,
                blockPos,
                newState,
                movedByPiston
        );
    }
}
