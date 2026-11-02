package madoku.craft.java.core.module;

import java.util.Set;

import net.minecraft.resources.Identifier;

/** Common initialization contract shared by standalone and unified modules. */
public interface MadokuModule {
	Identifier id();

	default Set<Identifier> dependencies() {
		return MadokuModuleIds.defaultDependencies(id());
	}

	void initialize();

	/** Clears runtime state without unregistering process-wide event or registry hooks. */
	default void reset() { }
}
