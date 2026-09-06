package org.Vikuro.mobbuckets.mixin;

import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EnderDragonPart.class)
public interface EnderDragonPartAccessor {

    @Accessor("parentMob")
    EnderDragon mobbuckets$getParentMob();
}
