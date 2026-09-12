package com.venacava.biggervaults.client;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.api.stress.BlockStressValues.GeneratedRpm;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import com.simibubi.create.content.kinetics.base.IRotate.StressImpact;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.item.TooltipModifier;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.simibubi.create.infrastructure.config.CKinetics;
import com.venacava.biggervaults.content.MaterialTier;
import net.createmod.catnip.lang.Lang;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public final class TieredSteamEngineStats
        implements TooltipModifier {

    private final MaterialTier tier;

    public TieredSteamEngineStats(
            MaterialTier tier
    ) {
        this.tier = tier;
    }

    @Override
    public void modify(
            ItemTooltipEvent context
    ) {
        Player player =
                context.getEntity();

        CKinetics config =
                AllConfigs.server().kinetics;

        double vanillaCapacity =
                BlockStressValues.getCapacity(
                        AllBlocks.STEAM_ENGINE.get()
                );

        double tieredCapacity =
                vanillaCapacity
                        * tier.multiplier();

        if (tieredCapacity <= 0) {
            return;
        }

        GeneratedRpm generatedRpm =
                BlockStressValues.RPM.get(
                        AllBlocks.STEAM_ENGINE.get()
                );

        StressImpact capacityLevel =
                tieredCapacity
                        >= config.highCapacity.get()
                        ? StressImpact.HIGH
                        : tieredCapacity
                        >= config.mediumCapacity.get()
                        ? StressImpact.MEDIUM
                        : StressImpact.LOW;

        StressImpact displayColour =
                StressImpact.values()[
                        StressImpact.values().length
                                - 2
                                - capacityLevel.ordinal()
                        ];

        context.getToolTip().add(
                CommonComponents.EMPTY
        );

        CreateLang.translate(
                        "tooltip.capacityProvided"
                )
                .style(ChatFormatting.GRAY)
                .addTo(context.getToolTip());

        LangBuilder capacityLine =
                CreateLang.builder()
                        .add(
                                CreateLang.text(
                                                TooltipHelper.makeProgressBar(
                                                        3,
                                                        capacityLevel.ordinal()
                                                                + 1
                                                )
                                        )
                                        .style(
                                                displayColour
                                                        .getAbsoluteColor()
                                        )
                        );

        if (GogglesItem.isWearingGoggles(player)) {
            capacityLine
                    .add(
                            CreateLang.number(
                                    tieredCapacity
                            )
                    )
                    .text("x ")
                    .add(
                            CreateLang.translate(
                                    "generic.unit.rpm"
                            )
                    )
                    .addTo(
                            context.getToolTip()
                    );

            if (generatedRpm != null) {
                LangBuilder maximumOutput =
                        CreateLang.number(
                                        tieredCapacity
                                                * generatedRpm.value()
                                )
                                .add(
                                        CreateLang.translate(
                                                "generic.unit.stress"
                                        )
                                );

                CreateLang.text(" -> ")
                        .add(
                                generatedRpm.mayGenerateLess()
                                        ? CreateLang.translate(
                                        "tooltip.up_to",
                                        maximumOutput
                                )
                                        : maximumOutput
                        )
                        .style(ChatFormatting.DARK_GRAY)
                        .addTo(
                                context.getToolTip()
                        );
            }

            return;
        }

        capacityLine
                .translate(
                        "tooltip.capacityProvided."
                                + Lang.asId(
                                capacityLevel.name()
                        )
                )
                .addTo(
                        context.getToolTip()
                );
    }
}