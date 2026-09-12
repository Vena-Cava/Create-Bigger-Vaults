package com.venacava.biggervaults.mixin;

import com.simibubi.create.content.logistics.vault.ItemVaultBlockEntity;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(
        value = ItemVaultBlockEntity.class,
        remap = false
)
public abstract class ItemVaultBlockEntityMixin {
    @Redirect(
            method = "initCapability",
            at = @org.spongepowered.asm.mixin.injection.At(
                    value = "INVOKE",
                    target = "Lcom/tterrag/registrate/util/entry/BlockEntityEntry;get()Ljava/lang/Object;"
            )
    )
    private Object biggerVaults$useCurrentBlockEntityType(
            BlockEntityEntry<?> original
    ) {
        ItemVaultBlockEntity self =
                (ItemVaultBlockEntity) (Object) this;

        return self.getType();
    }
}