package dev.nikey.optimalwaypoints.paper;

import dev.nikey.optimalwaypoints.paper.api.OptimalWaypointsApi;
import dev.nikey.optimalwaypoints.paper.api.WaypointModDetectedEvent;
import dev.nikey.optimalwaypoints.paper.api.WaypointPolicySendEvent;
import dev.nikey.optimalwaypoints.protocol.Protocol;
import dev.nikey.optimalwaypoints.protocol.ServerPolicy;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

public final class OptimalWaypointsPlugin extends JavaPlugin implements Listener, PluginMessageListener, OptimalWaypointsApi {
	private final Map<UUID, ModUser> users = new ConcurrentHashMap<>();
	private final Map<UUID, ServerPolicy> playerPolicies = new ConcurrentHashMap<>();
	private PolicySettings settings;

	@Override
	public void onEnable() {
		saveDefaultConfig();
		settings = PolicySettings.load(getConfig());
		getServer().getMessenger().registerIncomingPluginChannel(this, Protocol.HELLO_CHANNEL, this);
		getServer().getMessenger().registerOutgoingPluginChannel(this, Protocol.POLICY_CHANNEL);
		getServer().getPluginManager().registerEvents(this, this);
		getServer().getServicesManager().register(OptimalWaypointsApi.class, this, this, ServicePriority.Normal);
		PluginCommand command = getCommand("optimalwaypoints");
		if (command != null) {
			AdminCommand admin = new AdminCommand(this);
			command.setExecutor(admin);
			command.setTabCompleter(admin);
		}
	}

	@Override
	public void onDisable() {
		getServer().getServicesManager().unregisterAll(this);
		getServer().getMessenger().unregisterIncomingPluginChannel(this);
		getServer().getMessenger().unregisterOutgoingPluginChannel(this);
		users.clear();
		playerPolicies.clear();
	}

	@Override
	public void onPluginMessageReceived(String channel, Player player, byte[] message) {
		if (!Protocol.HELLO_CHANNEL.equals(channel)) {
			return;
		}
		String version;
		try {
			version = Protocol.decodeHello(message).modVersion();
		} catch (IllegalArgumentException e) {
			version = ModUser.UNKNOWN_VERSION;
		}
		ModUser previous = users.get(player.getUniqueId());
		users.put(player.getUniqueId(), previous == null ? new ModUser(player.getUniqueId(), player.getName(), version) : previous.withVersion(version));
		if (previous == null) {
			detected(player, version);
		}
		refresh(player);
	}

	@EventHandler
	public void onRegisterChannel(PlayerRegisterChannelEvent event) {
		if (!Protocol.POLICY_CHANNEL.equals(event.getChannel())) {
			return;
		}
		Player player = event.getPlayer();
		if (users.putIfAbsent(player.getUniqueId(), new ModUser(player.getUniqueId(), player.getName(), ModUser.UNKNOWN_VERSION)) == null) {
			detected(player, ModUser.UNKNOWN_VERSION);
		}
		refresh(player);
	}

	@EventHandler
	public void onWorldChange(PlayerChangedWorldEvent event) {
		if (isUsingMod(event.getPlayer())) {
			refresh(event.getPlayer());
		}
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		users.remove(event.getPlayer().getUniqueId());
		playerPolicies.remove(event.getPlayer().getUniqueId());
	}

	private void detected(Player player, String version) {
		getServer().getPluginManager().callEvent(new WaypointModDetectedEvent(player, version));
		if (settings.logUsers()) {
			getLogger().info(player.getName() + " uses Optimal Waypoints " + version);
		}
		if (!settings.notifyStaff()) {
			return;
		}
		Component notice = Text.prefix()
			.append(Component.text(player.getName(), Text.BODY))
			.append(Component.text(" uses Optimal Waypoints", Text.MUTED))
			.hoverEvent(HoverEvent.showText(Component.text("Version " + version, Text.MUTED)));
		for (Player online : getServer().getOnlinePlayers()) {
			if (online != player && online.hasPermission("optimalwaypoints.notify")) {
				online.sendMessage(notice);
			}
		}
	}

	@Override
	public boolean isUsingMod(Player player) {
		return users.containsKey(player.getUniqueId());
	}

	@Override
	public Optional<String> modVersion(Player player) {
		return Optional.ofNullable(users.get(player.getUniqueId())).map(ModUser::version);
	}

	@Override
	public Collection<Player> modUsers() {
		return users.keySet().stream().map(getServer()::getPlayer).filter(Objects::nonNull).toList();
	}

	@Override
	public ServerPolicy globalPolicy() {
		return settings.global();
	}

	@Override
	public void setGlobalPolicy(ServerPolicy policy, boolean saveToConfig) {
		PolicySettings.write(getConfig(), policy);
		if (saveToConfig) {
			saveConfig();
		}
		settings = PolicySettings.load(getConfig());
		refreshAll();
	}

	@Override
	public Optional<ServerPolicy> playerPolicy(Player player) {
		return Optional.ofNullable(playerPolicies.get(player.getUniqueId()));
	}

	@Override
	public void setPlayerPolicy(Player player, ServerPolicy policy) {
		playerPolicies.put(player.getUniqueId(), Objects.requireNonNull(policy));
		refresh(player);
	}

	@Override
	public void clearPlayerPolicy(Player player) {
		if (playerPolicies.remove(player.getUniqueId()) != null) {
			refresh(player);
		}
	}

	@Override
	public ServerPolicy effectivePolicy(Player player) {
		ServerPolicy policy;
		if (player.hasPermission("optimalwaypoints.bypass")) {
			policy = ServerPolicy.ALLOW_ALL;
		} else {
			policy = playerPolicies.getOrDefault(player.getUniqueId(), settings.forWorld(player.getWorld().getName()));
		}
		WaypointPolicySendEvent event = new WaypointPolicySendEvent(player, policy);
		getServer().getPluginManager().callEvent(event);
		return event.getPolicy();
	}

	@Override
	public void refresh(Player player) {
		if (player.getListeningPluginChannels().contains(Protocol.POLICY_CHANNEL)) {
			player.sendPluginMessage(this, Protocol.POLICY_CHANNEL, Protocol.encodePolicy(effectivePolicy(player)));
		}
	}

	void refreshAll() {
		for (Player player : getServer().getOnlinePlayers()) {
			refresh(player);
		}
	}

	void reload() {
		reloadConfig();
		settings = PolicySettings.load(getConfig());
		refreshAll();
	}

	Collection<ModUser> users() {
		return List.copyOf(users.values());
	}

	ModUser user(UUID uuid) {
		return users.get(uuid);
	}
}
