package org.Vikuro.mobbuckets.clutch;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;

import java.util.*;

public class ClutchManager {

    private static final double MIN_FALL_DISTANCE = 1.0D;
    private static final double HITBOX_EXPAND_XZ = 0.25D;
    private static final double HITBOX_EXPAND_Y = 0.35D;
    private static final int CLUTCH_TARGET_TICKS = 40;

    private static final Map<UUID, List<ClutchTarget>> ACTIVE_TARGETS = new HashMap<>();
    private static final Set<UUID> RECEIVING_TRANSFERRED_DAMAGE = new HashSet<>();

    public static void registerClutchTarget(ServerPlayerEntity user, Entity target) {
        ACTIVE_TARGETS
                .computeIfAbsent(user.getUuid(), uuid -> new ArrayList<>())
                .add(new ClutchTarget(target.getUuid(), CLUTCH_TARGET_TICKS));
    }

    public static void tick() {
        Iterator<Map.Entry<UUID, List<ClutchTarget>>> iterator = ACTIVE_TARGETS.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, List<ClutchTarget>> entry = iterator.next();
            entry.getValue().removeIf(target -> --target.ticksLeft <= 0);

            if (entry.getValue().isEmpty()) {
                iterator.remove();
            }
        }
    }

    public static boolean isReceivingTransferredDamage(ServerPlayerEntity player) {
        return RECEIVING_TRANSFERRED_DAMAGE.contains(player.getUuid());
    }

    public static boolean tryTransferFallDamage(ServerPlayerEntity user,
                                                DamageSource source,
                                                float damageAmount,
                                                double trackedFallDistance) {

        if (!(user.getEntityWorld() instanceof ServerWorld world)) return false;
        if (damageAmount <= 0.0F) return false;

        if (trackedFallDistance < MIN_FALL_DISTANCE && user.fallDistance < MIN_FALL_DISTANCE) {
            return false;
        }

        List<LivingEntity> targets = findTouchedRegisteredTargets(user);

        if (targets.isEmpty()) {
            return false;
        }

        for (LivingEntity target : targets) {
            transferDamage(world, user, target, source, damageAmount);
        }

        user.fallDistance = 0.0F;
        user.onLanding();
        ACTIVE_TARGETS.remove(user.getUuid());

        return true;
    }

    private static List<LivingEntity> findTouchedRegisteredTargets(ServerPlayerEntity user) {
        List<ClutchTarget> registered = ACTIVE_TARGETS.get(user.getUuid());

        if (registered == null || registered.isEmpty()) {
            return List.of();
        }

        Set<UUID> validIds = new HashSet<>();

        for (ClutchTarget target : registered) {
            validIds.add(target.entityUuid);
        }

        Box clutchBox = user.getBoundingBox()
                .expand(HITBOX_EXPAND_XZ, HITBOX_EXPAND_Y, HITBOX_EXPAND_XZ)
                .offset(0.0D, -0.15D, 0.0D);

        return user.getEntityWorld()
                .getOtherEntities(user, clutchBox, entity ->
                        entity instanceof LivingEntity living
                                && living.isAlive()
                                && !living.isRemoved()
                                && validIds.contains(entity.getUuid())
                )
                .stream()
                .filter(entity -> entity instanceof LivingEntity)
                .map(entity -> (LivingEntity) entity)
                .toList();
    }

    private static void transferDamage(ServerWorld world,
                                       ServerPlayerEntity user,
                                       LivingEntity target,
                                       DamageSource source,
                                       float damageAmount) {

        if (target instanceof ServerPlayerEntity playerTarget) {
            RECEIVING_TRANSFERRED_DAMAGE.add(playerTarget.getUuid());

            ClutchDeathTracker.markPlayerClutchDeath(playerTarget, user);

            try {
                target.damage(world, source, damageAmount);
            } finally {
                RECEIVING_TRANSFERRED_DAMAGE.remove(playerTarget.getUuid());
            }

            return;
        }

        target.damage(world, source, damageAmount);
    }

    private static class ClutchTarget {
        private final UUID entityUuid;
        private int ticksLeft;

        private ClutchTarget(UUID entityUuid, int ticksLeft) {
            this.entityUuid = entityUuid;
            this.ticksLeft = ticksLeft;
        }
    }
}