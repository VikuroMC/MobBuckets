package org.Vikuro.mobbuckets.mobbucket;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.storage.NbtWriteView;
import net.minecraft.util.ErrorReporter;
import org.Vikuro.mobbuckets.MobBuckets;

import java.util.ArrayList;
import java.util.List;

public class MobBucketManager {

    private static final String KEY_TYPE = "MobBucketsType";
    private static final String KEY_ENTITY_TYPE = "EntityType";
    private static final String KEY_ENTITY_NBT = "EntityNbt";
    private static final String KEY_END_FIGHT_DRAGON = "EndFightDragon";

    private static final String TYPE_MOB_BUCKET = "mob_bucket";

    public static Entity resolveCaptureTarget(Entity entity) {
        if (entity instanceof net.minecraft.entity.boss.dragon.EnderDragonPart dragonPart) {
            return dragonPart.owner;
        }

        return entity;
    }

    public static boolean canCapture(Entity entity) {
        if (!(entity instanceof LivingEntity living)) return false;
        if (!living.isAlive()) return false;

        EntityType<?> type = entity.getType();

        if (type == EntityType.PLAYER) return false;

        if (type == EntityType.COD) return false;
        if (type == EntityType.SALMON) return false;
        if (type == EntityType.PUFFERFISH) return false;
        if (type == EntityType.TROPICAL_FISH) return false;
        if (type == EntityType.AXOLOTL) return false;
        if (type == EntityType.TADPOLE) return false;

        return true;
    }

    public static ItemStack createMobBucket(Entity entity) {
        return createMobBucket(entity, false);
    }

    public static ItemStack createMobBucket(Entity entity, boolean endFightDragon) {
        Identifier entityId = Registries.ENTITY_TYPE.getId(entity.getType());

        String rawName = entityId.getPath();
        String displayName = formatEntityName(rawName);

        NbtCompound entityNbt;

        try (ErrorReporter.Logging logging =
                     new ErrorReporter.Logging(entity.getErrorReporterContext(), MobBuckets.LOGGER)) {

            NbtWriteView view = NbtWriteView.create(logging, entity.getRegistryManager());
            entity.saveSelfData(view);
            entityNbt = view.getNbt();
        }

        entityNbt.remove("Passengers");
        entityNbt.remove("Leash");
        entityNbt.remove("UUID");
        entityNbt.remove("Pos");
        entityNbt.remove("Motion");
        entityNbt.remove("Rotation");

        ItemStack bucket = new ItemStack(Items.BUCKET);

        bucket.set(
                DataComponentTypes.ITEM_MODEL,
                Identifier.of(MobBuckets.MOD_ID, "generic")
        );

        bucket.set(
                DataComponentTypes.CUSTOM_NAME,
                Text.literal("Bucket of " + displayName)
                        .setStyle(Style.EMPTY.withColor(Formatting.WHITE).withItalic(false))
        );

        bucket.set(
                DataComponentTypes.LORE,
                new LoreComponent(buildLore(rawName, displayName, entityNbt))
        );

        NbtCompound data = new NbtCompound();
        data.putString(KEY_TYPE, TYPE_MOB_BUCKET);
        data.putString(KEY_ENTITY_TYPE, entityId.toString());
        data.put(KEY_ENTITY_NBT, entityNbt);
        if (endFightDragon) {
            data.putBoolean(KEY_END_FIGHT_DRAGON, true);
        }

        bucket.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(data));

