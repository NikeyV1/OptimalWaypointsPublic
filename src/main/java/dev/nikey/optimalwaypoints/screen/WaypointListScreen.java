package dev.nikey.optimalwaypoints.screen;

import com.mojang.blaze3d.platform.InputConstants;
import dev.nikey.optimalwaypoints.OwpClient;
import dev.nikey.optimalwaypoints.feature.ChatModule;
import dev.nikey.optimalwaypoints.policy.ClientPolicy;
import dev.nikey.optimalwaypoints.protocol.Feature;
import dev.nikey.optimalwaypoints.render.DistanceFormat;
import dev.nikey.optimalwaypoints.render.WaypointIconRenderer;
import dev.nikey.optimalwaypoints.ui.Messages;
import dev.nikey.optimalwaypoints.waypoint.Dimensions;
import dev.nikey.optimalwaypoints.waypoint.Waypoint;
import dev.nikey.optimalwaypoints.waypoint.WaypointSet;
import dev.nikey.optimalwaypoints.waypoint.WaypointWorld;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.phys.Vec3;

public final class WaypointListScreen extends Screen {
	private enum Sort {
		NONE,
		NAME,
		DISTANCE;

		Component label() {
			return Component.translatable("optimalwaypoints.sort." + name().toLowerCase(Locale.ROOT));
		}
	}

	private static Sort sort = Sort.NONE;
	private static boolean allDimensions = true;

	private final Screen parent;
	private WaypointList list;
	private EditBox filterBox;
	private Button editButton;
	private Button shareButton;
	private Button toggleButton;
	private Button deleteButton;
	private UUID selectedId;

	public WaypointListScreen(Screen parent) {
		super(Component.translatable("optimalwaypoints.gui.waypoints"));
		this.parent = parent;
	}

	private static OwpClient client() {
		return OwpClient.get();
	}

