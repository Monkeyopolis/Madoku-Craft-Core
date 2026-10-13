package madoku.craft.java.core.season;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.biome.Biome;

/** Public client contract for synchronized seasonal and weather state. */
public final class SeasonClientAPIManager {
	private SeasonClientAPIManager() { }

	public static void update(
		String season,
		double temperatureOffset,
		double humidityOffset,
		String weatherCondition,
		int seasonDay,
		int seasonLengthDays
	) {
		ClientSeasonalPrecipitationState.update(
			season,
			temperatureOffset,
			humidityOffset,
			weatherCondition,
			seasonDay,
			seasonLengthDays
		);
	}

	public static void refresh(ClientLevel clientLevel) { ClientSeasonalPrecipitationState.refresh(clientLevel); }
	public static String getSeason() { return ClientSeasonalPrecipitationState.getSeason(); }
	public static int getSeasonDay() { return ClientSeasonalPrecipitationState.getSeasonDay(); }
	public static int getSeasonLengthDays() { return ClientSeasonalPrecipitationState.getSeasonLengthDays(); }
	public static double getSeasonDayValue() { return ClientSeasonalPrecipitationState.getSeasonDayValue(); }
	public static double getSeasonProgress() { return ClientSeasonalPrecipitationState.getSeasonProgress(); }
	public static boolean isSynchronized() { return ClientSeasonalPrecipitationState.isSynchronized(); }
	public static boolean isPrecipitating() { return ClientSeasonalPrecipitationState.isPrecipitating(); }
	public static Biome.Precipitation resolve(Biome biome) { return ClientSeasonalPrecipitationState.resolve(biome); }
	public static SeasonBiomeClimateAPIManager.Climate resolveClimate(Biome biome) { return ClientSeasonalPrecipitationState.resolveClimate(biome); }
	public static double resolveSeasonalTemperature(Biome biome) { return ClientSeasonalPrecipitationState.resolveSeasonalTemperature(biome); }
	public static float resolveRainLevel(float vanillaLevel) { return ClientSeasonalPrecipitationState.resolveRainLevel(vanillaLevel); }
	public static float resolveThunderLevel(float vanillaLevel) { return ClientSeasonalPrecipitationState.resolveThunderLevel(vanillaLevel); }
	public static void clear() { ClientSeasonalPrecipitationState.clear(); }
}
