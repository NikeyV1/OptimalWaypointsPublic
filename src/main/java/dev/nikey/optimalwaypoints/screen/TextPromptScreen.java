package dev.nikey.optimalwaypoints.screen;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public final class TextPromptScreen extends Screen {
	private final Screen parent;
	private final String initial;
	private final Consumer<String> onConfirm;
	private EditBox input;

	public TextPromptScreen(Screen parent, Component title, String initial, Consumer<String> onConfirm) {
		super(title);
		this.parent = parent;
		this.initial = initial;
		this.onConfirm = onConfirm;
	}

	@Override
	protected void init() {
		input = new EditBox(font, width / 2 - 100, height / 2 - 20, 200, 20, title);
		input.setMaxLength(48);
		input.setValue(initial);
		addRenderableWidget(input);
		setInitialFocus(input);
		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> confirm()).bounds(width / 2 - 100, height / 2 + 8, 98, 20).build());
		addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> onClose()).bounds(width / 2 + 2, height / 2 + 8, 98, 20).build());
	}

	private void confirm() {
		String value = input.getValue().trim();
		if (!value.isEmpty()) {
			onConfirm.accept(value);
		}
		onClose();
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_NUMPADENTER) {
			confirm();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		graphics.centeredText(font, title, width / 2, height / 2 - 36, 0xFFFFFFFF);
	}

	@Override
	public void onClose() {
		minecraft.gui.setScreen(parent);
	}
}
