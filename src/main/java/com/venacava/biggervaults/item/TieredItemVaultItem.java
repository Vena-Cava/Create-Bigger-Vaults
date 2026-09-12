package com.venacava.biggervaults.item;

import com.simibubi.create.content.equipment.symmetryWand.SymmetryWandItem;
import com.venacava.biggervaults.block.TieredItemVaultBlock;
import com.venacava.biggervaults.blockentity.TieredItemVaultBlockEntity;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class TieredItemVaultItem extends BlockItem {

    public TieredItemVaultItem(
            TieredItemVaultBlock block,
            Properties properties
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

        // Sneaking deliberately bypasses automatic layer placement.
        if (player.isShiftKeyDown()) {
            return;
        }

        // Match Create's behaviour and avoid interacting badly
        // with the Symmetry Wand's own placement system.
        if (SymmetryWandItem.presentInHotbar(player)) {
            return;
        }

        Direction clickedFace =
                context.getClickedFace();

        ItemStack heldStack =
                context.getItemInHand();

        Level level =
                context.getLevel();

        /*
         * BlockItem.place() has already placed the first block.
         * The existing vault that was clicked is one block behind it.
         */
        BlockPos placedPos =
                context.getClickedPos();

        BlockPos existingVaultPos =
                placedPos.relative(
                        clickedFace.getOpposite()
                );

        BlockState existingState =
                level.getBlockState(
                        existingVaultPos
                );

        if (!(existingState.getBlock()
                instanceof TieredItemVaultBlock existingBlock)) {
            return;
        }

        if (!(getBlock()
                instanceof TieredItemVaultBlock heldBlock)) {
            return;
        }

        // Never complete a layer using another vault tier.
        if (existingBlock.tier() != heldBlock.tier()) {
            return;
        }

        if (!(level.getBlockEntity(existingVaultPos)
                instanceof TieredItemVaultBlockEntity member)) {
            return;
        }

        TieredItemVaultBlockEntity controller =
                member.getControllerBE();

        if (controller == null) {
            return;
        }

        int width = controller.getWidth();

        /*
         * A 1x1 vault only needs the block already placed by the
         * normal BlockItem placement. No additional blocks are needed.
         */
        if (width <= 1) {
            return;
        }

        Direction.Axis vaultAxis =
                TieredItemVaultBlock.getVaultBlockAxis(
                        existingState
                );

        if (vaultAxis == null) {
            return;
        }

        /*
         * Automatic completion only occurs when extending one of
         * the two ends of the vault.
         */
        if (clickedFace.getAxis() != vaultAxis) {
            return;
        }

        Direction positiveDirection =
                Direction.fromAxisAndDirection(
                        vaultAxis,
                        Direction.AxisDirection.POSITIVE
                );

        BlockPos newLayerOrigin;

        /*
         * The controller sits at the minimum-coordinate corner.
         *
         * When extending from the negative end, the new layer starts
         * one block before the controller.
         *
         * When extending from the positive end, the new layer starts
         * immediately after the current length.
         */
        if (clickedFace
                == positiveDirection.getOpposite()) {
            newLayerOrigin =
                    controller.getBlockPos().relative(
                            positiveDirection.getOpposite()
                    );
        } else {
            newLayerOrigin =
                    controller.getBlockPos().relative(
                            positiveDirection,
                            controller.getHeight()
                    );
        }

        /*
         * Ensure the normal placement really occurred in the layer
         * that is about to be completed.
         */
        if (VecHelper.getCoordinate(
                newLayerOrigin,
                vaultAxis
        ) != VecHelper.getCoordinate(
                placedPos,
                vaultAxis
        )) {
            return;
        }

        int additionalBlocksRequired = 0;

        /*
         * First pass:
         * Verify that the entire layer can be completed and count
         * how many additional blocks are needed.
         */
        for (int first = 0;
             first < width;
             first++) {
            for (int second = 0;
                 second < width;
                 second++) {

                BlockPos targetPos =
                        getLayerPosition(
                                newLayerOrigin,
                                vaultAxis,
                                first,
                                second
                        );

                BlockState targetState =
                        level.getBlockState(targetPos);

                if (isMatchingVault(
                        targetState,
                        heldBlock
                )) {
                    continue;
                }

                if (!targetState.canBeReplaced()) {
                    return;
                }

                additionalBlocksRequired++;
            }
        }

        if (!player.isCreative()
                && heldStack.getCount()
                < additionalBlocksRequired) {
            return;
        }

        /*
         * Second pass:
         * Place every missing block using a normal BlockPlaceContext.
         * This preserves the vault axis and invokes all normal block
         * placement and connectivity behaviour.
         */
        for (int first = 0;
             first < width;
             first++) {
            for (int second = 0;
                 second < width;
                 second++) {

                BlockPos targetPos =
                        getLayerPosition(
                                newLayerOrigin,
                                vaultAxis,
                                first,
                                second
                        );

                BlockState targetState =
                        level.getBlockState(targetPos);

                if (isMatchingVault(
                        targetState,
                        heldBlock
                )) {
                    continue;
                }

                BlockPlaceContext additionalContext =
                        BlockPlaceContext.at(
                                context,
                                targetPos,
                                clickedFace
                        );

                super.place(additionalContext);
            }
        }
    }

    private static BlockPos getLayerPosition(
            BlockPos origin,
            Direction.Axis vaultAxis,
            int first,
            int second
    ) {
        /*
         * The length runs along the vault axis.
         * Its square cross-section occupies Y and the other
         * horizontal axis.
         */
        if (vaultAxis == Direction.Axis.X) {
            return origin.offset(
                    0,
                    first,
                    second
            );
        }

        return origin.offset(
                first,
                second,
                0
        );
    }

    private static boolean isMatchingVault(
            BlockState state,
            TieredItemVaultBlock expectedBlock
    ) {
        return state.getBlock() == expectedBlock;
    }
}