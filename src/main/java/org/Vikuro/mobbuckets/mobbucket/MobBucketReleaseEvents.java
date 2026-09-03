package org.Vikuro.mobbuckets.mobbucket;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.NbtReadView;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.Vikuro.mobbuckets.MobBuckets;
import org.Vikuro.mobbuckets.clutch.ClutchManager;
import org.Vikuro.mobbuckets.listener.BucketUseLockout;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MobBucketReleaseEvents {

    private static final int RELEASE_DELAY_TICKS = 2;
    private static final List<PendingRelease> PENDING_RELEASES = new ArrayList<>();

    public static void register() {

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {

            if (world.isClient()) return ActionResult.PASS;
            if (hand != Hand.MAIN_HAND) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;

            ItemStack stack = serverPlayer.getMainHandStack();

            if (!MobBucketManager.isMobBucket(stack)) {
                return ActionResult.PASS;
            }

            String entityTypeId = MobBucketManager.getStoredEntityType(stack);
            NbtCompound entityNbt = MobBucketManager.getStoredEntityNbt(stack);
            boolean endFightDragon = MobBucketManager.isStoredEndFightDragon(stack);

            if (entityTypeId == null || entityNbt == null) {
                return ActionResult.FAIL;
            }

            ServerWorld serverWorld = (ServerWorld) world;
            BlockPos spawnBlockPos = hitResult.getBlockPos().offset(hitResult.getSide());
            Vec3d spawnPos = Vec3d.ofBottomCenter(spawnBlockPos);

            if (!isSafeSpawn(serverWorld, spawnBlockPos, entityTypeId)) {
                return ActionResult.FAIL;
            }

            consumeIntoEmptyBucket(serverPlayer, stack);

            PENDING_RELEASES.add(new PendingRelease(
                    serverPlayer,
                    serverWorld,
                    spawnPos,
                    entityTypeId,
                    entityNbt,
                    endFightDragon,
                    RELEASE_DELAY_TICKS
            ));

            return ActionResult.FAIL;
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Iterator<PendingRelease> iterator = PENDING_RELEASES.iterator();

            while (iterator.hasNext()) {
                PendingRelease pending = iterator.next();
                pending.ticksLeft--;

                if (pending.ticksLeft <= 0) {
                    release(pending);
                    iterator.remove();
                }
            }
        });
    }

    private static void release(PendingRelease pending) {
        Identifier id = Identifier.of(pending.entityTypeId);
        EntityType<?> entityType = Registries.ENTITY_TYPE.get(id);

        if (entityType == null) {
            return;
        }

        Entity entity = entityType.create(pending.world, SpawnReason.BUCKET);

        if (entity == null) {
            return;
        }

        NbtCompound nbt = pending.entityNbt.copy();

        nbt.remove("Passengers");
        nbt.remove("Leash");
        nbt.remove("UUID");
        nbt.remove("Pos");
        nbt.remove("Motion");
        nbt.remove("Rotation");

        try (ErrorReporter.Logging logging =
                     new ErrorReporter.Logging(entity.getErrorReporterContext(), MobBuckets.LOGGER)) {

            entity.readData(NbtReadView.create(logging, entity.getRegistryManager(), nbt));
        }

        entity.refreshPositionAndAngles(
                pending.spawnPos.x,
                pending.spawnPos.y,
                pending.spawnPos.z,
                entity.getYaw(),
                entity.getPitch()
        );

        if (!pending.world.spawnEntity(entity)) {
            return;
        }

        if (entity instanceof EnderDragonEntity dragon && pending.endFightDragon) {
            EnderDragonBucketSupport.handleReleasedFightDragon(pending.world, dragon);
        }

        ClutchManager.registerClutchTarget(pending.user, entity);
    }

    private static void consumeIntoEmptyBucket(ServerPlayerEntity player, ItemStack stack) {
        stack.decrement(1);

        ItemStack emptyBucket = new ItemStack(Items.BUCKET);

        boolean inserted = player.getInventory().insertStack(emptyBucket);

        if (!inserted) {
            player.dropItem(emptyBucket, false);
        }

        player.currentScreenHandler.sendContentUpdates();
        BucketUseLockout.lock(player.getUuid());
    }

    private static boolean isSafeSpawn(ServerWorld world, BlockPos pos, String entityTypeId) {
        if (Registries.ENTITY_TYPE.getId(EntityType.ENDER_DRAGON).toString().equals(entityTypeId)) {
            // The dragon's multipart body is far larger than ordinary bucketed mobs. Requiring
            // a dragon-sized empty volume would make normal placement and clutching impractical.
            // Only require the actual release point itself to be replaceable; vanilla dragon
            // collision/destruction behavior handles the resulting large body naturally.
            return world.getBlockState(pos).isReplaceable();
        }

        return world.getBlockState(pos).isReplaceable()
                && world.getBlockState(pos.up()).isReplaceable();
    }

    private static class PendingRelease {
        private final ServerPlayerEntity user;
        private final ServerWorld world;
        private final Vec3d spawnPos;
        private final String entityTypeId;
        private final NbtCompound entityNbt;
        private final boolean endFightDragon;
        private int ticksLeft;

        private PendingRelease(ServerPlayerEntity user,
                               ServerWorld world,
                               Vec3d spawnPos,
                               String entityTypeId,
                               NbtCompound entityNbt,
                               boolean endFightDragon,
                               int ticksLeft) {

            this.user = user;
            this.world = world;
            this.spawnPos = spawnPos;
            this.entityTypeId = entityTypeId;
            this.entityNbt = entityNbt;
            this.endFightDragon = endFightDragon;
            this.ticksLeft = ticksLeft;
        }
    }
}