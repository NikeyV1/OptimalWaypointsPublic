package dev.nikey.optimalwaypoints.feature;

import dev.nikey.optimalwaypoints.OwpClient;
import dev.nikey.optimalwaypoints.api.ClientModule;
import dev.nikey.optimalwaypoints.api.WaypointEvents;
import dev.nikey.optimalwaypoints.ui.Messages;
import dev.nikey.optimalwaypoints.waypoint.Waypoint;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

public final class ArrivalModule implements ClientModule {
	private static final double ARRIVAL_DISTANCE = 4.0;
	private static final long DEATHPOINT_GRACE_MS = 10_000L;

	private int ticks;

	@Override
	public String id() {
		return "arrival";
	}

	@Override
	public void tick(OwpClient client, Minecraft mc) {
		if (mc.player == null || ++ticks % 10 != 0 || !client.waypoints().isLoaded() || mc.player.isDeadOrDying()) {
			return;
		}
		Vec3 pos = mc.player.position();
		String dimension = client.currentDimension();
		long now = System.currentTimeMillis();
		List<Waypoint> reached = new ArrayList<>();
		for (Waypoint waypoint : client.waypoints().current()) {
			boolean reachedDeath = waypoint.kind().isDeath() && client.config().deleteReachedDeathpoints && now - waypoint.created() > DEATHPOINT_GRACE_MS;
			boolean candidate = waypoint.deleteOnArrival() || reachedDeath;
			if (!candidate || !waypoint.enabled() || !waypoint.dimension().equals(dimension)) {
				continue;
			}
			double dx = waypoint.centerX() - pos.x;
			double dy = waypoint.y() - pos.y;
			double dz = waypoint.centerZ() - pos.z;
			if (dx * dx + dy * dy + dz * dz <= ARRIVAL_DISTANCE * ARRIVAL_DISTANCE) {
				reached.add(waypoint);
			}
		}
		for (Waypoint waypoint : reached) {
			WaypointEvents.REACHED.fire(waypoint);
			client.waypoints().remove(waypoint);
			Messages.toastSuccess(Component.translatable("optimalwaypoints.waypoint.reached", Messages.value(waypoint.name())));
		}
	}
}
