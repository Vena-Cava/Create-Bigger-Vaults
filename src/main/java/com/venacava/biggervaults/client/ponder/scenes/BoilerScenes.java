package com.venacava.biggervaults.client.ponder.scenes;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public final class BoilerScenes {

    private BoilerScenes() {
    }

    public static void buildingBetterBoilers(
            SceneBuilder builder,
            SceneBuildingUtil util
    ) {
        CreateSceneBuilder scene =
                new CreateSceneBuilder(builder);

        scene.title(
                "fluid_tank_boiler",
                "Building Better Boilers"
        );

        scene.scaleSceneView(0.85F);

        Selection platform =
                util.select().fromTo(
                        0,
                        0,
                        0,
                        6,
                        0,
                        6
                );

        Selection waterSupply =
                util.select().fromTo(
                        3,
                        0,
                        1,
                        5,
                        0,
                        1
                );

        Selection waterPipes =
                util.select().fromTo(
                        4,
                        1,
                        1,
                        5,
                        2,
                        2
                );

        Selection heatSources =
                util.select().fromTo(
                        4,
                        1,
                        3,
                        5,
                        1,
                        4
                );

        Selection tanks =
                util.select().fromTo(
                        4,
                        2,
                        3,
                        5,
                        2,
                        4
                );

        BlockPos tankControllerPos =
                util.grid().at(
                        4,
                        2,
                        3
                );

        BlockPos enginePos =
                util.grid().at(
                        3,
                        2,
                        3
                );

        Selection engine =
                util.select()
                        .position(enginePos)
                        .add(
                                util.select().position(
                                        util.grid().at(
                                                1,
                                                2,
                                                3
                                        )
                                )
                        );

        /*
         * Each higher tier is stored one level above the previous tier
         * in the Ponder structure.
         *
         * Tanks and engines are kept as separate independent sections.
         */

        Selection zincTanks =
                util.select().fromTo(
                        4,
                        3,
                        3,
                        5,
                        3,
                        4
                );

        Selection zincEngine =
                util.select()
                        .position(
                                util.grid().at(3, 3, 3)
                        )
                        .add(
                                util.select().position(
                                        util.grid().at(1, 3, 3)
                                )
                        );

        Selection ironTanks =
                util.select().fromTo(
                        4,
                        4,
                        3,
                        5,
                        4,
                        4
                );

        Selection ironEngine =
                util.select()
                        .position(
                                util.grid().at(3, 4, 3)
                        )
                        .add(
                                util.select().position(
                                        util.grid().at(1, 4, 3)
                                )
                        );

        Selection emeraldTanks =
                util.select().fromTo(
                        4,
                        5,
                        3,
                        5,
                        5,
                        4
                );

        Selection emeraldEngine =
                util.select()
                        .position(
                                util.grid().at(3, 5, 3)
                        )
                        .add(
                                util.select().position(
                                        util.grid().at(1, 5, 3)
                                )
                        );

        Selection goldTanks =
                util.select().fromTo(
                        4,
                        6,
                        3,
                        5,
                        6,
                        4
                );

        Selection goldEngine =
                util.select()
                        .position(
                                util.grid().at(3, 6, 3)
                        )
                        .add(
                                util.select().position(
                                        util.grid().at(1, 6, 3)
                                )
                        );

        Selection diamondTanks =
                util.select().fromTo(
                        4,
                        7,
                        3,
                        5,
                        7,
                        4
                );

        Selection diamondEngine =
                util.select()
                        .position(
                                util.grid().at(3, 7, 3)
                        )
                        .add(
                                util.select().position(
                                        util.grid().at(1, 7, 3)
                                )
                        );

        Selection obsidianTanks =
                util.select().fromTo(
                        4,
                        8,
                        3,
                        5,
                        8,
                        4
                );

        Selection obsidianEngine =
                util.select()
                        .position(
                                util.grid().at(3, 8, 3)
                        )
                        .add(
                                util.select().position(
                                        util.grid().at(1, 8, 3)
                                )
                        );

        Selection netheriteTanks =
                util.select().fromTo(
                        4,
                        9,
                        3,
                        5,
                        9,
                        4
                );

        Selection netheriteEngine =
                util.select()
                        .position(
                                util.grid().at(3, 9, 3)
                        )
                        .add(
                                util.select().position(
                                        util.grid().at(1, 9, 3)
                                )
                        );

        Selection outputShaft =
                util.select().position(
                        util.grid().at(
                                1,
                                2,
                                3
                        )
                );

        Selection pump =
                util.select().position(
                        util.grid().at(
                                5,
                                2,
                                2
                        )
                );

        Selection secondPumpCog =
                util.select().position(
                        util.grid().at(
                                5,
                                0,
                                7
                        )
                );

        Selection thirdPumpCog =
                util.select().position(
                        util.grid().at(
                                6,
                                1,
                                7
                        )
                );

        Selection pumpDriveShafts =
                util.select().fromTo(
                        6,
                        1,
                        2,
                        6,
                        1,
                        6
                );

        scene.world().showSection(
                platform,
                Direction.UP
        );

        scene.idle(15);

        scene.world().showSection(
                waterSupply,
                Direction.UP
        );

        scene.idle(8);

        scene.world().showSection(
                waterPipes,
                Direction.SOUTH
        );

        scene.idle(8);

        scene.world().showSection(
                secondPumpCog,
                Direction.DOWN
        );

        scene.world().showSection(
                thirdPumpCog,
                Direction.DOWN
        );

        scene.world().showSection(
                pumpDriveShafts,
                Direction.DOWN
        );

        scene.world().setKineticSpeed(
                secondPumpCog,
                -32.0F
        );

        scene.world().setKineticSpeed(
                thirdPumpCog,
                32.0F
        );

        scene.world().setKineticSpeed(
                pumpDriveShafts,
                32.0F
        );

        scene.world().setKineticSpeed(
                pump,
                -32.0F
        );

        scene.world().showSection(
                heatSources,
                Direction.DOWN
        );

        scene.idle(8);

        scene.world().showSection(
                tanks,
                Direction.DOWN
        );

        scene.idle(8);

        scene.overlay()
                .showText(55)
                .attachKeyFrame()
                .text(
                        "Water is pumped into the Fluid Tanks."
                )
                .pointAt(
                        util.vector().blockSurface(
                                tankControllerPos,
                                Direction.UP
                        )
                )
                .placeNearTarget();

        scene.idle(65);

        scene.overlay()
                .showText(60)
                .attachKeyFrame()
                .text(
                        "Heat and water allow Fluid Tanks to function as a boiler."
                )
                .pointAt(
                        util.vector().blockSurface(
                                tankControllerPos,
                                Direction.UP
                        )
                )
                .placeNearTarget();

        scene.idle(70);

        scene.world().showSection(
                engine,
                Direction.EAST
        );

        scene.idle(8);

        scene.overlay()
                .showText(65)
                .attachKeyFrame()
                .text(
                        "Steam Engines convert the boiler's steam into rotational force."
                )
                .pointAt(
                        util.vector().blockSurface(
                                enginePos,
                                Direction.UP
                        )
                )
                .placeNearTarget();

        scene.idle(75);

        scene.overlay()
                .showText(70)
                .attachKeyFrame()
                .text(
                        "Higher tier Fluid Tanks create more powerful boilers."
                )
                .pointAt(
                        util.vector().blockSurface(
                                tankControllerPos,
                                Direction.UP
                        )
                )
                .placeNearTarget();

        scene.idle(80);

        /*
         * Wooden -> Zinc
         */
        scene.world().hideSection(
                tanks,
                Direction.UP
        );

        scene.world().hideSection(
                engine,
                Direction.UP
        );

        scene.idle(15);

        ElementLink<WorldSectionElement> zincTankSection =
                showTierPart(
                        scene,
                        util,
                        zincTanks,
                        3
                );

        ElementLink<WorldSectionElement> zincEngineSection =
                showTierPart(
                        scene,
                        util,
                        zincEngine,
                        3
                );

        scene.effects().indicateSuccess(
                tankControllerPos
        );

        scene.idle(30);

        /*
         * Zinc -> Iron
         */
        hideTierPart(
                scene,
                zincTankSection
        );

        hideTierPart(
                scene,
                zincEngineSection
        );

        scene.idle(15);

        ElementLink<WorldSectionElement> ironTankSection =
                showTierPart(
                        scene,
                        util,
                        ironTanks,
                        4
                );

        ElementLink<WorldSectionElement> ironEngineSection =
                showTierPart(
                        scene,
                        util,
                        ironEngine,
                        4
                );

        scene.effects().indicateSuccess(
                tankControllerPos
        );

        scene.idle(30);

        /*
         * Iron -> Emerald
         */
        hideTierPart(
                scene,
                ironTankSection
        );

        hideTierPart(
                scene,
                ironEngineSection
        );

        scene.idle(15);

        ElementLink<WorldSectionElement> emeraldTankSection =
                showTierPart(
                        scene,
                        util,
                        emeraldTanks,
                        5
                );

        ElementLink<WorldSectionElement> emeraldEngineSection =
                showTierPart(
                        scene,
                        util,
                        emeraldEngine,
                        5
                );

        scene.effects().indicateSuccess(
                tankControllerPos
        );

        scene.idle(30);

        /*
         * Temporarily replace the Emerald Steam Engine
         * with an Iron Steam Engine.
         */
        hideTierPart(
                scene,
                emeraldEngineSection
        );

        scene.idle(15);

        ironEngineSection =
                showTierPart(
                        scene,
                        util,
                        ironEngine,
                        4
                );

        scene.effects().indicateRedstone(
                enginePos
        );

        scene.idle(15);

        scene.overlay()
                .showText(70)
                .attachKeyFrame()
                .text(
                        "Matching the Steam Engine tier prevents steam from being wasted."
                )
                .pointAt(
                        util.vector().blockSurface(
                                enginePos,
                                Direction.UP
                        )
                )
                .placeNearTarget();

        scene.idle(80);

        /*
         * Restore the Emerald Steam Engine.
         */
        hideTierPart(
                scene,
                ironEngineSection
        );

        scene.idle(15);

        emeraldEngineSection =
                showTierPart(
                        scene,
                        util,
                        emeraldEngine,
                        5
                );

        scene.effects().indicateSuccess(
                enginePos
        );

        scene.idle(15);

        /*
         * Emerald -> Gold
         */
        hideTierPart(
                scene,
                emeraldTankSection
        );

        hideTierPart(
                scene,
                emeraldEngineSection
        );

        scene.idle(15);

        ElementLink<WorldSectionElement> goldTankSection =
                showTierPart(
                        scene,
                        util,
                        goldTanks,
                        6
                );

        ElementLink<WorldSectionElement> goldEngineSection =
                showTierPart(
                        scene,
                        util,
                        goldEngine,
                        6
                );

        scene.effects().indicateSuccess(
                tankControllerPos
        );

        scene.idle(30);

        /*
         * Gold -> Diamond
         */
        hideTierPart(
                scene,
                goldTankSection
        );

        hideTierPart(
                scene,
                goldEngineSection
        );

        scene.idle(15);

        ElementLink<WorldSectionElement> diamondTankSection =
                showTierPart(
                        scene,
                        util,
                        diamondTanks,
                        7
                );

        ElementLink<WorldSectionElement> diamondEngineSection =
                showTierPart(
                        scene,
                        util,
                        diamondEngine,
                        7
                );

        scene.effects().indicateSuccess(
                tankControllerPos
        );

        scene.idle(30);

        /*
         * Diamond -> Obsidian
         */
        hideTierPart(
                scene,
                diamondTankSection
        );

        hideTierPart(
                scene,
                diamondEngineSection
        );

        scene.idle(15);

        ElementLink<WorldSectionElement> obsidianTankSection =
                showTierPart(
                        scene,
                        util,
                        obsidianTanks,
                        8
                );

        ElementLink<WorldSectionElement> obsidianEngineSection =
                showTierPart(
                        scene,
                        util,
                        obsidianEngine,
                        8
                );

        scene.effects().indicateSuccess(
                tankControllerPos
        );

        scene.idle(30);

        /*
         * Obsidian -> Netherite
         */
        hideTierPart(
                scene,
                obsidianTankSection
        );

        hideTierPart(
                scene,
                obsidianEngineSection
        );

        scene.idle(15);

        ElementLink<WorldSectionElement> netheriteTankSection =
                showTierPart(
                        scene,
                        util,
                        netheriteTanks,
                        9
                );

        ElementLink<WorldSectionElement> netheriteEngineSection =
                showTierPart(
                        scene,
                        util,
                        netheriteEngine,
                        9
                );

        scene.effects().indicateSuccess(
                tankControllerPos
        );

        scene.effects().indicateSuccess(
                enginePos
        );

        scene.idle(40);

        scene.markAsFinished();
    }

    private static ElementLink<WorldSectionElement> showTierPart(
            CreateSceneBuilder scene,
            SceneBuildingUtil util,
            Selection part,
            int sourceY
    ) {
        ElementLink<WorldSectionElement> section =
                scene.world().showIndependentSectionImmediately(
                        part
                );

        int targetOffset = 2 - sourceY;

        scene.world().moveSection(
                section,
                util.vector().of(
                        0,
                        targetOffset + 1,
                        0
                ),
                0
        );

        scene.idle(2);

        scene.world().moveSection(
                section,
                util.vector().of(
                        0,
                        -1,
                        0
                ),
                10
        );

        scene.idle(5);

        return section;
    }

    private static void hideTierPart(
            CreateSceneBuilder scene,
            ElementLink<WorldSectionElement> section
    ) {
        scene.world().hideIndependentSection(
                section,
                Direction.UP
        );
    }
}