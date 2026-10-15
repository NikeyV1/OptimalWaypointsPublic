package dev.nikey.optimalwaypoints.waypoint.io;

import dev.nikey.optimalwaypoints.OptimalWaypoints;
import dev.nikey.optimalwaypoints.waypoint.Waypoint;
import dev.nikey.optimalwaypoints.waypoint.WaypointColors;
import dev.nikey.optimalwaypoints.waypoint.WaypointKind;
import dev.nikey.optimalwaypoints.waypoint.WaypointManager;
import dev.nikey.optimalwaypoints.waypoint.WaypointSet;
import dev.nikey.optimalwaypoints.waypoint.WorldKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import net.fabricmc.loader.api.FabricLoader;

public final class XaeroImporter {
	public record Result(int imported, int skipped, int files) {
	}

	private XaeroImporter() {
	}

	public static Path minimapFolder() {
		Path game = FabricLoader.getInstance().getGameDir();
		Path modern = game.resolve("xaero").resolve("minimap");
		return Files.isDirectory(modern) ? modern : game.resolve("XaeroWaypoints");
	}

	public static List<String> containers() {
		Path root = minimapFolder();
		if (!Files.isDirectory(root)) {
			return List.of();
		}
		try (Stream<Path> stream = Files.list(root)) {
			return stream.filter(Files::isDirectory).map(p -> p.getFileName().toString()).sorted().toList();
		} catch (IOException e) {
			return List.of();
		}
	}

	public static String guessContainer(WorldKey key) {
		if (key == null) {
			return null;
		}
		if (key.singleplayer()) {
			return key.displayName();
		}
		if (key.serverAddress() == null) {
			return null;
		}
		String ip = key.serverAddress();
		int portDivider = ip.indexOf(':') != ip.lastIndexOf(':') ? ip.lastIndexOf("]:") + 1 : ip.indexOf(':');
		if (portDivider > 0) {
			ip = ip.substring(0, portDivider);
		}
		while (ip.endsWith(".")) {
			ip = ip.substring(0, ip.length() - 1);
		}
		ip = ip.replace("[", "").replace("]", "").replace(":", ".").replace("_", "%us%").replace("/", "%fs%").replace("\\", "%bs%").trim();
		String candidate = "Multiplayer_" + ip;
		for (String existing : containers()) {
			if (existing.equalsIgnoreCase(candidate)) {
				return existing;
			}
		}
		return candidate;
	}

	public static Result importContainer(String container, WaypointManager manager) {
		Path root = minimapFolder().resolve(container).normalize();
		if (!root.startsWith(minimapFolder().normalize()) || !Files.isDirectory(root)) {
			return new Result(0, 0, 0);
		}
		int imported = 0;
		int skipped = 0;
		int files = 0;
		try (Stream<Path> dims = Files.list(root)) {
			for (Path dimFolder : dims.filter(Files::isDirectory).toList()) {
				String dimension = dimensionFromFolder(dimFolder.getFileName().toString());
				if (dimension == null) {
					continue;
				}
				try (Stream<Path> txts = Files.list(dimFolder)) {
					for (Path file : txts.filter(p -> p.getFileName().toString().endsWith(".txt")).toList()) {
						files++;
						for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
							Parsed parsed = parseLine(line, dimension);
							if (parsed == null) {
								continue;
							}
							if (exists(manager, parsed.waypoint)) {
								skipped++;
								continue;
							}
							manager.addToSet(parsed.set, parsed.waypoint);
							imported++;
						}
					}
				}
			}
		} catch (IOException e) {
			OptimalWaypoints.LOGGER.error("Xaero import from {} failed", root, e);
		}
		manager.saveNow();
		return new Result(imported, skipped, files);
	}

	record Parsed(String set, Waypoint waypoint) {
	}

	static Parsed parseLine(String line, String dimension) {
		if (!line.startsWith("waypoint:")) {
			return null;
		}
		String[] args = line.split(":");
		if (args.length < 10) {
			return null;
		}
		try {
			String name = args[1].replace("§§", ":");
			String initials = args[2].replace("§§", ":");
			int x = Integer.parseInt(args[3]);
			int y = "~".equals(args[4]) ? 64 : Integer.parseInt(args[4]);
			int z = Integer.parseInt(args[5]);
			int color = WaypointColors.fromXaeroIndex(Integer.parseInt(args[6]));
			int type = Integer.parseInt(args[8]);
			Waypoint waypoint = new Waypoint(name, x, y, z, dimension, color, type == 0 ? WaypointKind.STANDARD : WaypointKind.OLD_DEATH);
			waypoint.setInitials(initials);
			waypoint.setEnabled(!"true".equals(args[7]));
			if (args.length > 12) {
				waypoint.setGlobal("true".equals(args[12]) || args[12].contains("GLOBAL"));
			}
			if (args.length > 13) {
				waypoint.setDeleteOnArrival("true".equals(args[13]));
			}
			String set = args[9].startsWith("gui.xaero_default") ? "default" : args[9];
			return new Parsed(set, waypoint);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	static String dimensionFromFolder(String folder) {
		return switch (folder) {
			case "dim%0" -> "minecraft:overworld";
			case "dim%-1" -> "minecraft:the_nether";
			case "dim%1" -> "minecraft:the_end";
			default -> {
				if (!folder.startsWith("dim%") || !folder.contains("$")) {
					yield null;
				}
				String body = folder.substring(4);
				int split = body.indexOf('$');
				yield body.substring(0, split) + ":" + body.substring(split + 1).replace('%', '/');
			}
		};
	}

	private static boolean exists(WaypointManager manager, Waypoint candidate) {
		for (WaypointSet set : manager.world().sets()) {
			for (Waypoint existing : set.waypoints()) {
				if (existing.x() == candidate.x() && existing.y() == candidate.y() && existing.z() == candidate.z()
					&& existing.dimension().equals(candidate.dimension()) && existing.name().equals(candidate.name())) {
					return true;
				}
			}
		}
		return false;
	}
}
