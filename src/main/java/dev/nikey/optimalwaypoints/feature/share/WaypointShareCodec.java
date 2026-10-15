package dev.nikey.optimalwaypoints.feature.share;

import dev.nikey.optimalwaypoints.waypoint.Waypoint;
import dev.nikey.optimalwaypoints.waypoint.WaypointColors;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class WaypointShareCodec {
	public static final String PREFIX = "owp-waypoint:";
	private static final String COLON = "^col^";
	private static final Pattern TOKEN = Pattern.compile(
		"owp-waypoint:1:([^:\\n]*):([^:\\n]*):(-?\\d+):(-?\\d+):(-?\\d+):([0-9A-Fa-f]{6}):(\\S*)");
	private static final Pattern XAERO_TOKEN = Pattern.compile(
		"xaero[-_]waypoint:([^:\\n]*):([^:\\n]*):(-?\\d+):(~|-?\\d+):(-?\\d+):(\\d+):(?:true|false):-?\\d+(?::(\\S+))?");
	private static final Pattern FORMATTING = Pattern.compile("§.");

	public record Match(SharedWaypoint waypoint, int start, int end, String token) {
	}

	private WaypointShareCodec() {
	}

	public static String encode(Waypoint waypoint) {
		return PREFIX + "1:" + escape(waypoint.name()) + ":" + escape(waypoint.initials()) + ":" + waypoint.x() + ":" + waypoint.y() + ":" + waypoint.z()
			+ ":" + WaypointColors.toHex(waypoint.color()) + ":" + escape(compactDimension(waypoint.dimension()));
	}

	public static Optional<SharedWaypoint> find(String text) {
		return locate(text).map(Match::waypoint);
	}

	public static Optional<SharedWaypoint> decode(String token) {
		return find(token.trim());
	}

	public static Optional<Match> locate(String text) {
		String clean = FORMATTING.matcher(text).replaceAll("");
		Matcher own = TOKEN.matcher(clean);
		while (own.find()) {
			Optional<SharedWaypoint> waypoint = fromOwn(own);
			if (waypoint.isPresent()) {
				return Optional.of(new Match(waypoint.get(), own.start(), own.end(), own.group()));
			}
		}
		Matcher xaero = XAERO_TOKEN.matcher(clean);
		while (xaero.find()) {
			Optional<SharedWaypoint> waypoint = fromXaero(xaero);
			if (waypoint.isPresent()) {
				return Optional.of(new Match(waypoint.get(), xaero.start(), xaero.end(), xaero.group()));
			}
		}
		return Optional.empty();
	}

	private static Optional<SharedWaypoint> fromOwn(Matcher m) {
		try {
			String name = limit(unescape(m.group(1)).trim(), 64);
			if (name.isBlank()) {
				return Optional.empty();
			}
			Integer color = WaypointColors.parseHex(m.group(6));
			return Optional.of(new SharedWaypoint(name, limit(unescape(m.group(2)).trim(), 3), Integer.parseInt(m.group(3)),
				Integer.parseInt(m.group(4)), Integer.parseInt(m.group(5)), color == null ? 0xFFFFFF : color, expandDimension(unescape(m.group(7)))));
		} catch (NumberFormatException e) {
			return Optional.empty();
		}
	}

	private static Optional<SharedWaypoint> fromXaero(Matcher m) {
		try {
			String name = limit(unescape(m.group(1)).trim(), 64);
			if (name.isBlank()) {
				return Optional.empty();
			}
			int y = "~".equals(m.group(4)) ? 64 : Integer.parseInt(m.group(4));
			int color = WaypointColors.fromXaeroIndex(Integer.parseInt(m.group(6)));
			return Optional.of(new SharedWaypoint(name, limit(unescape(m.group(2)).trim(), 3), Integer.parseInt(m.group(3)), y,
				Integer.parseInt(m.group(5)), color, xaeroDimension(m.group(7))));
		} catch (NumberFormatException e) {
			return Optional.empty();
		}
	}

	private static String xaeroDimension(String world) {
		if (world == null || !world.startsWith("Internal-")) {
			return null;
		}
		String rest = world.substring("Internal-".length());
		int dash = rest.indexOf('-');
		return switch (dash >= 0 ? rest.substring(0, dash) : rest) {
			case "overworld", "dim%0" -> "minecraft:overworld";
			case "the_nether", "dim%-1" -> "minecraft:the_nether";
			case "the_end", "dim%1" -> "minecraft:the_end";
			default -> null;
		};
	}

	static String compactDimension(String dimension) {
		return dimension.startsWith("minecraft:") ? dimension.substring("minecraft:".length()) : dimension;
	}

	static String expandDimension(String compact) {
		if (compact == null || compact.isBlank()) {
			return null;
		}
		return compact.contains(":") ? compact : "minecraft:" + compact;
	}

	private static String escape(String value) {
		return value.replace(COLON, "").replace(":", COLON).replace(' ', '\u00A0');
	}

	private static String unescape(String value) {
		return value.replace(COLON, ":").replace('\u00A0', ' ');
	}

	private static String limit(String value, int max) {
		return value.length() > max ? value.substring(0, max) : value;
	}
}
