package org.Vikuro.mobbuckets.mixin;

import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.boss.dragon.EnderDragonFight;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.UUID;

@Mixin(EnderDragonFight.class)
public interface EnderDragonFightAccessor {

    @Accessor("dragonUuid")
    void mobbuckets$setDragonUuid(UUID uuid);

    @Accessor("bossBar")
    ServerBossBar mobbuckets$getBossBar();
}
