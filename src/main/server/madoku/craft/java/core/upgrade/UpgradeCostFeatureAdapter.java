package madoku.craft.java.core.upgrade;

/** Optional feature behavior used when upgrade menus resolve experience-bottle costs. */
public interface UpgradeCostFeatureAdapter {
	default int adjustExperienceBottleCost(int cost) {
		return cost;
	}
}
