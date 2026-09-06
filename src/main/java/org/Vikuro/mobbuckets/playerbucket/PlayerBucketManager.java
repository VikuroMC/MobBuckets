package org.Vikuro.mobbuckets.playerbucket;

import com.mojang.authlib.GameProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.Vikuro.mobbuckets.MobBuckets;
import org.Vikuro.mobbuckets.clutch.ClutchManager;
import org.Vikuro.mobbuckets.listener.BucketUseLockout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class PlayerBucketManager {

    private static final String KEY_TYPE = "MobBucketsType";
    private static final String KEY_PLAYER_UUID = "PlayerUuid";
    private static final String KEY_PLAYER_NAME = "PlayerName";
    private static final String KEY_PLAYER_UUIDS = "PlayerUuids";
    private static final String KEY_PLAYER_NAMES = "PlayerNames";

    private static final String TYPE_PLAYER_BUCKET = "player_bucket";
    private static final int MAX_PLAYERS = 8;
    private static final int WRAP_LENGTH = 32;

    public static ItemStack createPlayerBucketFromHeads(List<ItemStack> heads) {
        List<GameProfile> profiles = new ArrayList<>();
        Set<UUID> seenUuids = new HashSet<>();

        for (ItemStack head : heads) {
            ResolvableProfile profile = head.get(DataComponents.PROFILE);
            if (profile == null) return ItemStack.EMPTY;

            GameProfile gameProfile = profile.partialProfile();
            UUID uuid = gameProfile.id();
            String name = gameProfile.name();

            if (uuid == null || name == null || name.isBlank()) return ItemStack.EMPTY;
            if (!seenUuids.add(uuid)) return ItemStack.EMPTY;

            profiles.add(gameProfile);
        }

        if (profiles.isEmpty() || profiles.size() > MAX_PLAYERS) {
            return ItemStack.EMPTY;
        }

        return createPlayerBucket(profiles);
    }

    public static ItemStack createPlayerBucketFromHead(ItemStack head) {
        return createPlayerBucketFromHeads(List.of(head));
    }

    public static ItemStack createPlayerBucket(UUID uuid, String name) {
        return createPlayerBucket(List.of(new GameProfile(uuid, name)));
    }

    public static ItemStack createPlayerBucket(List<GameProfile> profiles) {
        ItemStack bucket = new ItemStack(Items.BUCKET);
        bucket.set(
                DataComponents.ITEM_MODEL,
                Identifier.fromNamespaceAndPath(MobBuckets.MOD_ID, "player")
        );

        bucket.set(
                DataComponents.CUSTOM_NAME,
                Component.literal("Bucket of Player")
                        .setStyle(Style.EMPTY.withColor(ChatFormatting.WHITE).withItalic(false))
        );

        List<String> names = new ArrayList<>();
        List<String> uuids = new ArrayList<>();

        for (GameProfile profile : profiles) {
            names.add(profile.name());
            uuids.add(profile.id().toString());
        }

        bucket.set(
                DataComponents.LORE,
                new ItemLore(buildLore(names))
        );

        CompoundTag data = new CompoundTag();
        data.putString(KEY_TYPE, TYPE_PLAYER_BUCKET);
        data.putString(KEY_PLAYER_UUIDS, String.join("|", uuids));
        data.putString(KEY_PLAYER_NAMES, String.join("|", names));

        if (profiles.size() == 1) {
            data.putString(KEY_PLAYER_UUID, profiles.getFirst().id().toString());
            data.putString(KEY_PLAYER_NAME, profiles.getFirst().name());
        }

        bucket.set(DataComponents.CUSTOM_DATA, CustomData.of(data));

        return bucket;
    }

    public static boolean isPlayerBucket(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (!stack.is(Items.BUCKET)) return false;

        CompoundTag data = getCustomData(stack);
        if (data == null) return false;

        return TYPE_PLAYER_BUCKET.equals(data.getStringOr(KEY_TYPE, ""));
    }

    public static List<UUID> getStoredPlayerUuids(ItemStack stack) {
        CompoundTag data = getCustomData(stack);
        if (data == null) return List.of();

        String multiRaw = data.getStringOr(KEY_PLAYER_UUIDS, "");

        if (!multiRaw.isBlank()) {
            List<UUID> uuids = new ArrayList<>();

            for (String raw : multiRaw.split("\\|")) {
                try {
                    uuids.add(UUID.fromString(raw));
                } catch (IllegalArgumentException ignored) {
                }
            }

            return uuids;
        }

        String singleRaw = data.getStringOr(KEY_PLAYER_UUID, "");

        if (!singleRaw.isBlank()) {
            try {
                return List.of(UUID.fromString(singleRaw));
            } catch (IllegalArgumentException ignored) {
            }
        }

        return List.of();
    }

    public static List<String> getStoredPlayerNames(ItemStack stack) {
        CompoundTag data = getCustomData(stack);
        if (data == null) return List.of();

        String multiRaw = data.getStringOr(KEY_PLAYER_NAMES, "");

        if (!multiRaw.isBlank()) {
            return Arrays.stream(multiRaw.split("\\|"))
                    .filter(name -> !name.isBlank())
                    .toList();
        }

        String singleName = data.getStringOr(KEY_PLAYER_NAME, "");
        return singleName.isBlank() ? List.of() : List.of(singleName);
    }

    public static boolean usePlayerBucket(ServerPlayer user, ItemStack stack, Level world, BlockPos clickedPos) {
        if (!isPlayerBucket(stack)) return false;
        if (world.getServer() == null) return false;

        List<UUID> storedUuids = getStoredPlayerUuids(stack);
        List<String> storedNames = getStoredPlayerNames(stack);

        if (storedUuids.isEmpty() && storedNames.isEmpty()) {
            return false;
        }

        BlockPos teleportPos = clickedPos.above();
        Vec3 destination = Vec3.atBottomCenterOf(teleportPos);

        Set<UUID> handledTargets = new HashSet<>();

        for (UUID uuid : storedUuids) {
            ServerPlayer target = world.getServer().getPlayerList().getPlayer(uuid);

            if (target == null) continue;
            if (!handledTargets.add(target.getUUID())) continue;

            target.teleportTo(destination.x, destination.y, destination.z);
            ClutchManager.registerClutchTarget(user, target);
        }

        for (String name : storedNames) {
            ServerPlayer target = world.getServer().getPlayerList().getPlayer(name);

            if (target == null) continue;
            if (!handledTargets.add(target.getUUID())) continue;

            target.teleportTo(destination.x, destination.y, destination.z);
            ClutchManager.registerClutchTarget(user, target);
        }

        stack.shrink(1);

        ItemStack emptyBucket = new ItemStack(Items.BUCKET);
        boolean inserted = user.getInventory().add(emptyBucket);

        if (!inserted) {
            user.drop(emptyBucket, false, false);
        }

        user.containerMenu.broadcastChanges();
        BucketUseLockout.lock(user.getUUID());

        return true;
    }

    private static List<Component> buildLore(List<String> names) {
        List<Component> lore = new ArrayList<>();
        List<String> wrapped = wrapNames(names);

        for (int i = 0; i < wrapped.size(); i++) {
            if (i == 0) {
                lore.add(
                        Component.empty()
                                .append(Component.literal("Contains: ")
                                        .setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(false)))
                                .append(Component.literal(wrapped.get(i))
                                        .setStyle(Style.EMPTY.withColor(ChatFormatting.WHITE).withItalic(false)))
                );
            } else {
                lore.add(
                        Component.literal(wrapped.get(i))
                                .setStyle(Style.EMPTY.withColor(ChatFormatting.WHITE).withItalic(false))
                );
            }
        }

        return lore;
    }

    private static List<String> wrapNames(List<String> names) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String name : names) {
            String next = current.isEmpty() ? name : ", " + name;

            if (current.length() + next.length() > WRAP_LENGTH && !current.isEmpty()) {
                lines.add(current.toString());
                current = new StringBuilder(name);
            } else {
                current.append(next);
            }
        }

        if (!current.isEmpty()) {
            lines.add(current.toString());
        }

        return lines;
    }

    private static CompoundTag getCustomData(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;

        CustomData component = stack.get(DataComponents.CUSTOM_DATA);
        if (component == null || component.isEmpty()) return null;

        return component.copyTag();
    }
}
