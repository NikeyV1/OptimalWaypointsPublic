package dev.nikey.optimalwaypoints.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.nikey.optimalwaypoints.OwpClient;
import dev.nikey.optimalwaypoints.feature.share.SharedWaypoint;
import dev.nikey.optimalwaypoints.feature.share.WaypointShareCodec;
import dev.nikey.optimalwaypoints.policy.ClientPolicy;
import dev.nikey.optimalwaypoints.protocol.Feature;
import dev.nikey.optimalwaypoints.screen.ScreenOpener;
import dev.nikey.optimalwaypoints.screen.SettingsScreen;
import dev.nikey.optimalwaypoints.screen.WaypointListScreen;
import dev.nikey.optimalwaypoints.ui.Messages;
import dev.nikey.optimalwaypoints.waypoint.Waypoint;
import dev.nikey.optimalwaypoints.waypoint.WaypointColors;
import dev.nikey.optimalwaypoints.waypoint.WaypointKind;
import dev.nikey.optimalwaypoints.waypoint.io.XaeroImporter;
import java.util.Optional;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public final class OwpClientCommands {
	private OwpClientCommands() {
	}

	public static void register() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> register(dispatcher));
	}

	private static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		dispatcher.register(literal("owp")
			.executes(ctx -> open(ctx, false))
			.then(literal("menu").executes(ctx -> open(ctx, false)))
			.then(literal("settings").executes(ctx -> open(ctx, true)))
			.then(literal("here")
				.then(argument("name", StringArgumentType.greedyString()).executes(ctx -> addHere(ctx, StringArgumentType.getString(ctx, "name")))))
			.then(literal("add")
				.then(argument("x", IntegerArgumentType.integer())
					.then(argument("y", IntegerArgumentType.integer())
						.then(argument("z", IntegerArgumentType.integer())
							.then(argument("name", StringArgumentType.greedyString()).executes(OwpClientCommands::addAt))))))
			.then(literal("coords")
				.then(argument("x", IntegerArgumentType.integer())
					.then(argument("y", StringArgumentType.word())
						.then(argument("z", IntegerArgumentType.integer())
							.executes(ctx -> addTemporary(ctx, "Chat"))
							.then(argument("label", StringArgumentType.greedyString())
								.executes(ctx -> addTemporary(ctx, StringArgumentType.getString(ctx, "label"))))))))
			.then(literal("addshared")
				.then(argument("token", StringArgumentType.greedyString()).executes(OwpClientCommands::addShared)))
			.then(literal("import")
				.then(literal("xaero")
					.executes(ctx -> importXaero(ctx, XaeroImporter.guessContainer(OwpClient.get().waypoints().key())))
					.then(argument("container", StringArgumentType.greedyString())
						.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(XaeroImporter.containers(), builder))
						.executes(ctx -> importXaero(ctx, StringArgumentType.getString(ctx, "container"))))))
			.then(literal("status").executes(OwpClientCommands::status)));
	}

	private static boolean ready(CommandContext<FabricClientCommandSource> ctx) {
		if (!OwpClient.get().usable()) {
			ctx.getSource().sendFeedback(Messages.error(Component.translatable(OwpClient.get().policy().modAllowed()
				? "optimalwaypoints.command.not_ready" : "optimalwaypoints.policy.mod_disabled")));
			return false;
		}
		return true;
	}

	private static int open(CommandContext<FabricClientCommandSource> ctx, boolean settings) {
		if (!ready(ctx)) {
			return 0;
		}
		ScreenOpener.openLater(() -> settings ? new SettingsScreen(null) : new WaypointListScreen(null));
		return 1;
	}

	private static int addHere(CommandContext<FabricClientCommandSource> ctx, String name) {
		if (!ready(ctx)) {
			return 0;
		}
		Waypoint waypoint = ScreenOpener.createAtPlayer();
		waypoint.setName(name);
		waypoint.setInitials(Waypoint.deriveInitials(name));
		return finishAdd(ctx, waypoint);
	}

	private static int addAt(CommandContext<FabricClientCommandSource> ctx) {
		if (!ready(ctx)) {
			return 0;
		}
		String name = StringArgumentType.getString(ctx, "name");
		Waypoint waypoint = new Waypoint(name, IntegerArgumentType.getInteger(ctx, "x"), IntegerArgumentType.getInteger(ctx, "y"),
			IntegerArgumentType.getInteger(ctx, "z"), OwpClient.get().currentDimension(), WaypointColors.random(), WaypointKind.STANDARD);
		return finishAdd(ctx, waypoint);
	}

	private static int addTemporary(CommandContext<FabricClientCommandSource> ctx, String label) {
		if (!ready(ctx)) {
			return 0;
		}
		if (!OwpClient.get().policy().allows(Feature.CHAT_COORDINATES)) {
			ctx.getSource().sendFeedback(Messages.error(Component.translatable("optimalwaypoints.gui.blocked_by_server")));
			return 0;
		}
		String yRaw = StringArgumentType.getString(ctx, "y");
		int y;
		try {
			y = "~".equals(yRaw) ? ctx.getSource().getPlayer().blockPosition().getY() : Integer.parseInt(yRaw);
		} catch (NumberFormatException e) {
			ctx.getSource().sendFeedback(Messages.error(Component.translatable("optimalwaypoints.command.invalid_number", yRaw)));
			return 0;
		}
		Waypoint waypoint = new Waypoint(label, IntegerArgumentType.getInteger(ctx, "x"), y, IntegerArgumentType.getInteger(ctx, "z"),
			OwpClient.get().currentDimension(), WaypointColors.random(), WaypointKind.STANDARD);
		waypoint.setDeleteOnArrival(true);
		return finishAdd(ctx, waypoint);
	}

	private static int addShared(CommandContext<FabricClientCommandSource> ctx) {
		if (!ready(ctx)) {
			return 0;
		}
		if (!OwpClient.get().policy().allows(Feature.SHARING)) {
			ctx.getSource().sendFeedback(Messages.error(Component.translatable("optimalwaypoints.gui.blocked_by_server")));
			return 0;
		}
		Optional<SharedWaypoint> decoded = WaypointShareCodec.decode(StringArgumentType.getString(ctx, "token").trim());
		if (decoded.isEmpty()) {
			ctx.getSource().sendFeedback(Messages.error(Component.translatable("optimalwaypoints.share.invalid")));
			return 0;
		}
		return finishAdd(ctx, decoded.get().toWaypoint(OwpClient.get().currentDimension()));
	}

	private static int finishAdd(CommandContext<FabricClientCommandSource> ctx, Waypoint waypoint) {
		OwpClient client = OwpClient.get();
		client.waypoints().add(waypoint);
		ctx.getSource().sendFeedback(Messages.success(Component.translatable("optimalwaypoints.waypoint.created", Messages.value(waypoint.name())))
			.append(Component.literal("  " + waypoint.x() + ", " + waypoint.y() + ", " + waypoint.z()).withColor(Messages.MUTED)));
		return 1;
	}

	private static int importXaero(CommandContext<FabricClientCommandSource> ctx, String container) {
		if (!ready(ctx)) {
			return 0;
		}
		if (container == null || !XaeroImporter.containers().contains(container)) {
			ctx.getSource().sendFeedback(Messages.error(Component.translatable("optimalwaypoints.import.not_found", container == null ? "-" : container)));
			return 0;
		}
		XaeroImporter.Result result = XaeroImporter.importContainer(container, OwpClient.get().waypoints());
		ctx.getSource().sendFeedback(Messages.success(Component.translatable("optimalwaypoints.import.done",
			Messages.value(result.imported()), Messages.value(result.skipped()), Messages.value(result.files()))));
		return result.imported();
	}

	private static int status(CommandContext<FabricClientCommandSource> ctx) {
		ClientPolicy policy = OwpClient.get().policy();
		if (!policy.serverManaged()) {
			ctx.getSource().sendFeedback(Messages.line(Component.translatable("optimalwaypoints.status.no_server_mod")));
			return 1;
		}
		ctx.getSource().sendFeedback(Messages.line(Component.translatable("optimalwaypoints.status.server_mod")));
		ctx.getSource().sendFeedback(ClientPolicy.details(policy.current()));
		return 1;
	}
}
