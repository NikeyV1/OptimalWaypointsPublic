package dev.nikey.optimalwaypoints.waypoint.io;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import dev.nikey.optimalwaypoints.OptimalWaypoints;
import dev.nikey.optimalwaypoints.waypoint.WaypointWorld;
import dev.nikey.optimalwaypoints.waypoint.WorldKey;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.fabricmc.loader.api.FabricLoader;

public final class WaypointStorage {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	private WaypointStorage() {
	}

	public static Path root() {
		return FabricLoader.getInstance().getGameDir().resolve(OptimalWaypoints.MOD_ID);
	}

	public static Path file(WorldKey key) {
		return root().resolve("worlds").resolve(key.id() + ".json");
	}

	public static WaypointWorld load(WorldKey key) {
		Path path = file(key);
		if (!Files.exists(path)) {
			return WaypointWorld.createDefault();
		}
		try {
			WaypointWorld world = GSON.fromJson(Files.readString(path), WaypointWorld.class);
			if (world == null) {
				return WaypointWorld.createDefault();
			}
			world.validate();
			return world;
		} catch (IOException | JsonParseException e) {
			OptimalWaypoints.LOGGER.error("Failed to load waypoints from {}, keeping a backup", path, e);
			backupBroken(path);
			return WaypointWorld.createDefault();
		}
	}

	public static void save(WorldKey key, WaypointWorld world) {
		Path path = file(key);
		try {
			AtomicFiles.write(path, GSON.toJson(world));
		} catch (IOException e) {
			OptimalWaypoints.LOGGER.error("Failed to save waypoints to {}", path, e);
		}
	}

	private static void backupBroken(Path path) {
		try {
			Files.copy(path, path.resolveSibling(path.getFileName() + ".broken-" + System.currentTimeMillis()), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException ignored) {
		}
	}
}
