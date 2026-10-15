package dev.nikey.optimalwaypoints.config.option;

import dev.nikey.optimalwaypoints.protocol.Feature;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;

public final class BooleanOption extends ConfigOption<Boolean> {
	private final Supplier<Boolean> getter;
	private final Consumer<Boolean> setter;
	private final boolean defaultValue;

	public BooleanOption(String key, Feature feature, boolean defaultValue, Supplier<Boolean> getter, Consumer<Boolean> setter) {
		super(key, feature);
		this.getter = getter;
		this.setter = setter;
		this.defaultValue = defaultValue;
	}

	@Override
	protected AbstractWidget buildWidget(int x, int y, int width, Runnable onChange) {
		return CycleButton.onOffBuilder(getter.get()).create(x, y, width, 20, label(), (button, value) -> {
			setter.accept(value);
			onChange.run();
		});
	}

	@Override
	public void reset() {
		setter.accept(defaultValue);
	}
}
