package madoku.craft.java.core.module;

import java.util.Set;

import net.minecraft.resources.Identifier;

/** Stable internal IDs used by the Madoku module lifecycle. */
public final class MadokuModuleIds {
	public static final Identifier CORE = id("core");
	public static final Identifier ATTRIBUTES = id("attributes");
	public static final Identifier COMPAT = id("compat");
	public static final Identifier ECOSYSTEM = id("ecosystem");
	public static final Identifier FARMING = id("farming");
	public static final Identifier HUD = id("hud");
	public static final Identifier ITEMS = id("items");
	public static final Identifier LEVELS = id("levels");
	public static final Identifier MOBS = id("mobs");
	public static final Identifier PETS = id("pets");
	public static final Identifier UTILITY = id("utility");

	private MadokuModuleIds() { }

	public static Set<Identifier> defaultDependencies(Identifier moduleId) {
		return CORE.equals(moduleId) ? Set.of() : Set.of(CORE);
	}

	private static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath("madoku-craft", path);
	}
}
