package dev.nikey.optimalwaypoints.screen;

import dev.nikey.optimalwaypoints.OwpClient;
import dev.nikey.optimalwaypoints.config.option.ConfigOption;
import dev.nikey.optimalwaypoints.config.option.OptionCategory;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public final class SettingsScreen extends Screen {
	private final Screen parent;

	public SettingsScreen(Screen parent) {
		super(Component.translatable("optimalwaypoints.gui.settings"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		OwpClient client = OwpClient.get();
		OptionList list = new OptionList(minecraft, width, height - 32 - 36, 32);
		for (OptionCategory category : client.options().categories()) {
			list.addHeader(category.title());
			List<ConfigOption<?>> options = category.options();
			for (int i = 0; i < options.size(); i += 2) {
				List<AbstractWidget> row = new ArrayList<>(2);
				row.add(options.get(i).createWidget(0, 0, 150, client.configManager()::save));
				if (i + 1 < options.size()) {
					row.add(options.get(i + 1).createWidget(0, 0, 150, client.configManager()::save));
				}
				list.addRow(row);
			}
		}
		addRenderableWidget(list);
		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose()).bounds(width / 2 - 100, height - 28, 200, 20).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		graphics.centeredText(font, title, width / 2, 12, 0xFFFFFFFF);
	}

	@Override
	public void onClose() {
		OwpClient.get().configManager().save();
		minecraft.gui.setScreen(parent);
	}

	private final class OptionList extends ContainerObjectSelectionList<OptionList.Row> {
		OptionList(Minecraft mc, int width, int height, int top) {
			super(mc, width, height, top, 24);
		}

		void addHeader(Component title) {
			addEntry(new Row(List.of(), title.copy().withStyle(ChatFormatting.YELLOW)));
		}

		void addRow(List<AbstractWidget> widgets) {
			addEntry(new Row(widgets, null));
		}

		@Override
		public int getRowWidth() {
			return 310;
		}

		final class Row extends ContainerObjectSelectionList.Entry<Row> {
			private final List<AbstractWidget> widgets;
			private final Component header;

			Row(List<AbstractWidget> widgets, Component header) {
				this.widgets = widgets;
				this.header = header;
			}

			@Override
			public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
				if (header != null) {
					graphics.centeredText(font, header, getContentXMiddle(), getContentY() + 8, 0xFFFFFFFF);
					return;
				}
				int x = getContentX();
				for (AbstractWidget widget : widgets) {
					widget.setX(x);
					widget.setY(getContentY());
					widget.extractRenderState(graphics, mouseX, mouseY, partialTick);
					x += 160;
				}
			}

			@Override
			public List<? extends GuiEventListener> children() {
				return widgets;
			}

			@Override
			public List<? extends NarratableEntry> narratables() {
				return widgets;
			}
		}
	}
}
