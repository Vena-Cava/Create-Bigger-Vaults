package com.venacava.biggervaults.block;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.item.ItemHelper;
import com.venacava.biggervaults.blockentity.TieredItemVaultBlockEntity;
import com.venacava.biggervaults.content.MaterialTier;
import com.venacava.biggervaults.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

public class TieredItemVaultBlock extends Block
        implements IWrenchable,
        IBE<TieredItemVaultBlockEntity> {

    public static final Property<Direction.Axis> HORIZONTAL_AXIS =
            BlockStateProperties.HORIZONTAL_AXIS;

    public static final BooleanProperty LARGE =
            BooleanProperty.create("large");

    private final MaterialTier tier;

    public TieredItemVaultBlock(
            MaterialTier tier,
            Properties properties
    ) {
        super(properties);

        this.tier = tier;

        registerDefaultState(
                defaultBlockState()
                        .setValue(
                                HORIZONTAL_AXIS,
                                Direction.Axis.X
                        )
                        .setValue(
                                LARGE,
                                false
                        )
        );
    }

    public MaterialTier tier() {
        return tier;
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(
                HORIZONTAL_AXIS,
                LARGE
        );

        super.createBlockStateDefinition(builder);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(
            BlockPlaceContext context
    ) {
        if (context.getPlayer() == null
                || !context.getPlayer().isShiftKeyDown()) {

            BlockState neighbouringState =
                    context.getLevel().getBlockState(
                            context.getClickedPos().relative(
                                    context.getClickedFace()
                                            .getOpposite()
                            )
                    );

            if (neighbouringState.getBlock()
                    instanceof TieredItemVaultBlock neighbouringVault
                    && neighbouringVault.tier() == tier) {

                Direction.Axis neighbouringAxis =
                        neighbouringState.getValue(
                                HORIZONTAL_AXIS
                        );

                return defaultBlockState()
                        .setValue(
                                HORIZONTAL_AXIS,
                                neighbouringAxis
                        )
                        .setValue(
                                LARGE,
                                false
                        );
            }
        }

        return defaultBlockState()
                .setValue(
                        HORIZONTAL_AXIS,
                        context.getHorizontalDirection()
                                .getAxis()
                )
                .setValue(
                        LARGE,
                        false
                );
    }

    @Override
    public void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston
    ) {
        super.onPlace(
                state,
                level,
                pos,
                oldState,
                movedByPiston
        );

        if (oldState.getBlock() == state.getBlock()) {
            return;
        }

        if (movedByPiston || level.isClientSide) {
            return;
        }

        withBlockEntityDo(
                level,
                pos,
                ConnectivityHandler::formMulti
        );
    }

    @Override
    public InteractionResult onWrenched(
            BlockState state,
            UseOnContext context
    ) {
        if (context.getClickedFace()
                .getAxis()
                .isVertical()) {

            BlockEntity blockEntity =
                    context.getLevel().getBlockEntity(
                            context.getClickedPos()
                    );

            if (blockEntity
                    instanceof TieredItemVaultBlockEntity vault) {

                ConnectivityHandler.splitMulti(vault);
                vault.removeController(true);
            }

            state = state.setValue(
                    LARGE,
                    false
            );
        }

        return IWrenchable.super.onWrenched(
                state,
                context
        );
    }

    @Override
    public void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean movedByPiston
    ) {
        if (state.getBlock() == newState.getBlock()) {
            super.onRemove(
                    state,
                    level,
                    pos,
                    newState,
                    movedByPiston
            );
            return;
        }

        BlockEntity blockEntity =
                level.getBlockEntity(pos);

        if (blockEntity
                instanceof TieredItemVaultBlockEntity vault) {

            ItemHelper.dropContents(
                    level,
                    pos,
                    vault.getInventoryOfBlock()
            );

            ConnectivityHandler.splitMulti(vault);
        }

        super.onRemove(
                state,
                level,
                pos,
                newState,
                movedByPiston
        );
    }

    public static boolean isVault(
            BlockState state
    ) {
        return state.getBlock()
                instanceof TieredItemVaultBlock;
    }

    @Nullable
    public static Direction.Axis getVaultBlockAxis(
            BlockState state
    ) {
        if (!isVault(state)) {
            return null;
        }

        return state.getValue(
                HORIZONTAL_AXIS
        );
    }

    public static boolean isLarge(
            BlockState state
    ) {
        return isVault(state)
                && state.getValue(LARGE);
    }

    @Override
    public BlockState rotate(
            BlockState state,
            Rotation rotation
    ) {
        Direction.Axis currentAxis =
                state.getValue(HORIZONTAL_AXIS);

        Direction rotatedDirection =
                rotation.rotate(
                        Direction.fromAxisAndDirection(
                                currentAxis,
                                Direction.AxisDirection.POSITIVE
                        )
                );

        return state.setValue(
                HORIZONTAL_AXIS,
                rotatedDirection.getAxis()
        );
    }

    @Override
    public BlockState mirror(
            BlockState state,
            Mirror mirror
    ) {
        return state;
    }

    @Override
    public SoundType getSoundType(
            BlockState state,
            LevelReader level,
            BlockPos pos,
            @Nullable Entity entity
    ) {
        return super.getSoundType(
                state,
                level,
                pos,
                entity
        );
    }

    @Override
    public boolean hasAnalogOutputSignal(
            BlockState state
    ) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(
            BlockState state,
            Level level,
            BlockPos pos
    ) {
        TieredItemVaultBlockEntity vault =
                getBlockEntity(level, pos);

        if (vault == null) {
            return 0;
        }

        return ItemHelper.calcRedstoneFromInventory(
                vault.getCombinedInventory()
        );
    }

    @Override
    public BlockEntityType<? extends TieredItemVaultBlockEntity>
    getBlockEntityType() {
        return ModBlockEntities.ITEM_VAULTS
                .get(tier)
                .get();
    }

    @Override
    public Class<TieredItemVaultBlockEntity>
    getBlockEntityClass() {
        return TieredItemVaultBlockEntity.class;
    }
}
