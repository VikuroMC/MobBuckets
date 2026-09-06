package org.Vikuro.mobbuckets.mobbucket;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import org.Vikuro.mobbuckets.mixin.EnderDragonFightAccessor;

import java.util.UUID;

public final class EnderDragonBucketSupport {

    public static final UUID BUCKETED_DRAGON_UUID =
            UUID.fromString("1f0c2d9a-2f85-4fe8-b0c6-6da89b38e5ab");

    private EnderDragonBucketSupport() {
    }

    public static boolean isBucketedFight(EnderDragonFight fight) {
        return fight != null && BUCKETED_DRAGON_UUID.equals(fight.dragonUUID());
    }

    public static boolean isActiveFightDragon(EnderDragon dragon) {
        if (!(dragon.level() instanceof ServerLevel world)) {
            return false;
        }

        EnderDragonFight fight = world.getDragonFight();

        if (fight == null || dragon.getDragonFight() != fight) {
            return false;
        }

        UUID trackedDragon = fight.dragonUUID();
        return trackedDragon != null && trackedDragon.equals(dragon.getUUID());
    }

    public static void pauseFightForCapture(EnderDragon dragon) {
        if (!isActiveFightDragon(dragon)) {
            return;
        }

        EnderDragonFight fight = dragon.getDragonFight();
        if (fight == null) {
            return;
        }

        EnderDragonFightAccessor accessor = (EnderDragonFightAccessor) fight;
        accessor.mobbuckets$getDragonEvent().setVisible(false);
        accessor.mobbuckets$setDragonUUID(BUCKETED_DRAGON_UUID);
        fight.setDirty();
    }

    public static void handleReleasedFightDragon(ServerLevel releaseWorld, EnderDragon dragon) {
        EnderDragonFight releaseFight = releaseWorld.getDragonFight();

        if (isBucketedFight(releaseFight)) {
            dragon.setDragonFight(releaseFight);
            EnderDragonFightAccessor accessor = (EnderDragonFightAccessor) releaseFight;
            accessor.mobbuckets$setDragonUUID(dragon.getUUID());
            releaseFight.updateDragon(dragon);
            accessor.mobbuckets$getDragonEvent().setVisible(true);
            releaseFight.setDirty();
            return;
        }

        ServerLevel vanillaEnd = releaseWorld.getServer().getLevel(Level.END);
        if (vanillaEnd == null) {
            return;
        }

        EnderDragonFight originalFight = vanillaEnd.getDragonFight();
        if (isBucketedFight(originalFight)) {
            EnderDragonFightAccessor accessor = (EnderDragonFightAccessor) originalFight;
            accessor.mobbuckets$setDragonUUID(null);
            accessor.mobbuckets$getDragonEvent().setVisible(true);
            originalFight.setDirty();
        }
    }
}
