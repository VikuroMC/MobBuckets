package org.Vikuro.mobbuckets.clutch;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.HashSet;

public class ClutchDeathTracker {

    private static final int CONTEXT_TICKS = 60;
    private static final Map<UUID, DeathContext> CONTEXTS = new HashMap<>();
    private static final Set<UUID> SUPPRESS_DEATH_MESSAGE = new HashSet<>();

    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {

            if (!(entity instanceof ServerPlayerEntity victim)) {
                return;
            }

            DeathContext context = CONTEXTS.remove(victim.getUuid());

            if (context == null) {
                return;
            }

            String message = DeathMessageManager.randomPvpClutch(
                    victim.getName().getString(),
                    context.userName
            );

            victim.getEntityWorld()
                    .getServer()
                    .getPlayerManager()
                    .broadcast(Text.literal(message), false);
        });
    }

    public static void markPlayerClutchDeath(ServerPlayerEntity victim, ServerPlayerEntity user) {

        SUPPRESS_DEATH_MESSAGE.add(victim.getUuid());

        CONTEXTS.put(
                victim.getUuid(),
                new DeathContext(user.getName().getString(), CONTEXT_TICKS)
        );
    }

    public static boolean shouldSuppressVanillaDeathMessage(UUID uuid) {
        return SUPPRESS_DEATH_MESSAGE.remove(uuid);
    }

    public static void tick() {
        Iterator<Map.Entry<UUID, DeathContext>> iterator = CONTEXTS.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, DeathContext> entry = iterator.next();

            entry.getValue().ticksLeft--;

            if (entry.getValue().ticksLeft <= 0) {
                iterator.remove();
            }
        }
    }

    private static class DeathContext {
        private final String userName;
        private int ticksLeft;

        private DeathContext(String userName, int ticksLeft) {
            this.userName = userName;
            this.ticksLeft = ticksLeft;
        }
    }
}