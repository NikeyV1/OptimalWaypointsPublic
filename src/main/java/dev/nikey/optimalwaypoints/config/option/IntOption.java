package dev.nikey.optimalwaypoints.config.option;

import dev.nikey.optimalwaypoints.protocol.Feature;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public final class IntOption extends ConfigOption<Integer> {
	private final int min;
	private final int max;
	private final int step;
	private final int defaultValue;
	private final Supplier<Integer> getter;
	private final Consumer<Integer> setter;
	private final IntFunction<Component> formatter;

	public IntOption(String key, Feature feature, int min, int max, int step, int defaultValue,
		Supplier<Integer> getter, Consumer<Integer> setter, IntFunction<Component> formatter) {
		super(key, feature);
		this.min = min;
		this.max = max;
		this.step = Math.max(1, step);
		this.defaultValue = defaultValue;
		this.getter = getter;
		this.setter = setter;
		this.formatter = formatter;
	}

	public static IntFunction<Component> percent() {
		return value -> Component.literal(value + "%");
	}

	public static IntFunction<Component> blocksOrUnlimited() {
		return value -> value <= 0 ? Component.translatable("optimalwaypoints.unit.unlimited") : Component.literal(value + "m");
	}

	public static IntFunction<Component> tenths() {
		return value -> Component.literal(String.format(java.util.Locale.ROOT, "%.1f", value / 10.0));
	}

	private double toSlider(int value) {
		return max == min ? 0 : (double) (Mth.clamp(value, min, max) - min) / (max - min);
	}

	private int fromSlider(double slider) {
		int raw = (int) Math.round(min + slider * (max - min));
		int snapped = min + Math.round((raw - min) / (float) step) * step;
		return Mth.clamp(snapped, min, max);
	}

	@Override
	protected AbstractWidget buildWidget(int x, int y, int width, Runnable onChange) {
		IntOption option = this;
		return new AbstractSliderButton(x, y, width, 20, Component.empty(), toSlider(getter.get())) {
			{
				updateMessage();
			}

			@Override
			protected void updateMessage() {
				setMessage(CommonComponents.optionNameValue(option.label(), formatter.apply(fromSlider(value))));
			}

			@Override
			protected void applyValue() {
				setter.accept(fromSlider(value));
				onChange.run();
			}
		};
	}

	@Override
	public void reset() {
		setter.accept(defaultValue);
	}
}
