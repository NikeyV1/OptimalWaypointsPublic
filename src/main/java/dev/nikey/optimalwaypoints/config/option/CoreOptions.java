package dev.nikey.optimalwaypoints.config.option;

import dev.nikey.optimalwaypoints.OwpClient;
import dev.nikey.optimalwaypoints.config.DistanceMode;
import dev.nikey.optimalwaypoints.config.OwpConfig;
import dev.nikey.optimalwaypoints.protocol.Feature;
import net.minecraft.network.chat.Component;

public final class CoreOptions {
	private CoreOptions() {
	}

	private static OwpConfig c() {
		return OwpClient.get().config();
	}

	public static void register(OptionRegistry registry) {
		OwpConfig d = new OwpConfig();

		registry.category("waypoints")
			.add(new BooleanOption("show_waypoints", Feature.IN_WORLD_WAYPOINTS, d.showWaypoints, () -> c().showWaypoints, v -> c().showWaypoints = v))
			.add(new IntOption("scale", null, 50, 300, 25, d.scale, () -> c().scale, v -> c().scale = v, IntOption.percent()))
			.add(new IntOption("opacity", null, 10, 100, 5, d.opacity, () -> c().opacity, v -> c().opacity = v, IntOption.percent()))
			.add(new BooleanOption("grow_on_look", null, d.growOnLook, () -> c().growOnLook, v -> c().growOnLook = v))
			.add(new EnumOption<>("distance", Feature.DISTANCE_DISPLAY, DistanceMode.class, d.distance, () -> c().distance, v -> c().distance = v,
				mode -> Component.translatable(mode.translationKey())))
			.add(new BooleanOption("names_with_distance", null, d.namesWithDistance, () -> c().namesWithDistance, v -> c().namesWithDistance = v))
			.add(new IntOption("max_distance", null, 0, 10000, 250, d.maxDistance, () -> c().maxDistance, v -> c().maxDistance = v, IntOption.blocksOrUnlimited()))
			.add(new BooleanOption("cross_dimension", Feature.CROSS_DIMENSION, d.crossDimension, () -> c().crossDimension, v -> c().crossDimension = v));

		registry.category("blocks")
			.add(new BooleanOption("through_walls", Feature.THROUGH_WALLS, d.throughWalls, () -> c().throughWalls, v -> c().throughWalls = v))
			.add(new IntOption("fill_opacity", Feature.BLOCK_HIGHLIGHTS, 0, 100, 5, d.fillOpacity, () -> c().fillOpacity, v -> c().fillOpacity = v, IntOption.percent()))
			.add(new IntOption("outline_width", Feature.BLOCK_HIGHLIGHTS, 5, 60, 5, d.outlineWidth, () -> c().outlineWidth, v -> c().outlineWidth = v, IntOption.tenths()));

		registry.category("other")
			.add(new BooleanOption("deathpoints", Feature.DEATHPOINTS, d.deathpoints, () -> c().deathpoints, v -> c().deathpoints = v))
			.add(new BooleanOption("keep_old_deathpoints", Feature.DEATHPOINTS, d.keepOldDeathpoints, () -> c().keepOldDeathpoints, v -> c().keepOldDeathpoints = v))
			.add(new BooleanOption("delete_reached_deathpoints", Feature.DEATHPOINTS, d.deleteReachedDeathpoints, () -> c().deleteReachedDeathpoints, v -> c().deleteReachedDeathpoints = v))
			.add(new BooleanOption("chat_coordinates", Feature.CHAT_COORDINATES, d.chatCoordinates, () -> c().chatCoordinates, v -> c().chatCoordinates = v));
	}
}
