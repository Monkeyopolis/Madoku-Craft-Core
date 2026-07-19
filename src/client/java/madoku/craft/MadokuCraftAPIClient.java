package madoku.craft;

import madoku.craft.api.season.SeasonPayloadManager;
import madoku.craft.api.sync.MadokuSyncManager;
import madoku.craft.api.time.TimePayloadManager;
import madoku.craft.season.ClientSeasonalPrecipitationState;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class MadokuCraftAPIClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MadokuSyncManager.initializeClient();

		ClientTickEvents.END_CLIENT_TICK.register(client ->
			ClientSeasonalPrecipitationState.refresh(client.level));

		ClientPlayNetworking.registerGlobalReceiver(SeasonPayloadManager.TYPE, (payload, context) ->
			context.client().execute(() -> {
				ClientSeasonalPrecipitationState.update(
					payload.season(),
					payload.temperatureOffset(),
					payload.humidityOffset(),
					payload.weatherCondition(),
					payload.seasonDay(),
					payload.seasonLengthDays());
				ClientSeasonalPrecipitationState.refresh(context.client().level);
			})
		);

		ClientPlayNetworking.registerGlobalReceiver(TimePayloadManager.TYPE, (payload, context) -> {
			// The API registers the shared time payload for API consumers; no API-owned HUD is required.
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
			ClientSeasonalPrecipitationState.clear());
	}
}
