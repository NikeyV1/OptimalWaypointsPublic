package dev.nikey.optimalwaypoints.paper;

import java.util.UUID;

record ModUser(UUID uuid, String name, String version) {
	static final String UNKNOWN_VERSION = "?";

	ModUser withVersion(String newVersion) {
		return new ModUser(uuid, name, newVersion);
	}
}
