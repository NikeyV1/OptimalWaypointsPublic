package dev.nikey.optimalwaypoints.feature;

import dev.nikey.optimalwaypoints.OwpClient;
import dev.nikey.optimalwaypoints.api.ClientModule;
import dev.nikey.optimalwaypoints.config.OwpConfig;
import dev.nikey.optimalwaypoints.protocol.Feature;
import dev.nikey.optimalwaypoints.waypoint.Waypoint;
import dev.nikey.optimalwaypoints.waypoint.WaypointKind;
import dev.nikey.optimalwaypoints.waypoint.WaypointManager;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public final class DeathpointModule implements ClientModule {
	private static final int DEATH_COLOR = 0x000000;
	private static final int MAX_OLD_DEATHPOINTS = 3;

	private LocalPlayer trackedPlayer;
	private boolean recorded;
	private BlockPos lastPos;
	private String lastDimension;

	@Override
	public String id() {
		return "deathpoints";
	}

	@Override
	public void onJoin(OwpClient client, Minecraft mc) {
		trackedPlayer = null;
		recorded = false;
	}

	@Override
	public void tick(OwpClient client, Minecraft mc) {
		LocalPlayer player = mc.player;
		if (player == null) {
			return;
		}
		if (player != trackedPlayer) {
			if (trackedPlayer != null && !recorded && trackedPlayer.getHealth() <= 0.0F && lastPos != null) {
				record(client, lastPos, lastDimension);
			}
			trackedPlayer = player;
			recorded = false;
		}
		boolean dead = player.isDeadOrDying();
		if (dead && !recorded) {
			record(client, player.blockPosition(), client.currentDimension());
			recorded = true;
		} else if (!dead) {
			recorded = false;
			lastPos = player.blockPosition();
			lastDimension = client.currentDimension();
		}
	}

	private void record(OwpClient client, BlockPos pos, String dimension) {
		OwpConfig cfg = client.config();
		if (!cfg.deathpoints || !client.policy().allows(Feature.DEATHPOINTS) || !client.waypoints().isLoaded()) {
			return;
		}
		recorded = true;
		WaypointManager manager = client.waypoints();
		for (Waypoint previous : manager.ofKind(WaypointKind.DEATH)) {
			if (cfg.keepOldDeathpoints) {
				previous.setKind(WaypointKind.OLD_DEATH);
				previous.setName(Component.translatable("optimalwaypoints.waypoint.old_death").getString());
				manager.changed(previous);
			} else {
				manager.remove(previous);
			}
		}
		List<Waypoint> old = manager.ofKind(WaypointKind.OLD_DEATH);
		old.sort(Comparator.comparingLong(Waypoint::created));
		while (old.size() > MAX_OLD_DEATHPOINTS) {
			manager.remove(old.removeFirst());
		}
		Waypoint death = new Waypoint(Component.translatable("optimalwaypoints.waypoint.death").getString(),
			pos.getX(), pos.getY(), pos.getZ(), dimension, DEATH_COLOR, WaypointKind.DEATH);
		death.setGlobal(true);
		manager.add(death);
		manager.saveNow();
	}
}
