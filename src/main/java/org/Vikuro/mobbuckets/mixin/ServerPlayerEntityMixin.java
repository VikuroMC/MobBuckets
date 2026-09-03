package org.Vikuro.mobbuckets.mixin;

import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.Vikuro.mobbuckets.clutch.ClutchDeathTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {

    @Redirect(
            method = "onDeath",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/PlayerManager;broadcast(Lnet/minecraft/text/Text;Z)V"
            )
    )
    private void mobbuckets$suppressVanillaClutchDeathMessage(
            PlayerManager playerManager,
            Text message,
            boolean overlay
    ) {
        ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;

        if (ClutchDeathTracker.shouldSuppressVanillaDeathMessage(self.getUuid())) {
            return;
        }

        playerManager.broadcast(message, overlay);
    }
}