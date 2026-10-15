package dev.nikey.optimalwaypoints.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

public final class Messages {
	public static final int ACCENT = 0x5EC8E5;
	public static final int BODY = 0xC8C8C8;
	public static final int HIGHLIGHT = 0xFFFFFF;
	public static final int MUTED = 0x8A8A8A;
	public static final int SEPARATOR = 0x555555;
	public static final int GOOD = 0x7BD88F;
	public static final int WARN = 0xF2C14E;
	public static final int BAD = 0xF07178;

	private Messages() {
	}

	public static MutableComponent prefix() {
		return Component.literal("Waypoints").withColor(ACCENT).append(Component.literal(" » ").withColor(SEPARATOR));
	}

	public static MutableComponent value(Object value) {
		return Component.literal(String.valueOf(value)).withColor(HIGHLIGHT);
	}

	public static MutableComponent muted(Component text) {
		return text.copy().withColor(MUTED);
	}

	public static MutableComponent line(Component body) {
		return prefix().append(body.copy().withColor(BODY));
	}

	public static MutableComponent success(Component body) {
		return symbol("✔ ", GOOD, body);
	}

	public static MutableComponent warning(Component body) {
		return symbol("⚠ ", WARN, body);
	}

	public static MutableComponent error(Component body) {
		return symbol("✖ ", BAD, body);
	}

	private static MutableComponent symbol(String symbol, int color, Component body) {
		return prefix().append(Component.literal(symbol).withColor(color)).append(body.copy().withColor(BODY));
	}

	public static MutableComponent button(Component label, String command, Component hover) {
		return Component.literal("[").append(label).append("]").withStyle(style -> style
			.withColor(GOOD)
			.withClickEvent(new ClickEvent.RunCommand(command))
			.withHoverEvent(new HoverEvent.ShowText(hover)));
	}

	public static void chat(Component message) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) {
			mc.player.sendSystemMessage(message);
		}
	}

	public static void actionBar(int color, String symbol, Component body) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) {
			mc.player.sendOverlayMessage(Component.literal(symbol + " ").withColor(color).append(body.copy().withColor(BODY)));
		}
	}

	public static void toast(Component body) {
		actionBar(ACCENT, "◆", body);
	}

	public static void toastSuccess(Component body) {
		actionBar(GOOD, "✔", body);
	}

	public static void toastError(Component body) {
		actionBar(BAD, "✖", body);
	}
}
