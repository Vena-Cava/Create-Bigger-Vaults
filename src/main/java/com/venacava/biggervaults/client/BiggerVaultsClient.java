package com.venacava.biggervaults.client;

import com.venacava.biggervaults.BiggerVaults;
import com.venacava.biggervaults.registry.ModBlocks;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(
        modid = BiggerVaults.MOD_ID,
        bus = EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public final class BiggerVaultsClient {

    private BiggerVaultsClient() {
    }

    @SubscribeEvent
    public static void onClientSetup(
            FMLClientSetupEvent event
    ) {
        event.enqueueWork(() -> {
            ModBlocks.FLUID_TANKS.values().forEach(holder ->
                    ItemBlockRenderTypes.setRenderLayer(
                            holder.get(),
                            RenderType.cutoutMipped()
                    )
            );
        });
    }
}