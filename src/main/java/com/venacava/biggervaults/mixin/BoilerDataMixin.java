package com.venacava.biggervaults.mixin;

import com.simibubi.create.content.fluids.tank.BoilerData;
import com.venacava.biggervaults.block.TieredSteamEngineBlock;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BoilerData.class)
public abstract class BoilerDataMixin {

    /**
     * Makes Create's ordinary Fluid Tank boiler count Bigger Vaults
     * Steam Engines alongside Create's own Steam Engine.
     */
    @Redirect(
            method = "evaluate",
            at = @At(
                    value = "INVOKE",
                    target =
                            "Lcom/tterrag/registrate/util/entry/BlockEntry;"
                                    + "has("
                                    + "Lnet/minecraft/world/level/block/state/BlockState;"
                                    + ")Z",
                    ordinal = 0
            )
    )
    private boolean biggerVaults$countTieredSteamEngines(
            BlockEntry<?> blockEntry,
            BlockState blockState
    ) {
        return blockEntry.has(blockState)
                || blockState.getBlock()
                instanceof TieredSteamEngineBlock;
    }
}