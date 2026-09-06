package org.Vikuro.mobbuckets;

import net.fabricmc.api.ModInitializer;
import org.Vikuro.mobbuckets.clutch.ClutchDeathTracker;
import org.Vikuro.mobbuckets.clutch.FallDamageEvents;
import org.Vikuro.mobbuckets.listener.BucketVanillaBlockerEvents;
import org.Vikuro.mobbuckets.mobbucket.MobBucketReleaseEvents;
import org.Vikuro.mobbuckets.mobbucket.MobCaptureEvents;
import org.Vikuro.mobbuckets.playerbucket.PlayerBucketRecipeRegistry;
import org.Vikuro.mobbuckets.playerbucket.PlayerBucketUseEvents;
import org.Vikuro.mobbuckets.playerbucket.PlayerDeathEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MobBuckets implements ModInitializer {

    public static final String MOD_ID = "mobbuckets";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("MobBuckets Fabric has loaded.");
        PlayerBucketRecipeRegistry.register();
        PlayerDeathEvents.register();
        PlayerBucketUseEvents.register();
        MobCaptureEvents.register();
        MobBucketReleaseEvents.register();
        FallDamageEvents.register();
        BucketVanillaBlockerEvents.register();
        ClutchDeathTracker.register();
    }
}
