package dev.nikey.optimalwaypoints.feature;

import dev.nikey.optimalwaypoints.OwpClient;
import dev.nikey.optimalwaypoints.api.ClientModule;
import dev.nikey.optimalwaypoints.input.BlockMarkerController;
import dev.nikey.optimalwaypoints.input.OwpKeybinds;
import dev.nikey.optimalwaypoints.screen.ScreenOpener;
import dev.nikey.optimalwaypoints.ui.Messages;
import dev.nikey.optimalwaypoints.waypoint.Waypoint;
import dev.nikey.optimalwaypoints.waypoint.WaypointColors;
import dev.nikey.optimalwaypoints.waypoint.WaypointKind;
import dev.nikey.optimalwaypoints.waypoint.WaypointSet;
import dev.nikey.optimalwaypoints.waypoint.WaypointWorld;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class InputModule implements ClientModule {
	private final BlockMarkerController markerController = new BlockMarkerController();

	@Override
	public String id() {
		return "input";
	}

	@Override
	public void tick(OwpClient client, Minecraft mc) {
		boolean inGame = client.usable() && mc.gui.screen() == null;
		if (!inGame) {
			drain();
			markerController.tick(mc, client, false);
			return;
		}
		while (OwpKeybinds.openMenu.consumeClick()) {
			ScreenOpener.openList();
		}
		while (OwpKeybinds.newWaypoint.consumeClick()) {
			ScreenOpener.openNewWaypoint();
		}
		while (OwpKeybinds.toggleVisibility.consumeClick()) {
			client.config().hiddenByKey = !client.config().hiddenByKey;
			client.configManager().save();
			Messages.toast(Component.translatable(client.config().hiddenByKey ? "optimalwaypoints.toggle.hidden" : "optimalwaypoints.toggle.shown"));
		}
		while (OwpKeybinds.clearUnlocked.consumeClick()) {
			int removed = client.waypoints().clearUnlockedBlockMarkers(client.currentDimension());
			Messages.toast(Component.translatable("optimalwaypoints.marker.cleared", Messages.value(removed)));
		}
		while (OwpKeybinds.switchSet.consumeClick()) {
			cycleSet(client);
		}
		while (OwpKeybinds.tempWaypoint.consumeClick()) {
			createTemporary(client, mc);
		}
		markerController.tick(mc, client, OwpKeybinds.markBlock.isDown());
	}

	private static void drain() {
		for (var key : List.of(OwpKeybinds.openMenu, OwpKeybinds.newWaypoint, OwpKeybinds.toggleVisibility,
			OwpKeybinds.clearUnlocked, OwpKeybinds.switchSet, OwpKeybinds.tempWaypoint)) {
			while (key.consumeClick()) {
				if (key == OwpKeybinds.openMenu || key == OwpKeybinds.newWaypoint) {
					notifyBlocked();
				}
			}
		}
	}

	private static void notifyBlocked() {
		OwpClient client = OwpClient.get();
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null && mc.gui.screen() == null && !client.policy().modAllowed()) {
			Messages.toastError(Component.translatable("optimalwaypoints.policy.mod_disabled"));
		}
	}

	private static void cycleSet(OwpClient client) {
		WaypointWorld world = client.waypoints().world();
		List<WaypointSet> sets = world.sets();
		int index = sets.indexOf(world.currentSet());
		WaypointSet next = sets.get((index + 1) % sets.size());
		world.setCurrentSet(next.name());
		client.waypoints().markDirty();
		Messages.toast(Component.translatable("optimalwaypoints.set.switched", Messages.value(next.name())));
	}

	private static void createTemporary(OwpClient client, Minecraft mc) {
		HitResult hit = mc.player.pick(512.0, mc.getDeltaTracker().getGameTimeDeltaPartialTick(true), false);
		BlockPos pos = hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK
			? blockHit.getBlockPos().above()
			: mc.player.blockPosition();
		Waypoint waypoint = new Waypoint(Component.translatable("optimalwaypoints.waypoint.temporary").getString(),
			pos.getX(), pos.getY(), pos.getZ(), client.currentDimension(), WaypointColors.random(), WaypointKind.STANDARD);
		waypoint.setDeleteOnArrival(true);
		client.waypoints().add(waypoint);
		Messages.toastSuccess(Component.translatable("optimalwaypoints.waypoint.created", Messages.value(waypoint.name())));
	}
}
