package org.Vikuro.mobbuckets.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.Vikuro.mobbuckets.clutch.ClutchDeathTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerEntityMixin {

    @Redirect(
            method = "die",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"
            )
    )
    private void mobbuckets$suppressVanillaClutchDeathMessage(
            PlayerList playerList,
            Component message,
            boolean overlay
    ) {
        ServerPlayer self = (ServerPlayer) (Object) this;

        if (ClutchDeathTracker.shouldSuppressVanillaDeathMessage(self.getUUID())) {
            return;
        }

        playerList.broadcastSystemMessage(message, overlay);
    }
}
