package com.venacava.biggervaults.registry;

import com.venacava.biggervaults.blockentity.TieredFluidTankBlockEntity;
import com.venacava.biggervaults.blockentity.TieredItemVaultBlockEntity;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

public final class ModCapabilities {
    private ModCapabilities() {
    }

    public static void register(
            RegisterCapabilitiesEvent event
    ) {
        ModBlockEntities.ITEM_VAULTS.values().forEach(
                blockEntityType -> event.registerBlockEntity(
                        Capabilities.ItemHandler.BLOCK,
                        blockEntityType.get(),
                        ModCapabilities::getVaultInventory
                )
        );
        ModBlockEntities.FLUID_TANKS.values().forEach(
                blockEntityType -> event.registerBlockEntity(
                        Capabilities.FluidHandler.BLOCK,
                        blockEntityType.get(),
                        ModCapabilities::getFluidInventory
                )
        );
    }

    @Nullable
    private static IItemHandler getVaultInventory(
            TieredItemVaultBlockEntity vault,
            @Nullable Direction side
    ) {
        TieredItemVaultBlockEntity controller =
                getController(vault);

        if (controller == null || controller.isRemoved()) {
            return null;
        }

        return controller.getCombinedInventory();
    }

    @Nullable
    private static TieredItemVaultBlockEntity getController(
            TieredItemVaultBlockEntity vault
    ) {
        if (vault.getControllerBE()
                instanceof TieredItemVaultBlockEntity controller) {
            return controller;
        }

        if (vault.isController()) {
            return vault;
        }

        return null;
    }

    @Nullable
    private static IFluidHandler getFluidInventory(
            TieredFluidTankBlockEntity tank,
            @Nullable Direction side
    ) {
        TieredFluidTankBlockEntity controller =
                getFluidController(tank);

        if (controller == null || controller.isRemoved()) {
            return null;
        }

        /*
         * When the multiblock is acting as a boiler, incoming water must
         * be routed into the boiler's supply handler rather than stored
         * in the ordinary fluid inventory.
         *
         * The boiler handler consumes the incoming water and records its
         * flow rate for TieredBoilerData.
         */
        if (controller.getBoilerData().isActive()) {
            return controller.getBoilerFluidHandler();
        }

        return controller.getTankInventory();
    }

    @Nullable
    private static TieredFluidTankBlockEntity getFluidController(
            TieredFluidTankBlockEntity tank
    ) {
        if (tank.getControllerBE()
                instanceof TieredFluidTankBlockEntity controller) {
            return controller;
        }

        if (tank.isController()) {
            return tank;
        }

        return null;
    }
}