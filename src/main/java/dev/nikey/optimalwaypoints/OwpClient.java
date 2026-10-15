package dev.nikey.optimalwaypoints;

import dev.nikey.optimalwaypoints.api.ClientModule;
import dev.nikey.optimalwaypoints.api.ModuleRegistry;
import dev.nikey.optimalwaypoints.config.ConfigManager;
import dev.nikey.optimalwaypoints.config.OwpConfig;
import dev.nikey.optimalwaypoints.config.option.OptionRegistry;
import dev.nikey.optimalwaypoints.policy.ClientPolicy;
import dev.nikey.optimalwaypoints.waypoint.Dimensions;
import dev.nikey.optimalwaypoints.waypoint.WaypointManager;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;

public final class OwpClient {
	private static OwpClient instance;

	private final ConfigManager configManager = new ConfigManager();
	private final WaypointManager waypoints = new WaypointManager();
	private final ClientPolicy policy = new ClientPolicy();
	private final ModuleRegistry modules = new ModuleRegistry();
	private final OptionRegistry options = new OptionRegistry();

	OwpClient() {
		instance = this;
	}

	public static OwpClient get() {
		return instance;
	}

	public OwpConfig config() {
		return configManager.get();
	}

	public ConfigManager configManager() {
		return configManager;
	}

	public WaypointManager waypoints() {
		return waypoints;
	}

	public ClientPolicy policy() {
		return policy;
	}

	public ModuleRegistry modules() {
		return modules;
	}

	public OptionRegistry options() {
		return options;
	}

	public boolean usable() {
		Minecraft mc = Minecraft.getInstance();
		return mc.player != null && mc.level != null && waypoints.isLoaded() && policy.modAllowed();
	}

	public String currentDimension() {
		Minecraft mc = Minecraft.getInstance();
		return mc.level == null ? Dimensions.OVERWORLD : mc.level.dimension().identifier().toString();
	}

	void forEachModule(Consumer<ClientModule> action) {
		for (ClientModule module : modules.all()) {
			try {
				action.accept(module);
			} catch (RuntimeException e) {
				OptimalWaypoints.LOGGER.error("Optimal Waypoints module '{}' failed", module.id(), e);
			}
		}
	}
}
