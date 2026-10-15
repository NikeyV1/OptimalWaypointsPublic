package dev.nikey.optimalwaypoints.waypoint;

import dev.nikey.optimalwaypoints.api.WaypointEvents;
import dev.nikey.optimalwaypoints.waypoint.io.WaypointStorage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

public final class WaypointManager {
	private static final int SAVE_DELAY_TICKS = 40;

	private WorldKey key;
	private WaypointWorld world;
	private boolean dirty;
	private int ticksSinceChange;

	public void load(Minecraft mc) {
		unload();
		key = WorldKey.current(mc);
		world = WaypointStorage.load(key);
	}

	public void unload() {
		if (world != null && key != null) {
			WaypointStorage.save(key, world);
		}
		world = null;
		key = null;
		dirty = false;
	}

	public boolean isLoaded() {
		return world != null;
	}

	public void tick() {
		if (!dirty || world == null) {
			return;
		}
		if (++ticksSinceChange >= SAVE_DELAY_TICKS) {
			saveNow();
		}
	}

	public void saveNow() {
		if (world != null && key != null) {
			WaypointStorage.save(key, world);
		}
		dirty = false;
		ticksSinceChange = 0;
	}

	public void markDirty() {
		dirty = true;
		ticksSinceChange = 0;
	}

	public WorldKey key() {
		return key;
	}

	public WaypointWorld world() {
		return world;
	}

	public List<Waypoint> current() {
		if (world == null) {
			return Collections.emptyList();
		}
		return world.currentSet().waypoints();
	}

	public Waypoint byId(UUID id) {
		for (Waypoint waypoint : current()) {
			if (waypoint.id().equals(id)) {
				return waypoint;
			}
		}
		return null;
	}

	public void add(Waypoint waypoint) {
		if (world == null) {
			return;
		}
		world.currentSet().waypoints().addFirst(waypoint);
		markDirty();
		WaypointEvents.ADDED.fire(waypoint);
	}

	public void addToSet(String setName, Waypoint waypoint) {
		if (world == null) {
			return;
		}
		world.createSet(setName).waypoints().addFirst(waypoint);
		markDirty();
		WaypointEvents.ADDED.fire(waypoint);
	}

	public boolean remove(Waypoint waypoint) {
		if (world == null) {
			return false;
		}
		boolean removed = false;
		for (WaypointSet set : world.sets()) {
			removed |= set.waypoints().removeIf(w -> w.id().equals(waypoint.id()));
		}
		if (removed) {
			markDirty();
			WaypointEvents.REMOVED.fire(waypoint);
		}
		return removed;
	}

	public void changed(Waypoint waypoint) {
		markDirty();
		WaypointEvents.CHANGED.fire(waypoint);
	}

	public void moveToSet(Waypoint waypoint, String setName) {
		WaypointSet target = world == null ? null : world.set(setName);
		if (target == null || target.waypoints().contains(waypoint)) {
			return;
		}
		for (WaypointSet set : world.sets()) {
			set.waypoints().removeIf(w -> w.id().equals(waypoint.id()));
		}
		target.waypoints().addFirst(waypoint);
		markDirty();
	}

	public Waypoint blockMarkerAt(BlockPos pos, String dimension) {
		for (Waypoint waypoint : current()) {
			if (waypoint.isBlockMarker() && waypoint.dimension().equals(dimension) && waypoint.containsBlock(pos)) {
				return waypoint;
			}
		}
		return null;
	}

	public int clearUnlockedBlockMarkers(String dimension) {
		List<Waypoint> removed = new ArrayList<>();
		for (Waypoint waypoint : current()) {
			if (waypoint.isBlockMarker() && !waypoint.locked() && waypoint.dimension().equals(dimension)) {
				removed.add(waypoint);
			}
		}
		removed.forEach(this::remove);
		return removed.size();
	}

	public List<Waypoint> ofKind(WaypointKind kind) {
		List<Waypoint> result = new ArrayList<>();
		for (Waypoint waypoint : current()) {
			if (waypoint.kind() == kind) {
				result.add(waypoint);
			}
		}
		return result;
	}
}
