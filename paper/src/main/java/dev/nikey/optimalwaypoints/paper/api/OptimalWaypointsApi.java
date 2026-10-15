package dev.nikey.optimalwaypoints.paper.api;

import dev.nikey.optimalwaypoints.protocol.ServerPolicy;
import java.util.Collection;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public interface OptimalWaypointsApi {
	static OptimalWaypointsApi get() {
		OptimalWaypointsApi api = Bukkit.getServicesManager().load(OptimalWaypointsApi.class);
		if (api == null) {
			throw new IllegalStateException("OptimalWaypoints is not enabled");
		}
		return api;
	}

	boolean isUsingMod(Player player);

	Optional<String> modVersion(Player player);

	Collection<Player> modUsers();

	ServerPolicy globalPolicy();

	void setGlobalPolicy(ServerPolicy policy, boolean saveToConfig);

	Optional<ServerPolicy> playerPolicy(Player player);

	void setPlayerPolicy(Player player, ServerPolicy policy);

	void clearPlayerPolicy(Player player);

	ServerPolicy effectivePolicy(Player player);

	void refresh(Player player);
}
