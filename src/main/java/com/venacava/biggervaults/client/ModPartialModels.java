package com.venacava.biggervaults.client;

import com.venacava.biggervaults.BiggerVaults;
import com.venacava.biggervaults.content.MaterialTier;
import com.venacava.biggervaults.content.SteamEngineProperties;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;

import java.util.EnumMap;
import java.util.Map;

public final class ModPartialModels {

    public static final Map<
            MaterialTier,
            PartialModel
            > BOILER_GAUGES =
            new EnumMap<>(MaterialTier.class);

    public static final Map<
            MaterialTier,
            PartialModel
            > BOILER_GAUGE_DIALS =
            new EnumMap<>(MaterialTier.class);

    static {
        for (MaterialTier tier
                : SteamEngineProperties.supportedTiers()) {

            String modelFolder =
                    "block/"
                            + tier.serializedName()
                            + "_steam_engine/";

            BOILER_GAUGES.put(
                    tier,
                    PartialModel.of(
                            BiggerVaults.asResource(
                                    modelFolder + "gauge"
                            )
                    )
            );

            BOILER_GAUGE_DIALS.put(
                    tier,
                    PartialModel.of(
                            BiggerVaults.asResource(
                                    modelFolder + "gauge_dial"
                            )
                    )
            );
        }
    }

    private ModPartialModels() {
    }

    public static void register() {
        // Calling this method loads the class before models are baked.
    }
}