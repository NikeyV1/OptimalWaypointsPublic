package dev.nikey.optimalwaypoints.api;

import dev.nikey.optimalwaypoints.waypoint.Waypoint;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class WaypointEvents {
	public static final Listeners<Waypoint> ADDED = new Listeners<>();
	public static final Listeners<Waypoint> REMOVED = new Listeners<>();
	public static final Listeners<Waypoint> CHANGED = new Listeners<>();
	public static final Listeners<Waypoint> REACHED = new Listeners<>();

	private WaypointEvents() {
	}

	public static final class Listeners<T> {
		private final List<Consumer<T>> listeners = new CopyOnWriteArrayList<>();

		public void register(Consumer<T> listener) {
			listeners.add(listener);
		}

		public void fire(T value) {
			for (Consumer<T> listener : listeners) {
				listener.accept(value);
			}
		}
	}
}
