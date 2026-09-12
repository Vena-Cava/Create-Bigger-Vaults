package com.venacava.biggervaults.contraption;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import com.simibubi.create.api.contraption.storage.item.WrapperMountedItemStorage;
import com.simibubi.create.foundation.codec.CreateCodecs;
import com.venacava.biggervaults.blockentity.TieredItemVaultBlockEntity;
import com.venacava.biggervaults.registry.ModMountedStorageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

public class TieredItemVaultMountedStorage
        extends WrapperMountedItemStorage<ItemStackHandler> {

    public static final MapCodec<TieredItemVaultMountedStorage> CODEC =
            CreateCodecs.ITEM_STACK_HANDLER
                    .xmap(
                            TieredItemVaultMountedStorage::new,
                            storage -> storage.wrapped
                    )
                    .fieldOf("value");

    public TieredItemVaultMountedStorage(
            ItemStackHandler inventory
    ) {
        this(
                ModMountedStorageTypes.ITEM_VAULT.get(),
                inventory
        );
    }

    protected TieredItemVaultMountedStorage(
            MountedItemStorageType<?> type,
            ItemStackHandler inventory
    ) {
        super(type, inventory);
    }

    public static TieredItemVaultMountedStorage fromVault(
            TieredItemVaultBlockEntity vault
    ) {
        return new TieredItemVaultMountedStorage(
                copyToItemStackHandler(
                        vault.getInventoryOfBlock()
                )
        );
    }

    @Override
    public void unmount(
            Level level,
            BlockState state,
            BlockPos pos,
            BlockEntity blockEntity
    ) {
        if (!(blockEntity
                instanceof TieredItemVaultBlockEntity vault)) {
            return;
        }

        vault.applyInventoryToBlock(wrapped);
        vault.requestContraptionRefresh();
    }
}