	@Override
	protected void init() {
		WaypointWorld world = client().waypoints().world();
		int center = width / 2;
		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose()).bounds(center - 100, height - 29, 200, 20).build());
		if (world == null) {
			return;
		}

		addRenderableWidget(Button.builder(Component.translatable("optimalwaypoints.gui.options"), b -> minecraft.gui.setScreen(new SettingsScreen(this)))
			.bounds(center - 203, 32, 75, 20).build());
		List<String> setNames = world.sets().stream().map(WaypointSet::name).toList();
		addRenderableWidget(CycleButton.builder((String name) -> Component.literal(name), world.currentSet().name()).withValues(setNames)
			.create(center - 100, 32, 200, 20, Component.translatable("optimalwaypoints.gui.set"), (b, value) -> {
				world.setCurrentSet(value);
				client().waypoints().markDirty();
				selectedId = null;
				refresh();
			}));
		addRenderableWidget(Button.builder(Component.literal("+"), b -> minecraft.gui.setScreen(new TextPromptScreen(this,
				Component.translatable("optimalwaypoints.gui.new_set"), "", name -> {
					world.createSet(name);
					world.setCurrentSet(name);
					client().waypoints().markDirty();
				})))
			.bounds(center + 104, 32, 20, 20).build());
		Button deleteSet = addRenderableWidget(Button.builder(Component.literal("-"), b -> confirmDeleteSet(world))
			.bounds(center + 126, 32, 20, 20).build());
		deleteSet.active = world.sets().size() > 1;

		list = addRenderableOnly(new WaypointList(minecraft, width, Math.max(4, height - 61 - 58), 58));

		filterBox = addRenderableWidget(new EditBox(font, center - 203, 60, 75, 20, Component.translatable("optimalwaypoints.gui.filter")));
		filterBox.setHint(Component.translatable("optimalwaypoints.gui.filter"));
		filterBox.setResponder(value -> refresh());
		addRenderableWidget(CycleButton.builder(Sort::label, sort).withValues(Sort.values()).displayOnlyValue()
			.create(center - 203, 84, 75, 20, Component.translatable("optimalwaypoints.gui.sort"), (b, value) -> {
				sort = value;
				refresh();
			}));
		addRenderableWidget(CycleButton.builder((Boolean all) -> Component.translatable(all ? "optimalwaypoints.filter.all" : "optimalwaypoints.filter.current"), allDimensions)
			.withValues(true, false).displayOnlyValue()
			.create(center - 203, 108, 75, 20, Component.translatable("optimalwaypoints.gui.dimension"), (b, value) -> {
				allDimensions = value;
				refresh();
			}));
		addRenderableWidget(Button.builder(Component.translatable("optimalwaypoints.gui.import"), b -> minecraft.gui.setScreen(new ImportScreen(this)))
			.bounds(center - 203, 132, 75, 20).build());

		addWidget(list);

		int y = height - 53;
		addRenderableWidget(Button.builder(Component.translatable("optimalwaypoints.gui.add"), b -> ScreenOpener.openNewWaypoint())
			.bounds(center - 203, y, 75, 20).build());
		editButton = addRenderableWidget(Button.builder(Component.translatable("optimalwaypoints.gui.edit"), b -> editSelected())
			.bounds(center - 120, y, 75, 20).build());
		shareButton = addRenderableWidget(Button.builder(Component.translatable("optimalwaypoints.gui.share"), b -> shareSelected())
			.bounds(center - 37, y, 75, 20).build());
		toggleButton = addRenderableWidget(Button.builder(Component.translatable("optimalwaypoints.gui.disable"), b -> toggleSelected())
			.bounds(center + 46, y, 75, 20).build());
		deleteButton = addRenderableWidget(Button.builder(Component.translatable("optimalwaypoints.gui.delete"), b -> deleteSelected())
			.bounds(center + 129, y, 75, 20).build());
		refresh();
	}

	private void refresh() {
		if (list != null) {
			list.rebuild();
			updateButtons();
		}
	}

	private Waypoint selected() {
		return selectedId == null ? null : client().waypoints().byId(selectedId);
	}

	private void updateButtons() {
		Waypoint selected = selected();
		boolean has = selected != null;
		editButton.active = has;
		deleteButton.active = has;
		toggleButton.active = has;
		shareButton.active = has && !selected.isBlockMarker() && client().policy().allows(Feature.SHARING);
		toggleButton.setMessage(Component.translatable(has && !selected.enabled() ? "optimalwaypoints.gui.enable" : "optimalwaypoints.gui.disable"));
	}

	private void editSelected() {
		Waypoint selected = selected();
		if (selected != null) {
			minecraft.gui.setScreen(new WaypointEditScreen(this, selected, false));
		}
	}

	private void shareSelected() {
		Waypoint selected = selected();
		if (selected == null) {
			return;
		}
		minecraft.gui.setScreen(new ConfirmScreen(confirmed -> {
			minecraft.gui.setScreen(confirmed ? null : this);
			if (confirmed) {
				ChatModule.share(selected);
			}
		}, Component.translatable("optimalwaypoints.gui.share_confirm"), Component.translatable("optimalwaypoints.gui.share_confirm.detail", selected.name())));
	}

	private void toggleSelected() {
		Waypoint selected = selected();
		if (selected != null) {
			selected.setEnabled(!selected.enabled());
			client().waypoints().changed(selected);
			refresh();
		}
	}

	private void deleteSelected() {
		Waypoint selected = selected();
		if (selected == null) {
			return;
		}
		minecraft.gui.setScreen(new ConfirmScreen(confirmed -> {
			if (confirmed) {
				client().waypoints().remove(selected);
				selectedId = null;
			}
			minecraft.gui.setScreen(this);
		}, Component.translatable("optimalwaypoints.gui.delete_confirm"), Component.literal(selected.name())));
	}

	private void confirmDeleteSet(WaypointWorld world) {
		WaypointSet set = world.currentSet();
		minecraft.gui.setScreen(new ConfirmScreen(confirmed -> {
			if (confirmed) {
				world.removeSet(set.name());
				client().waypoints().markDirty();
			}
			minecraft.gui.setScreen(this);
		}, Component.translatable("optimalwaypoints.gui.delete_set_confirm", set.name()),
			Component.translatable("optimalwaypoints.gui.delete_set_confirm.detail", set.waypoints().size())));
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (filterBox != null && !filterBox.isFocused() && selected() != null) {
			if (event.key() == InputConstants.KEY_DELETE) {
				deleteSelected();
				return true;
			}
			if (event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_NUMPADENTER) {
				editSelected();
				return true;
			}
		}
		return super.keyPressed(event);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		graphics.centeredText(font, title, width / 2, 12, 0xFFFFFFFF);
		if (client().waypoints().world() == null) {
			graphics.centeredText(font, Component.translatable("optimalwaypoints.command.not_ready"), width / 2, height / 2, 0xFFFF5555);
			return;
		}
		if (client().policy().serverManaged()) {
			Component rules = Component.translatable("optimalwaypoints.gui.server_rules");
			int rulesX = width - font.width(rules) - 8;
			boolean over = mouseX >= rulesX - 2 && mouseX <= width - 6 && mouseY >= 10 && mouseY <= 22;
			graphics.text(font, rules, rulesX, 12, over ? 0xFFFFFFFF : 0xFF000000 | Messages.ACCENT);
			if (over) {
				graphics.setTooltipForNextFrame(font, ClientPolicy.details(client().policy().current()), mouseX, mouseY);
				return;
			}
		}
		WaypointList.Entry hovered = list.hoveredEntry();
		if (hovered != null && minecraft.player != null) {
			double distance = distance(hovered.waypoint, minecraft.player.position(), client().currentDimension());
			if (!Double.isNaN(distance)) {
				graphics.setTooltipForNextFrame(font, Component.literal(DistanceFormat.format(distance)), mouseX, mouseY);
			}
		}
	}

	@Override
	public void onClose() {
		client().waypoints().saveNow();
		minecraft.gui.setScreen(parent);
	}

	private static double distance(Waypoint waypoint, Vec3 player, String dimension) {
		double x = waypoint.centerX();
		double z = waypoint.centerZ();
		if (!waypoint.dimension().equals(dimension)) {
			double scale = Dimensions.coordinateScale(waypoint.dimension()) / Dimensions.coordinateScale(dimension);
			if (scale == 1.0) {
				return Double.NaN;
			}
			x *= scale;
			z *= scale;
		}
		double dx = x - player.x;
		double dy = waypoint.y() - player.y;
		double dz = z - player.z;
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	private final class WaypointList extends ObjectSelectionList<WaypointList.Entry> {
		WaypointList(Minecraft mc, int width, int height, int top) {
			super(mc, width, height, top, 18);
		}

		@Override
		public int getRowWidth() {
			return 220;
		}

		Entry hoveredEntry() {
			return getHovered();
		}

		void rebuild() {
			String dimension = client().currentDimension();
			String filter = filterBox.getValue().toLowerCase(Locale.ROOT).trim();
			Vec3 player = minecraft.player == null ? Vec3.ZERO : minecraft.player.position();
			List<Waypoint> waypoints = new ArrayList<>();
			for (Waypoint waypoint : client().waypoints().current()) {
				boolean dimensionMatch = allDimensions || waypoint.dimension().equals(dimension);
				if (dimensionMatch && (filter.isEmpty() || waypoint.name().toLowerCase(Locale.ROOT).contains(filter))) {
					waypoints.add(waypoint);
				}
			}
			switch (sort) {
				case NAME -> waypoints.sort(Comparator.comparing(w -> w.name().toLowerCase(Locale.ROOT)));
				case DISTANCE -> waypoints.sort(Comparator.comparingDouble(w -> {
					double d = distance(w, player, dimension);
					return Double.isNaN(d) ? Double.MAX_VALUE : d;
				}));
				case NONE -> {
				}
			}
			List<Entry> entries = new ArrayList<>();
			Entry selectedEntry = null;
			for (Waypoint waypoint : waypoints) {
				Entry entry = new Entry(waypoint);
				entries.add(entry);
				if (waypoint.id().equals(selectedId)) {
					selectedEntry = entry;
				}
			}
			replaceEntries(entries);
			setSelected(selectedEntry);
		}

		@Override
		public void setSelected(Entry entry) {
			super.setSelected(entry);
			selectedId = entry == null ? null : entry.waypoint.id();
			updateButtons();
		}

		final class Entry extends ObjectSelectionList.Entry<Entry> {
			private final Waypoint waypoint;

			Entry(Waypoint waypoint) {
				this.waypoint = waypoint;
			}

			@Override
			public Component getNarration() {
				return Component.literal(waypoint.name());
			}

			@Override
			public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
				int x = getX();
				int y = getY() + 2;
				graphics.centeredText(font, label(), x + 110, y + 1, 0xFFFFFFFF);
				int iconX = x + 12;
				int iconY = y + 6;
				if (waypoint.global()) {
					graphics.centeredText(font, "*", iconX - 25, iconY - 3, 0xFFFFFFFF);
				}
				WaypointIconRenderer.drawGuiIcon(graphics, font, waypoint, iconX, iconY, 0.9F);
			}

			private Component label() {
				MutableComponent label = Component.literal(waypoint.name());
				String dimension = client().currentDimension();
				if (!waypoint.dimension().equals(dimension)) {
					label.append(Component.literal(" (").append(Dimensions.name(waypoint.dimension())).append(")").withStyle(ChatFormatting.GRAY));
				}
				if (!waypoint.enabled()) {
					label.append(Component.literal(" ").append(Component.translatable("optimalwaypoints.state.disabled")).withStyle(ChatFormatting.DARK_RED));
				} else if (waypoint.deleteOnArrival()) {
					label.append(Component.literal(" ").append(Component.translatable("optimalwaypoints.state.temporary")).withStyle(ChatFormatting.DARK_RED));
				}
				return label;
			}

			@Override
			public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
				WaypointList.this.setSelected(this);
				if (doubleClick) {
					editSelected();
				}
				return true;
			}
		}
	}
}
