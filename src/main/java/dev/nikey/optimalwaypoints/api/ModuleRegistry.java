package dev.nikey.optimalwaypoints.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ModuleRegistry {
	private final List<ClientModule> modules = new ArrayList<>();

	public void register(ClientModule module) {
		for (ClientModule existing : modules) {
			if (existing.id().equals(module.id())) {
				throw new IllegalArgumentException("Duplicate Optimal Waypoints module id: " + module.id());
			}
		}
		modules.add(module);
	}

	public List<ClientModule> all() {
		return Collections.unmodifiableList(modules);
	}

	@SuppressWarnings("unchecked")
	public <T extends ClientModule> T get(Class<T> type) {
		for (ClientModule module : modules) {
			if (type.isInstance(module)) {
				return (T) module;
			}
		}
		return null;
	}
}
