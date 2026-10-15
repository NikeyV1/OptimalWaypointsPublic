package dev.nikey.optimalwaypoints.paper;

import dev.nikey.optimalwaypoints.protocol.Feature;
import dev.nikey.optimalwaypoints.protocol.ServerPolicy;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

final class PolicySettings {
	private final ServerPolicy global;
	private final Map<String, ServerPolicy> worlds;
	private final boolean notifyStaff;
	private final boolean logUsers;

	private PolicySettings(ServerPolicy global, Map<String, ServerPolicy> worlds, boolean notifyStaff, boolean logUsers) {
		this.global = global;
		this.worlds = worlds;
		this.notifyStaff = notifyStaff;
		this.logUsers = logUsers;
	}

	static PolicySettings load(FileConfiguration config) {
		ServerPolicy global = read(config, ServerPolicy.ALLOW_ALL);
		Map<String, ServerPolicy> worlds = new HashMap<>();
		ConfigurationSection section = config.getConfigurationSection("worlds");
		if (section != null) {
			for (String world : section.getKeys(false)) {
				ConfigurationSection override = section.getConfigurationSection(world);
				if (override != null) {
					worlds.put(world, read(override, global));
				}
			}
		}
		return new PolicySettings(global, worlds, config.getBoolean("notify-staff", true), config.getBoolean("log-users", true));
	}

	private static ServerPolicy read(ConfigurationSection section, ServerPolicy base) {
		ServerPolicy policy = base;
		for (Feature feature : Feature.values()) {
			String path = "features." + feature.key();
			if (section.contains(path)) {
				policy = policy.with(feature, section.getBoolean(path));
			}
		}
		if (section.contains("max-distance")) {
			policy = policy.withMaxRenderDistance(section.getInt("max-distance"));
		}
		if (section.contains("message")) {
			policy = policy.withMessage(section.getString("message", ""));
		}
		return policy;
	}

	static void write(FileConfiguration config, ServerPolicy policy) {
		for (Feature feature : Feature.values()) {
			config.set("features." + feature.key(), !policy.isDisabled(feature));
		}
		config.set("max-distance", policy.maxRenderDistance());
		config.set("message", policy.message());
	}

	ServerPolicy global() {
		return global;
	}

	ServerPolicy forWorld(String world) {
		return worlds.getOrDefault(world, global);
	}

	boolean notifyStaff() {
		return notifyStaff;
	}

	boolean logUsers() {
		return logUsers;
	}
}
