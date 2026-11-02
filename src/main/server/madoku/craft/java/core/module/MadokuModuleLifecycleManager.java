package madoku.craft.java.core.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.resources.Identifier;

/**
 * Coordinates module initialization independently for the server and client sides.
 * Registration is explicit; dependency order and once-only initialization are centralized.
 */
public final class MadokuModuleLifecycleManager {
	public enum State {
		REGISTERED,
		WAITING_FOR_DEPENDENCY,
		INITIALIZING,
		RUNNING,
		FAILED
	}

	private static final MadokuModuleLifecycleManager SERVER = new MadokuModuleLifecycleManager("server");
	private static final MadokuModuleLifecycleManager CLIENT = new MadokuModuleLifecycleManager("client");

	private final Logger logger;
	private final Map<Identifier, Registration> registrations = new LinkedHashMap<>();
	private final List<Identifier> initializationOrder = new ArrayList<>();
	private final List<Integration> pendingIntegrations = new ArrayList<>();
	private boolean processing;

	private MadokuModuleLifecycleManager(String scope) {
		this.logger = LoggerFactory.getLogger("MadokuModuleLifecycle." + scope);
	}

	public static MadokuModuleLifecycleManager server() {
		return SERVER;
	}

	public static MadokuModuleLifecycleManager client() {
		return CLIENT;
	}

	public synchronized void register(MadokuModule module) {
		if (module == null || module.id() == null) {
			throw new IllegalArgumentException("Madoku module and ID must not be null.");
		}
		Registration existing = registrations.get(module.id());
		if (existing != null) {
			if (existing.module() != module) {
				throw new IllegalStateException("Madoku module is registered more than once: " + module.id());
			}
			return;
		}
		registrations.put(module.id(), new Registration(module, State.REGISTERED));
		initializeRegistered();
	}

	public synchronized void initializeRegistered() {
		if (processing) return;
		processing = true;
		try {
			boolean progress;
			do {
				progress = false;
				for (Identifier id : new ArrayList<>(registrations.keySet())) {
					State before = registrations.get(id).state();
					initializeModule(id, new LinkedHashSet<>());
					progress |= before != registrations.get(id).state();
				}
			} while (progress);
		} finally {
			processing = false;
		}
		runReadyIntegrations();
	}

	/**
	 * Runs an optional integration once all listed modules have reached RUNNING.
	 *
	 * Integrations may be registered before an optional module exists. In that case
	 * the action remains pending and is evaluated again when another module registers.
	 */
	public synchronized void whenModulesRunning(Set<Identifier> moduleIds, Runnable action) {
		if (moduleIds == null || moduleIds.isEmpty()) {
			throw new IllegalArgumentException("Integration module IDs must not be null or empty.");
		}
		for (Identifier moduleId : moduleIds) {
			if (moduleId == null) {
				throw new IllegalArgumentException("Integration module IDs must not be null or empty.");
			}
		}
		if (action == null) {
			throw new IllegalArgumentException("Integration action must not be null.");
		}
		pendingIntegrations.add(new Integration(Set.copyOf(moduleIds), action));
		if (!processing) runReadyIntegrations();
	}

	public synchronized State state(Identifier id) {
		Registration registration = registrations.get(id);
		return registration == null ? null : registration.state();
	}

	public synchronized boolean isRunning(Identifier id) {
		return state(id) == State.RUNNING;
	}

	public synchronized List<Identifier> modules() {
		return List.copyOf(registrations.keySet());
	}

	/** Resets one module's runtime state while keeping it initialized. */
	public synchronized void reset(Identifier id) {
		Registration registration = registrations.get(id);
		if (registration == null || registration.state() != State.RUNNING) return;
		try {
			registration.module().reset();
		} catch (RuntimeException exception) {
			logger.error("Failed to reset Madoku module {}.", id, exception);
		}
	}

	/** Resets running modules in reverse dependency/initialization order. */
	public synchronized void resetAll() {
		List<Identifier> reverseOrder = new ArrayList<>(initializationOrder);
		Collections.reverse(reverseOrder);
		for (Identifier id : reverseOrder) reset(id);
	}

	private boolean initializeModule(Identifier id, Set<Identifier> visiting) {
		Registration registration = registrations.get(id);
		if (registration == null) return false;
		if (registration.state() == State.RUNNING) return true;
		if (registration.state() == State.FAILED) {
			throw new IllegalStateException("Madoku module previously failed: " + id);
		}
		if (!visiting.add(id)) {
			registration.state(State.FAILED);
			throw new IllegalStateException("Circular Madoku module dependency involving " + id);
		}

		for (Identifier dependency : registration.module().dependencies()) {
			Registration dependencyRegistration = registrations.get(dependency);
			if (dependencyRegistration == null) {
				registration.state(State.WAITING_FOR_DEPENDENCY);
				visiting.remove(id);
				return false;
			}
			if (!initializeModule(dependency, visiting)) {
				registration.state(State.WAITING_FOR_DEPENDENCY);
				visiting.remove(id);
				return false;
			}
		}

		registration.state(State.INITIALIZING);
		try {
			registration.module().initialize();
			registration.state(State.RUNNING);
			if (!initializationOrder.contains(id)) initializationOrder.add(id);
			return true;
		} catch (RuntimeException exception) {
			registration.state(State.FAILED);
			logger.error("Failed to initialize Madoku module {}.", id, exception);
			throw exception;
		} finally {
			visiting.remove(id);
		}
	}

	private void runReadyIntegrations() {
		if (processing) return;
		for (int index = 0; index < pendingIntegrations.size();) {
			Integration integration = pendingIntegrations.get(index);
			if (!integration.moduleIds().stream().allMatch(this::isRunning)) {
				index++;
				continue;
			}
			pendingIntegrations.remove(index);
			integration.action().run();
		}
	}

	private static final class Registration {
		private final MadokuModule module;
		private State state;

		private Registration(MadokuModule module, State state) {
			this.module = module;
			this.state = state;
		}

		private MadokuModule module() { return module; }
		private State state() { return state; }
		private void state(State value) { state = value; }
	}

	private record Integration(Set<Identifier> moduleIds, Runnable action) {
	}
}
