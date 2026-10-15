package dev.nikey.optimalwaypoints.config.option;

import dev.nikey.optimalwaypoints.OwpClient;
import dev.nikey.optimalwaypoints.protocol.Feature;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

public abstract class ConfigOption<T> {
	protected final String key;
	protected final Feature feature;

	protected ConfigOption(String key, Feature feature) {
		this.key = key;
		this.feature = feature;
	}

	public String key() {
		return key;
	}

	public Component label() {
		return Component.translatable("optimalwaypoints.option." + key);
	}

	public Component tooltip() {
		return Component.translatable("optimalwaypoints.option." + key + ".tooltip");
	}

	public boolean blockedByServer() {
		return feature != null && !OwpClient.get().policy().allows(feature);
	}

	public final AbstractWidget createWidget(int x, int y, int width, Runnable onChange) {
		AbstractWidget widget = buildWidget(x, y, width, onChange);
		if (blockedByServer()) {
			widget.active = false;
			widget.setTooltip(Tooltip.create(Component.translatable("optimalwaypoints.gui.blocked_by_server").withStyle(ChatFormatting.RED)));
		} else {
			widget.setTooltip(Tooltip.create(tooltip()));
		}
		return widget;
	}

	protected abstract AbstractWidget buildWidget(int x, int y, int width, Runnable onChange);

	public abstract void reset();
}
