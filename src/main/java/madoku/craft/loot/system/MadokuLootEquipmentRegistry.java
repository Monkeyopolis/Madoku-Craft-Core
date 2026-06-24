package madoku.craft.loot.system;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MadokuLootEquipmentRegistry {
	private static final Map<String, String> FILE_KEYS_BY_ENTITY_ID = new ConcurrentHashMap<>();

	static {
		registerDefault("minecraft:skeleton", "minecraft-equipment-skeleton");
		registerDefault("minecraft:stray", "minecraft-equipment-stray");
		registerDefault("minecraft:bogged", "minecraft-equipment-bogged");
		registerDefault("minecraft:parched", "minecraft-equipment-parched");
		registerDefault("minecraft:wither_skeleton", "minecraft-equipment-wither-skeleton");
		registerDefault("minecraft:husk", "minecraft-equipment-husk");
		registerDefault("minecraft:drowned", "minecraft-equipment-drowned");
		registerDefault("minecraft:zombie_villager", "minecraft-equipment-zombie-villager");
		registerDefault("minecraft:zombie", "minecraft-equipment-zombie");
	}

	private MadokuLootEquipmentRegistry() {
	}

	public static void register(EntityType<?> type, String fileKey) {
		String entityId = resolveEntityId(type);
		String normalizedFileKey = normalizeFileKey(fileKey);
		if (entityId.isBlank() || normalizedFileKey.isBlank()) {
			return;
		}
		FILE_KEYS_BY_ENTITY_ID.put(entityId, normalizedFileKey);
	}

	public static String resolveFileKey(EntityType<?> type) {
		String entityId = resolveEntityId(type);
		if (entityId.isBlank()) {
			return "";
		}
		String registered = FILE_KEYS_BY_ENTITY_ID.get(entityId);
		return registered == null ? "" : registered;
	}

	private static void registerDefault(String entityId, String fileKey) {
		if (entityId == null || fileKey == null) {
			return;
		}
		FILE_KEYS_BY_ENTITY_ID.put(entityId.trim().toLowerCase(Locale.ROOT), normalizeFileKey(fileKey));
	}

	private static String resolveEntityId(EntityType<?> type) {
		if (type == null) {
			return "";
		}
		Identifier identifier = BuiltInRegistries.ENTITY_TYPE.getKey(type);
		return identifier == null ? "" : identifier.toString().trim().toLowerCase(Locale.ROOT);
	}

	private static String normalizeFileKey(String value) {
		if (value == null || value.isBlank()) {
			return "";
		}
		String normalized = value.trim().toLowerCase(Locale.ROOT).replace('\\', '/');
		int slashIndex = normalized.lastIndexOf('/');
		if (slashIndex >= 0 && slashIndex < normalized.length() - 1) {
			normalized = normalized.substring(slashIndex + 1);
		}
		if (normalized.endsWith(".json")) {
			normalized = normalized.substring(0, normalized.length() - ".json".length());
		}
		return normalized;
	}
}
