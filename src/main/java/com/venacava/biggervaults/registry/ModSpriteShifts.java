package com.venacava.biggervaults.registry;

import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;
import com.venacava.biggervaults.BiggerVaults;
import com.venacava.biggervaults.content.FluidTankProperties;
import com.venacava.biggervaults.content.ItemVaultProperties;
import com.venacava.biggervaults.content.MaterialTier;
import net.minecraft.resources.ResourceLocation;

import java.util.EnumMap;
import java.util.Map;

public final class ModSpriteShifts {

    public record VaultSpriteShifts(
            CTSpriteShiftEntry medium,
            CTSpriteShiftEntry large
    ) {
        public CTSpriteShiftEntry get(boolean useMedium) {
            return useMedium ? medium : large;
        }
    }

    public record TankSpriteShifts(
            CTSpriteShiftEntry side,
            CTSpriteShiftEntry top,
            CTSpriteShiftEntry inner
    ) {
    }

    public static final Map<MaterialTier, VaultSpriteShifts>
            VAULT_TOP = new EnumMap<>(MaterialTier.class);

    public static final Map<MaterialTier, VaultSpriteShifts>
            VAULT_FRONT = new EnumMap<>(MaterialTier.class);

    public static final Map<MaterialTier, VaultSpriteShifts>
            VAULT_SIDE = new EnumMap<>(MaterialTier.class);

    public static final Map<MaterialTier, VaultSpriteShifts>
            VAULT_BOTTOM = new EnumMap<>(MaterialTier.class);
    public static final Map<MaterialTier, TankSpriteShifts>
            FLUID_TANK = new EnumMap<>(MaterialTier.class);

    static {
        for (MaterialTier tier : ItemVaultProperties.supportedTiers()) {
            VAULT_TOP.put(
                    tier,
                    createVaultShifts(tier, "top")
            );

            VAULT_FRONT.put(
                    tier,
                    createVaultShifts(tier, "front")
            );

            VAULT_SIDE.put(
                    tier,
                    createVaultShifts(tier, "side")
            );

            VAULT_BOTTOM.put(
                    tier,
                    createVaultShifts(tier, "bottom")
            );
        }
        for (MaterialTier tier : FluidTankProperties.supportedTiers()) {
            FLUID_TANK.put(
                    tier,
                    createTankShifts(tier)
            );
        }
    }


    private ModSpriteShifts() {
    }

    private static VaultSpriteShifts createVaultShifts(
            MaterialTier tier,
            String face
    ) {
        String basePath =
                "block/vault/"
                        + tier.serializedName()
                        + "/vault_"
                        + face;

        ResourceLocation original =
                BiggerVaults.asResource(
                        basePath + "_small"
                );

        CTSpriteShiftEntry medium =
                CTSpriteShifter.getCT(
                        AllCTTypes.RECTANGLE,
                        original,
                        BiggerVaults.asResource(
                                basePath + "_medium"
                        )
                );

        CTSpriteShiftEntry large =
                CTSpriteShifter.getCT(
                        AllCTTypes.RECTANGLE,
                        original,
                        BiggerVaults.asResource(
                                basePath + "_large"
                        )
                );

        return new VaultSpriteShifts(
                medium,
                large
        );
    }

    private static TankSpriteShifts createTankShifts(
            MaterialTier tier
    ) {
        String base =
                "block/tank/"
                        + tier.serializedName()
                        + "/fluid_tank";

        CTSpriteShiftEntry side =
                CTSpriteShifter.getCT(
                        AllCTTypes.RECTANGLE,
                        BiggerVaults.asResource(
                                base
                        ),
                        BiggerVaults.asResource(
                                base + "_connected"
                        )
                );

        CTSpriteShiftEntry top =
                CTSpriteShifter.getCT(
                        AllCTTypes.RECTANGLE,
                        BiggerVaults.asResource(
                                base + "_top"
                        ),
                        BiggerVaults.asResource(
                                base + "_top_connected"
                        )
                );

        CTSpriteShiftEntry inner =
                CTSpriteShifter.getCT(
                        AllCTTypes.RECTANGLE,
                        BiggerVaults.asResource(
                                base + "_inner"
                        ),
                        BiggerVaults.asResource(
                                base + "_inner_connected"
                        )
                );

        return new TankSpriteShifts(
                side,
                top,
                inner
        );
    }

    public static void register() {
        // Loads the static sprite-shift maps.
    }
}
