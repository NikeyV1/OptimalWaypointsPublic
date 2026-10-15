package dev.nikey.optimalwaypoints.screen;

import dev.nikey.optimalwaypoints.OwpClient;
import dev.nikey.optimalwaypoints.waypoint.Dimensions;
import dev.nikey.optimalwaypoints.waypoint.DisplayMode;
import dev.nikey.optimalwaypoints.waypoint.Waypoint;
import dev.nikey.optimalwaypoints.waypoint.WaypointColors;
import dev.nikey.optimalwaypoints.waypoint.WaypointSet;
import dev.nikey.optimalwaypoints.waypoint.WaypointWorld;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public final class WaypointEditScreen extends Screen {
	private static final int ERROR_COLOR = 0xFFFF5555;
	private static final int TEXT_COLOR = 0xFFE0E0E0;
	private static final int SWATCH_SIZE = 12;
	private static final int SWATCH_STEP = 13;

	private enum State {
		ENABLED,
		DISABLED,
		TEMPORARY;

		Component label() {
			return Component.translatable("optimalwaypoints.state." + name().toLowerCase(Locale.ROOT));
		}
	}

	private final Screen parent;
	private final Waypoint original;
	private final Waypoint draft;
	private final boolean isNew;
	private String targetSet;

	private EditBox nameBox;
	private EditBox initialsBox;
	private EditBox xBox;
	private EditBox yBox;
	private EditBox zBox;

	public WaypointEditScreen(Screen parent, Waypoint waypoint, boolean isNew) {
		super(Component.translatable(isNew ? "optimalwaypoints.gui.new_waypoint" : "optimalwaypoints.gui.edit_waypoint"));
		this.parent = parent;
		this.original = waypoint;
		this.draft = waypoint.copy();
		this.isNew = isNew;
		WaypointWorld world = OwpClient.get().waypoints().world();
		this.targetSet = world == null ? WaypointWorld.DEFAULT_SET : setOf(world, waypoint);
	}

	private static String setOf(WaypointWorld world, Waypoint waypoint) {
		for (WaypointSet set : world.sets()) {
			for (Waypoint w : set.waypoints()) {
				if (w.id().equals(waypoint.id())) {
					return set.name();
				}
			}
		}
		return world.currentSet().name();
	}

	@Override
	protected void init() {
		int center = width / 2;
		WaypointWorld world = OwpClient.get().waypoints().world();
		List<String> setNames = new ArrayList<>();
		if (world != null) {
			world.sets().forEach(set -> setNames.add(set.name()));
		}
		if (!setNames.contains(targetSet)) {
			setNames.add(targetSet);
		}
		addRenderableWidget(CycleButton.builder((String name) -> Component.literal(name), targetSet).withValues(setNames)
			.create(center - 101, 60, 201, 20, Component.translatable("optimalwaypoints.gui.set"), (b, v) -> targetSet = v));

		int swatchesX = center - WaypointColors.PALETTE.length * SWATCH_STEP / 2;
		for (int i = 0; i < WaypointColors.PALETTE.length; i++) {
			addRenderableWidget(new ColorSwatch(swatchesX + i * SWATCH_STEP, 84, WaypointColors.PALETTE[i]));
		}

		nameBox = box(center - 100, 104, 200, draft.name(), 32);
		nameBox.setHint(Component.translatable("optimalwaypoints.gui.name"));
		nameBox.setResponder(value -> {
			draft.setName(value);
			initialsBox.setHint(Component.literal(Waypoint.deriveInitials(value)));
		});
		xBox = box(center - 109, 134, 50, String.valueOf(draft.x()), 9);
		yBox = box(center - 53, 134, 50, String.valueOf(draft.y()), 6);
		zBox = box(center + 3, 134, 50, String.valueOf(draft.z()), 9);
		xBox.setHint(Component.literal("X"));
		yBox.setHint(Component.literal("Y"));
		zBox.setHint(Component.literal("Z"));
		boolean marker = draft.isBlockMarker();
		xBox.setEditable(!marker);
		yBox.setEditable(!marker);
		zBox.setEditable(!marker);
		addRenderableWidget(CycleButton.builder(Dimensions::name, draft.dimension())
			.withValues(Dimensions.known(OwpClient.get().currentDimension(), draft.dimension())).displayOnlyValue()
			.create(center + 59, 134, 72, 20, Component.translatable("optimalwaypoints.gui.dimension"), (b, v) -> draft.setDimension(v)));

		initialsBox = box(center - 25, 164, 50, draft.rawInitials(), 3);
		initialsBox.setHint(Component.literal(Waypoint.deriveInitials(draft.name())));
		setInitialFocus(nameBox);

		if (marker) {
			addRenderableWidget(CycleButton.builder((DisplayMode mode) -> Component.translatable(mode.translationKey()), draft.display())
				.withValues(DisplayMode.values()).displayOnlyValue()
				.create(center - 109, 164, 79, 20, Component.translatable("optimalwaypoints.gui.display"), (b, v) -> draft.setDisplay(v)));
			addRenderableWidget(CycleButton.booleanBuilder(Component.translatable("optimalwaypoints.gui.locked"), Component.translatable("optimalwaypoints.gui.unlocked"), draft.locked())
				.displayOnlyValue()
				.create(center + 31, 164, 79, 20, Component.translatable("optimalwaypoints.gui.locked"), (b, v) -> draft.setLocked(v)));
		} else {
			addRenderableWidget(CycleButton.booleanBuilder(Component.translatable("optimalwaypoints.gui.global"), Component.translatable("optimalwaypoints.gui.local"), draft.global())
				.displayOnlyValue()
				.create(center - 109, 164, 79, 20, Component.translatable("optimalwaypoints.gui.visibility"), (b, v) -> draft.setGlobal(v)));
			addRenderableWidget(CycleButton.builder(State::label, state()).withValues(State.values()).displayOnlyValue()
				.create(center + 31, 164, 79, 20, Component.translatable("optimalwaypoints.gui.state"), (b, v) -> applyState(v)));
		}

		int buttonsY = Math.min(height / 6 + 168, height - 26);
		addRenderableWidget(Button.builder(Component.translatable("optimalwaypoints.gui.confirm"), b -> save()).bounds(center - 155, buttonsY, 150, 20).build());
		addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> onClose()).bounds(center + 5, buttonsY, 150, 20).build());
	}

	private State state() {
		if (!draft.enabled()) {
			return State.DISABLED;
		}
		return draft.deleteOnArrival() ? State.TEMPORARY : State.ENABLED;
	}

	private void applyState(State state) {
		draft.setEnabled(state != State.DISABLED);
		draft.setDeleteOnArrival(state == State.TEMPORARY);
	}

	private static Component colorName(int rgb) {
		return Component.translatable("optimalwaypoints.color." + WaypointColors.NAMES[Math.max(0, WaypointColors.paletteIndex(rgb))]);
	}

	private final class ColorSwatch extends AbstractWidget {
		private final int rgb;

		ColorSwatch(int x, int y, int rgb) {
			super(x, y, SWATCH_SIZE, SWATCH_SIZE, colorName(rgb));
			this.rgb = rgb;
			setTooltip(Tooltip.create(getMessage()));
		}

		@Override
		protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
			boolean selected = draft.color() == rgb;
			int border = selected ? 0xFFFFFFFF : isHoveredOrFocused() ? 0xFFA0A0A0 : 0xFF000000;
			graphics.fill(getX() - 1, getY() - 1, getRight() + 1, getBottom() + 1, border);
			graphics.fill(getX(), getY(), getRight(), getBottom(), 0xFF000000 | rgb);
			if (selected) {
				graphics.fill(getX() + 4, getBottom() + 2, getRight() - 4, getBottom() + 3, 0xFFFFFFFF);
			}
		}

		@Override
		public void onClick(MouseButtonEvent event, boolean doubleClick) {
			draft.setColor(rgb);
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
			defaultButtonNarrationText(output);
		}
	}

	private EditBox box(int x, int y, int w, String value, int max) {
		EditBox box = new EditBox(font, x, y, w, 20, Component.empty());
		box.setMaxLength(max);
		box.setValue(value);
		return addRenderableWidget(box);
	}

	private Integer parse(EditBox box) {
		try {
			int value = Integer.parseInt(box.getValue().trim());
			box.setTextColor(TEXT_COLOR);
			return value;
		} catch (NumberFormatException e) {
			box.setTextColor(ERROR_COLOR);
			return null;
		}
	}

	private void save() {
		String name = nameBox.getValue().trim();
		Integer x = parse(xBox);
		Integer y = parse(yBox);
		Integer z = parse(zBox);
		if (name.isEmpty()) {
			nameBox.setTextColor(ERROR_COLOR);
			setFocused(nameBox);
			return;
		}
		if (x == null || y == null || z == null) {
			return;
		}
		draft.setName(name);
		draft.setInitials(initialsBox.getValue().trim());
		if (!draft.isBlockMarker()) {
			draft.setPosition(x, y, z);
		}
		OwpClient client = OwpClient.get();
		if (isNew) {
			client.waypoints().addToSet(targetSet, draft);
		} else {
			original.copyFrom(draft);
			client.waypoints().changed(original);
			client.waypoints().moveToSet(original, targetSet);
		}
		onClose();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		graphics.centeredText(font, title, width / 2, 20, 0xFFFFFFFF);
	}

	@Override
	public void onClose() {
		minecraft.gui.setScreen(parent);
	}
}
