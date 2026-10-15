package dev.nikey.optimalwaypoints.feature;

import dev.nikey.optimalwaypoints.OwpClient;
import dev.nikey.optimalwaypoints.api.ClientModule;
import dev.nikey.optimalwaypoints.feature.share.ChatCoordinateParser;
import dev.nikey.optimalwaypoints.feature.share.SharedWaypoint;
import dev.nikey.optimalwaypoints.feature.share.WaypointShareCodec;
import dev.nikey.optimalwaypoints.protocol.Feature;
import dev.nikey.optimalwaypoints.ui.Messages;
import dev.nikey.optimalwaypoints.waypoint.Waypoint;
import dev.nikey.optimalwaypoints.waypoint.WaypointColors;
import java.util.Optional;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

public final class ChatModule implements ClientModule {
	private static final long OWN_SHARE_WINDOW_MS = 10_000L;

	private static SharedWaypoint lastShared;
	private static long lastSharedAt;

	@Override
	public String id() {
		return "chat";
	}

	@Override
	public void init(OwpClient client) {
		ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signed, sender, params, timestamp) ->
			!replaceShare(client, message.getString(), sender == null ? null : sender.name()));
		ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> overlay || !replaceShare(client, message.getString(), null));
		ClientReceiveMessageEvents.CHAT.register((message, signed, sender, params, timestamp) ->
			offerCoordinates(client, message.getString(), sender == null ? null : sender.name()));
	}

	private static boolean replaceShare(OwpClient client, String text, String sender) {
		if (!client.usable() || text == null || !client.policy().allows(Feature.SHARING)) {
			return false;
		}
		String clean = text.replaceAll("§.", "");
		Optional<WaypointShareCodec.Match> match = WaypointShareCodec.locate(clean);
		if (match.isEmpty()) {
			return false;
		}
		SharedWaypoint shared = match.get().waypoint();
		String name = sender != null ? sender : senderBefore(clean, match.get().start());
		if (isOwnShare(shared, name)) {
			addLocal(Messages.success(Component.translatable("optimalwaypoints.share.sent", nameComponent(shared))));
		} else {
			offerShared(shared, name, match.get().token());
		}
		return true;
	}

	private static void offerCoordinates(OwpClient client, String text, String sender) {
		if (!client.usable() || text == null || isSelf(sender) || !client.config().chatCoordinates || !client.policy().allows(Feature.CHAT_COORDINATES)) {
			return;
		}
		ChatCoordinateParser.find(text).ifPresent(coords -> offerCoordinates(coords, sender));
	}

	private static String senderBefore(String text, int tokenStart) {
		String[] words = text.substring(0, tokenStart).trim().split("\\s+");
		for (int i = words.length - 1; i >= 0; i--) {
			String word = words[i].replaceAll("[^A-Za-z0-9_]", "");
			if (word.length() >= 3 && word.length() <= 16) {
				return word;
			}
		}
		return null;
	}

	private static boolean isSelf(String sender) {
		Minecraft mc = Minecraft.getInstance();
		return sender != null && mc.player != null && sender.equals(mc.player.getGameProfile().name());
	}

	private static boolean isOwnShare(SharedWaypoint shared, String sender) {
		if (isSelf(sender)) {
			return true;
		}
		return shared.equals(lastShared) && System.currentTimeMillis() - lastSharedAt < OWN_SHARE_WINDOW_MS;
	}

	private static Component nameComponent(SharedWaypoint waypoint) {
		String coords = waypoint.x() + ", " + waypoint.y() + ", " + waypoint.z();
		return Component.literal(waypoint.name()).withStyle(style -> style.withColor(readable(waypoint.color()))
			.withHoverEvent(new HoverEvent.ShowText(Component.literal(coords))));
	}

	private static void offerShared(SharedWaypoint waypoint, String sender, String token) {
		Component who = sender == null ? Component.translatable("optimalwaypoints.share.someone") : Messages.value(sender);
		String coords = waypoint.x() + ", " + waypoint.y() + ", " + waypoint.z();
		addLocal(Messages.line(Component.translatable("optimalwaypoints.share.received", who, nameComponent(waypoint)))
			.append("  ")
			.append(Messages.button(Component.translatable("optimalwaypoints.share.add"), "/owp addshared " + token,
				Component.translatable("optimalwaypoints.share.add.tooltip", coords))));
	}

	private static void offerCoordinates(ChatCoordinateParser.Coordinates coords, String sender) {
		String y = coords.y() == null ? "~" : String.valueOf(coords.y());
		String label = sender == null ? "Chat" : sender;
		Component who = sender == null ? Component.translatable("optimalwaypoints.share.someone") : Messages.value(sender);
		addLocal(Messages.line(Component.translatable("optimalwaypoints.share.coordinates", who,
				Messages.value(coords.x() + ", " + y + ", " + coords.z())))
			.append("  ")
			.append(Messages.button(Component.translatable("optimalwaypoints.share.add_temporary"),
				"/owp coords " + coords.x() + " " + y + " " + coords.z() + " " + label,
				Component.translatable("optimalwaypoints.share.add_temporary.tooltip"))));
	}

	private static int readable(int rgb) {
		return WaypointColors.luminance(rgb) < 0.25F ? Messages.HIGHLIGHT : rgb;
	}

	private static void addLocal(Component line) {
		Minecraft mc = Minecraft.getInstance();
		mc.execute(() -> mc.gui.hud.getChat().addServerSystemMessage(line));
	}

	public static void share(Waypoint waypoint) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return;
		}
		String message = WaypointShareCodec.encode(waypoint);
		lastShared = WaypointShareCodec.find(message).orElse(null);
		lastSharedAt = System.currentTimeMillis();
		mc.gui.hud.getChat().addRecentChat(message);
		mc.player.connection.sendChat(message);
	}
}
