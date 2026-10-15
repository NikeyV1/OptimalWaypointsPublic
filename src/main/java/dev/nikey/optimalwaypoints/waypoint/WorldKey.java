package dev.nikey.optimalwaypoints.waypoint;

import java.nio.file.Path;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.level.storage.LevelResource;

public record WorldKey(String id, String displayName, boolean singleplayer, String serverAddress) {
	public static WorldKey current(Minecraft mc) {
		IntegratedServer local = mc.getSingleplayerServer();
		if (local != null) {
			String folder = worldFolder(local);
			return new WorldKey("sp_" + sanitize(folder), folder, true, null);
		}
		ServerData server = mc.getCurrentServer();
		if (server != null) {
			String address = normalizeAddress(server.ip);
			String prefix = server.isRealm() ? "realms_" : "mp_";
			String id = server.isRealm() ? prefix + sanitize(server.name) : prefix + sanitize(address);
			return new WorldKey(id, server.isRealm() ? server.name : address, false, address);
		}
		return new WorldKey("unknown", "unknown", false, null);
	}

	private static String worldFolder(IntegratedServer server) {
		Path root = server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
		Path name = root.getFileName();
		return name == null ? "world" : name.toString();
	}

	private static String normalizeAddress(String ip) {
		if (ip == null || ip.isBlank()) {
			return "unknown";
		}
		String address = ip.trim().toLowerCase(Locale.ROOT);
		if (address.endsWith(":25565")) {
			address = address.substring(0, address.length() - ":25565".length());
		}
		while (address.endsWith(".")) {
			address = address.substring(0, address.length() - 1);
		}
		return address;
	}

	public static String sanitize(String raw) {
		StringBuilder out = new StringBuilder();
		for (char c : raw.toCharArray()) {
			if (Character.isLetterOrDigit(c) || c == '.' || c == '-' || c == '_') {
				out.append(c);
			} else {
				out.append('_');
			}
		}
		return out.isEmpty() ? "_" : out.toString();
	}
}
