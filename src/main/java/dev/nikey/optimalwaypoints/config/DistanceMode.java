package dev.nikey.optimalwaypoints.config;

public enum DistanceMode {
	OFF,
	LOOKING,
	ALL;

	public String translationKey() {
		return "optimalwaypoints.distance_mode." + name().toLowerCase(java.util.Locale.ROOT);
	}
}
