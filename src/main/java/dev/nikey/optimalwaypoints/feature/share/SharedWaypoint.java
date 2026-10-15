package dev.nikey.optimalwaypoints.feature.share;

import dev.nikey.optimalwaypoints.waypoint.Waypoint;
import dev.nikey.optimalwaypoints.waypoint.WaypointKind;

public record SharedWaypoint(String name, String initials, int x, int y, int z, int color, String dimension) {
	public Waypoint toWaypoint(String fallbackDimension) {
		Waypoint waypoint = new Waypoint(name, x, y, z, dimension == null ? fallbackDimension : dimension, color, WaypointKind.STANDARD);
		if (!initials.isBlank()) {
			waypoint.setInitials(initials);
		}
		return waypoint;
	}
}
