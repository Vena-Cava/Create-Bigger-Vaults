package com.venacava.biggervaults.blockentity;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.IFluidTank;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.minecraft.core.Direction;
import com.venacava.biggervaults.block.TieredFluidTankBlock;
import com.venacava.biggervaults.content.MaterialTier;
import com.venacava.biggervaults.content.FluidTankProperties;
import com.venacava.biggervaults.content.TieredBoilerData;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TieredFluidTankBlockEntity
        extends SmartBlockEntity
        implements IMultiBlockEntityContainer.Fluid,
        IHaveGoggleInformation {
    protected final FluidTank tankInventory;
    protected final TieredBoilerData boiler;
    protected final IFluidHandler boilerFluidHandler;
    protected boolean forceFluidLevelUpdate;
    protected LerpedFloat fluidLevel;

    protected BlockPos controller;
    protected BlockPos lastKnownPos;

    protected int width;
    protected int height;

    protected boolean updateConnectivity;
    protected boolean updateVisuals;
    protected boolean window;

    /*
     * When a contraption disassembles, Create restores its Block Entities
     * over several stages. This countdown will allow us to delay rebuilding
     * the tank until every neighbouring tank has been restored.
     */
    protected int contraptionRefreshTicks;

    private boolean removalHandled;

    public TieredFluidTankBlockEntity(
            BlockEntityType<?> blockEntityType,
            BlockPos blockPos,
            BlockState blockState
    ) {
        super(
                blockEntityType,
                blockPos,
                blockState
        );

        tankInventory = createInventory();
        boiler = new TieredBoilerData();
        boilerFluidHandler = boiler.createHandler();

        forceFluidLevelUpdate = true;

        fluidLevel = LerpedFloat.linear()
                .startWithValue(0);

        controller = null;
        lastKnownPos = blockPos;

        width = 1;
        height = 1;

        updateConnectivity = true;
        updateVisuals = true;
        window = true;

        contraptionRefreshTicks = 0;
        removalHandled = false;
    }

    protected SmartFluidTank createInventory() {
        return new SmartFluidTank(
                FluidTankProperties.get(getTier()).fluidCapacityPerBlock(),
                this::onFluidStackChanged
        );
    }

    protected void onFluidStackChanged(
            FluidStack fluidStack
    ) {
        forceFluidLevelUpdate = true;

        if (level == null) {
            return;
        }

        if (isVirtual()) {
            float fillState = getFillState();

            if (fluidLevel == null) {
                fluidLevel = LerpedFloat.linear()
                        .startWithValue(fillState);
            }

            fluidLevel.chase(
                    fillState,
                    0.5f,
                    Chaser.EXP
            );
        }

        if (!level.isClientSide) {
            setChanged();
            sendData();
            notifyComparators();
        }
    }

    @Override
    public void addBehaviours(
            List<BlockEntityBehaviour> behaviours
    ) {
    }

    public MaterialTier getTier() {
        if (getBlockState().getBlock()
                instanceof TieredFluidTankBlock tankBlock) {
            return tankBlock.tier();
        }

        return MaterialTier.WOODEN;
    }

    public void applyFluidTankSize(int tankBlockCount) {
        int blockCount = Math.max(1, tankBlockCount);
        int capacityPerBlock =
                FluidTankProperties
                        .get(getTier())
                        .fluidCapacityPerBlock();

        long requestedCapacity =
                (long) blockCount * capacityPerBlock;

        int capacity =
                requestedCapacity > Integer.MAX_VALUE
                        ? Integer.MAX_VALUE
                        : (int) requestedCapacity;

        /*
         * Changing the configured capacity must not delete fluid.
         *
         * FluidTank permits its stored FluidStack to contain more than its
         * current capacity. While over capacity, normal filling returns zero,
         * but draining remains possible.
         */
        tankInventory.setCapacity(capacity);

        forceFluidLevelUpdate = true;

        if (level != null && !level.isClientSide) {
            setChanged();
            sendData();
            notifyComparators();
        }
    }

    /**
     * Resizes the local tank while Create's ConnectivityHandler is splitting
     * or rebuilding the multiblock.
     *
     * ConnectivityHandler copies the fluid that will be redistributed before
     * calling setTankSize(). The local controller must therefore be clamped to
     * its temporary capacity, or the copied fluid will be duplicated when the
     * multiblock reforms.
     *
     * Normal capacity/configuration changes must continue using
     * applyFluidTankSize(), which intentionally preserves overflow.
     */
    private void applyConnectivityFluidTankSize(int tankBlockCount) {
        int blockCount = Math.max(1, tankBlockCount);

        int capacityPerBlock =
                FluidTankProperties
                        .get(getTier())
                        .fluidCapacityPerBlock();

        long requestedCapacity =
                (long) blockCount * capacityPerBlock;

        int capacity =
                requestedCapacity > Integer.MAX_VALUE
                        ? Integer.MAX_VALUE
                        : (int) requestedCapacity;

        tankInventory.setCapacity(capacity);

        int overflow =
                tankInventory.getFluidAmount() - capacity;

        if (overflow > 0) {
            tankInventory.drain(
                    overflow,
                    IFluidHandler.FluidAction.EXECUTE
            );
        }

        forceFluidLevelUpdate = true;

        if (level != null && !level.isClientSide) {
            setChanged();
            sendData();
            notifyComparators();
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (lastKnownPos == null) {
            lastKnownPos = worldPosition;
        } else if (!lastKnownPos.equals(worldPosition)) {
            onPositionChanged();
            return;
        }

        if (fluidLevel != null) {
            fluidLevel.tickChaser();
        }

        if (isController()) {
            boiler.tick(this);
        }

        if (level == null || level.isClientSide) {
            return;
        }

        if (contraptionRefreshTicks > 0) {
            contraptionRefreshTicks--;

            if (contraptionRefreshTicks == 0) {
                controller = null;
                width = 1;
                height = 1;

                updateConnectivity = true;
                updateVisuals = true;

                setChanged();
                sendData();
            }
        }

        if (updateConnectivity) {
            updateConnectivity();
        }

        if (updateVisuals) {
            updateVisuals = false;
            updateBlockState();

            if (isController()) {
                updateWindowShapes();
            }
        }
    }

    protected void updateConnectivity() {
        updateConnectivity = false;

        if (level == null || level.isClientSide) {
            return;
        }

        if (!isController()) {
            return;
        }

        ConnectivityHandler.formMulti(this);
    }

    protected void updateBlockState() {
        if (level == null) {
            return;
        }

        BlockState currentState = getBlockState();

        if (!(currentState.getBlock()
                instanceof TieredFluidTankBlock)) {
            return;
        }

        BlockPos controllerPos = getController();

        boolean bottom =
                controllerPos.getY() == worldPosition.getY();

        boolean top =
                controllerPos.getY() + getHeight() - 1
                        == worldPosition.getY();

        BlockState updatedState = currentState
                .setValue(
                        TieredFluidTankBlock.BOTTOM,
                        bottom
                )
                .setValue(
                        TieredFluidTankBlock.TOP,
                        top
                );

        if (updatedState != currentState) {
            level.setBlock(
                    worldPosition,
                    updatedState,
                    net.minecraft.world.level.block.Block.UPDATE_CLIENTS
                            | net.minecraft.world.level.block.Block.UPDATE_INVISIBLE
            );
        }
    }

    protected void updateWindowShapes() {
        if (level == null || !isController()) {
            return;
        }

        BlockPos controllerPos = getController();
        int multiblockWidth = getWidth();
        int multiblockHeight = getHeight();

        for (int yOffset = 0;
             yOffset < multiblockHeight;
             yOffset++) {

            for (int xOffset = 0;
                 xOffset < multiblockWidth;
                 xOffset++) {

                for (int zOffset = 0;
                     zOffset < multiblockWidth;
                     zOffset++) {

                    BlockPos partPos = controllerPos.offset(
                            xOffset,
                            yOffset,
                            zOffset
                    );

                    BlockState partState =
                            level.getBlockState(partPos);

                    if (!(partState.getBlock()
                            instanceof TieredFluidTankBlock)) {
                        continue;
                    }

                    FluidTankBlock.Shape shape =
                            getWindowShape(
                                    multiblockWidth,
                                    xOffset,
                                    zOffset
                            );

                    BlockState updatedState = partState
                            .setValue(
                                    TieredFluidTankBlock.SHAPE,
                                    shape
                            );

                    if (updatedState != partState) {
                        level.setBlock(
                                partPos,
                                updatedState,
                                net.minecraft.world.level.block.Block.UPDATE_CLIENTS
                                        | net.minecraft.world.level.block.Block.UPDATE_INVISIBLE
                                        | net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE
                        );
                    }
                }
            }
        }
    }

    protected FluidTankBlock.Shape getWindowShape(
            int multiblockWidth,
            int xOffset,
            int zOffset
    ) {
        /*
         * Create boilers use a sealed, windowless tank appearance.
         *
         * Do not overwrite the player's window preference. Returning PLAIN here
         * only changes the displayed block state while the boiler is active, so
         * the previous window arrangement can return afterwards.
         */
        if (boiler.isActive() || !window) {
            return FluidTankBlock.Shape.PLAIN;
        }

        if (multiblockWidth == 1) {
            return FluidTankBlock.Shape.WINDOW;
        }

        if (multiblockWidth == 2) {
            if (xOffset == 0 && zOffset == 0) {
                return FluidTankBlock.Shape.WINDOW_NW;
            }

            if (xOffset == 0 && zOffset == 1) {
                return FluidTankBlock.Shape.WINDOW_SW;
            }

            if (xOffset == 1 && zOffset == 0) {
                return FluidTankBlock.Shape.WINDOW_NE;
            }

            return FluidTankBlock.Shape.WINDOW_SE;
        }

        if (multiblockWidth == 3
                && Math.abs(
                Math.abs(xOffset)
                        - Math.abs(zOffset)
        ) == 1) {
            return FluidTankBlock.Shape.WINDOW;
        }

        return FluidTankBlock.Shape.PLAIN;
    }

    protected void onPositionChanged() {
        removeControllerInternal();
        lastKnownPos = worldPosition;
    }

    @Override
    public boolean isController() {
        return controller == null
                || worldPosition.equals(controller);
    }

    @Override
    public BlockPos getController() {
        return isController()
                ? worldPosition
                : controller;
    }

    @Nullable
    public TieredFluidTankBlockEntity getControllerBE() {
        if (isController()) {
            return this;
        }

        if (level == null || controller == null) {
            return null;
        }

        BlockEntity blockEntity =
                level.getBlockEntity(controller);

        if (blockEntity
                instanceof TieredFluidTankBlockEntity tank) {
            return tank;
        }

        return null;
    }

    @Override
    public void setController(BlockPos controllerPos) {
        if (level != null && level.isClientSide && !isVirtual()) {
            return;
        }

        if (controllerPos.equals(controller)) {
            return;
        }

        controller = controllerPos;
        updateVisuals = true;

        setChanged();
        sendData();
    }

    private void removeControllerInternal() {
        if (level != null && level.isClientSide) {
            return;
        }

        TieredFluidTankBlockEntity previousController =
                getControllerBE();

        controller = null;
        width = 1;
        height = 1;

        updateConnectivity = true;
        updateVisuals = true;

        if (previousController != null
                && previousController != this) {
            previousController.updateConnectivity = true;
            previousController.updateVisuals = true;
            previousController.setChanged();
            previousController.sendData();
        }

        setChanged();
        sendData();
    }

    @Override
    public void removeController(boolean keepContents) {
        if (level != null && level.isClientSide) {
            return;
        }

        updateConnectivity = true;

        if (!keepContents) {
            applyFluidTankSize(1);
        }

        controller = null;
        width = 1;
        height = 1;

        updateVisuals = true;

        onFluidStackChanged(tankInventory.getFluid());

        setChanged();
        sendData();
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public void setWidth(int width) {
        this.width = Math.max(1, width);
        updateVisuals = true;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void setHeight(int height) {
        this.height = Math.max(1, height);
        updateVisuals = true;
    }

    @Override
    public BlockPos getLastKnownPos() {
        return lastKnownPos;
    }

    public void preventConnectivityUpdate() {
        updateConnectivity = false;
        updateVisuals = true;
    }

    public void requestContraptionRefresh() {
        contraptionRefreshTicks = 2;

        setChanged();
    }

    public void setLastKnownPos(BlockPos pos) {
        lastKnownPos = pos;
    }

    @Override
    public void notifyMultiUpdated() {
        updateVisuals = true;
        applyFluidTankSize(getTotalTankSize());

        updateBlockState();

        if (isController()) {
            boolean wasActive =
                    boiler.isActive();

            boolean attachmentsChanged =
                    boiler.evaluate(this);

            boolean isActive =
                    boiler.isActive();

            /*
             * Evaluate attachments before selecting window shapes, otherwise a newly
             * formed boiler may briefly retain ordinary tank windows.
             */
            updateWindowShapes();

            if (attachmentsChanged
                    || wasActive != isActive) {
                boiler.needsHeatLevelUpdate = true;
            }
        }

        setChanged();
        sendData();
    }

    @Override
    public Direction.Axis getMainConnectionAxis() {
        return Direction.Axis.Y;
    }

    @Override
    public boolean hasTank() {
        return true;
    }

    @Override
    public int getTankSize(int tank) {
        return FluidTankProperties.get(getTier()).fluidCapacityPerBlock();
    }

    @Override
    public void setTankSize(int tank, int tankBlockCount) {
        applyConnectivityFluidTankSize(tankBlockCount);
    }

    @Override
    public IFluidTank getTank(int tank) {
        return tankInventory;
    }

    @Override
    public FluidStack getFluid(int tank) {
        return tankInventory.getFluid().copy();
    }

    public FluidTank getTankInventory() {
        TieredFluidTankBlockEntity controllerBE =
                getControllerBE();

        if (controllerBE == null) {
            return tankInventory;
        }

        return controllerBE.tankInventory;
    }

    public TieredBoilerData getBoilerData() {
        TieredFluidTankBlockEntity controllerBE =
                getControllerBE();

        if (controllerBE == null) {
            return boiler;
        }

        return controllerBE.boiler;
    }

    public IFluidHandler getBoilerFluidHandler() {
        TieredFluidTankBlockEntity controllerBE =
                getControllerBE();

        if (controllerBE == null) {
            return boilerFluidHandler;
        }

        return controllerBE.boilerFluidHandler;
    }

    public void evaluateBoiler() {
        TieredFluidTankBlockEntity controllerBE =
                getControllerBE();

        if (controllerBE == null) {
            return;
        }

        boolean wasActive =
                controllerBE.boiler.isActive();

        boolean attachmentsChanged =
                controllerBE.boiler.evaluate(
                        controllerBE
                );

        boolean isActive =
                controllerBE.boiler.isActive();

        /*
         * Rebuild every tank block's model whenever the multiblock enters or
         * leaves boiler mode.
         */
        if (wasActive != isActive) {
            controllerBE.updateVisuals = true;
            controllerBE.updateWindowShapes();
        }

        if (attachmentsChanged
                || wasActive != isActive) {
            controllerBE.setChanged();
            controllerBE.sendData();
        }
    }

    public void markBoilerHeatDirty() {
        TieredFluidTankBlockEntity controllerBE =
                getControllerBE();

        if (controllerBE == null) {
            return;
        }

        controllerBE.boiler
                .needsHeatLevelUpdate = true;

        controllerBE.setChanged();
    }

    @Override
    public boolean addToGoggleTooltip(
            List<Component> tooltip,
            boolean isPlayerSneaking
    ) {
        TieredFluidTankBlockEntity controllerBE =
                getControllerBE();

        if (controllerBE == null) {
            return false;
        }

        if (controllerBE.boiler
                .addToGoggleTooltip(
                        tooltip,
                        isPlayerSneaking,
                        controllerBE
                                .getTotalTankSize(),
                        FluidTankProperties
                                .get(controllerBE.getTier())
                                .boilerOutputMultiplier()
                )) {
            return true;
        }

        return containedFluidTooltip(
                tooltip,
                isPlayerSneaking,
                controllerBE.tankInventory
        );
    }

    public int getTotalTankSize() {
        return getWidth() * getWidth() * getHeight();
    }

    @Override
    protected AABB createRenderBoundingBox() {
        if (!isController()) {
            return super.createRenderBoundingBox();
        }

        return super.createRenderBoundingBox()
                .expandTowards(
                        getWidth() - 1,
                        getHeight() - 1,
                        getWidth() - 1
                );
    }

    public boolean hasWindows() {
        TieredFluidTankBlockEntity controllerBE =
                getControllerBE();

        if (controllerBE == null) {
            return window;
        }

        return controllerBE.window;
    }

    public void toggleWindows() {
        TieredFluidTankBlockEntity controller = getControllerBE();

        if (controller == null) {
            return;
        }

        controller.window = !controller.window;
        controller.updateVisuals = true;

        controller.updateWindowShapes();

        controller.setChanged();
        controller.sendData();
    }

    public float getFillState() {
        FluidTank inventory = getTankInventory();

        if (inventory.getCapacity() <= 0) {
            return 0;
        }

        return Math.min(
                1.0f,
                (float) inventory.getFluidAmount()
                        / inventory.getCapacity()
        );
    }

    public void notifyComparators() {
        if (level == null || level.isClientSide) {
            return;
        }

        TieredFluidTankBlockEntity controllerBE =
                getControllerBE();

        if (controllerBE == null) {
            return;
        }

        /*
         * Always perform the update from the controller so every
         * block belonging to the connected tank is covered.
         */
        if (controllerBE != this) {
            controllerBE.notifyComparators();
            return;
        }

        BlockPos controllerPos =
                getController();

        int tankWidth =
                getWidth();

        int tankHeight =
                getHeight();

        for (int yOffset = 0;
             yOffset < tankHeight;
             yOffset++) {

            for (int xOffset = 0;
                 xOffset < tankWidth;
                 xOffset++) {

                for (int zOffset = 0;
                     zOffset < tankWidth;
                     zOffset++) {

                    BlockPos partPos =
                            controllerPos.offset(
                                    xOffset,
                                    yOffset,
                                    zOffset
                            );

                    BlockState partState =
                            level.getBlockState(partPos);

                    if (!(partState.getBlock()
                            instanceof TieredFluidTankBlock)) {
                        continue;
                    }

                    level.updateNeighbourForOutputSignal(
                            partPos,
                            partState.getBlock()
                    );
                }
            }
        }
    }

    public LerpedFloat getFluidLevel() {
        TieredFluidTankBlockEntity controllerBE =
                getControllerBE();

        if (controllerBE == null) {
            return fluidLevel;
        }

        return controllerBE.fluidLevel;
    }

    public void setFluidLevel(LerpedFloat fluidLevel) {
        this.fluidLevel = fluidLevel;
    }

    @Override
    public int getMaxWidth() {
        return FluidTankProperties.MAXIMUM_BASE_SIZE;
    }

    @Override
    public int getMaxLength(Direction.Axis axis, int width) {
        return FluidTankProperties.MAXIMUM_HEIGHT;
    }

    @Override
    public void setExtraData(@Nullable Object data) {
        if (data instanceof Boolean windows) {
            window = windows;
        }
    }

    @Override
    @Nullable
    public Object getExtraData() {
        return window;
    }

    @Override
    public Object modifyExtraData(Object data) {
        if (data instanceof Boolean windows) {
            return windows || window;
        }

        return data;
    }

    public static void tankTick(
            Level level,
            BlockPos blockPos,
            BlockState blockState,
            TieredFluidTankBlockEntity blockEntity
    ) {
        blockEntity.tick();
    }

    public void beforeBlockRemoved(ServerLevel level) {
        if (removalHandled) {
            return;
        }

        removalHandled = true;

        TieredFluidTankBlockEntity controllerBE =
                getControllerBE();

        removeControllerInternal();

        if (controllerBE != null
                && controllerBE != this) {
            controllerBE.updateConnectivity = true;
            controllerBE.updateVisuals = true;
            controllerBE.setChanged();
            controllerBE.sendData();
        }
    }

    @Override
    protected void read(
            CompoundTag tag,
            HolderLookup.Provider registries,
            boolean clientPacket
    ) {
        super.read(tag, registries, clientPacket);
        float previousFillState = getFillState();

        if (tag.contains("Controller")) {
            controller = BlockPos.of(
                    tag.getLong("Controller")
            );
        } else {
            controller = null;
        }

        if (tag.contains("LastKnownPos")) {
            lastKnownPos = BlockPos.of(
                    tag.getLong("LastKnownPos")
            );
        } else {
            lastKnownPos = worldPosition;
        }

        width = Math.max(
                1,
                tag.getInt("Width")
        );

        height = Math.max(
                1,
                tag.getInt("Height")
        );

        if (isController()) {
            applyFluidTankSize(getTotalTankSize());

            if (tag.contains("TankContent")) {
                tankInventory.readFromNBT(
                        registries,
                        tag.getCompound("TankContent")
                );
            }

            if (tag.contains("Boiler")) {
                boiler.read(
                        tag.getCompound("Boiler"),
                        getTotalTankSize()
                );
            }
        } else {
            applyFluidTankSize(1);
        }

        float fillState = getFillState();

        if (fluidLevel == null
                || tag.contains("ForceFluidLevel")) {
            fluidLevel = LerpedFloat.linear()
                    .startWithValue(fillState);
        }

        if (isController()) {
            window = !tag.contains("Window")
                    || tag.getBoolean("Window");
        }

        if (clientPacket) {
            updateVisuals = true;

            if (isController()) {
                if (tag.contains("ForceFluidLevel")) {
                    fluidLevel = LerpedFloat.linear()
                            .startWithValue(fillState);
                } else {
                    fluidLevel.chase(
                            fillState,
                            0.5f,
                            Chaser.EXP
                    );
                }
            }

            if (previousFillState != fillState
                    && level != null) {
                level.sendBlockUpdated(
                        worldPosition,
                        getBlockState(),
                        getBlockState(),
                        net.minecraft.world.level.block.Block.UPDATE_CLIENTS
                );
            }
        }
    }

    @Override
    protected void write(
            CompoundTag tag,
            HolderLookup.Provider registries,
            boolean clientPacket
    ) {
        super.write(tag, registries, clientPacket);

        if (controller != null) {
            tag.putLong(
                    "Controller",
                    controller.asLong()
            );
        }

        if (lastKnownPos != null) {
            tag.putLong(
                    "LastKnownPos",
                    lastKnownPos.asLong()
            );
        }

        if (isController()) {
            tag.putBoolean(
                    "Window",
                    window
            );

            tag.put(
                    "TankContent",
                    tankInventory.writeToNBT(
                            registries,
                            new CompoundTag()
                    )
            );

            tag.put(
                    "Boiler",
                    boiler.write()
            );
        }

        tag.putInt("Width", width);
        tag.putInt("Height", height);

        if (clientPacket && forceFluidLevelUpdate) {
            tag.putBoolean("ForceFluidLevel", true);
            forceFluidLevelUpdate = false;
        }
    }
}
