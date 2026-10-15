package dev.nikey.optimalwaypoints.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import dev.nikey.optimalwaypoints.OptimalWaypoints;
import dev.nikey.optimalwaypoints.waypoint.io.AtomicFiles;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

public final class ConfigManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private OwpConfig config = new OwpConfig();

	public OwpConfig get() {
		return config;
	}

	public Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve(OptimalWaypoints.MOD_ID + "-client.json");
	}

	public void load() {
		Path path = path();
		if (Files.exists(path)) {
			try {
				OwpConfig loaded = GSON.fromJson(Files.readString(path), OwpConfig.class);
				if (loaded != null) {
					config = loaded;
				}
			} catch (IOException | JsonParseException e) {
				OptimalWaypoints.LOGGER.error("Failed to read client config, using defaults", e);
			}
		}
		save();
	}

	public void reset() {
		config = new OwpConfig();
		save();
	}

	public void save() {
		Path path = path();
		try {
			AtomicFiles.write(path, GSON.toJson(config));
		} catch (IOException e) {
			OptimalWaypoints.LOGGER.error("Failed to save client config", e);
		}
	}
}
