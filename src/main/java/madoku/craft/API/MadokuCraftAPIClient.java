package madoku.craft.API;

import madoku.craft.clock.MadokuTicks;
import madoku.craft.debug.MadokuDebug;
import madoku.craft.network.WorldSeasonPayload;
import madoku.craft.season.MadokuSeason;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class MadokuCraftAPIClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(WorldSeasonPayload.TYPE, (payload, context) -> {
			MadokuSeason.setSyncedClientSeason(payload.season());
			if (MadokuDebug.shouldEmit(MadokuDebug.Domain.SEASON, "season.sync_receive")) {
				MadokuDebug.event("season.sync_receive", MadokuDebug.Domain.SEASON)
					.side(MadokuDebug.Side.CLIENT)
					.tick(MadokuTicks.getGameplayTicks())
					.subject("world_season")
					.field("season", payload.season())
					.log();
			}
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> MadokuSeason.clearSyncedClientSeason());
	}
}
