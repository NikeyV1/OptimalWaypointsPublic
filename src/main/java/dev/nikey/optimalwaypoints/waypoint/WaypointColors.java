package dev.nikey.optimalwaypoints.waypoint;

import java.util.concurrent.ThreadLocalRandom;

public final class WaypointColors {
	public static final int[] PALETTE = {
		0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
		0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF
	};
	public static final String[] NAMES = {
		"black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple", "gold", "gray",
		"dark_gray", "blue", "green", "aqua", "red", "light_purple", "yellow", "white"
	};

	private WaypointColors() {
	}

	public static int random() {
		return PALETTE[1 + ThreadLocalRandom.current().nextInt(PALETTE.length - 2)];
	}

	public static int fromXaeroIndex(int index) {
		return index >= 0 && index < PALETTE.length ? PALETTE[index] : PALETTE[15];
	}

	public static int paletteIndex(int rgb) {
		for (int i = 0; i < PALETTE.length; i++) {
			if (PALETTE[i] == (rgb & 0xFFFFFF)) {
				return i;
			}
		}
		return -1;
	}

	public static int next(int rgb) {
		int index = paletteIndex(rgb);
		return PALETTE[(index + 1) % PALETTE.length];
	}

	public static String toHex(int rgb) {
		return String.format("%06X", rgb & 0xFFFFFF);
	}

	public static Integer parseHex(String text) {
		String value = text.trim();
		if (value.startsWith("#")) {
			value = value.substring(1);
		}
		if (value.length() != 6) {
			return null;
		}
		try {
			return Integer.parseInt(value, 16);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public static float luminance(int rgb) {
		return (0.299F * ((rgb >> 16) & 0xFF) + 0.587F * ((rgb >> 8) & 0xFF) + 0.114F * (rgb & 0xFF)) / 255.0F;
	}
}
