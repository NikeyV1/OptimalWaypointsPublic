package dev.nikey.optimalwaypoints.protocol;

import java.util.Locale;

public enum Feature {
	MOD(0),
	IN_WORLD_WAYPOINTS(1),
	BLOCK_HIGHLIGHTS(2),
	THROUGH_WALLS(3),
	DEATHPOINTS(4),
	SHARING(5),
	CHAT_COORDINATES(6),
	DISTANCE_DISPLAY(7),
	CROSS_DIMENSION(8);

	// Bit indices are part of the network protocol: never reorder, only append.
	private final int bit;

	Feature(int bit) {
		this.bit = bit;
	}

	public long mask() {
		return 1L << bit;
	}

	public String key() {
		return name().toLowerCase(Locale.ROOT);
	}

	public String translationKey() {
		return "optimalwaypoints.feature." + key();
	}

	public static Feature byKey(String key) {
		for (Feature feature : values()) {
			if (feature.key().equalsIgnoreCase(key)) {
				return feature;
			}
		}
		return null;
	}
}
