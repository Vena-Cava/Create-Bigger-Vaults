package com.venacava.biggervaults.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.venacava.biggervaults.blockentity.TieredFluidTankBlockEntity;
import com.venacava.biggervaults.content.MaterialTier;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.platform.NeoForgeCatnipServices;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public class TieredFluidTankRenderer
        extends SafeBlockEntityRenderer<
        TieredFluidTankBlockEntity
        > {

    public TieredFluidTankRenderer(
            BlockEntityRendererProvider.Context context
    ) {
    }

    @Override
    protected void renderSafe(
            TieredFluidTankBlockEntity blockEntity,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (!blockEntity.isController()) {
            return;
        }

        /*
         * Active boilers are sealed and do not display their stored fluid.
         * Render Create's pressure gauges instead.
         */
        if (blockEntity
                .getBoilerData()
                .isActive()) {

            renderAsBoiler(
                    blockEntity,
                    partialTicks,
                    poseStack,
                    bufferSource,
                    packedLight
            );

            return;
        }

        if (!blockEntity.hasWindows()) {
            return;
        }

        renderFluid(
                blockEntity,
                partialTicks,
                poseStack,
                bufferSource,
                packedLight
        );
    }

    private void renderFluid(
            TieredFluidTankBlockEntity blockEntity,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        LerpedFloat fluidLevel =
                blockEntity.getFluidLevel();

        if (fluidLevel == null) {
            return;
        }

        float capHeight =
                1.0f / 4.0f;

        float tankHullWidth =
                1.0f / 16.0f
                        + 1.0f / 128.0f;

        float minimumPuddleHeight =
                1.0f / 16.0f;

        float totalHeight =
                blockEntity.getHeight()
                        - 2.0f * capHeight
                        - minimumPuddleHeight;

        if (totalHeight <= 0) {
            return;
        }

        float level =
                fluidLevel.getValue(
                        partialTicks
                );

        if (level
                < 1.0f
                / (
                512.0f
                        * totalHeight
        )) {
            return;
        }

        float clampedLevel =
                Mth.clamp(
                        level * totalHeight,
                        0,
                        totalHeight
                );

        FluidTank tank =
                blockEntity.getTankInventory();

        FluidStack fluidStack =
                tank.getFluid();

        if (fluidStack.isEmpty()) {
            return;
        }

        boolean lighterThanAir =
                fluidStack
                        .getFluid()
                        .getFluidType()
                        .isLighterThanAir();

        float xMin =
                tankHullWidth;

        float xMax =
                xMin
                        + blockEntity.getWidth()
                        - 2.0f * tankHullWidth;

        float yMin =
                totalHeight
                        + capHeight
                        + minimumPuddleHeight
                        - clampedLevel;

        float yMax =
                yMin + clampedLevel;

        if (lighterThanAir) {
            yMin +=
                    totalHeight
                            - clampedLevel;

            yMax +=
                    totalHeight
                            - clampedLevel;
        }

        float zMin =
                tankHullWidth;

        float zMax =
                zMin
                        + blockEntity.getWidth()
                        - 2.0f * tankHullWidth;

        poseStack.pushPose();

        poseStack.translate(
                0,
                clampedLevel
                        - totalHeight,
                0
        );

        NeoForgeCatnipServices
                .FLUID_RENDERER
                .renderFluidBox(
                        fluidStack,
                        xMin,
                        yMin,
                        zMin,
                        xMax,
                        yMax,
                        zMax,
                        bufferSource,
                        poseStack,
                        packedLight,
                        false,
                        true
                );

        poseStack.popPose();
    }

    private void renderAsBoiler(
            TieredFluidTankBlockEntity blockEntity,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        blockEntity
                .getBoilerData()
                .updateOcclusion(
                        blockEntity
                );

        BlockState blockState =
                blockEntity.getBlockState();

        MaterialTier tier =
                blockEntity.getTier();

        PartialModel gaugeModel =
                ModPartialModels.BOILER_GAUGES.get(tier);

        PartialModel gaugeDialModel =
                ModPartialModels.BOILER_GAUGE_DIALS.get(tier);

        if (gaugeModel == null
                || gaugeDialModel == null) {
            return;
        }

        VertexConsumer vertexConsumer =
                bufferSource.getBuffer(
                        RenderType.cutout()
                );

        poseStack.pushPose();

        TransformStack.of(
                poseStack
        ).translate(
                blockEntity.getWidth()
                        / 2.0f,
                0.5f,
                blockEntity.getWidth()
                        / 2.0f
        );

        float dialPivotY =
                6.0f / 16.0f;

        float dialPivotZ =
                8.0f / 16.0f;

        float progress =
                blockEntity
                        .getBoilerData()
                        .gauge
                        .getValue(
                                partialTicks
                        );

        for (Direction direction
                : Iterate.horizontalDirections) {

            if (blockEntity
                    .getBoilerData()
                    .occludedDirections[
                    direction.get2DDataValue()
                    ]) {

                continue;
            }

            poseStack.pushPose();

            float rotation =
                    -direction.toYRot()
                            - 90.0f;

            CachedBuffers.partial(
                            gaugeModel,
                            blockState
                    )
                    .rotateYDegrees(
                            rotation
                    )
                    .uncenter()
                    .translate(
                            blockEntity.getWidth()
                                    / 2.0f
                                    - 6.0f / 16.0f,
                            0,
                            0
                    )
                    .light(
                            packedLight
                    )
                    .renderInto(
                            poseStack,
                            vertexConsumer
                    );

            CachedBuffers.partial(
                            gaugeDialModel,
                            blockState
                    )
                    .rotateYDegrees(
                            rotation
                    )
                    .uncenter()
                    .translate(
                            blockEntity.getWidth()
                                    / 2.0f
                                    - 6.0f / 16.0f,
                            0,
                            0
                    )
                    .translate(
                            0,
                            dialPivotY,
                            dialPivotZ
                    )
                    .rotateXDegrees(
                            -145.0f
                                    * progress
                                    + 90.0f
                    )
                    .translate(
                            0,
                            -dialPivotY,
                            -dialPivotZ
                    )
                    .light(
                            packedLight
                    )
                    .renderInto(
                            poseStack,
                            vertexConsumer
                    );

            poseStack.popPose();
        }

        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(
            TieredFluidTankBlockEntity blockEntity
    ) {
        return blockEntity.isController();
    }
}