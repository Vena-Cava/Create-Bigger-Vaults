package com.venacava.biggervaults.blockentity;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.foundation.ICapabilityProvider;
import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.venacava.biggervaults.block.TieredItemVaultBlock;
import com.venacava.biggervaults.content.MaterialTier;
import com.venacava.biggervaults.content.ItemVaultProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Clearable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TieredItemVaultBlockEntity extends SmartBlockEntity
        implements IMultiBlockEntityContainer.Inventory, Clearable {

    protected ICapabilityProvider<IItemHandler> itemCapability;
    protected ItemStackHandler inventory;

    /*
     * Allows saved data and contraption data to restore stacks into
     * overflow slots without making those slots normally writable.
     */
    protected boolean allowOverflowSlotWrites;

    protected BlockPos controller;
    protected BlockPos lastKnownPos;

    protected boolean updateConnectivity;

    protected boolean updateVisuals;

    /*
     * Contraptions restore Block Entities in several stages.
     * Waiting briefly ensures that connectivity is rebuilt only after
     * Create has finished placing and restoring every vault block.
     */
    protected int contraptionRefreshTicks;

    protected int radius;
    protected int length;

    public TieredItemVaultBlockEntity(
            BlockEntityType<?> blockEntityType,
            BlockPos blockPos,
            BlockState blockState
    ) {
        super(blockEntityType, blockPos, blockState);

        this.itemCapability = null;
        this.allowOverflowSlotWrites = false;
        this.inventory = createTierInventory();

        this.radius = 1;
        this.length = 1;
        this.updateConnectivity = true;
        this.updateVisuals = true;
        this.contraptionRefreshTicks = 0;
    }

    public MaterialTier getTier() {
        if (getBlockState().getBlock()
                instanceof TieredItemVaultBlock vaultBlock) {
            return vaultBlock.tier();
        }

        return MaterialTier.WOODEN;
    }

    private int getConfiguredSlotsPerBlock() {
        return Math.max(
                1,
                ItemVaultProperties
                        .get(getTier())
                        .itemSlotsPerBlock()
        );
    }

    private ItemStackHandler createTierInventory() {
        return createTierInventory(
                getConfiguredSlotsPerBlock()
        );
    }

    private ItemStackHandler createTierInventory(
            int physicalSlotCount
    ) {
        int safePhysicalSlotCount =
                Math.max(1, physicalSlotCount);

        return new ItemStackHandler(
                safePhysicalSlotCount
        ) {
            private boolean compactingInventory;

            private void compactInventory() {
                int destinationSlot = 0;

                for (int sourceSlot = 0;
                     sourceSlot < getSlots();
                     sourceSlot++) {

                    ItemStack stack =
                            getStackInSlot(sourceSlot);

                    if (stack.isEmpty()) {
                        continue;
                    }

                    if (sourceSlot != destinationSlot) {
                        /*
                         * Call the parent implementation directly so internal
                         * compaction is not rejected by overflow-slot protection.
                         */
                        super.setStackInSlot(
                                destinationSlot,
                                stack
                        );

                        super.setStackInSlot(
                                sourceSlot,
                                ItemStack.EMPTY
                        );
                    }

                    destinationSlot++;
                }
            }

            @Override
            public ItemStack insertItem(
                    int slot,
                    ItemStack stack,
                    boolean simulate
            ) {
                if (slot >= getConfiguredSlotsPerBlock()
                        && !allowOverflowSlotWrites) {
                    return stack;
                }

                return super.insertItem(
                        slot,
                        stack,
                        simulate
                );
            }

            @Override
            public boolean isItemValid(
                    int slot,
                    ItemStack stack
            ) {
                if (slot >= getConfiguredSlotsPerBlock()
                        && !allowOverflowSlotWrites) {
                    return false;
                }

                return super.isItemValid(slot, stack);
            }

            @Override
            public void setStackInSlot(
                    int slot,
                    ItemStack stack
            ) {
                if (!allowOverflowSlotWrites
                        && slot >= getConfiguredSlotsPerBlock()
                        && !isPermittedOverflowChange(
                        slot,
                        stack
                )) {
                    return;
                }

                super.setStackInSlot(
                        slot,
                        stack
                );
            }

            /*
             * External systems may use setStackInSlot() while removing
             * items. Permit reductions and clearing, but reject increases,
             * replacements and insertion into empty overflow slots.
             */
            private boolean isPermittedOverflowChange(
                    int slot,
                    ItemStack replacement
            ) {
                ItemStack existing =
                        getStackInSlot(slot);

                if (replacement.isEmpty()) {
                    return true;
                }

                if (existing.isEmpty()) {
                    return false;
                }

                if (!ItemStack.isSameItemSameComponents(
                        existing,
                        replacement
                )) {
                    return false;
                }

                return replacement.getCount()
                        <= existing.getCount();
            }

            @Override
            protected void onContentsChanged(int slot) {
                /*
                 * Loading and contraption restoration must preserve the exact saved
                 * inventory until restoration is complete. Normal gameplay changes,
                 * however, keep all non-empty stacks packed toward slot zero.
                 */
                if (!compactingInventory
                        && !allowOverflowSlotWrites) {

                    compactingInventory = true;

                    try {
                        compactInventory();
                    } finally {
                        compactingInventory = false;
                    }
                }

                setChanged();
                updateComparators();
            }
        };
    }

    private void runWithOverflowWritesAllowed(
            Runnable operation
    ) {
        boolean previousValue =
                allowOverflowSlotWrites;

        allowOverflowSlotWrites = true;

        try {
            operation.run();
        } finally {
            allowOverflowSlotWrites = previousValue;
        }
    }

    @Override
    public void addBehaviours(
            List<BlockEntityBehaviour> behaviours
    ) {
    }

    @Override
    public void tick() {
        super.tick();

        if (lastKnownPos == null) {
            lastKnownPos = getBlockPos();
        } else if (!lastKnownPos.equals(worldPosition)
                && worldPosition != null) {
            onPositionChanged();
            return;
        }

        /*
         * Create may call preventConnectivityUpdate() while restoring the
         * contraption. Once restoration is completely finished, force a
         * fresh multiblock scan and visual rebuild.
         */
        if (level != null
                && !level.isClientSide
                && contraptionRefreshTicks > 0) {

            contraptionRefreshTicks--;

            if (contraptionRefreshTicks == 0) {
                controller = null;
                radius = 1;
                length = 1;
                itemCapability = null;

                updateConnectivity = true;
                updateVisuals = true;

                level.invalidateCapabilities(worldPosition);

                setChanged();
                sendData();
            }
        }

        if (updateConnectivity) {
            updateConnectivity();
        }

        if (updateVisuals && level != null && !level.isClientSide) {
            updateVisuals = false;
            refreshMultiblockVisuals();
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

    private void onPositionChanged() {
        removeController(true);
        lastKnownPos = worldPosition;
    }

    protected void updateComparators() {
        TieredItemVaultBlockEntity controllerBE =
                getControllerBE();

        if (controllerBE == null || level == null) {
            return;
        }

        level.blockEntityChanged(controllerBE.getBlockPos());

        int width = controllerBE.radius;
        int vaultLength = controllerBE.length;

        Direction.Axis axis =
                controllerBE.getMainConnectionAxis();

        int sizeX = axis == Direction.Axis.X
                ? vaultLength
                : width;

        int sizeZ = axis == Direction.Axis.Z
                ? vaultLength
                : width;

        BlockPos controllerPos =
                controllerBE.getBlockPos();

        for (int y = 0; y < width; y++) {
            for (int x = 0; x < sizeX; x++) {
                for (int z = 0; z < sizeZ; z++) {
                    BlockPos vaultPos =
                            controllerPos.offset(x, y, z);

                    if (!level.hasChunkAt(vaultPos)) {
                        continue;
                    }

                    level.updateNeighbourForOutputSignal(
                            vaultPos,
                            level.getBlockState(vaultPos).getBlock()
                    );
                }
            }
        }
    }

    private void invalidateMultiblockCapabilities() {
        if (level == null || level.isClientSide) {
            return;
        }

        TieredItemVaultBlockEntity controllerBE =
                getControllerBE();

        if (controllerBE == null) {
            level.invalidateCapabilities(worldPosition);
            BlockState state = getBlockState();

            if (state.hasProperty(
                    TieredItemVaultBlock.LARGE
            ) && state.getValue(
                    TieredItemVaultBlock.LARGE
            )) {
                level.setBlock(
                        worldPosition,
                        state.setValue(
                                TieredItemVaultBlock.LARGE,
                                false
                        ),
                        22
                );
            }
            return;
        }

        controllerBE.itemCapability = null;

        Direction.Axis axis =
                controllerBE.getMainConnectionAxis();

        int width = controllerBE.getWidth();
        int vaultLength = controllerBE.getHeight();

        int sizeX = axis == Direction.Axis.X
                ? vaultLength
                : width;

        int sizeZ = axis == Direction.Axis.Z
                ? vaultLength
                : width;

        BlockPos controllerPos =
                controllerBE.getBlockPos();

        for (int y = 0; y < width; y++) {
            for (int x = 0; x < sizeX; x++) {
                for (int z = 0; z < sizeZ; z++) {
                    BlockPos memberPos =
                            controllerPos.offset(x, y, z);

                    level.invalidateCapabilities(memberPos);
                }
            }
        }
    }

    @Override
    public BlockPos getLastKnownPos() {
        return lastKnownPos;
    }

    @Override
    public boolean isController() {
        return controller == null
                || worldPosition.equals(controller);
    }

    @Nullable
    @Override
    public TieredItemVaultBlockEntity getControllerBE() {
        if (isController()) {
            return this;
        }

        if (level == null || controller == null) {
            return null;
        }

        BlockEntity blockEntity =
                level.getBlockEntity(controller);

        if (blockEntity
                instanceof TieredItemVaultBlockEntity vault) {
            return vault;
        }

        return null;
    }

    @Override
    public void removeController(boolean keepContents) {
        if (level == null || level.isClientSide) {
            return;
        }

        TieredItemVaultBlockEntity previousController =
                getControllerBE();

        controller = null;
        radius = 1;
        length = 1;
        itemCapability = null;
        updateConnectivity = true;
        updateVisuals = true;

        level.invalidateCapabilities(worldPosition);

        if (previousController != null
                && previousController != this) {
            previousController.itemCapability = null;
            previousController.updateConnectivity = true;
            previousController.updateVisuals = true;
            previousController.invalidateMultiblockCapabilities();
            previousController.setChanged();
            previousController.sendData();
        }

        setChanged();
        sendData();
    }

    @Override
    public void setController(BlockPos controller) {
        if (level != null && level.isClientSide) {
            return;
        }

        this.controller = controller;
        this.itemCapability = null;
        this.updateVisuals = true;

        if (level != null && !level.isClientSide) {
            level.invalidateCapabilities(worldPosition);
        }

        setChanged();
        sendData();
    }

    @Override
    public BlockPos getController() {
        return isController()
                ? worldPosition
                : controller;
    }

    @Override
    public void preventConnectivityUpdate() {
        updateConnectivity = false;
        updateVisuals = true;
    }

    public void requestContraptionRefresh() {
        itemCapability = null;

        /*
         * Do not reform immediately. Create may still be restoring nearby
         * blocks and may subsequently call preventConnectivityUpdate().
         */
        contraptionRefreshTicks = 2;

        if (level != null && !level.isClientSide) {
            level.invalidateCapabilities(worldPosition);
        }

        setChanged();
    }

    private void refreshMultiblockVisuals() {
        if (level == null || level.isClientSide) {
            return;
        }

        TieredItemVaultBlockEntity controllerBE =
                isController()
                        ? this
                        : getControllerBE();

        if (controllerBE == null) {
            refreshSingleBlockVisuals();
            return;
        }

        if (controllerBE != this) {
            controllerBE.updateVisuals = true;
            return;
        }

        Direction.Axis axis =
                getMainConnectionAxis();

        int width = getWidth();
        int vaultLength = getHeight();

        int sizeX = axis == Direction.Axis.X
                ? vaultLength
                : width;

        int sizeZ = axis == Direction.Axis.Z
                ? vaultLength
                : width;

        boolean shouldBeLarge =
                width >= 3;

        BlockPos controllerPos =
                getBlockPos();

        for (int y = 0; y < width; y++) {
            for (int x = 0; x < sizeX; x++) {
                for (int z = 0; z < sizeZ; z++) {
                    BlockPos memberPos =
                            controllerPos.offset(x, y, z);

                    if (!level.hasChunkAt(memberPos)) {
                        continue;
                    }

                    BlockEntity blockEntity =
                            level.getBlockEntity(memberPos);

                    if (!(blockEntity
                            instanceof TieredItemVaultBlockEntity member)) {
                        continue;
                    }

                    if (member.getTier() != getTier()) {
                        continue;
                    }

                    if (!controllerPos.equals(
                            member.getController()
                    )) {
                        continue;
                    }

                    BlockState oldState =
                            member.getBlockState();

                    if (!(oldState.getBlock()
                            instanceof TieredItemVaultBlock)) {
                        continue;
                    }

                    BlockState newState =
                            oldState.setValue(
                                    TieredItemVaultBlock.LARGE,
                                    shouldBeLarge
                            );

                    if (oldState != newState) {
                        level.setBlock(
                                memberPos,
                                newState,
                                6
                        );
                    }

                    member.setChanged();
                    member.sendData();

                    level.sendBlockUpdated(
                            memberPos,
                            oldState,
                            newState,
                            3
                    );

                    member.updateVisuals = false;
                }
            }
        }
    }

    private void refreshSingleBlockVisuals() {
        if (level == null) {
            return;
        }

        BlockState oldState =
                getBlockState();

        if (!(oldState.getBlock()
                instanceof TieredItemVaultBlock)) {
            return;
        }

        BlockState newState =
                oldState.setValue(
                        TieredItemVaultBlock.LARGE,
                        false
                );

        if (oldState != newState) {
            level.setBlock(
                    worldPosition,
                    newState,
                    6
            );
        }

        level.sendBlockUpdated(
                worldPosition,
                oldState,
                newState,
                3
        );
    }

    @Override
    public void notifyMultiUpdated() {
        itemCapability = null;
        invalidateCapabilities();

        TieredItemVaultBlockEntity controllerBE =
                isController()
                        ? this
                        : getControllerBE();

        if (controllerBE != null) {
            controllerBE.itemCapability = null;
            controllerBE.updateVisuals = true;
            controllerBE.invalidateCapabilities();
            controllerBE.setChanged();
            controllerBE.sendData();
        } else {
            updateVisuals = true;
        }

        invalidateMultiblockCapabilities();

        setChanged();
        sendData();
    }

    @Override
    public Direction.Axis getMainConnectionAxis() {
        BlockState state = getBlockState();

        if (state.hasProperty(
                TieredItemVaultBlock.HORIZONTAL_AXIS
        )) {
            return state.getValue(
                    TieredItemVaultBlock.HORIZONTAL_AXIS
            );
        }

        return Direction.Axis.X;
    }

    @Override
    public int getMaxLength(
            Direction.Axis axis,
            int width
    ) {
        if (axis == Direction.Axis.Y) {
            return getMaxWidth();
        }

        return ItemVaultProperties.get(getTier()).maximumLength(width);
    }

    @Override
    public int getMaxWidth() {
        return 3;
    }

    @Override
    public int getHeight() {
        return length;
    }

    @Override
    public int getWidth() {
        return radius;
    }

    @Override
    public void setHeight(int height) {
        length = height;
        itemCapability = null;
        updateVisuals = true;

        setChanged();
        sendData();
    }

    @Override
    public void setWidth(int width) {
        radius = width;
        itemCapability = null;
        updateVisuals = true;

        setChanged();
        sendData();
    }

    public boolean hasInventory() {
        return true;
    }

    public ItemStackHandler getInventoryOfBlock() {
        return inventory;
    }

    public void applyInventoryToBlock(
            ItemStackHandler replacement
    ) {
        if (replacement == null) {
            return;
        }

        int physicalSlotCount = Math.max(
                getConfiguredSlotsPerBlock(),
                replacement.getSlots()
        );

        ItemStackHandler newInventory =
                createTierInventory(physicalSlotCount);

        /*
         * Restore stacks directly to their original slot indices.
         *
         * We deliberately use setStackInSlot() rather than
         * insertItem(). Overflow slots reject normal insertion,
         * but saved or contraption-mounted contents must still be
         * restored without being deleted or dropped.
         */
        runWithOverflowWritesAllowed(() -> {
            for (int slot = 0;
                 slot < replacement.getSlots();
                 slot++) {
                newInventory.setStackInSlot(
                        slot,
                        replacement
                                .getStackInSlot(slot)
                                .copy()
                );
            }
        });

        inventory = newInventory;
        itemCapability = null;

        if (level != null && !level.isClientSide) {
            level.invalidateCapabilities(worldPosition);
        }

        setChanged();
        sendData();
        updateComparators();
    }

    @Nullable
    public IItemHandler getCombinedInventory() {
        TieredItemVaultBlockEntity controllerBE =
                getControllerBE();

        if (controllerBE == null) {
            return null;
        }

        return controllerBE.getOrCreateCombinedInventory();
    }

    private IItemHandler getOrCreateCombinedInventory() {
        if (!isController()) {
            TieredItemVaultBlockEntity controllerBE =
                    getControllerBE();

            return controllerBE == null
                    ? inventory
                    : controllerBE.getOrCreateCombinedInventory();
        }

        if (itemCapability == null) {
            initCapability();
        }

        return itemCapability == null
                ? inventory
                : itemCapability.getCapability();
    }

    private void initCapability() {
        List<IItemHandlerModifiable> inventories =
                new ArrayList<>();

        if (level == null) {
            inventories.add(inventory);
        } else {
            Direction.Axis axis =
                    getMainConnectionAxis();

            int sizeX = axis == Direction.Axis.X
                    ? length
                    : radius;

            int sizeZ = axis == Direction.Axis.Z
                    ? length
                    : radius;

            for (int y = 0; y < radius; y++) {
                for (int x = 0; x < sizeX; x++) {
                    for (int z = 0; z < sizeZ; z++) {
                        BlockPos memberPos =
                                worldPosition.offset(x, y, z);

                        BlockEntity blockEntity =
                                level.getBlockEntity(memberPos);

                        if (!(blockEntity
                                instanceof TieredItemVaultBlockEntity member)) {
                            continue;
                        }

                        if (member.getTier() != getTier()) {
                            continue;
                        }

                        if (!worldPosition.equals(
                                member.getController()
                        )) {
                            continue;
                        }

                        inventories.add(
                                member.getInventoryOfBlock()
                        );
                    }
                }
            }
        }

        if (inventories.isEmpty()) {
            inventories.add(inventory);
        }

        IItemHandler combined;

        if (inventories.size() == 1) {
            combined = inventories.getFirst();
        } else {
            combined = new CombinedInvWrapper(
                    inventories.toArray(
                            IItemHandlerModifiable[]::new
                    )
            );
        }

        itemCapability = () -> combined;
    }

    @Override
    public void clearContent() {
        runWithOverflowWritesAllowed(() -> {
            for (int slot = 0;
                 slot < inventory.getSlots();
                 slot++) {
                inventory.setStackInSlot(
                        slot,
                        ItemStack.EMPTY
                );
            }
        });

        setChanged();
        updateComparators();
    }

    @Override
    protected void read(
            CompoundTag tag,
            HolderLookup.Provider registries,
            boolean clientPacket
    ) {
        super.read(tag, registries, clientPacket);

        if (tag.contains("Controller")) {
            controller = BlockPos.of(
                    tag.getLong("Controller")
            );
        } else {
            controller = null;
        }

        lastKnownPos = tag.contains("LastKnownPos")
                ? BlockPos.of(
                tag.getLong("LastKnownPos")
        )
                : worldPosition;

        radius = Math.max(
                1,
                tag.getInt("Width")
        );

        length = Math.max(
                1,
                tag.getInt("Height")
        );

        if (!clientPacket
                && tag.contains("Inventory")) {
            CompoundTag inventoryTag =
                    tag.getCompound("Inventory");

            /*
             * ItemStackHandler stores its physical slot count as
             * "Size" in its serialized NBT.
             *
             * Start with the saved size so lowering the config cannot
             * remove old slots before their contents are read.
             */
            int savedPhysicalSlotCount =
                    Math.max(
                            1,
                            inventoryTag.getInt("Size")
                    );

            ItemStackHandler loadedInventory =
                    createTierInventory(
                            savedPhysicalSlotCount
                    );

            allowOverflowSlotWrites = true;

            try {
                loadedInventory.deserializeNBT(
                        registries,
                        inventoryTag
                );
            } finally {
                allowOverflowSlotWrites = false;
            }

            int configuredSlotCount =
                    getConfiguredSlotsPerBlock();

            /*
             * Config increases should enlarge an existing vault.
             * Config decreases should retain the larger saved handler
             * so its excess items remain available for extraction.
             */
            if (loadedInventory.getSlots()
                    < configuredSlotCount) {
                ItemStackHandler expandedInventory =
                        createTierInventory(
                                configuredSlotCount
                        );

                allowOverflowSlotWrites = true;

                try {
                    for (int slot = 0;
                         slot < loadedInventory.getSlots();
                         slot++) {
                        expandedInventory.setStackInSlot(
                                slot,
                                loadedInventory
                                        .getStackInSlot(slot)
                                        .copy()
                        );
                    }
                } finally {
                    allowOverflowSlotWrites = false;
                }

                loadedInventory = expandedInventory;
            }

            inventory = loadedInventory;
        }

        itemCapability = null;

        if (clientPacket) {
            updateVisuals = true;

            if (level != null) {
                level.sendBlockUpdated(
                        worldPosition,
                        getBlockState(),
                        getBlockState(),
                        3
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

        tag.putInt("Width", radius);
        tag.putInt("Height", length);

        if (!clientPacket) {
            tag.put(
                    "Inventory",
                    inventory.serializeNBT(registries)
            );
        }
    }
}
