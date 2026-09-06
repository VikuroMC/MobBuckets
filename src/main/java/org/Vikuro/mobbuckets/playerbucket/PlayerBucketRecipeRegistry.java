package org.Vikuro.mobbuckets.playerbucket;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.Vikuro.mobbuckets.MobBuckets;

public class PlayerBucketRecipeRegistry {

    public static final RecipeSerializer<PlayerBucketRecipe> PLAYER_BUCKET_SERIALIZER =
            new RecipeSerializer<>(
                    MapCodec.unit(PlayerBucketRecipe.INSTANCE),
                    StreamCodec.unit(PlayerBucketRecipe.INSTANCE)
            );

    public static void register() {
        Registry.register(
                BuiltInRegistries.RECIPE_SERIALIZER,
                Identifier.fromNamespaceAndPath(MobBuckets.MOD_ID, "player_bucket"),
                PLAYER_BUCKET_SERIALIZER
        );
    }
}
