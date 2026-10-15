package dev.nikey.optimalwaypoints.feature;

import dev.nikey.optimalwaypoints.OptimalWaypoints;
import dev.nikey.optimalwaypoints.OwpClient;
import dev.nikey.optimalwaypoints.api.ClientModule;
import dev.nikey.optimalwaypoints.render.BlockHighlightRenderer;
import dev.nikey.optimalwaypoints.render.WaypointHudRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;

public final class RenderModule implements ClientModule {
	private final BlockHighlightRenderer blockRenderer = new BlockHighlightRenderer();
	private final WaypointHudRenderer hudRenderer = new WaypointHudRenderer();

	@Override
	public String id() {
		return "render";
	}

	@Override
	public void init(OwpClient client) {
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, OptimalWaypoints.id("waypoints"), (graphics, delta) -> {
			if (client.usable()) {
				hudRenderer.extractRenderState(graphics, delta);
			}
		});
		LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
			if (client.usable()) {
				blockRenderer.collect(context);
			}
		});
		LevelRenderEvents.BEFORE_BLOCK_OUTLINE.register((context, outline) -> !client.usable() || blockRenderer.allowVanillaOutline());
	}
}
