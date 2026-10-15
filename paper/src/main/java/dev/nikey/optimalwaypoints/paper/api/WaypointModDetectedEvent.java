package dev.nikey.optimalwaypoints.paper.api;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public final class WaypointModDetectedEvent extends PlayerEvent {
	private static final HandlerList HANDLERS = new HandlerList();

	private final String modVersion;

	public WaypointModDetectedEvent(Player player, String modVersion) {
		super(player);
		this.modVersion = modVersion;
	}

	public String getModVersion() {
		return modVersion;
	}

	@Override
	public HandlerList getHandlers() {
		return HANDLERS;
	}

	public static HandlerList getHandlerList() {
		return HANDLERS;
	}
}
