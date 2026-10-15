package dev.nikey.optimalwaypoints.paper.api;

import dev.nikey.optimalwaypoints.protocol.ServerPolicy;
import java.util.Objects;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public final class WaypointPolicySendEvent extends PlayerEvent {
	private static final HandlerList HANDLERS = new HandlerList();

	private ServerPolicy policy;

	public WaypointPolicySendEvent(Player player, ServerPolicy policy) {
		super(player);
		this.policy = policy;
	}

	public ServerPolicy getPolicy() {
		return policy;
	}

	public void setPolicy(ServerPolicy policy) {
		this.policy = Objects.requireNonNull(policy);
	}

	@Override
	public HandlerList getHandlers() {
		return HANDLERS;
	}

	public static HandlerList getHandlerList() {
		return HANDLERS;
	}
}
