package madoku.craft.java.core.menu;

import net.minecraft.resources.Identifier;

/** Public description of a button exposed by the Core menu. */
public record MenuEntry(
	String id,
	String labelKey,
	Identifier texture,
	Identifier highlightedTexture,
	int order,
	MenuEntryAction action
) {
	public MenuEntry {
		if (id == null || id.isBlank()) throw new IllegalArgumentException("Menu entry id must not be blank.");
		if (labelKey == null || labelKey.isBlank()) throw new IllegalArgumentException("Menu entry label key must not be blank.");
		if (texture == null) throw new IllegalArgumentException("Menu entry texture must not be null.");
		if (highlightedTexture == null) throw new IllegalArgumentException("Menu entry highlighted texture must not be null.");
	}
}
