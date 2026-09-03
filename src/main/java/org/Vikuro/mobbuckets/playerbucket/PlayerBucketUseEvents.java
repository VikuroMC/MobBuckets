package org.Vikuro.mobbuckets.playerbucket;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

public class PlayerBucketUseEvents {

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {

            if (world.isClient()) return ActionResult.PASS;
            if (hand != Hand.MAIN_HAND) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;

            if (!PlayerBucketManager.isPlayerBucket(serverPlayer.getMainHandStack())) {
                return ActionResult.PASS;
            }

            PlayerBucketManager.usePlayerBucket(
                    serverPlayer,
                    serverPlayer.getMainHandStack(),
                    world,
                    hitResult.getBlockPos()
            );

            return ActionResult.FAIL;
        });
    }
}