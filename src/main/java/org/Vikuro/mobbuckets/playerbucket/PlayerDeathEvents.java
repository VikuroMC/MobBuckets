package org.Vikuro.mobbuckets.playerbucket;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.ResolvableProfile;

import java.util.List;

public class PlayerDeathEvents {

    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (!(entity instanceof ServerPlayer player)) {
                return;
            }

            ItemStack head = new ItemStack(Items.PLAYER_HEAD);

            head.set(
                    DataComponents.PROFILE,
                    ResolvableProfile.createResolved(player.getGameProfile())
            );

            head.set(
                    DataComponents.CUSTOM_NAME,
                    Component.literal(player.getName().getString() + "'s Head")
                            .setStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW).withItalic(false))
            );

            head.set(
                    DataComponents.LORE,
                    new ItemLore(List.of(
                            Component.literal("Craft with a bucket to create:")
                                    .setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(false)),
                            Component.literal("Bucket of " + player.getName().getString())
                                    .setStyle(Style.EMPTY.withColor(ChatFormatting.WHITE).withItalic(false))
                    ))
            );

            player.drop(head, true, false);
        });
    }
}
