package dev.nikey.optimalwaypoints;

import dev.nikey.optimalwaypoints.api.OptimalWaypointsAddon;
import dev.nikey.optimalwaypoints.command.OwpClientCommands;
import dev.nikey.optimalwaypoints.config.option.CoreOptions;
import dev.nikey.optimalwaypoints.feature.ArrivalModule;
import dev.nikey.optimalwaypoints.feature.ChatModule;
import dev.nikey.optimalwaypoints.feature.DeathpointModule;
import dev.nikey.optimalwaypoints.feature.InputModule;
import dev.nikey.optimalwaypoints.feature.RenderModule;
import dev.nikey.optimalwaypoints.input.OwpKeybinds;
import dev.nikey.optimalwaypoints.net.ClientNetworking;
import dev.nikey.optimalwaypoints.screen.ScreenOpener;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OptimalWaypoints implements ClientModInitializer {
	public static final String MOD_ID = "optimalwaypoints";
	public static final String ADDON_ENTRYPOINT = "optimalwaypoints";
	public static final Logger LOGGER = LoggerFactory.getLogger("OptimalWaypoints");

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	public static String version() {
		return FabricLoader.getInstance().getModContainer(MOD_ID)
			.map(c -> c.getMetadata().getVersion().getFriendlyString())
			.orElse("unknown");
	}

	@Override
	public void onInitializeClient() {
		OwpClient client = new OwpClient();
		client.configManager().load();

		client.modules().register(new RenderModule());
		client.modules().register(new InputModule());
		client.modules().register(new DeathpointModule());
		client.modules().register(new ArrivalModule());
		client.modules().register(new ChatModule());
		for (EntrypointContainer<OptimalWaypointsAddon> addon : FabricLoader.getInstance().getEntrypointContainers(ADDON_ENTRYPOINT, OptimalWaypointsAddon.class)) {
			try {
				addon.getEntrypoint().registerModules(client.modules());
			} catch (RuntimeException e) {
				LOGGER.error("Addon from {} failed to register", addon.getProvider().getMetadata().getId(), e);
			}
		}

		CoreOptions.register(client.options());
		client.forEachModule(module -> module.registerOptions(client.options()));
		client.forEachModule(module -> module.init(client));

		OwpKeybinds.register();
		ClientNetworking.register(client);
		OwpClientCommands.register();

		ClientPlayConnectionEvents.INIT.register((handler, mc) -> client.policy().reset());
		ClientPlayConnectionEvents.JOIN.register((handler, sender, mc) -> {
			client.waypoints().load(mc);
			client.forEachModule(module -> module.onJoin(client, mc));
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) -> {
			client.forEachModule(module -> module.onLeave(client, mc));
			client.waypoints().unload();
			client.policy().reset();
		});
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			ScreenOpener.tick(mc);
			client.policy().tick();
			if (mc.player == null) {
				return;
			}
			client.forEachModule(module -> module.tick(client, mc));
			client.waypoints().tick();
		});
		ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> {
			client.waypoints().unload();
			client.configManager().save();
		});
	}
}
