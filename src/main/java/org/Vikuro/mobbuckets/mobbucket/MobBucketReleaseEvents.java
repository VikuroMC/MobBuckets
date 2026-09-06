package org.Vikuro.mobbuckets.mobbucket;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;
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
            if (world.isClientSide()) return InteractionResult.PASS;
            if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
            if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

            ItemStack stack = serverPlayer.getMainHandItem();

            if (!MobBucketManager.isMobBucket(stack)) {
                return InteractionResult.PASS;
            }

            String entityTypeId = MobBucketManager.getStoredEntityType(stack);
            CompoundTag entityNbt = MobBucketManager.getStoredEntityNbt(stack);
            boolean endFightDragon = MobBucketManager.isStoredEndFightDragon(stack);

            if (entityTypeId == null || entityNbt == null) {
                return InteractionResult.FAIL;
            }

            ServerLevel serverWorld = (ServerLevel) world;
            BlockPos spawnBlockPos = hitResult.getBlockPos().relative(hitResult.getDirection());
            Vec3 spawnPos = Vec3.atBottomCenterOf(spawnBlockPos);

            if (!isSafeSpawn(serverWorld, spawnBlockPos, entityTypeId)) {
                return InteractionResult.FAIL;
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

            return InteractionResult.FAIL;
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
        Identifier id = Identifier.parse(pending.entityTypeId);
        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.getValue(id);

        if (entityType == null) {
            return;
        }

        Entity entity = entityType.create(pending.world, EntitySpawnReason.BUCKET);

        if (entity == null) {
            return;
        }

        CompoundTag nbt = pending.entityNbt.copy();

        nbt.remove("Passengers");
        nbt.remove("Leash");
        nbt.remove("UUID");
        nbt.remove("Pos");
        nbt.remove("Motion");
        nbt.remove("Rotation");

        entity.load(TagValueInput.create(
                ProblemReporter.DISCARDING,
                entity.registryAccess(),
                nbt
        ));

        entity.snapTo(
                pending.spawnPos.x,
                pending.spawnPos.y,
                pending.spawnPos.z,
                entity.getYRot(),
                entity.getXRot()
        );

        if (!pending.world.addFreshEntity(entity)) {
            return;
        }

        if (entity instanceof EnderDragon dragon && pending.endFightDragon) {
            EnderDragonBucketSupport.handleReleasedFightDragon(pending.world, dragon);
        }

        ClutchManager.registerClutchTarget(pending.user, entity);
    }

    private static void consumeIntoEmptyBucket(ServerPlayer player, ItemStack stack) {
        stack.shrink(1);

        ItemStack emptyBucket = new ItemStack(Items.BUCKET);

        boolean inserted = player.getInventory().add(emptyBucket);

        if (!inserted) {
            player.drop(emptyBucket, false, false);
        }

        player.containerMenu.broadcastChanges();
        BucketUseLockout.lock(player.getUUID());
    }

    private static boolean isSafeSpawn(ServerLevel world, BlockPos pos, String entityTypeId) {
        Identifier dragonId = BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.ENDER_DRAGON);

        if (dragonId != null && dragonId.toString().equals(entityTypeId)) {
            return world.getBlockState(pos).canBeReplaced();
        }

        return world.getBlockState(pos).canBeReplaced()
                && world.getBlockState(pos.above()).canBeReplaced();
    }

    private static class PendingRelease {
        private final ServerPlayer user;
        private final ServerLevel world;
        private final Vec3 spawnPos;
        private final String entityTypeId;
        private final CompoundTag entityNbt;
        private final boolean endFightDragon;
        private int ticksLeft;

        private PendingRelease(ServerPlayer user,
                               ServerLevel world,
                               Vec3 spawnPos,
                               String entityTypeId,
                               CompoundTag entityNbt,
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
