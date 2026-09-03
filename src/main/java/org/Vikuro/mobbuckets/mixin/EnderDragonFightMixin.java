package org.Vikuro.mobbuckets.mixin;

import net.minecraft.entity.boss.dragon.EnderDragonFight;
import org.Vikuro.mobbuckets.mobbucket.EnderDragonBucketSupport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnderDragonFight.class)
public abstract class EnderDragonFightMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void mobbuckets$pauseFightWhileDragonIsBucketed(CallbackInfo ci) {
        if (EnderDragonBucketSupport.isBucketedFight((EnderDragonFight) (Object) this)) {
            ci.cancel();
        }
    }
}
