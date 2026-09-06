package org.Vikuro.mobbuckets.mixin;

import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.UUID;

@Mixin(EnderDragonFight.class)
public interface EnderDragonFightAccessor {

    @Accessor("dragonUUID")
    void mobbuckets$setDragonUUID(UUID uuid);

    @Accessor("dragonEvent")
    ServerBossEvent mobbuckets$getDragonEvent();
}
