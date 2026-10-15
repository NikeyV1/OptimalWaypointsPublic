package dev.nikey.optimalwaypoints.config.option;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;

public final class OptionCategory {
	private final String key;
	private final List<ConfigOption<?>> options = new ArrayList<>();

	public OptionCategory(String key) {
		this.key = key;
	}

	public String key() {
		return key;
	}

	public Component title() {
		return Component.translatable("optimalwaypoints.category." + key);
	}

	public OptionCategory add(ConfigOption<?> option) {
		options.add(option);
		return this;
	}

	public List<ConfigOption<?>> options() {
		return options;
	}
}
