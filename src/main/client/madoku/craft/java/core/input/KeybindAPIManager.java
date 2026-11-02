package madoku.craft.java.core.input;

import java.util.LinkedHashMap;
import java.util.Map;

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

/** Shared registration boundary for customizable client keybinds. */
public final class KeybindAPIManager {
	public static final Identifier MADOKU_CRAFT_CATEGORY_ID = Identifier.fromNamespaceAndPath("madoku-craft", "main");
	private static final Map<Identifier, KeyMapping> KEYBINDS = new LinkedHashMap<>();
	private static KeyMapping.Category madokuCraftCategory;

	private KeybindAPIManager() { }

	public static KeyMapping register(Identifier id, String translationKey, int defaultKeyCode) {
		if (id == null) throw new IllegalArgumentException("Keybind id must not be null.");
		if (translationKey == null || translationKey.isBlank()) {
			throw new IllegalArgumentException("Keybind translation key must not be blank.");
		}

		KeyMapping existing = KEYBINDS.get(id);
		if (existing != null) return existing;

		KeyMapping mapping = new KeyMapping(translationKey, defaultKeyCode, madokuCraftCategory());
		KeyMappingHelper.registerKeyMapping(mapping);
		KEYBINDS.put(id, mapping);
		return mapping;
	}

	public static KeyMapping.Category madokuCraftCategory() {
		if (madokuCraftCategory == null) {
			madokuCraftCategory = KeyMapping.Category.register(MADOKU_CRAFT_CATEGORY_ID);
		}
		return madokuCraftCategory;
	}

	public static KeyMapping get(Identifier id) {
		return id == null ? null : KEYBINDS.get(id);
	}
}
