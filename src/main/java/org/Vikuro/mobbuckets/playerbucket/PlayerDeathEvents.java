package org.Vikuro.mobbuckets.playerbucket;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class PlayerDeathEvents {

    public static void register() {

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {

            if (!(entity instanceof ServerPlayerEntity player)) {
                return;
            }

            ItemStack head = new ItemStack(Items.PLAYER_HEAD);

            head.set(
                    DataComponentTypes.PROFILE,
                    ProfileComponent.ofStatic(player.getGameProfile())
            );

            head.set(
                    DataComponentTypes.CUSTOM_NAME,
                    Text.literal(player.getName().getString() + "'s Head")
                            .setStyle(Style.EMPTY.withColor(Formatting.YELLOW).withItalic(false))
            );

            head.set(
                    DataComponentTypes.LORE,
                    new LoreComponent(List.of(
                            Text.literal("Craft with a bucket to create:")
                                    .setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(false)),

                            Text.literal("Bucket of " + player.getName().getString())
                                    .setStyle(Style.EMPTY.withColor(Formatting.WHITE).withItalic(false))
                    ))
            );

            player.dropItem(head, true, false);
        });
    }
}