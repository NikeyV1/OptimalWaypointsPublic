package dev.nikey.optimalwaypoints.paper;

import dev.nikey.optimalwaypoints.protocol.Feature;
import dev.nikey.optimalwaypoints.protocol.ServerPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

final class AdminCommand implements TabExecutor {
	private static final List<String> SUBCOMMANDS = List.of("users", "check", "policy", "maxdistance", "message", "reload");

	private final OptimalWaypointsPlugin plugin;

	AdminCommand(OptimalWaypointsPlugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		switch (args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT)) {
			case "users" -> users(sender);
			case "check" -> check(sender, args);
			case "policy" -> policy(sender, args);
			case "maxdistance" -> maxDistance(sender, args);
			case "message" -> message(sender, args);
			case "reload" -> {
				plugin.reload();
				sender.sendMessage(Text.success("Config reloaded."));
			}
			default -> sender.sendMessage(Text.line("/" + label + " <" + String.join("|", SUBCOMMANDS) + ">"));
		}
		return true;
	}

	private void users(CommandSender sender) {
		Collection<ModUser> users = plugin.users();
		sender.sendMessage(Text.line(users.size() + " of " + plugin.getServer().getOnlinePlayers().size() + " online players use Optimal Waypoints"));
		for (ModUser user : users) {
			sender.sendMessage(Component.text("  • ", Text.ACCENT).append(Component.text(user.name(), Text.BODY))
				.append(Component.text("  " + user.version(), Text.MUTED)));
		}
	}

	private void check(CommandSender sender, String[] args) {
		if (args.length < 2) {
			sender.sendMessage(Text.error("Usage: /optimalwaypoints check <player>"));
			return;
		}
		Player player = plugin.getServer().getPlayerExact(args[1]);
		if (player == null) {
			sender.sendMessage(Text.error(args[1] + " is not online."));
			return;
		}
		ModUser user = plugin.user(player.getUniqueId());
		sender.sendMessage(user == null
			? Text.line(player.getName() + " does not use Optimal Waypoints.")
			: Text.success(player.getName() + " uses Optimal Waypoints " + user.version() + "."));
	}

	private void policy(CommandSender sender, String[] args) {
		ServerPolicy policy = plugin.globalPolicy();
		if (args.length < 3) {
			sender.sendMessage(Text.line("Global rules"));
			for (Feature feature : Feature.values()) {
				sender.sendMessage(Text.state(feature.key(), !policy.isDisabled(feature)));
			}
			String distance = policy.maxRenderDistance() == 0 ? "unlimited" : policy.maxRenderDistance() + " blocks";
			sender.sendMessage(Component.text("  Max distance: ", Text.MUTED).append(Component.text(distance, Text.BODY)));
			return;
		}
		Feature feature = Feature.byKey(args[1]);
		if (feature == null || !(args[2].equalsIgnoreCase("true") || args[2].equalsIgnoreCase("false"))) {
			sender.sendMessage(Text.error("Usage: /optimalwaypoints policy <feature> <true|false>"));
			return;
		}
		boolean allowed = Boolean.parseBoolean(args[2]);
		plugin.setGlobalPolicy(policy.with(feature, allowed), true);
		sender.sendMessage(Text.success(feature.key() + " is now " + (allowed ? "allowed" : "disabled") + "."));
	}

	private void maxDistance(CommandSender sender, String[] args) {
		int blocks;
		try {
			blocks = Math.max(0, Integer.parseInt(args.length < 2 ? "" : args[1]));
		} catch (NumberFormatException e) {
			sender.sendMessage(Text.error("Usage: /optimalwaypoints maxdistance <blocks> (0 = unlimited)"));
			return;
		}
		plugin.setGlobalPolicy(plugin.globalPolicy().withMaxRenderDistance(blocks), true);
		sender.sendMessage(Text.success(blocks == 0 ? "Max distance removed." : "Max distance set to " + blocks + " blocks."));
	}

	private void message(CommandSender sender, String[] args) {
		String text = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
		plugin.setGlobalPolicy(plugin.globalPolicy().withMessage(text), true);
		sender.sendMessage(Text.success(text.isEmpty() ? "Message cleared." : "Message set."));
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		List<String> options = new ArrayList<>();
		if (args.length == 1) {
			options.addAll(SUBCOMMANDS);
		} else if (args.length == 2 && args[0].equalsIgnoreCase("policy")) {
			Arrays.stream(Feature.values()).map(Feature::key).forEach(options::add);
		} else if (args.length == 3 && args[0].equalsIgnoreCase("policy")) {
			options.addAll(List.of("true", "false"));
		} else if (args.length == 2 && args[0].equalsIgnoreCase("check")) {
			plugin.getServer().getOnlinePlayers().forEach(p -> options.add(p.getName()));
		}
		String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
		options.removeIf(option -> !option.toLowerCase(Locale.ROOT).startsWith(prefix));
		return options;
	}
}
