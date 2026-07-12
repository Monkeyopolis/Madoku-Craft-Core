package madoku.craft;

import madoku.craft.network.WorldSeasonPayload;
import madoku.craft.season.ClientSeasonalPrecipitationState;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class MadokuCraftAPIClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientTickEvents.END_CLIENT_TICK.register(client ->
			ClientSeasonalPrecipitationState.refresh(client.level));

		ClientPlayNetworking.registerGlobalReceiver(WorldSeasonPayload.TYPE, (payload, context) ->
			context.client().execute(() -> {
				ClientSeasonalPrecipitationState.update(
					payload.season(),
					payload.temperatureOffset(),
					payload.humidityOffset());
				ClientSeasonalPrecipitationState.refresh(context.client().level);
			})
		);

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
			ClientSeasonalPrecipitationState.clear());
	}
}
