package com.venacava.biggervaults.client;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.foundation.block.connected.CTModel;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import net.createmod.catnip.data.Iterate;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TieredFluidTankModel extends CTModel {

    private static final ModelProperty<CullData>
            CULL_PROPERTY = new ModelProperty<>();

    public TieredFluidTankModel(
            BakedModel originalModel,
            CTSpriteShiftEntry sideShift,
            CTSpriteShiftEntry topShift,
            CTSpriteShiftEntry innerShift
    ) {
        super(
                originalModel,
                new TieredFluidTankCTBehaviour(
                        sideShift,
                        topShift,
                        innerShift
                )
        );
    }

    @Override
    protected ModelData.Builder gatherModelData(
            ModelData.Builder builder,
            BlockAndTintGetter world,
            BlockPos pos,
            BlockState state,
            ModelData modelData
    ) {
        super.gatherModelData(
                builder,
                world,
                pos,
                state,
                modelData
        );

        CullData cullData = new CullData();

        for (Direction direction : Iterate.horizontalDirections) {
            boolean connected =
                    ConnectivityHandler.isConnected(
                            world,
                            pos,
                            pos.relative(direction)
                    );

            cullData.setCulled(
                    direction,
                    connected
            );
        }

        return builder.with(
                CULL_PROPERTY,
                cullData
        );
    }

    @Override
    public List<BakedQuad> getQuads(
            BlockState state,
            Direction side,
            RandomSource random,
            ModelData modelData,
            RenderType renderType
    ) {
        if (side != null) {
            return Collections.emptyList();
        }

        List<BakedQuad> quads = new ArrayList<>();

        for (Direction direction : Iterate.directions) {
            if (modelData.has(CULL_PROPERTY)) {
                CullData cullData =
                        modelData.get(CULL_PROPERTY);

                if (cullData != null
                        && cullData.isCulled(direction)) {
                    continue;
                }
            }

            quads.addAll(
                    super.getQuads(
                            state,
                            direction,
                            random,
                            modelData,
                            renderType
                    )
            );
        }

        quads.addAll(
                super.getQuads(
                        state,
                        null,
                        random,
                        modelData,
                        renderType
                )
        );

        return quads;
    }

    private static class CullData {

        private final boolean[] culledFaces =
                new boolean[4];

        private CullData() {
            Arrays.fill(
                    culledFaces,
                    false
            );
        }

        private void setCulled(
                Direction direction,
                boolean culled
        ) {
            if (direction.getAxis().isVertical()) {
                return;
            }

            culledFaces[
                    direction.get2DDataValue()
                    ] = culled;
        }

        private boolean isCulled(
                Direction direction
        ) {
            if (direction.getAxis().isVertical()) {
                return false;
            }

            return culledFaces[
                    direction.get2DDataValue()
                    ];
        }
    }
}