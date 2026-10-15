package dev.nikey.optimalwaypoints.config.option;

import dev.nikey.optimalwaypoints.protocol.Feature;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.network.chat.Component;

public final class EnumOption<E extends Enum<E>> extends ConfigOption<E> {
	private final Class<E> type;
	private final Supplier<E> getter;
	private final Consumer<E> setter;
	private final Function<E, Component> namer;
	private final E defaultValue;

	public EnumOption(String key, Feature feature, Class<E> type, E defaultValue, Supplier<E> getter, Consumer<E> setter, Function<E, Component> namer) {
		super(key, feature);
		this.type = type;
		this.getter = getter;
		this.setter = setter;
		this.namer = namer;
		this.defaultValue = defaultValue;
	}

	@Override
	protected AbstractWidget buildWidget(int x, int y, int width, Runnable onChange) {
		return CycleButton.builder(namer, getter.get())
			.withValues(type.getEnumConstants())
			.create(x, y, width, 20, label(), (button, value) -> {
				setter.accept(value);
				onChange.run();
			});
	}

	@Override
	public void reset() {
		setter.accept(defaultValue);
	}
}
