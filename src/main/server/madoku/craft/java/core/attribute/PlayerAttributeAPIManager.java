package madoku.craft.java.core.attribute;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.state.BlockState;

/** Central registry and resolver for additive Madoku player-stat contributions. */
public final class PlayerAttributeAPIManager {
	private static final double EPSILON = 1.0E-9D;
	private static final Map<String, PlayerAttributeProvider> PROVIDERS = new ConcurrentHashMap<>();
	private static final Map<PlayerAttributeStat, Set<Identifier>> KNOWN_MODIFIERS = new EnumMap<>(PlayerAttributeStat.class);

	static {
		for (PlayerAttributeStat stat : PlayerAttributeStat.values()) {
			Set<Identifier> modifiers = ConcurrentHashMap.newKeySet();
			modifiers.add(modifierIdForResolvedProviders(stat));
			KNOWN_MODIFIERS.put(stat, modifiers);
		}
	}

	private PlayerAttributeAPIManager() {
	}

	public static void registerProvider(PlayerAttributeProvider candidate) {
		if (candidate == null || candidate.id() == null || candidate.id().isBlank()) {
			throw new IllegalArgumentException("Player-attribute provider and id must not be null or blank.");
		}
		PROVIDERS.put(candidate.id(), candidate);
	}

	public static void unregisterProvider(String id) {
		if (id != null && !id.isBlank()) {
			PROVIDERS.remove(id);
		}
	}

	/** Returns the combined contribution from all registered providers for one stat. */
	public static double resolveBonus(ServerPlayer player, PlayerAttributeStat stat) {
		return resolveBonus((Player) player, stat, null);
	}

	public static double resolveBonus(Player player, PlayerAttributeStat stat) {
		return resolveBonus(player, stat, null);
	}

	public static double resolveBonus(Player player, PlayerAttributeStat stat, BlockState state) {
		if (player == null || stat == null) return 0.0D;

		double total = 0.0D;
		for (PlayerAttributeProvider provider : PROVIDERS.values()) {
			double bonus = provider.bonus(player, stat, state);
			if (Double.isFinite(bonus)) total += bonus;
		}
		return Double.isFinite(total) ? total : 0.0D;
	}

	/**
	 * Rebuilds all Madoku-owned vanilla attribute modifiers for a player. Vanilla
	 * equipment and effect modifiers are left untouched.
	 */
	public static void apply(ServerPlayer player) {
		if (player == null) return;

		for (PlayerAttributeStat stat : PlayerAttributeStat.values()) {
			AttributeInstance attribute = attribute(player, stat);
			if (attribute == null) continue;
			for (Identifier modifierId : KNOWN_MODIFIERS.get(stat)) {
				attribute.removeModifier(modifierId);
			}

			double amount = resolveBonus(player, stat);
			if (Math.abs(amount) <= EPSILON) continue;
			attribute.addOrUpdateTransientModifier(new AttributeModifier(
				modifierIdForResolvedProviders(stat),
				amount,
				AttributeModifier.Operation.ADD_VALUE
			));
		}
	}

	private static AttributeInstance attribute(ServerPlayer player, PlayerAttributeStat stat) {
		return switch (stat) {
			case HEALTH -> player.getAttribute(Attributes.MAX_HEALTH);
			case STRENGTH -> player.getAttribute(Attributes.ATTACK_DAMAGE);
			case LUCK -> player.getAttribute(Attributes.LUCK);
			case MOVEMENT_SPEED -> player.getAttribute(Attributes.MOVEMENT_SPEED);
			case DEFENSE, HUNGER, OXYGEN, MINING -> null;
		};
	}

	private static Identifier modifierIdForResolvedProviders(PlayerAttributeStat stat) {
		return Identifier.fromNamespaceAndPath("madoku-craft", "player_attribute/combined_" + stat.name().toLowerCase());
	}

}
