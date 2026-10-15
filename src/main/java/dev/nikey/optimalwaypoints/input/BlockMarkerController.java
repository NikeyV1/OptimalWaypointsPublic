package dev.nikey.optimalwaypoints.input;

import dev.nikey.optimalwaypoints.OwpClient;
import dev.nikey.optimalwaypoints.protocol.Feature;
import dev.nikey.optimalwaypoints.ui.Messages;
import dev.nikey.optimalwaypoints.waypoint.Waypoint;
import dev.nikey.optimalwaypoints.waypoint.WaypointColors;
import dev.nikey.optimalwaypoints.waypoint.WaypointKind;
import dev.nikey.optimalwaypoints.waypoint.WaypointManager;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class BlockMarkerController {
	private static final double RANGE = 96.0;
	private static final int MAX_BLOCKS_PER_MARKER = 4096;

	private enum Mode {
		NONE,
		ADD,
		REMOVE
	}

	private boolean down;
	private Mode mode = Mode.NONE;
	private UUID target;
	private UUID lastEdited;
	private long lastPos;
	private int changes;

	public void tick(Minecraft mc, OwpClient client, boolean keyDown) {
		if (!keyDown) {
			if (down) {
				finish(client);
			}
			return;
		}
		if (!client.policy().allows(Feature.BLOCK_HIGHLIGHTS)) {
			if (!down) {
				Messages.toastError(Component.translatable("optimalwaypoints.gui.blocked_by_server"));
				down = true;
			}
			return;
		}
		BlockPos pos = targetBlock(mc);
		if (!down) {
			down = true;
			changes = 0;
			begin(mc, client, pos);
			return;
		}
		if (pos != null && pos.asLong() != lastPos && mode != Mode.NONE) {
			lastPos = pos.asLong();
			paint(client, pos);
		}
	}

	private void begin(Minecraft mc, OwpClient client, BlockPos pos) {
		mode = Mode.NONE;
		target = null;
		if (pos == null) {
			Messages.toastError(Component.translatable("optimalwaypoints.marker.no_block"));
			return;
		}
		lastPos = pos.asLong();
		WaypointManager manager = client.waypoints();
		String dimension = client.currentDimension();
		Waypoint existing = manager.blockMarkerAt(pos, dimension);
		if (existing != null) {
			if (existing.locked()) {
				Messages.actionBar(Messages.WARN, "⚠", Component.translatable("optimalwaypoints.marker.locked", Messages.value(existing.name())));
				return;
			}
			mode = Mode.REMOVE;
			removeBlock(client, existing, pos);
			return;
		}
		mode = Mode.ADD;
		Waypoint extend = mc.player != null && mc.player.isShiftKeyDown() && lastEdited != null ? manager.byId(lastEdited) : null;
		if (extend != null && extend.isBlockMarker() && extend.dimension().equals(dimension) && !extend.locked()) {
			target = extend.id();
			addBlock(client, extend, pos);
			return;
		}
		String name = mc.level.getBlockState(pos).getBlock().getName().getString();
		Waypoint marker = new Waypoint(name, pos.getX(), pos.getY(), pos.getZ(), dimension, WaypointColors.random(), WaypointKind.BLOCK);
		marker.addBlock(pos);
		manager.add(marker);
		target = marker.id();
		lastEdited = marker.id();
		changes++;
	}

	private void paint(OwpClient client, BlockPos pos) {
		WaypointManager manager = client.waypoints();
		String dimension = client.currentDimension();
		Waypoint at = manager.blockMarkerAt(pos, dimension);
		if (mode == Mode.REMOVE) {
			if (at != null && !at.locked()) {
				removeBlock(client, at, pos);
			}
			return;
		}
		Waypoint marker = target == null ? null : manager.byId(target);
		if (marker == null || at != null || marker.blocks().size() >= MAX_BLOCKS_PER_MARKER) {
			return;
		}
		addBlock(client, marker, pos);
	}

	private void addBlock(OwpClient client, Waypoint marker, BlockPos pos) {
		if (marker.addBlock(pos)) {
			changes++;
			lastEdited = marker.id();
			client.waypoints().changed(marker);
		}
	}

	private void removeBlock(OwpClient client, Waypoint marker, BlockPos pos) {
		if (marker.removeBlock(pos)) {
			changes++;
			if (marker.blocks().isEmpty()) {
				client.waypoints().remove(marker);
			} else {
				client.waypoints().changed(marker);
			}
		}
	}

	private void finish(OwpClient client) {
		down = false;
		if (changes > 1) {
			Messages.toast(Component.translatable(mode == Mode.REMOVE ? "optimalwaypoints.marker.removed_many" : "optimalwaypoints.marker.added_many", Messages.value(changes)));
		}
		mode = Mode.NONE;
		target = null;
		changes = 0;
	}

	private static BlockPos targetBlock(Minecraft mc) {
		if (mc.player == null || mc.level == null) {
			return null;
		}
		HitResult hit = mc.player.pick(RANGE, mc.getDeltaTracker().getGameTimeDeltaPartialTick(true), false);
		if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
			return blockHit.getBlockPos();
		}
		return null;
	}
}
