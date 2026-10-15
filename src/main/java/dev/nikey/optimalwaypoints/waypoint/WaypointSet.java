package dev.nikey.optimalwaypoints.waypoint;

import java.util.ArrayList;
import java.util.List;

public final class WaypointSet {
	private String name;
	private List<Waypoint> waypoints = new ArrayList<>();

	public WaypointSet() {
	}

	public WaypointSet(String name) {
		this.name = name;
	}

	public String name() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Waypoint> waypoints() {
		if (waypoints == null) {
			waypoints = new ArrayList<>();
		}
		return waypoints;
	}
}
