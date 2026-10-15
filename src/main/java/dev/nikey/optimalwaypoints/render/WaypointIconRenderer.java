package dev.nikey.optimalwaypoints.render;

import dev.nikey.optimalwaypoints.waypoint.Waypoint;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

public final class WaypointIconRenderer {
	public static final String DEATH_SYMBOL = "☠";
	private static final int DEATH_SYMBOL_WIDTH = 7;

	private WaypointIconRenderer() {
	}

	public static int addedFrame(Font font, Waypoint waypoint) {
		int symbolWidth = symbolWidth(font, waypoint);
		if (symbolWidth <= 8) {
			return 0;
		}
		int toAdd = symbolWidth - 8;
		return toAdd - toAdd / 2;
	}

	private static int symbolWidth(Font font, Waypoint waypoint) {
		return waypoint.kind().isDeath() ? DEATH_SYMBOL_WIDTH : font.width(waypoint.initials());
	}

	public static void drawWorldIcon(GuiGraphicsExtractor graphics, Font font, Waypoint waypoint, float alpha) {
		int added = addedFrame(font, waypoint);
		graphics.fill(-5 - added, -9, 4 + added, 0, argb(alpha, waypoint.color()));
		if (waypoint.kind().isDeath()) {
			graphics.text(font, DEATH_SYMBOL, -font.width(DEATH_SYMBOL) / 2, -8, 0xFFFCFCFC, false);
		} else {
			graphics.text(font, waypoint.initials(), -font.width(waypoint.initials()) / 2, -8, 0xFFFFFFFF, false);
		}
	}

	public static void drawGuiIcon(GuiGraphicsExtractor graphics, Font font, Waypoint waypoint, int x, int y, float alpha) {
		int added = addedFrame(font, waypoint);
		graphics.fill(x - 4 - added, y - 4, x + 5 + added, y + 5, argb(alpha, waypoint.color()));
		String symbol = waypoint.kind().isDeath() ? DEATH_SYMBOL : waypoint.initials();
		graphics.text(font, symbol, x + 1 - symbolWidth(font, waypoint) / 2, y - 3, 0xFFFFFFFF, true);
	}

	private static int argb(float alpha, int rgb) {
		return (Mth.clamp(Math.round(alpha * 255.0F), 0, 255) << 24) | (rgb & 0xFFFFFF);
	}
}
