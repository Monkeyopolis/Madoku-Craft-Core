package madoku.craft.java.core.upgrade;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Access point for optional feature behavior used by upgrade menus. */
public final class UpgradeCostFeatureAPIManager {
	private static final List<UpgradeCostFeatureAdapter> adapters = new CopyOnWriteArrayList<>();

	private UpgradeCostFeatureAPIManager() {
	}

	public static void registerAdapter(UpgradeCostFeatureAdapter candidate) {
		if (candidate == null) {
			throw new IllegalArgumentException("Upgrade-cost feature adapter must not be null.");
		}
		if (!adapters.contains(candidate)) {
			adapters.add(candidate);
		}
	}

	public static void unregisterAdapter() {
		adapters.clear();
	}

	public static int adjustExperienceBottleCost(int cost) {
		int adjusted = Math.max(1, cost);
		for (UpgradeCostFeatureAdapter adapter : adapters) {
			adjusted = Math.max(1, adapter.adjustExperienceBottleCost(adjusted));
		}
		return adjusted;
	}
}
