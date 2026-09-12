package com.venacava.biggervaults.content;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.api.boiler.BoilerHeater;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.decoration.steamWhistle.WhistleBlock;
import com.simibubi.create.content.decoration.steamWhistle.WhistleBlockEntity;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlock;
import com.simibubi.create.foundation.fluid.FluidHelper;
import com.simibubi.create.foundation.utility.CreateLang;
import com.venacava.biggervaults.block.TieredFluidTankBlock;
import com.venacava.biggervaults.blockentity.TieredFluidTankBlockEntity;
import joptsimple.internal.Strings;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.data.Iterate;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TieredBoilerData {
    private static final int SAMPLE_RATE = 5;
    private static final int WATER_SUPPLY_PER_LEVEL = 10;
    private static final float PASSIVE_ENGINE_EFFICIENCY =
            1.0f / 8.0f;

    private int gatheredSupply;
    private final float[] supplyOverTime =
            new float[10];

    private int ticksUntilNextSample;
    private int currentIndex;

    public boolean needsHeatLevelUpdate;
    public boolean passiveHeat;
    public int activeHeat;
    public float waterSupply;
    public int attachedEngines;
    public int attachedWhistles;

    private int maxHeatForSize;
    private int maxHeatForWater;
    private int minValue;
    private int maxValue;

    /*
     * One entry for each horizontal direction. A gauge is hidden when another
     * block occupies the space where that gauge would be rendered.
     */
    public final boolean[] occludedDirections = {
            true,
            true,
            true,
            true
    };

    public final LerpedFloat gauge =
            LerpedFloat.linear();

    public void tick(
            TieredFluidTankBlockEntity controller
    ) {
        if (!isActive()) {
            return;
        }

        Level level = controller.getLevel();

        if (level == null) {
            return;
        }

        if (level.isClientSide) {
            gauge.tickChaser();
            return;
        }

        if (needsHeatLevelUpdate
                && updateTemperature(controller)) {
            controller.notifyUpdate();
        }

        ticksUntilNextSample--;

        if (ticksUntilNextSample > 0) {
            return;
        }

        int capacity =
                controller
                        .getTankInventory()
                        .getCapacity();

        if (capacity <= 0) {
            return;
        }

        ticksUntilNextSample = SAMPLE_RATE;

        supplyOverTime[currentIndex] =
                gatheredSupply
                        / (float) SAMPLE_RATE;

        waterSupply = Math.max(
                waterSupply,
                supplyOverTime[currentIndex]
        );

        currentIndex =
                (currentIndex + 1)
                        % supplyOverTime.length;

        gatheredSupply = 0;

        if (currentIndex == 0) {
            waterSupply = 0;

            for (float sample : supplyOverTime) {
                waterSupply = Math.max(
                        waterSupply,
                        sample
                );
            }
        }

        controller.notifyUpdate();
    }

    public boolean evaluate(
            TieredFluidTankBlockEntity controller
    ) {
        Level level = controller.getLevel();

        if (level == null) {
            return false;
        }

        int previousEngines = attachedEngines;
        int previousWhistles = attachedWhistles;

        attachedEngines = 0;
        attachedWhistles = 0;

        BlockPos controllerPos =
                controller.getBlockPos();

        int width = controller.getWidth();
        int height = controller.getHeight();

        for (int yOffset = 0;
             yOffset < height;
             yOffset++) {

            for (int xOffset = 0;
                 xOffset < width;
                 xOffset++) {

                for (int zOffset = 0;
                     zOffset < width;
                     zOffset++) {

                    BlockPos tankPos =
                            controllerPos.offset(
                                    xOffset,
                                    yOffset,
                                    zOffset
                            );

                    BlockState tankState =
                            level.getBlockState(
                                    tankPos
                            );

                    if (!(tankState.getBlock()
                            instanceof TieredFluidTankBlock)) {
                        continue;
                    }

                    for (Direction direction
                            : Iterate.directions) {

                        BlockPos attachedPos =
                                tankPos.relative(
                                        direction
                                );

                        BlockState attachedState =
                                level.getBlockState(
                                        attachedPos
                                );

                        if (attachedState.getBlock()
                                instanceof SteamEngineBlock
                                && SteamEngineBlock
                                .getFacing(
                                        attachedState
                                ) == direction) {

                            attachedEngines++;
                        }

                        if (AllBlocks.STEAM_WHISTLE
                                .has(attachedState)
                                && WhistleBlock
                                .getAttachedDirection(
                                        attachedState
                                )
                                .getOpposite()
                                == direction) {

                            attachedWhistles++;
                        }
                    }
                }
            }
        }

        needsHeatLevelUpdate = true;

        return previousEngines
                != attachedEngines
                || previousWhistles
                != attachedWhistles;
    }

    /**
     * Checks which sides of the boiler have enough free space to display
     * Create's pressure gauge.
     *
     * This matches the positioning used by Create's Fluid Tank renderer.
     */
    public void updateOcclusion(
            TieredFluidTankBlockEntity controller
    ) {
        Level level = controller.getLevel();

        if (level == null || !level.isClientSide) {
            return;
        }

        if (!isActive()) {
            Arrays.fill(
                    occludedDirections,
                    true
            );
            return;
        }

        int width = controller.getWidth();

        for (Direction direction
                : Iterate.horizontalDirections) {

            AABB gaugeBounds =
                    new AABB(
                            controller.getBlockPos()
                    )
                            .move(
                                    width / 2.0f - 0.5f,
                                    0,
                                    width / 2.0f - 0.5f
                            )
                            .deflate(
                                    5.0f / 8.0f
                            );

            gaugeBounds = gaugeBounds.move(
                    direction.getStepX()
                            * (
                            width / 2.0f
                                    + 1.0f / 4.0f
                    ),
                    0,
                    direction.getStepZ()
                            * (
                            width / 2.0f
                                    + 1.0f / 4.0f
                    )
            );

            gaugeBounds = gaugeBounds.inflate(
                    Math.abs(
                            direction.getStepZ()
                    ) / 2.0f,
                    0.25f,
                    Math.abs(
                            direction.getStepX()
                    ) / 2.0f
            );

            occludedDirections[
                    direction.get2DDataValue()
                    ] = !level.noCollision(
                    gaugeBounds
            );
        }
    }

    public boolean updateTemperature(
            TieredFluidTankBlockEntity controller
    ) {
        Level level = controller.getLevel();

        if (level == null) {
            return false;
        }

        needsHeatLevelUpdate = false;

        boolean previousPassive =
                passiveHeat;

        int previousActive =
                activeHeat;

        passiveHeat = false;
        activeHeat = 0;

        BlockPos controllerPos =
                controller.getBlockPos();

        int width = controller.getWidth();

        for (int xOffset = 0;
             xOffset < width;
             xOffset++) {

            for (int zOffset = 0;
                 zOffset < width;
                 zOffset++) {

                BlockPos heaterPos =
                        controllerPos.offset(
                                xOffset,
                                -1,
                                zOffset
                        );

                BlockState heaterState =
                        level.getBlockState(
                                heaterPos
                        );

                float heat =
                        BoilerHeater.findHeat(
                                level,
                                heaterPos,
                                heaterState
                        );

                if (heat == 0) {
                    passiveHeat = true;
                } else if (heat > 0) {
                    activeHeat += heat;
                }
            }
        }

        passiveHeat &=
                activeHeat == 0;

        return previousActive != activeHeat
                || previousPassive != passiveHeat;
    }

    /**
     * Retained for compatibility with any call sites which do not yet supply
     * a material multiplier.
     */
     public float getEngineEfficiency(
     int boilerSize
    ) {
        return getEngineEfficiency(
                boilerSize,
                1.0f
        );
    }

    public float getEngineEfficiency(
            int boilerSize,
            float outputMultiplier
    ) {
        float safeMultiplier =
                Math.max(
                        0.01f,
                        outputMultiplier
                );

        if (attachedEngines <= 0) {
            return 0.0f;
        }

        if (isPassive(boilerSize)) {
            return PASSIVE_ENGINE_EFFICIENCY
                    * safeMultiplier
                    / attachedEngines;
        }

        if (activeHeat == 0) {
            return 0.0f;
        }

        float effectiveBoilerLevel =
                getEffectiveBoilerLevel(
                        boilerSize,
                        safeMultiplier
                );

        return effectiveBoilerLevel
                / attachedEngines;
    }

    private int getActualHeat(
            int boilerSize
    ) {
        int sizeLimit =
                getMaxHeatLevelForBoilerSize(
                        boilerSize
                );

        int waterLimit =
                getMaxHeatLevelForWaterSupply();

        return Math.min(
                activeHeat,
                Math.min(
                        sizeLimit,
                        waterLimit
                )
        );
    }

    private float getEffectiveBoilerLevel(
            int boilerSize,
            float outputMultiplier
    ) {
        int ordinaryBoilerLevel =
                getActualHeat(boilerSize);

        return Math.max(
                0.0f,
                ordinaryBoilerLevel
                        * outputMultiplier
        );
    }

    public int getMaxHeatLevelForBoilerSize(
            int boilerSize
    ) {
        return Math.min(
                18,
                boilerSize / 4
        );
    }

    public int getMaxHeatLevelForWaterSupply() {
        return Math.min(
                18,
                Mth.ceil(waterSupply)
                        / WATER_SUPPLY_PER_LEVEL
        );
    }

    public boolean isPassive() {
        return passiveHeat
                && maxHeatForSize > 0
                && maxHeatForWater > 0;
    }

    public boolean isPassive(
            int boilerSize
    ) {
        calculateDisplayLimits(
                boilerSize
        );

        return isPassive();
    }

    public boolean isActive() {
        return attachedEngines > 0
                || attachedWhistles > 0;
    }

    /**
     * Retained for compatibility with older call sites.
     */
    public boolean addToGoggleTooltip(
            List<Component> tooltip,
            boolean isPlayerSneaking,
            int boilerSize
    ) {
        return addToGoggleTooltip(
                tooltip,
                isPlayerSneaking,
                boilerSize,
                1.0f
        );
    }

    public boolean addToGoggleTooltip(
            List<Component> tooltip,
            boolean isPlayerSneaking,
            int boilerSize,
            float outputMultiplier
    ) {
        if (!isActive()) {
            return false;
        }

        calculateDisplayLimits(
                boilerSize
        );

        CreateLang.translate(
                        "boiler.status",
                        getHeatLevelTextComponent()
                                .withStyle(
                                        ChatFormatting.GREEN
                                )
                )
                .forGoggles(tooltip);

        CreateLang.builder()
                .add(
                        getSizeComponent(
                                true,
                                false
                        )
                )
                .forGoggles(
                        tooltip,
                        1
                );

        CreateLang.builder()
                .add(
                        getWaterComponent(
                                true,
                                false
                        )
                )
                .forGoggles(
                        tooltip,
                        1
                );

        CreateLang.builder()
                .add(
                        getHeatComponent(
                                true,
                                false
                        )
                )
                .forGoggles(
                        tooltip,
                        1
                );

        if (attachedEngines == 0) {
            return true;
        }

        float effectiveBoilerLevel;

        if (isPassive(boilerSize)) {
            effectiveBoilerLevel =
                    PASSIVE_ENGINE_EFFICIENCY
                            * Math.max(
                            0.01f,
                            outputMultiplier
                    );
        } else {
            effectiveBoilerLevel =
                    getEffectiveBoilerLevel(
                            boilerSize,
                            outputMultiplier
                    );
        }

        double totalStressCapacity =
                effectiveBoilerLevel
                        * 16
                        * BlockStressValues
                        .getCapacity(
                                AllBlocks
                                        .STEAM_ENGINE
                                        .get()
                        );

        tooltip.add(
                CommonComponents.EMPTY
        );

        CreateLang.translate(
                        "tooltip.capacityProvided"
                )
                .style(
                        ChatFormatting.GRAY
                )
                .forGoggles(tooltip);

        CreateLang.number(
                        totalStressCapacity
                )
                .translate(
                        "generic.unit.stress"
                )
                .style(
                        ChatFormatting.AQUA
                )
                .space()
                .add(
                        (
                                attachedEngines == 1
                                        ? CreateLang.translate(
                                        "boiler.via_one_engine"
                                )
                                        : CreateLang.translate(
                                        "boiler.via_engines",
                                        attachedEngines
                                )
                        )
                                .style(
                                        ChatFormatting.DARK_GRAY
                                )
                )
                .forGoggles(
                        tooltip,
                        1
                );

        return true;
    }

    public void calculateDisplayLimits(
            int boilerSize
    ) {
        maxHeatForSize =
                getMaxHeatLevelForBoilerSize(
                        boilerSize
                );

        maxHeatForWater =
                getMaxHeatLevelForWaterSupply();

        minValue =
                Math.min(
                        passiveHeat ? 1 : activeHeat,
                        Math.min(
                                maxHeatForWater,
                                maxHeatForSize
                        )
                );

        maxValue =
                Math.max(
                        passiveHeat ? 1 : activeHeat,
                        Math.max(
                                maxHeatForWater,
                                maxHeatForSize
                        )
                );
    }

    @NotNull
    public MutableComponent
    getHeatLevelTextComponent() {
        int boilerLevel =
                Math.min(
                        activeHeat,
                        Math.min(
                                maxHeatForWater,
                                maxHeatForSize
                        )
                );

        if (isPassive()) {
            return CreateLang.translateDirect(
                    "boiler.passive"
            );
        }

        if (boilerLevel == 0) {
            return CreateLang.translateDirect(
                    "boiler.idle"
            );
        }

        if (boilerLevel == 18) {
            return CreateLang.translateDirect(
                    "boiler.max_lvl"
            );
        }

        return CreateLang.translateDirect(
                "boiler.lvl",
                String.valueOf(boilerLevel)
        );
    }

    public MutableComponent getSizeComponent(
            boolean forGoggles,
            boolean useBlocksAsBars,
            ChatFormatting... styles
    ) {
        return componentHelper(
                "size",
                maxHeatForSize,
                forGoggles,
                useBlocksAsBars,
                styles
        );
    }

    public MutableComponent getWaterComponent(
            boolean forGoggles,
            boolean useBlocksAsBars,
            ChatFormatting... styles
    ) {
        return componentHelper(
                "water",
                maxHeatForWater,
                forGoggles,
                useBlocksAsBars,
                styles
        );
    }

    public MutableComponent getHeatComponent(
            boolean forGoggles,
            boolean useBlocksAsBars,
            ChatFormatting... styles
    ) {
        return componentHelper(
                "heat",
                passiveHeat ? 1 : activeHeat,
                forGoggles,
                useBlocksAsBars,
                styles
        );
    }

    private MutableComponent componentHelper(
            String label,
            int level,
            boolean forGoggles,
            boolean useBlocksAsBars,
            ChatFormatting... styles
    ) {
        MutableComponent base =
                useBlocksAsBars
                        ? blockComponent(level)
                        : barComponent(level);

        if (!forGoggles) {
            return base;
        }

        ChatFormatting firstStyle =
                styles.length >= 1
                        ? styles[0]
                        : ChatFormatting.GRAY;

        ChatFormatting secondStyle =
                styles.length >= 2
                        ? styles[1]
                        : ChatFormatting.DARK_GRAY;

        return CreateLang.translateDirect(
                        "boiler." + label
                )
                .withStyle(firstStyle)
                .append(
                        CreateLang.translateDirect(
                                        "boiler."
                                                + label
                                                + "_dots"
                                )
                                .withStyle(
                                        secondStyle
                                )
                )
                .append(base);
    }

    private MutableComponent blockComponent(
            int level
    ) {
        return Component.literal(
                "\u2588".repeat(
                        Math.max(
                                0,
                                minValue
                        )
                )
                        + "\u2592".repeat(
                        Math.max(
                                0,
                                level - minValue
                        )
                )
                        + "\u2591".repeat(
                        Math.max(
                                0,
                                maxValue - level
                        )
                )
        );
    }

    private MutableComponent barComponent(
            int level
    ) {
        return Component.empty()
                .append(
                        bars(
                                Math.max(
                                        0,
                                        minValue - 1
                                ),
                                ChatFormatting.DARK_GREEN
                        )
                )
                .append(
                        bars(
                                minValue > 0
                                        ? 1
                                        : 0,
                                ChatFormatting.GREEN
                        )
                )
                .append(
                        bars(
                                Math.max(
                                        0,
                                        level - minValue
                                ),
                                ChatFormatting.DARK_GREEN
                        )
                )
                .append(
                        bars(
                                Math.max(
                                        0,
                                        maxValue - level
                                ),
                                ChatFormatting.DARK_RED
                        )
                )
                .append(
                        bars(
                                Math.max(
                                        0,
                                        Math.min(
                                                18 - maxValue,
                                                (
                                                        maxValue / 5
                                                                + 1
                                                ) * 5
                                                        - maxValue
                                        )
                                ),
                                ChatFormatting.DARK_GRAY
                        )
                );
    }

    private MutableComponent bars(
            int amount,
            ChatFormatting formatting
    ) {
        return Component.literal(
                        Strings.repeat(
                                '|',
                                amount
                        )
                )
                .withStyle(formatting);
    }

    public void checkPipeOrganAdvancement(
            TieredFluidTankBlockEntity controller
    ) {
        Level level = controller.getLevel();

        if (level == null) {
            return;
        }

        BlockPos controllerPos =
                controller.getBlockPos();

        Set<Integer> whistlePitches =
                new HashSet<>();

        int width = controller.getWidth();
        int height = controller.getHeight();

        for (int yOffset = 0;
             yOffset < height;
             yOffset++) {

            for (int xOffset = 0;
                 xOffset < width;
                 xOffset++) {

                for (int zOffset = 0;
                     zOffset < width;
                     zOffset++) {

                    BlockPos tankPos =
                            controllerPos.offset(
                                    xOffset,
                                    yOffset,
                                    zOffset
                            );

                    for (Direction direction
                            : Iterate.directions) {

                        BlockPos attachedPos =
                                tankPos.relative(
                                        direction
                                );

                        BlockState attachedState =
                                level.getBlockState(
                                        attachedPos
                                );

                        if (!AllBlocks.STEAM_WHISTLE
                                .has(attachedState)) {
                            continue;
                        }

                        if (WhistleBlock
                                .getAttachedDirection(
                                        attachedState
                                )
                                .getOpposite()
                                != direction) {
                            continue;
                        }

                        if (level.getBlockEntity(
                                attachedPos
                        ) instanceof
                                WhistleBlockEntity whistle) {

                            whistlePitches.add(
                                    whistle.getPitchId()
                            );
                        }
                    }
                }
            }
        }

        /*
         * The advancement award itself will be connected after the
         * compatibility mixins can provide Create's AdvancementBehaviour.
         */
    }

    public void clear() {
        waterSupply = 0;
        activeHeat = 0;
        passiveHeat = false;
        attachedEngines = 0;
        attachedWhistles = 0;
        gatheredSupply = 0;

        Arrays.fill(
                supplyOverTime,
                0
        );
    }

    public CompoundTag write() {
        CompoundTag tag =
                new CompoundTag();

        tag.putFloat(
                "Supply",
                waterSupply
        );

        tag.putInt(
                "ActiveHeat",
                activeHeat
        );

        tag.putBoolean(
                "PassiveHeat",
                passiveHeat
        );

        tag.putInt(
                "Engines",
                attachedEngines
        );

        tag.putInt(
                "Whistles",
                attachedWhistles
        );

        tag.putBoolean(
                "Update",
                needsHeatLevelUpdate
        );

        return tag;
    }

    public void read(
            CompoundTag tag,
            int boilerSize
    ) {
        waterSupply =
                tag.getFloat("Supply");

        activeHeat =
                tag.getInt("ActiveHeat");

        passiveHeat =
                tag.getBoolean(
                        "PassiveHeat"
                );

        attachedEngines =
                tag.getInt("Engines");

        attachedWhistles =
                tag.getInt("Whistles");

        needsHeatLevelUpdate =
                tag.getBoolean("Update");

        Arrays.fill(
                supplyOverTime,
                waterSupply
        );

        int sizeLimit =
                getMaxHeatLevelForBoilerSize(
                        boilerSize
                );

        int waterLimit =
                getMaxHeatLevelForWaterSupply();

        int actualHeat =
                Math.min(
                        activeHeat,
                        Math.min(
                                waterLimit,
                                sizeLimit
                        )
                );

        float target =
                isPassive(boilerSize)
                        ? PASSIVE_ENGINE_EFFICIENCY
                        : sizeLimit == 0
                        ? 0
                        : actualHeat
                        / (float) sizeLimit;

        gauge.chase(
                target,
                0.125f,
                Chaser.EXP
        );
    }

    public IFluidHandler createHandler() {
        return new BoilerFluidHandler();
    }

    private class BoilerFluidHandler
            implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(
                int tank
        ) {
            return FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(
                int tank
        ) {
            return 10_000;
        }

        @Override
        public boolean isFluidValid(
                int tank,
                FluidStack stack
        ) {
            return FluidHelper.isWater(
                    stack.getFluid()
            );
        }

        @Override
        public int fill(
                FluidStack resource,
                FluidAction action
        ) {
            if (!isFluidValid(
                    0,
                    resource
            )) {
                return 0;
            }

            int amount =
                    resource.getAmount();

            if (action.execute()) {
                gatheredSupply += amount;
            }

            return amount;
        }

        @Override
        public FluidStack drain(
                FluidStack resource,
                FluidAction action
        ) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(
                int maxDrain,
                FluidAction action
        ) {
            return FluidStack.EMPTY;
        }
    }
}