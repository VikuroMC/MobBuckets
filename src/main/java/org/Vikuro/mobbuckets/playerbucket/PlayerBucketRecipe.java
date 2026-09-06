package org.Vikuro.mobbuckets.playerbucket;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.Vikuro.mobbuckets.mobbucket.MobBucketManager;

import java.util.ArrayList;
import java.util.List;

public class PlayerBucketRecipe extends CustomRecipe {

    private static final int MAX_HEADS = 8;
    static final PlayerBucketRecipe INSTANCE = new PlayerBucketRecipe();

    private PlayerBucketRecipe() {
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return !getValidResult(input).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        return getValidResult(input);
    }

    @Override
    public RecipeSerializer<PlayerBucketRecipe> getSerializer() {
        return PlayerBucketRecipeRegistry.PLAYER_BUCKET_SERIALIZER;
    }

    private ItemStack getValidResult(CraftingInput input) {
        List<ItemStack> heads = new ArrayList<>();
        ItemStack bucket = ItemStack.EMPTY;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);

            if (stack.isEmpty()) continue;

            if (stack.is(Items.PLAYER_HEAD)) {
                if (stack.get(DataComponents.PROFILE) == null) return ItemStack.EMPTY;

                heads.add(stack);

                if (heads.size() > MAX_HEADS) {
                    return ItemStack.EMPTY;
                }

                continue;
            }

            if (stack.is(Items.BUCKET)) {
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
        if (!stack.is(Items.BUCKET)) return false;

        if (MobBucketManager.isMobBucket(stack)) return false;
        if (PlayerBucketManager.isPlayerBucket(stack)) return false;

        if (stack.has(DataComponents.CUSTOM_DATA)) {
            CustomData data = stack.get(DataComponents.CUSTOM_DATA);
            if (data != null && !data.isEmpty()) return false;
        }

        return true;
    }
}
