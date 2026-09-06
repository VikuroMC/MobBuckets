package org.Vikuro.mobbuckets.clutch;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FallDamageEvents {

    private static final Map<UUID, Double> TRACKED_FALL_DISTANCE = new HashMap<>();

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ClutchManager.tick();
            ClutchDeathTracker.tick();

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                UUID uuid = player.getUUID();

                if (player.fallDistance > 0.0F) {
                    double current = TRACKED_FALL_DISTANCE.getOrDefault(uuid, 0.0D);
                    TRACKED_FALL_DISTANCE.put(uuid, Math.max(current, player.fallDistance));
                    continue;
                }

                if (player.onGround() || player.isInWater()) {
                    TRACKED_FALL_DISTANCE.remove(uuid);
                }
            }
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayer player)) {
                return true;
            }

            if (ClutchManager.isReceivingTransferredDamage(player)) {
                return true;
            }

            if (!isFallDamage(source)) {
                return true;
            }

            double trackedFallDistance =
                    TRACKED_FALL_DISTANCE.getOrDefault(player.getUUID(), (double) player.fallDistance);

            boolean transferred = ClutchManager.tryTransferFallDamage(
                    player,
                    source,
                    amount,
                    trackedFallDistance
            );

            if (transferred) {
                TRACKED_FALL_DISTANCE.remove(player.getUUID());
                return false;
            }

            return true;
        });
    }

    private static boolean isFallDamage(DamageSource source) {
        return source.is(DamageTypeTags.IS_FALL);
    }
}
