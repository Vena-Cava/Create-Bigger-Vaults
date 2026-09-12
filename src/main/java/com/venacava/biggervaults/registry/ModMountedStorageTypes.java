package com.venacava.biggervaults.registry;

import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import com.simibubi.create.api.registry.CreateRegistries;
import com.venacava.biggervaults.BiggerVaults;
import com.venacava.biggervaults.contraption.TieredItemVaultMountedStorageType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

public final class ModMountedStorageTypes {

    public static final DeferredRegister<
            MountedItemStorageType<?>
            > MOUNTED_ITEM_STORAGE_TYPES =
            DeferredRegister.create(
                    CreateRegistries.MOUNTED_ITEM_STORAGE_TYPE,
                    BiggerVaults.MOD_ID
            );

    public static final DeferredHolder<
            MountedItemStorageType<?>,
            TieredItemVaultMountedStorageType
            > ITEM_VAULT =
            MOUNTED_ITEM_STORAGE_TYPES.register(
                    "item_vault",
                    TieredItemVaultMountedStorageType::new
            );

    public static void registerBlockAssociations(
            FMLCommonSetupEvent event
    ) {
        event.enqueueWork(() -> {
            TieredItemVaultMountedStorageType storageType =
                    ITEM_VAULT.get();

            ModBlocks.ITEM_VAULTS.values()
                    .forEach(holder ->
                            MountedItemStorageType.REGISTRY.register(
                                    holder.get(),
                                    storageType
                            )
                    );
        });
    }

    private ModMountedStorageTypes() {
    }
}