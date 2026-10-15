package dev.nikey.optimalwaypoints.waypoint;

import java.util.ArrayList;
import java.util.List;

public final class WaypointWorld {
	public static final int FORMAT_VERSION = 1;
	public static final String DEFAULT_SET = "default";

	private int formatVersion = FORMAT_VERSION;
	private String currentSet = DEFAULT_SET;
	private List<WaypointSet> sets = new ArrayList<>();

	public static WaypointWorld createDefault() {
		WaypointWorld world = new WaypointWorld();
		world.sets.add(new WaypointSet(DEFAULT_SET));
		return world;
	}

	public void validate() {
		if (sets == null) {
			sets = new ArrayList<>();
		}
		sets.removeIf(set -> set == null || set.name() == null || set.name().isBlank());
		if (sets.isEmpty()) {
			sets.add(new WaypointSet(DEFAULT_SET));
		}
		for (WaypointSet set : sets) {
			set.waypoints().removeIf(w -> w == null || w.id() == null);
		}
		if (set(currentSet) == null) {
			currentSet = sets.getFirst().name();
		}
		formatVersion = FORMAT_VERSION;
	}

	public int formatVersion() {
		return formatVersion;
	}

	public List<WaypointSet> sets() {
		return sets;
	}

	public WaypointSet set(String name) {
		for (WaypointSet set : sets) {
			if (set.name().equals(name)) {
				return set;
			}
		}
		return null;
	}

	public WaypointSet currentSet() {
		WaypointSet set = set(currentSet);
		if (set == null) {
			validate();
			set = set(currentSet);
		}
		return set;
	}

	public void setCurrentSet(String name) {
		if (set(name) != null) {
			currentSet = name;
		}
	}

	public WaypointSet createSet(String name) {
		WaypointSet existing = set(name);
		if (existing != null) {
			return existing;
		}
		WaypointSet set = new WaypointSet(name);
		sets.add(set);
		return set;
	}

	public boolean removeSet(String name) {
		if (sets.size() <= 1) {
			return false;
		}
		boolean removed = sets.removeIf(set -> set.name().equals(name));
		if (removed && currentSet.equals(name)) {
			currentSet = sets.getFirst().name();
		}
		return removed;
	}
}
