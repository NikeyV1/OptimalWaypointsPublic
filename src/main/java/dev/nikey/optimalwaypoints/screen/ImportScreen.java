package dev.nikey.optimalwaypoints.screen;

import dev.nikey.optimalwaypoints.OwpClient;
import dev.nikey.optimalwaypoints.feature.share.SharedWaypoint;
import dev.nikey.optimalwaypoints.feature.share.WaypointShareCodec;
import dev.nikey.optimalwaypoints.protocol.Feature;
import dev.nikey.optimalwaypoints.ui.Messages;
import dev.nikey.optimalwaypoints.waypoint.Waypoint;
import dev.nikey.optimalwaypoints.waypoint.io.XaeroImporter;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public final class ImportScreen extends Screen {
	private final Screen parent;
	private EditBox codeBox;
	private Button addButton;
	private Component sharedStatus = Component.empty();
	private Component xaeroStatus = Component.empty();
	private String container;
	private int top;

	public ImportScreen(Screen parent) {
		super(Component.translatable("optimalwaypoints.gui.import.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int left = width / 2 - 150;
		top = Math.max(32, height / 2 - 70);

		codeBox = addRenderableWidget(new EditBox(font, left, top + 12, 230, 20, Component.translatable("optimalwaypoints.gui.import.shared")));
		codeBox.setMaxLength(512);
		codeBox.setHint(Component.translatable("optimalwaypoints.gui.import.shared.hint"));
		String clipboard = minecraft.keyboardHandler.getClipboard();
		if (codeBox.getValue().isEmpty() && WaypointShareCodec.find(clipboard).isPresent()) {
			codeBox.setValue(clipboard.trim());
		}
		codeBox.setResponder(value -> updateShared());
		addButton = addRenderableWidget(Button.builder(Component.translatable("optimalwaypoints.gui.import.add"), b -> addShared())
			.bounds(left + 234, top + 12, 66, 20).build());
		updateShared();

		List<String> containers = XaeroImporter.containers();
		if (container == null || !containers.contains(container)) {
			String guess = XaeroImporter.guessContainer(OwpClient.get().waypoints().key());
			container = containers.contains(guess) ? guess : containers.isEmpty() ? null : containers.getFirst();
		}
		if (container == null) {
			xaeroStatus = Component.translatable("optimalwaypoints.gui.import.xaero.none").withColor(Messages.MUTED);
		} else {
			addRenderableWidget(CycleButton.builder((String name) -> Component.literal(name), container).withValues(containers).displayOnlyValue()
				.create(left, top + 76, 230, 20, Component.translatable("optimalwaypoints.gui.import.xaero"), (b, value) -> container = value));
			addRenderableWidget(Button.builder(Component.translatable("optimalwaypoints.gui.import.run"), b -> importXaero())
				.bounds(left + 234, top + 76, 66, 20).tooltip(Tooltip.create(Component.translatable("optimalwaypoints.gui.import.xaero.tooltip"))).build());
		}

		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose()).bounds(width / 2 - 100, height - 29, 200, 20).build());
	}

	private Optional<SharedWaypoint> decoded() {
		return WaypointShareCodec.find(codeBox.getValue());
	}

	private void updateShared() {
		boolean allowed = OwpClient.get().policy().allows(Feature.SHARING);
		Optional<SharedWaypoint> shared = decoded();
		addButton.active = allowed && shared.isPresent();
		if (!allowed) {
			sharedStatus = Component.translatable("optimalwaypoints.gui.blocked_by_server").withColor(Messages.BAD);
		} else if (shared.isPresent()) {
			SharedWaypoint w = shared.get();
			sharedStatus = Component.literal(w.name()).withColor(Messages.HIGHLIGHT)
				.append(Component.literal("  " + w.x() + ", " + w.y() + ", " + w.z()).withColor(Messages.MUTED));
		} else {
			sharedStatus = codeBox.getValue().isBlank() ? Component.empty()
				: Component.translatable("optimalwaypoints.share.invalid").withColor(Messages.BAD);
		}
	}

	private void addShared() {
		Optional<SharedWaypoint> shared = decoded();
		if (shared.isEmpty() || !OwpClient.get().policy().allows(Feature.SHARING)) {
			return;
		}
		Waypoint waypoint = shared.get().toWaypoint(OwpClient.get().currentDimension());
		OwpClient.get().waypoints().add(waypoint);
		codeBox.setValue("");
		sharedStatus = Component.translatable("optimalwaypoints.waypoint.created", waypoint.name()).withColor(Messages.GOOD);
	}

	private void importXaero() {
		if (container == null) {
			return;
		}
		XaeroImporter.Result result = XaeroImporter.importContainer(container, OwpClient.get().waypoints());
		xaeroStatus = Component.translatable("optimalwaypoints.import.done", result.imported(), result.skipped(), result.files())
			.withColor(result.imported() > 0 ? Messages.GOOD : Messages.BODY);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		int left = width / 2 - 150;
		graphics.centeredText(font, title, width / 2, 12, 0xFFFFFFFF);
		graphics.text(font, Component.translatable("optimalwaypoints.gui.import.shared"), left, top, 0xFFA0A0A0);
		graphics.text(font, sharedStatus, left, top + 37, 0xFFFFFFFF);
		graphics.text(font, Component.translatable("optimalwaypoints.gui.import.xaero"), left, top + 64, 0xFFA0A0A0);
		graphics.text(font, xaeroStatus, left, container == null ? top + 78 : top + 101, 0xFFFFFFFF);
	}

	@Override
	public void onClose() {
		minecraft.gui.setScreen(parent);
	}
}
