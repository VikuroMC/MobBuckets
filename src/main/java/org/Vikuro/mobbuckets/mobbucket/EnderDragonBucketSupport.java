package org.Vikuro.mobbuckets.mobbucket;

import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.EnderDragonFight;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.Vikuro.mobbuckets.mixin.EnderDragonFightAccessor;

import java.util.UUID;

/**
 * Keeps the vanilla End fight coherent while its active dragon is stored in a MobBucket.
 * Capturing is intentionally not treated as killing the dragon.
 */
public final class EnderDragonBucketSupport {

    /**
     * A reserved UUID persisted through EnderDragonFight.Data while the real active dragon is
     * inside a bucket. The fight tick mixin recognizes this marker and pauses automatic
     * missing-dragon recovery until the bucketed dragon is released.
     */
    public static final UUID BUCKETED_DRAGON_UUID =
            UUID.fromString("1f0c2d9a-2f85-4fe8-b0c6-6da89b38e5ab");

    private EnderDragonBucketSupport() {
    }

    public static boolean isBucketedFight(EnderDragonFight fight) {
        return fight != null && BUCKETED_DRAGON_UUID.equals(fight.getDragonUuid());
    }

    public static boolean isActiveFightDragon(EnderDragonEntity dragon) {
        if (!(dragon.getEntityWorld() instanceof ServerWorld world)) {
            return false;
        }

        EnderDragonFight fight = world.getEnderDragonFight();
        if (fight == null || dragon.getFight() != fight) {
            return false;
        }

        UUID trackedDragon = fight.getDragonUuid();
        return trackedDragon != null && trackedDragon.equals(dragon.getUuid());
    }

    public static void pauseFightForCapture(EnderDragonEntity dragon) {
        if (!isActiveFightDragon(dragon)) {
            return;
        }

        EnderDragonFight fight = dragon.getFight();
        if (fight == null) {
            return;
        }

        EnderDragonFightAccessor accessor = (EnderDragonFightAccessor) fight;
        accessor.mobbuckets$getBossBar().setVisible(false);
        accessor.mobbuckets$setDragonUuid(BUCKETED_DRAGON_UUID);
    }

    /**
     * Reconnect an active fight dragon when it is released back into The End. If the bucketed
     * boss is instead released into another dimension, it becomes a standalone dragon and the
     * original vanilla End fight is unpaused so it can recover normally.
     */
    public static void handleReleasedFightDragon(ServerWorld releaseWorld, EnderDragonEntity dragon) {
        EnderDragonFight releaseFight = releaseWorld.getEnderDragonFight();

        if (isBucketedFight(releaseFight)) {
            dragon.setFight(releaseFight);
            EnderDragonFightAccessor accessor = (EnderDragonFightAccessor) releaseFight;
            accessor.mobbuckets$setDragonUuid(dragon.getUuid());
            releaseFight.updateFight(dragon);
            accessor.mobbuckets$getBossBar().setVisible(true);
            return;
        }

        ServerWorld vanillaEnd = releaseWorld.getServer().getWorld(World.END);
        if (vanillaEnd == null) {
            return;
        }

        EnderDragonFight originalFight = vanillaEnd.getEnderDragonFight();
        if (isBucketedFight(originalFight)) {
            // The tracked boss was deliberately transported out of its fight. The released
            // dragon remains standalone here; clearing the marker lets the vanilla End fight
            // perform its normal missing-dragon recovery instead of staying soft-locked.
            EnderDragonFightAccessor accessor = (EnderDragonFightAccessor) originalFight;
            accessor.mobbuckets$setDragonUuid(null);
            // The fight is active again and may now perform vanilla missing-dragon recovery.
            // Make its boss bar eligible to render again when vanilla resumes managing it.
            accessor.mobbuckets$getBossBar().setVisible(true);
        }
    }
}
