package madoku.craft.java.core;

import madoku.craft.java.core.season.MadokuSeasonClient;
import madoku.craft.java.core.sync.MadokuSyncClient;
import madoku.craft.java.core.sync.SyncAPIManager;
import madoku.craft.java.core.menu.MadokuMenuClient;
import madoku.craft.java.core.module.MadokuClientRuntime;
import madoku.craft.java.core.module.MadokuModule;
import madoku.craft.java.core.module.MadokuModuleIds;
import madoku.craft.java.core.ui.UiLayoutAPIManager;
import net.fabricmc.api.ClientModInitializer;

/** Fabric client entrypoint for the standalone Core jar. */
public final class MadokuCoreClientInitializer implements ClientModInitializer, MadokuModule {
	@Override
	public net.minecraft.resources.Identifier id() { return MadokuModuleIds.CORE; }

	@Override
	public void onInitializeClient() {
		MadokuClientRuntime.initialize(this);
	}

	@Override
	public void initialize() {
		SyncAPIManager.initializeClient();
		MadokuSyncClient.initialize();
		MadokuSeasonClient.initialize();
		UiLayoutAPIManager.initialize();
		MadokuMenuClient.initialize();
	}
}
