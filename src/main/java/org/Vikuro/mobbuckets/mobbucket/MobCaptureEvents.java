package org.Vikuro.mobbuckets.mobbucket;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.Vikuro.mobbuckets.playerbucket.PlayerBucketManager;

public class MobCaptureEvents {

    public static void register() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClientSide()) return InteractionResult.PASS;
            if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
            if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
            if (!serverPlayer.isShiftKeyDown()) return InteractionResult.PASS;

            ItemStack handStack = serverPlayer.getMainHandItem();

            if (!isPlainEmptyBucket(handStack)) {
                return InteractionResult.PASS;
            }

            Entity captureTarget = MobBucketManager.resolveCaptureTarget(entity);

            if (!MobBucketManager.canCapture(captureTarget)) {
                return InteractionResult.PASS;
            }

            capture(serverPlayer, handStack, captureTarget);
            return InteractionResult.SUCCESS_SERVER;
        });
    }

    private static boolean isPlainEmptyBucket(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (!stack.is(Items.BUCKET)) return false;

        if (MobBucketManager.isMobBucket(stack)) return false;
        if (PlayerBucketManager.isPlayerBucket(stack)) return false;

        if (stack.has(DataComponents.CUSTOM_DATA)) {
            CustomData data = stack.get(DataComponents.CUSTOM_DATA);
            if (data != null && !data.isEmpty()) return false;
        }

        return true;
    }

    private static void capture(ServerPlayer player, ItemStack handStack, Entity entity) {
        boolean activeEndFightDragon = entity instanceof EnderDragon dragon
                && EnderDragonBucketSupport.isActiveFightDragon(dragon);

        ItemStack mobBucket = MobBucketManager.createMobBucket(entity, activeEndFightDragon);

        if (mobBucket.isEmpty()) return;

        if (activeEndFightDragon) {
            EnderDragonBucketSupport.pauseFightForCapture((EnderDragon) entity);
        }

        handStack.shrink(1);

        boolean inserted = player.getInventory().add(mobBucket);

        if (!inserted) {
            player.drop(mobBucket, false, false);
        }

        entity.discard();
    }
}
