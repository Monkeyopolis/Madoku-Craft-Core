package madoku.craft.API;

import net.fabricmc.api.ClientModInitializer;

import madoku.craft.API.system.MadokuClientTickSystem;

public class MadokuCraftAPIClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MadokuClientTickSystem.init();
	}
}