        return bucket;
    }

    public static boolean isMobBucket(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (!stack.isOf(Items.BUCKET)) return false;

        NbtCompound data = getCustomData(stack);
        if (data == null) return false;

        return TYPE_MOB_BUCKET.equals(data.getString(KEY_TYPE, ""));
    }

    public static String getStoredEntityType(ItemStack stack) {
        NbtCompound data = getCustomData(stack);
        if (data == null) return null;

        String raw = data.getString(KEY_ENTITY_TYPE, "");
        return raw.isBlank() ? null : raw;
    }

    public static boolean isStoredEndFightDragon(ItemStack stack) {
        NbtCompound data = getCustomData(stack);
        return data != null && data.getBoolean(KEY_END_FIGHT_DRAGON, false);
    }

    public static NbtCompound getStoredEntityNbt(ItemStack stack) {
        NbtCompound data = getCustomData(stack);
        if (data == null) return null;

        return data.getCompound(KEY_ENTITY_NBT)
                .map(NbtCompound::copy)
                .orElse(null);
    }

    private static List<Text> buildLore(String rawName, String displayName, NbtCompound entityNbt) {
        List<Text> lore = new ArrayList<>();

        lore.add(
                Text.empty()
                        .append(Text.literal("Contents: ")
                                .setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(false)))
                        .append(Text.literal(displayName)
                                .setStyle(Style.EMPTY.withColor(Formatting.WHITE).withItalic(false)))
        );

        if (rawName.equals("sheep")) {
            lore.add(
                    Text.empty()
                            .append(Text.literal("Color: ")
                                    .setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(false)))
                            .append(Text.literal(getSheepColor(entityNbt))
                                    .setStyle(Style.EMPTY.withColor(Formatting.YELLOW).withItalic(false)))
            );
        }

        if (rawName.equals("villager")) {
            NbtCompound villagerData = entityNbt.getCompound("VillagerData")
                    .orElse(new NbtCompound());

            String profession = formatRegistryValue(
                    villagerData.getString("profession", "minecraft:none")
            );

            int level = villagerData.getInt("level", 1);

            lore.add(
                    Text.empty()
                            .append(Text.literal("Profession: ")
                                    .setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(false)))
                            .append(Text.literal(profession)
                                    .setStyle(Style.EMPTY.withColor(Formatting.GREEN).withItalic(false)))
            );

            lore.add(
                    Text.empty()
                            .append(Text.literal("Mastery: ")
                                    .setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(false)))
                            .append(Text.literal(getVillagerMastery(level))
                                    .setStyle(Style.EMPTY.withColor(Formatting.AQUA).withItalic(false)))
            );
        }

        return lore;
    }

    private static String getSheepColor(NbtCompound entityNbt) {
        int color = entityNbt.getByte("Color", (byte) 0);

        return switch (color) {
            case 0 -> "WHITE";
            case 1 -> "ORANGE";
            case 2 -> "MAGENTA";
            case 3 -> "LIGHT_BLUE";
            case 4 -> "YELLOW";
            case 5 -> "LIME";
            case 6 -> "PINK";
            case 7 -> "GRAY";
            case 8 -> "LIGHT_GRAY";
            case 9 -> "CYAN";
            case 10 -> "PURPLE";
            case 11 -> "BLUE";
            case 12 -> "BROWN";
            case 13 -> "GREEN";
            case 14 -> "RED";
            case 15 -> "BLACK";
            default -> "UNKNOWN";
        };
    }

    private static String getVillagerMastery(int level) {
        return switch (level) {
            case 1 -> "NOVICE";
            case 2 -> "APPRENTICE";
            case 3 -> "JOURNEYMAN";
            case 4 -> "EXPERT";
            case 5 -> "MASTER";
            default -> "UNKNOWN";
        };
    }

    private static String formatRegistryValue(String raw) {
        if (raw == null || raw.isBlank()) return "UNKNOWN";

        String value = raw.contains(":")
                ? raw.substring(raw.indexOf(":") + 1)
                : raw;

        return value.toUpperCase();
    }

    private static NbtCompound getCustomData(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;

        NbtComponent component = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (component == null || component.isEmpty()) return null;

        return component.copyNbt();
    }

    private static String formatEntityName(String raw) {
        String[] parts = raw.split("_");
        StringBuilder builder = new StringBuilder();

        for (String part : parts) {
            if (part.isBlank()) continue;

            builder.append(Character.toUpperCase(part.charAt(0)))
                    .append(part.substring(1))
                    .append(" ");
        }

        return builder.toString().trim();
    }
}