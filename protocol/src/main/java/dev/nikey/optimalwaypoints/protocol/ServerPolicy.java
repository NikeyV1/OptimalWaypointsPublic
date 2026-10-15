package dev.nikey.optimalwaypoints.protocol;

import java.util.EnumSet;
import java.util.Set;

public record ServerPolicy(long disabledMask, int maxRenderDistance, String message) {
	public static final ServerPolicy ALLOW_ALL = new ServerPolicy(0L, 0, "");

	public ServerPolicy {
		maxRenderDistance = Math.max(0, maxRenderDistance);
		message = message == null ? "" : message;
	}

	public boolean allows(Feature feature) {
		if (feature != Feature.MOD && (disabledMask & Feature.MOD.mask()) != 0) {
			return false;
		}
		return (disabledMask & feature.mask()) == 0;
	}

	public boolean isDisabled(Feature feature) {
		return (disabledMask & feature.mask()) != 0;
	}

	public Set<Feature> disabledFeatures() {
		EnumSet<Feature> set = EnumSet.noneOf(Feature.class);
		for (Feature feature : Feature.values()) {
			if (isDisabled(feature)) {
				set.add(feature);
			}
		}
		return set;
	}

	public boolean isUnrestricted() {
		return disabledMask == 0L && maxRenderDistance == 0;
	}

	public ServerPolicy with(Feature feature, boolean allowed) {
		long mask = allowed ? disabledMask & ~feature.mask() : disabledMask | feature.mask();
		return new ServerPolicy(mask, maxRenderDistance, message);
	}

	public ServerPolicy withMaxRenderDistance(int distance) {
		return new ServerPolicy(disabledMask, distance, message);
	}

	public ServerPolicy withMessage(String newMessage) {
		return new ServerPolicy(disabledMask, maxRenderDistance, newMessage);
	}
}
