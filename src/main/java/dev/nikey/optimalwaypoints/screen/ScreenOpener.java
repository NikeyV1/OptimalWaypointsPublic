package dev.nikey.optimalwaypoints.screen;

import dev.nikey.optimalwaypoints.OwpClient;
import dev.nikey.optimalwaypoints.waypoint.Waypoint;
import dev.nikey.optimalwaypoints.waypoint.WaypointColors;
import dev.nikey.optimalwaypoints.waypoint.WaypointKind;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;

public final class ScreenOpener {
	private static Supplier<Screen> pending;

	private ScreenOpener() {
	}

	public static void tick(Minecraft mc) {
		if (pending != null) {
			Supplier<Screen> next = pending;
			pending = null;
			mc.gui.setScreen(next.get());
		}
	}

	public static void openLater(Supplier<Screen> screen) {
		pending = screen;
	}

	public static void openList() {
		Minecraft mc = Minecraft.getInstance();
		mc.gui.setScreen(new WaypointListScreen(mc.gui.screen()));
	}

	public static void openNewWaypoint() {
		Minecraft mc = Minecraft.getInstance();
		mc.gui.setScreen(new WaypointEditScreen(mc.gui.screen(), createAtPlayer(), true));
	}

	public static Waypoint createAtPlayer() {
		Minecraft mc = Minecraft.getInstance();
		BlockPos pos = mc.player == null ? BlockPos.ZERO : mc.player.blockPosition();
		return new Waypoint("", pos.getX(), pos.getY(), pos.getZ(), OwpClient.get().currentDimension(), WaypointColors.random(), WaypointKind.STANDARD);
	}
}
