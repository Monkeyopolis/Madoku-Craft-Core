package madoku.craft;

import madoku.craft.core.season.SeasonPayloadManager;
import madoku.craft.core.sync.MadokuSyncManager;
import madoku.craft.core.sync.SyncConfigManager;
import madoku.craft.core.sync.SyncPayloadManager;
import madoku.craft.core.time.TimePayloadManager;
import madoku.craft.season.ClientSeasonalPrecipitationState;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class MadokuCraftCoreClient implements ClientModInitializer {
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
			// Core registers the shared time payload; no Core-owned HUD is required.
		});

		ClientPlayNetworking.registerGlobalReceiver(SyncPayloadManager.TYPE, (payload, context) ->
			context.client().execute(() ->
				SyncConfigManager.applyClientSnapshot(payload.configId(), payload.snapshot()))
		);

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			SyncConfigManager.resetClientSynchronizedState();
			ClientSeasonalPrecipitationState.clear();
		});
	}
}
