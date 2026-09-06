package org.Vikuro.mobbuckets.mobbucket;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.storage.TagValueOutput;
import org.Vikuro.mobbuckets.MobBuckets;
import org.Vikuro.mobbuckets.mixin.EnderDragonPartAccessor;

import java.util.ArrayList;
import java.util.List;

public class MobBucketManager {

    private static final String KEY_TYPE = "MobBucketsType";
    private static final String KEY_ENTITY_TYPE = "EntityType";
    private static final String KEY_ENTITY_NBT = "EntityNbt";
    private static final String KEY_END_FIGHT_DRAGON = "EndFightDragon";

    private static final String TYPE_MOB_BUCKET = "mob_bucket";

    public static Entity resolveCaptureTarget(Entity entity) {
        if (entity instanceof EnderDragonPart dragonPart) {
            return ((EnderDragonPartAccessor) (Object) dragonPart).mobbuckets$getParentMob();
        }

        return entity;
    }

    public static boolean canCapture(Entity entity) {
        if (!(entity instanceof LivingEntity living)) return false;
        if (!living.isAlive()) return false;

        EntityType<?> type = entity.getType();

        if (type == EntityTypes.PLAYER) return false;

        if (type == EntityTypes.COD) return false;
        if (type == EntityTypes.SALMON) return false;
        if (type == EntityTypes.PUFFERFISH) return false;
        if (type == EntityTypes.TROPICAL_FISH) return false;
        if (type == EntityTypes.AXOLOTL) return false;
        if (type == EntityTypes.TADPOLE) return false;

        return true;
    }

    public static ItemStack createMobBucket(Entity entity) {
        return createMobBucket(entity, false);
    }

    public static ItemStack createMobBucket(Entity entity, boolean endFightDragon) {
        Identifier entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());

        if (entityId == null) {
            return ItemStack.EMPTY;
        }

        String rawName = entityId.getPath();
        String displayName = formatEntityName(rawName);

        TagValueOutput output = TagValueOutput.createWithContext(
                ProblemReporter.DISCARDING,
                entity.registryAccess()
        );
        entity.saveWithoutId(output);
        CompoundTag entityNbt = output.buildResult();

        entityNbt.remove("Passengers");
        entityNbt.remove("Leash");
        entityNbt.remove("UUID");
        entityNbt.remove("Pos");
        entityNbt.remove("Motion");
        entityNbt.remove("Rotation");

        ItemStack bucket = new ItemStack(Items.BUCKET);

        bucket.set(
                DataComponents.ITEM_MODEL,
                Identifier.fromNamespaceAndPath(MobBuckets.MOD_ID, "generic")
        );

        bucket.set(
                DataComponents.CUSTOM_NAME,
                Component.literal("Bucket of " + displayName)
                        .setStyle(Style.EMPTY.withColor(ChatFormatting.WHITE).withItalic(false))
        );

        bucket.set(
                DataComponents.LORE,
                new ItemLore(buildLore(rawName, displayName, entityNbt))
        );

        CompoundTag data = new CompoundTag();
        data.putString(KEY_TYPE, TYPE_MOB_BUCKET);
        data.putString(KEY_ENTITY_TYPE, entityId.toString());
        data.put(KEY_ENTITY_NBT, entityNbt);
        if (endFightDragon) {
            data.putBoolean(KEY_END_FIGHT_DRAGON, true);
        }

        bucket.set(DataComponents.CUSTOM_DATA, CustomData.of(data));

        return bucket;
    }

    public static boolean isMobBucket(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (!stack.is(Items.BUCKET)) return false;

        CompoundTag data = getCustomData(stack);
        if (data == null) return false;

        return TYPE_MOB_BUCKET.equals(data.getStringOr(KEY_TYPE, ""));
    }

    public static String getStoredEntityType(ItemStack stack) {
        CompoundTag data = getCustomData(stack);
        if (data == null) return null;

        String raw = data.getStringOr(KEY_ENTITY_TYPE, "");
        return raw.isBlank() ? null : raw;
    }

    public static boolean isStoredEndFightDragon(ItemStack stack) {
        CompoundTag data = getCustomData(stack);
        return data != null && data.getBooleanOr(KEY_END_FIGHT_DRAGON, false);
    }

    public static CompoundTag getStoredEntityNbt(ItemStack stack) {
        CompoundTag data = getCustomData(stack);
        if (data == null || !data.contains(KEY_ENTITY_NBT)) return null;

        return data.getCompoundOrEmpty(KEY_ENTITY_NBT).copy();
    }

    private static List<Component> buildLore(String rawName, String displayName, CompoundTag entityNbt) {
        List<Component> lore = new ArrayList<>();

        lore.add(
                Component.empty()
                        .append(Component.literal("Contents: ")
                                .setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(false)))
                        .append(Component.literal(displayName)
                                .setStyle(Style.EMPTY.withColor(ChatFormatting.WHITE).withItalic(false)))
        );

        if (rawName.equals("sheep")) {
            lore.add(
                    Component.empty()
                            .append(Component.literal("Color: ")
                                    .setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(false)))
                            .append(Component.literal(getSheepColor(entityNbt))
                                    .setStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW).withItalic(false)))
            );
        }

        if (rawName.equals("villager")) {
            CompoundTag villagerData = entityNbt.getCompoundOrEmpty("VillagerData");

            String profession = formatRegistryValue(
                    villagerData.getStringOr("profession", "minecraft:none")
            );

            int level = villagerData.getIntOr("level", 1);

            lore.add(
                    Component.empty()
                            .append(Component.literal("Profession: ")
                                    .setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(false)))
                            .append(Component.literal(profession)
                                    .setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withItalic(false)))
            );

            lore.add(
                    Component.empty()
                            .append(Component.literal("Mastery: ")
                                    .setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(false)))
                            .append(Component.literal(getVillagerMastery(level))
                                    .setStyle(Style.EMPTY.withColor(ChatFormatting.AQUA).withItalic(false)))
            );
        }

        return lore;
    }

    private static String getSheepColor(CompoundTag entityNbt) {
        int color = entityNbt.getByteOr("Color", (byte) 0);

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

    private static CompoundTag getCustomData(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;

        CustomData component = stack.get(DataComponents.CUSTOM_DATA);
        if (component == null || component.isEmpty()) return null;

        return component.copyTag();
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
