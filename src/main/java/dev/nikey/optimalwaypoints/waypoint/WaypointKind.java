package dev.nikey.optimalwaypoints.waypoint;

public enum WaypointKind {
	STANDARD,
	BLOCK,
	DEATH,
	OLD_DEATH;

	public boolean isDeath() {
		return this == DEATH || this == OLD_DEATH;
	}
}
