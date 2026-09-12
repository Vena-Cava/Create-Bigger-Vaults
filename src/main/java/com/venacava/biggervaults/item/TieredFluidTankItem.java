package com.venacava.biggervaults.item;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.equipment.symmetryWand.SymmetryWandItem;
import com.venacava.biggervaults.block.TieredFluidTankBlock;
import com.venacava.biggervaults.blockentity.TieredFluidTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class TieredFluidTankItem extends BlockItem {

    private static final String SILENCE_TANK_SOUND =
            "SilenceTankSound";

    public TieredFluidTankItem(
            Block block,
            Item.Properties properties
    ) {
        super(block, properties);
    }

    @Override
    public InteractionResult place(
            BlockPlaceContext context
    ) {
        InteractionResult result =
                super.place(context);

        if (!result.consumesAction()) {
            return result;
        }

        tryMultiPlace(context);

        return result;
    }

    private void tryMultiPlace(
            BlockPlaceContext context
    ) {
        Player player = context.getPlayer();

        if (player == null) {
            return;
        }

        /*
         * Sneaking deliberately disables layer placement.
         */
        if (player.isShiftKeyDown()) {
            return;
        }

        Direction placedDirection =
                context.getClickedFace();

        /*
         * Tank layers may only be added above or below.
         */
        if (!placedDirection.getAxis().isVertical()) {
            return;
        }

        /*
         * The first block has already been consumed by super.place().
         */
        ItemStack heldStack =
                context.getItemInHand();

        Level level = context.getLevel();

        /*
         * This is the position at which the first new tank was placed.
         */
        BlockPos firstPlacedPos =
                context.getClickedPos();

        /*
         * Move one block back towards the tank that was clicked.
         */
        BlockPos existingTankPos =
                firstPlacedPos.relative(
                        placedDirection.getOpposite()
                );

        BlockState existingTankState =
                level.getBlockState(existingTankPos);

        if (!(existingTankState.getBlock()
                instanceof TieredFluidTankBlock existingTankBlock)) {
            return;
        }

        if (!(getBlock()
                instanceof TieredFluidTankBlock heldTankBlock)) {
            return;
        }

        /*
         * Different tiers must never be completed together.
         */
        if (existingTankBlock.tier()
                != heldTankBlock.tier()) {
            return;
        }

        /*
         * Match Create's behaviour when a Symmetry Wand is present.
         */
        if (SymmetryWandItem.presentInHotbar(player)) {
            return;
        }

        BlockEntityType<TieredFluidTankBlockEntity>
                blockEntityType =
                heldTankBlock.getTankBlockEntityType();

        TieredFluidTankBlockEntity existingPart =
                ConnectivityHandler.partAt(
                        blockEntityType,
                        level,
                        existingTankPos
                );

        if (existingPart == null) {
            return;
        }

        TieredFluidTankBlockEntity controller =
                existingPart.getControllerBE();

        if (controller == null) {
            return;
        }

        int width = controller.getWidth();

        /*
         * There is no layer to complete on a 1 × 1 tank.
         */
        if (width == 1) {
            return;
        }

        BlockPos layerOrigin;

        if (placedDirection == Direction.DOWN) {
            layerOrigin = controller
                    .getBlockPos()
                    .below();
        } else {
            layerOrigin = controller
                    .getBlockPos()
                    .above(controller.getHeight());
        }

        /*
         * The initially placed block must be in the layer that would
         * be completed. This prevents surprising placement when the
         * player clicks an unrelated part of the structure.
         */
        if (layerOrigin.getY()
                != firstPlacedPos.getY()) {
            return;
        }

        int requiredBlocks = 0;

        /*
         * Validate the whole layer before placing anything else.
         */
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < width; z++) {
                BlockPos targetPos =
                        layerOrigin.offset(
                                x,
                                0,
                                z
                        );

                BlockState targetState =
                        level.getBlockState(targetPos);

                if (isSameTierTank(
                        targetState,
                        heldTankBlock
                )) {
                    continue;
                }

                if (!targetState.canBeReplaced()) {
                    return;
                }

                requiredBlocks++;
            }
        }

        if (!player.isCreative()
                && heldStack.getCount()
                < requiredBlocks) {
            return;
        }

        for (int x = 0; x < width; x++) {
            for (int z = 0; z < width; z++) {
                BlockPos targetPos =
                        layerOrigin.offset(
                                x,
                                0,
                                z
                        );

                BlockState targetState =
                        level.getBlockState(targetPos);

                if (isSameTierTank(
                        targetState,
                        heldTankBlock
                )) {
                    continue;
                }

                BlockPlaceContext additionalContext =
                        BlockPlaceContext.at(
                                context,
                                targetPos,
                                placedDirection
                        );

                player.getPersistentData()
                        .putBoolean(
                                SILENCE_TANK_SOUND,
                                true
                        );

                try {
                    super.place(additionalContext);
                } finally {
                    player.getPersistentData()
                            .remove(
                                    SILENCE_TANK_SOUND
                            );
                }
            }
        }
    }

    private static boolean isSameTierTank(
            BlockState state,
            TieredFluidTankBlock expectedTank
    ) {
        return state.getBlock()
                instanceof TieredFluidTankBlock tank
                && tank.tier() == expectedTank.tier();
    }
}