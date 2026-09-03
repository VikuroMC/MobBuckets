package org.Vikuro.mobbuckets.playerbucket;

import com.mojang.authlib.GameProfile;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.Vikuro.mobbuckets.clutch.ClutchManager;
import org.Vikuro.mobbuckets.listener.BucketUseLockout;
import net.minecraft.util.Identifier;
import org.Vikuro.mobbuckets.MobBuckets;

import java.util.*;

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
            ProfileComponent profile = head.get(DataComponentTypes.PROFILE);
            if (profile == null) return ItemStack.EMPTY;

            GameProfile gameProfile = profile.getGameProfile();
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
                DataComponentTypes.ITEM_MODEL,
                Identifier.of(MobBuckets.MOD_ID, "player")
        );

        bucket.set(
                DataComponentTypes.CUSTOM_NAME,
                Text.literal("Bucket of Player")
                        .setStyle(Style.EMPTY.withColor(Formatting.WHITE).withItalic(false))
        );

        List<String> names = new ArrayList<>();
        List<String> uuids = new ArrayList<>();

        for (GameProfile profile : profiles) {
            names.add(profile.name());
            uuids.add(profile.id().toString());
        }

        bucket.set(
                DataComponentTypes.LORE,
                new LoreComponent(buildLore(names))
        );

        NbtCompound data = new NbtCompound();
        data.putString(KEY_TYPE, TYPE_PLAYER_BUCKET);
        data.putString(KEY_PLAYER_UUIDS, String.join("|", uuids));
        data.putString(KEY_PLAYER_NAMES, String.join("|", names));

        if (profiles.size() == 1) {
            data.putString(KEY_PLAYER_UUID, profiles.getFirst().id().toString());
            data.putString(KEY_PLAYER_NAME, profiles.getFirst().name());
        }

        bucket.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(data));

        return bucket;
    }

    public static boolean isPlayerBucket(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (!stack.isOf(Items.BUCKET)) return false;

        NbtCompound data = getCustomData(stack);
        if (data == null) return false;

        return TYPE_PLAYER_BUCKET.equals(data.getString(KEY_TYPE, ""));
    }

    public static List<UUID> getStoredPlayerUuids(ItemStack stack) {
        NbtCompound data = getCustomData(stack);
        if (data == null) return List.of();

        String multiRaw = data.getString(KEY_PLAYER_UUIDS, "");

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

        String singleRaw = data.getString(KEY_PLAYER_UUID, "");

        if (!singleRaw.isBlank()) {
            try {
                return List.of(UUID.fromString(singleRaw));
            } catch (IllegalArgumentException ignored) {
            }
        }

        return List.of();
    }

    public static List<String> getStoredPlayerNames(ItemStack stack) {
        NbtCompound data = getCustomData(stack);
        if (data == null) return List.of();

        String multiRaw = data.getString(KEY_PLAYER_NAMES, "");

        if (!multiRaw.isBlank()) {
            return Arrays.stream(multiRaw.split("\\|"))
                    .filter(name -> !name.isBlank())
                    .toList();
        }

        String singleName = data.getString(KEY_PLAYER_NAME, "");
        return singleName.isBlank() ? List.of() : List.of(singleName);
    }

    public static boolean usePlayerBucket(ServerPlayerEntity user, ItemStack stack, World world, BlockPos clickedPos) {
        if (!isPlayerBucket(stack)) return false;
        if (world.getServer() == null) return false;

        List<UUID> storedUuids = getStoredPlayerUuids(stack);
        List<String> storedNames = getStoredPlayerNames(stack);

        if (storedUuids.isEmpty() && storedNames.isEmpty()) {
            return false;
        }

        BlockPos teleportPos = clickedPos.up();
        Vec3d destination = Vec3d.ofBottomCenter(teleportPos);

        Set<UUID> handledTargets = new HashSet<>();

        for (UUID uuid : storedUuids) {
            ServerPlayerEntity target = world.getServer().getPlayerManager().getPlayer(uuid);

            if (target == null) continue;
            if (!handledTargets.add(target.getUuid())) continue;

            target.requestTeleport(destination.x, destination.y, destination.z);
            ClutchManager.registerClutchTarget(user, target);
        }

        for (String name : storedNames) {
            ServerPlayerEntity target = world.getServer().getPlayerManager().getPlayer(name);

            if (target == null) continue;
            if (!handledTargets.add(target.getUuid())) continue;

            target.requestTeleport(destination.x, destination.y, destination.z);
            ClutchManager.registerClutchTarget(user, target);
        }

        stack.decrement(1);

        boolean inserted = user.getInventory().insertStack(new ItemStack(Items.BUCKET));

        if (!inserted) {
            user.dropItem(new ItemStack(Items.BUCKET), false);
        }

        user.currentScreenHandler.sendContentUpdates();
        BucketUseLockout.lock(user.getUuid());

        return true;
    }

    private static List<Text> buildLore(List<String> names) {
        List<Text> lore = new ArrayList<>();
        List<String> wrapped = wrapNames(names);

        for (int i = 0; i < wrapped.size(); i++) {
            if (i == 0) {
                lore.add(
                        Text.empty()
                                .append(Text.literal("Contains: ")
                                        .setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(false)))
                                .append(Text.literal(wrapped.get(i))
                                        .setStyle(Style.EMPTY.withColor(Formatting.WHITE).withItalic(false)))
                );
            } else {
                lore.add(
                        Text.literal(wrapped.get(i))
                                .setStyle(Style.EMPTY.withColor(Formatting.WHITE).withItalic(false))
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

    private static NbtCompound getCustomData(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;

        NbtComponent component = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (component == null || component.isEmpty()) return null;

        return component.copyNbt();
    }
}