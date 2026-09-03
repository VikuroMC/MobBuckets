package org.Vikuro.mobbuckets.playerbucket;

import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.Vikuro.mobbuckets.MobBuckets;

public class PlayerBucketRecipeRegistry {

    public static RecipeSerializer<PlayerBucketRecipe> PLAYER_BUCKET_SERIALIZER;

    public static void register() {
        PLAYER_BUCKET_SERIALIZER = Registry.register(
                Registries.RECIPE_SERIALIZER,
                Identifier.of(MobBuckets.MOD_ID, "player_bucket"),
                new SpecialCraftingRecipe.SpecialRecipeSerializer<>(PlayerBucketRecipe::new)
        );
    }
}