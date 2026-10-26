package madoku.craft.java.core.status;

import madoku.craft.java.core.time.TimeAPIManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Shared, short-lived stun state used by combat and pet abilities. */
public final class EntityStunManager {
	private static final Map<UUID, StunState> ACTIVE_STUNS = new ConcurrentHashMap<>();

	private EntityStunManager() {
	}

	public static void applyStun(LivingEntity entity, long durationTicks) {
		applyStun(entity, durationTicks, false);
	}

	public static void applyStun(LivingEntity entity, long durationTicks, boolean blocksPlayerActions) {
		if (entity == null || entity.isRemoved() || !entity.isAlive() || durationTicks <= 0L) {
			return;
		}

		long now = currentTick();
		long durationEnd = safeAdd(now, durationTicks);
		ACTIVE_STUNS.compute(entity.getUUID(), (ignored, existing) -> {
			long until = existing == null || now >= existing.untilTick()
				? durationEnd
				: safeAdd(existing.untilTick(), durationTicks);
			boolean blockActions = blocksPlayerActions || (existing != null && existing.blocksPlayerActions());
			return new StunState(until, blockActions);
		});
		stopMovement(entity);
	}

	public static boolean isStunned(Entity entity) {
		StunState state = activeState(entity);
		return state != null;
	}

	public static boolean blocksPlayerActions(Entity entity) {
		StunState state = activeState(entity);
		return state != null && state.blocksPlayerActions();
	}

	public static void clear(Entity entity) {
		if (entity != null) {
			ACTIVE_STUNS.remove(entity.getUUID());
		}
	}

	/** Removes expired entries only; callers do not scan entities. */
	public static void onServerTick() {
		if (ACTIVE_STUNS.isEmpty()) {
			return;
		}
		long now = currentTick();
		ACTIVE_STUNS.entrySet().removeIf(entry -> entry.getValue() == null || now >= entry.getValue().untilTick());
	}

	public static void reset() {
		ACTIVE_STUNS.clear();
	}

	private static StunState activeState(Entity entity) {
		if (ACTIVE_STUNS.isEmpty()) {
			return null;
		}
		if (!(entity instanceof LivingEntity livingEntity) || entity.isRemoved() || !entity.isAlive()) {
			if (entity != null) {
				ACTIVE_STUNS.remove(entity.getUUID());
			}
			return null;
		}
		StunState state = ACTIVE_STUNS.get(entity.getUUID());
		if (state == null || currentTick() >= state.untilTick()) {
			if (state != null) {
				ACTIVE_STUNS.remove(livingEntity.getUUID(), state);
			}
			return null;
		}
		return state;
	}

	private static void stopMovement(LivingEntity entity) {
		Vec3 movement = entity.getDeltaMovement();
		entity.setDeltaMovement(new Vec3(0.0D, movement.y, 0.0D));
		if (entity instanceof Mob mob) {
			mob.getNavigation().stop();
		}
	}

	private static long currentTick() {
		return Math.max(0L, TimeAPIManager.getGameplayTicks());
	}

	private static long safeAdd(long base, long amount) {
		if (amount <= 0L || Long.MAX_VALUE - base < amount) {
			return Long.MAX_VALUE;
		}
		return base + amount;
	}

	private record StunState(long untilTick, boolean blocksPlayerActions) {
	}
}
