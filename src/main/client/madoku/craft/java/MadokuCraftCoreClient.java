package madoku.craft.java;

import madoku.craft.java.core.season.SeasonPayloadAPIManager;
import madoku.craft.java.core.sync.SyncAPIManager;
import madoku.craft.java.core.sync.SyncConfigAPIManager;
import madoku.craft.java.core.sync.SyncPayloadAPIManager;
import madoku.craft.java.core.time.TimePayloadAPIManager;
import madoku.craft.java.season.ClientSeasonalPrecipitationState;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class MadokuCraftCoreClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		SyncAPIManager.initializeClient();

		ClientTickEvents.END_CLIENT_TICK.register(client ->
			ClientSeasonalPrecipitationState.refresh(client.level));

		ClientPlayNetworking.registerGlobalReceiver(SeasonPayloadAPIManager.TYPE, (payload, context) ->
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

		ClientPlayNetworking.registerGlobalReceiver(TimePayloadAPIManager.TYPE, (payload, context) -> {
			// Core registers the shared time payload; no Core-owned HUD is required.
		});

		ClientPlayNetworking.registerGlobalReceiver(SyncPayloadAPIManager.TYPE, (payload, context) ->
			context.client().execute(() ->
				SyncConfigAPIManager.applyClientSnapshot(payload.configId(), payload.snapshot()))
		);

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			SyncConfigAPIManager.resetClientSynchronizedState();
			ClientSeasonalPrecipitationState.clear();
		});
	}
}
