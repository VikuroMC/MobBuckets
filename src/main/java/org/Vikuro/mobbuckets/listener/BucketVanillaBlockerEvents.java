package org.Vikuro.mobbuckets.listener;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import org.Vikuro.mobbuckets.mobbucket.MobBucketManager;
import org.Vikuro.mobbuckets.playerbucket.PlayerBucketManager;

public class BucketVanillaBlockerEvents {

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> BucketUseLockout.tick());

        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);

            if (BucketUseLockout.isLocked(player.getUuid())) {
                return ActionResult.FAIL;
            }

            if (MobBucketManager.isMobBucket(stack) || PlayerBucketManager.isPlayerBucket(stack)) {
                return ActionResult.FAIL;
            }

            return ActionResult.PASS;
        });
    }
}