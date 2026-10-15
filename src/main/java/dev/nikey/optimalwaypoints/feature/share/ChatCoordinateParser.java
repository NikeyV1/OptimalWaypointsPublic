package dev.nikey.optimalwaypoints.feature.share;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ChatCoordinateParser {
	private static final Pattern TRIPLE = Pattern.compile("(?<![\\w.\\-])(-?\\d{1,8})\\s*[,/\\s]\\s*(-?\\d{1,4}|~)\\s*[,/\\s]\\s*(-?\\d{1,8})(?![\\w.])");
	private static final Pattern PAIR = Pattern.compile("(?i)\\bx\\s*[:=]?\\s*(-?\\d{1,8})[,\\s]+(?:y\\s*[:=]?\\s*(-?\\d{1,4})[,\\s]+)?z\\s*[:=]?\\s*(-?\\d{1,8})\\b");

	public record Coordinates(int x, Integer y, int z) {
	}

	private ChatCoordinateParser() {
	}

	public static Optional<Coordinates> find(String text) {
		Matcher labeled = PAIR.matcher(text);
		if (labeled.find()) {
			Integer y = labeled.group(2) == null ? null : Integer.parseInt(labeled.group(2));
			return valid(Integer.parseInt(labeled.group(1)), y, Integer.parseInt(labeled.group(3)));
		}
		Matcher triple = TRIPLE.matcher(text);
		if (triple.find()) {
			String yRaw = triple.group(2);
			Integer y = "~".equals(yRaw) ? null : Integer.parseInt(yRaw);
			return valid(Integer.parseInt(triple.group(1)), y, Integer.parseInt(triple.group(3)));
		}
		return Optional.empty();
	}

	private static Optional<Coordinates> valid(int x, Integer y, int z) {
		if (Math.abs(x) > 30_000_000 || Math.abs(z) > 30_000_000 || (y != null && (y < -2048 || y > 2048))) {
			return Optional.empty();
		}
		return Optional.of(new Coordinates(x, y, z));
	}
}
