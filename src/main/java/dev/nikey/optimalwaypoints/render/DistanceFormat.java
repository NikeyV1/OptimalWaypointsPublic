package dev.nikey.optimalwaypoints.render;

import java.util.Locale;

public final class DistanceFormat {
	private static final double KM_THRESHOLD = 10000.0;

	private DistanceFormat() {
	}

	public static String format(double blocks) {
		if (blocks >= KM_THRESHOLD) {
			return String.format(Locale.ROOT, "%.1fkm", blocks / 1000.0);
		}
		return String.format(Locale.ROOT, "%.1fm", blocks);
	}
}
