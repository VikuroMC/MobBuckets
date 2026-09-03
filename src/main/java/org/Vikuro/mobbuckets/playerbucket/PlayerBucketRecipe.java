package org.Vikuro.mobbuckets.playerbucket;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;
import org.Vikuro.mobbuckets.mobbucket.MobBucketManager;

import java.util.ArrayList;
import java.util.List;

public class PlayerBucketRecipe extends SpecialCraftingRecipe {

    private static final int MAX_HEADS = 8;

    public PlayerBucketRecipe(CraftingRecipeCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingRecipeInput input, World world) {
        return !getValidResult(input).isEmpty();
    }

    @Override
    public ItemStack craft(CraftingRecipeInput input, RegistryWrapper.WrapperLookup registries) {
        return getValidResult(input);
    }

    @Override
    public RecipeSerializer<PlayerBucketRecipe> getSerializer() {
        return PlayerBucketRecipeRegistry.PLAYER_BUCKET_SERIALIZER;
    }

    private ItemStack getValidResult(CraftingRecipeInput input) {
        List<ItemStack> heads = new ArrayList<>();
        ItemStack bucket = ItemStack.EMPTY;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getStackInSlot(i);

            if (stack.isEmpty()) continue;

            if (stack.isOf(Items.PLAYER_HEAD)) {
                if (stack.get(DataComponentTypes.PROFILE) == null) return ItemStack.EMPTY;

                heads.add(stack);

                if (heads.size() > MAX_HEADS) {
                    return ItemStack.EMPTY;
                }

                continue;
            }

            if (stack.isOf(Items.BUCKET)) {
                if (!bucket.isEmpty()) return ItemStack.EMPTY;
                if (!isPlainEmptyBucket(stack)) return ItemStack.EMPTY;

                bucket = stack;
                continue;
            }

            return ItemStack.EMPTY;
        }

        if (bucket.isEmpty()) return ItemStack.EMPTY;
        if (heads.isEmpty()) return ItemStack.EMPTY;

        return PlayerBucketManager.createPlayerBucketFromHeads(heads);
    }

    private boolean isPlainEmptyBucket(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (!stack.isOf(Items.BUCKET)) return false;

        if (MobBucketManager.isMobBucket(stack)) return false;
        if (PlayerBucketManager.isPlayerBucket(stack)) return false;

        if (stack.contains(DataComponentTypes.CUSTOM_DATA)) {
            NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
            if (data != null && !data.isEmpty()) return false;
        }

        return true;
    }
}