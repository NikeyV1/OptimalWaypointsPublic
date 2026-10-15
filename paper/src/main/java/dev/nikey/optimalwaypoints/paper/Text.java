package dev.nikey.optimalwaypoints.paper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

final class Text {
	static final TextColor ACCENT = TextColor.color(0x5EC8E5);
	static final TextColor BODY = TextColor.color(0xE0E0E0);
	static final TextColor MUTED = TextColor.color(0x8A8A8A);
	static final TextColor GOOD = TextColor.color(0x7BD88F);
	static final TextColor BAD = TextColor.color(0xF07178);
	static final TextColor SEPARATOR = TextColor.color(0x555555);

	private Text() {
	}

	static Component prefix() {
		return Component.text("Waypoints", ACCENT).append(Component.text(" » ", SEPARATOR));
	}

	static Component line(String text) {
		return prefix().append(Component.text(text, BODY));
	}

	static Component success(String text) {
		return prefix().append(Component.text("✔ ", GOOD)).append(Component.text(text, BODY));
	}

	static Component error(String text) {
		return prefix().append(Component.text("✖ ", BAD)).append(Component.text(text, BODY));
	}

	static Component state(String label, boolean allowed) {
		return Component.text("  " + (allowed ? "✔ " : "✘ "), allowed ? GOOD : BAD).append(Component.text(label, allowed ? BODY : MUTED));
	}
}
