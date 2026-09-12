package com.venacava.biggervaults.contraption;

import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import com.venacava.biggervaults.blockentity.TieredItemVaultBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class TieredItemVaultMountedStorageType
        extends MountedItemStorageType<
        TieredItemVaultMountedStorage
        > {

    public TieredItemVaultMountedStorageType() {
        super(TieredItemVaultMountedStorage.CODEC);
    }

    @Nullable
    @Override
    public TieredItemVaultMountedStorage mount(
            Level level,
            BlockState state,
            BlockPos pos,
            BlockEntity blockEntity
    ) {
        if (!(blockEntity
                instanceof TieredItemVaultBlockEntity vault)) {
            return null;
        }

        return TieredItemVaultMountedStorage.fromVault(vault);
    }
}