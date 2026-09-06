package org.Vikuro.mobbuckets.playerbucket;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;

public class PlayerBucketUseEvents {

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClientSide()) return InteractionResult.PASS;
            if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
            if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

            if (!PlayerBucketManager.isPlayerBucket(serverPlayer.getMainHandItem())) {
                return InteractionResult.PASS;
            }

            PlayerBucketManager.usePlayerBucket(
                    serverPlayer,
                    serverPlayer.getMainHandItem(),
                    world,
                    hitResult.getBlockPos()
            );

            return InteractionResult.FAIL;
        });
    }
}
