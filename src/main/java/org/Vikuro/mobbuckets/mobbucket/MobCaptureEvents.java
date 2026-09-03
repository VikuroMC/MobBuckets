package org.Vikuro.mobbuckets.mobbucket;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.Vikuro.mobbuckets.playerbucket.PlayerBucketManager;

public class MobCaptureEvents {

    public static void register() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {

            if (world.isClient()) return ActionResult.PASS;
            if (hand != Hand.MAIN_HAND) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;
            if (!serverPlayer.isSneaking()) return ActionResult.PASS;

            ItemStack handStack = serverPlayer.getMainHandStack();

            if (!isPlainEmptyBucket(handStack)) {
                return ActionResult.PASS;
            }

            Entity captureTarget = MobBucketManager.resolveCaptureTarget(entity);

            if (!MobBucketManager.canCapture(captureTarget)) {
                return ActionResult.PASS;
            }

            capture(serverPlayer, handStack, captureTarget);
            return ActionResult.SUCCESS_SERVER;
        });
    }

    private static boolean isPlainEmptyBucket(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (!stack.isOf(Items.BUCKET)) return false;

        if (MobBucketManager.isMobBucket(stack)) return false;
        if (PlayerBucketManager.isPlayerBucket(stack)) return false;

        if (stack.contains(DataComponentTypes.CUSTOM_DATA)) {
            NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
            if (data != null && !data.isEmpty()) return false;
        }

        return true;
    }

    private static void capture(ServerPlayerEntity player, ItemStack handStack, Entity entity) {
        boolean activeEndFightDragon = entity instanceof EnderDragonEntity dragon
                && EnderDragonBucketSupport.isActiveFightDragon(dragon);

        ItemStack mobBucket = MobBucketManager.createMobBucket(entity, activeEndFightDragon);

        if (mobBucket.isEmpty()) return;

        if (activeEndFightDragon) {
            EnderDragonBucketSupport.pauseFightForCapture((EnderDragonEntity) entity);
        }

        handStack.decrement(1);

        boolean inserted = player.getInventory().insertStack(mobBucket);

        if (!inserted) {
            player.dropItem(mobBucket, false);
        }

        entity.discard();
    }
}