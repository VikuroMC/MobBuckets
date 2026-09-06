package org.Vikuro.mobbuckets.clutch;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ClutchManager {

    private static final double MIN_FALL_DISTANCE = 1.0D;
    private static final double HITBOX_EXPAND_XZ = 0.25D;
    private static final double HITBOX_EXPAND_Y = 0.35D;
    private static final int CLUTCH_TARGET_TICKS = 40;

    private static final Map<UUID, List<ClutchTarget>> ACTIVE_TARGETS = new HashMap<>();
    private static final Set<UUID> RECEIVING_TRANSFERRED_DAMAGE = new HashSet<>();

    public static void registerClutchTarget(ServerPlayer user, Entity target) {
        ACTIVE_TARGETS
                .computeIfAbsent(user.getUUID(), uuid -> new ArrayList<>())
                .add(new ClutchTarget(target.getUUID(), CLUTCH_TARGET_TICKS));
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

    public static boolean isReceivingTransferredDamage(ServerPlayer player) {
        return RECEIVING_TRANSFERRED_DAMAGE.contains(player.getUUID());
    }

    public static boolean tryTransferFallDamage(ServerPlayer user,
                                                DamageSource source,
                                                float damageAmount,
                                                double trackedFallDistance) {
        ServerLevel world = user.level();
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

        user.resetFallDistance();
        ACTIVE_TARGETS.remove(user.getUUID());

        return true;
    }

    private static List<LivingEntity> findTouchedRegisteredTargets(ServerPlayer user) {
        List<ClutchTarget> registered = ACTIVE_TARGETS.get(user.getUUID());

        if (registered == null || registered.isEmpty()) {
            return List.of();
        }

        Set<UUID> validIds = new HashSet<>();

        for (ClutchTarget target : registered) {
            validIds.add(target.entityUuid);
        }

        AABB clutchBox = user.getBoundingBox()
                .inflate(HITBOX_EXPAND_XZ, HITBOX_EXPAND_Y, HITBOX_EXPAND_XZ)
                .move(0.0D, -0.15D, 0.0D);

        return user.level()
                .getEntities(user, clutchBox, entity ->
                        entity instanceof LivingEntity living
                                && living.isAlive()
                                && !living.isRemoved()
                                && validIds.contains(entity.getUUID())
                )
                .stream()
                .map(entity -> (LivingEntity) entity)
                .toList();
    }

    private static void transferDamage(ServerLevel world,
                                       ServerPlayer user,
                                       LivingEntity target,
                                       DamageSource source,
                                       float damageAmount) {
        if (target instanceof ServerPlayer playerTarget) {
            RECEIVING_TRANSFERRED_DAMAGE.add(playerTarget.getUUID());
            ClutchDeathTracker.markPlayerClutchDeath(playerTarget, user);

            try {
                target.hurtServer(world, source, damageAmount);
            } finally {
                RECEIVING_TRANSFERRED_DAMAGE.remove(playerTarget.getUUID());
            }

            return;
        }

        target.hurtServer(world, source, damageAmount);
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
