package dev.nikey.optimalwaypoints.render;

import dev.nikey.optimalwaypoints.OwpClient;
import dev.nikey.optimalwaypoints.config.DistanceMode;
import dev.nikey.optimalwaypoints.config.OwpConfig;
import dev.nikey.optimalwaypoints.policy.ClientPolicy;
import dev.nikey.optimalwaypoints.protocol.Feature;
import dev.nikey.optimalwaypoints.waypoint.Dimensions;
import dev.nikey.optimalwaypoints.waypoint.Waypoint;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix4f;
import org.joml.Vector3fc;
import org.joml.Vector4f;

public final class WaypointHudRenderer implements HudElement {
	private static final int NEAR_DISTANCE = 20;
	private static final int BLOCK_MARKER_ICON_RANGE = 64;
	private static final double LOOK_ANGLE = 10.0;
	private static final float CLOSE_SCALE_FACTOR = 0.021333335F;
	private static final float ICON_ALPHA = 0.52274513F;
	private static final int LABEL_BACKGROUND = 0x5A000000;
	private static final float RESTING_SCALE = 0.8F;
	private static final float GROW_PER_SECOND = 6.0F;

	private final Matrix4f projection = new Matrix4f();
	private final Vector4f clip = new Vector4f();
	private final Map<UUID, Float> growth = new HashMap<>();
	private long lastFrame;

	private record Entry(Waypoint waypoint, float x, float y, double depth, double distance, boolean looking, boolean hovered) {
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		OwpClient client = OwpClient.get();
		OwpConfig cfg = client.config();
		ClientPolicy policy = client.policy();
		if (mc.player == null || mc.level == null || !cfg.showWaypoints || cfg.hiddenByKey || !policy.allows(Feature.IN_WORLD_WAYPOINTS)) {
			return;
		}
		float guiScale = (float) mc.getWindow().getGuiScale();
		float width = graphics.guiWidth() * guiScale;
		float height = graphics.guiHeight() * guiScale;
		Camera camera = mc.gameRenderer.mainCamera();
		Vec3 cam = camera.position();
		Vector3fc forward = camera.forwardVector();
		camera.getViewRotationProjectionMatrix(projection);

		double fov = mc.options.fov().get();
		double tanHalfFov = Math.tan(Math.toRadians(fov / 2.0));
		float userScale = cfg.scale / 100.0F;
		int uiScale = uiScale(mc.getWindow().getWidth(), mc.getWindow().getHeight());
		double clampDepth = CLOSE_SCALE_FACTOR * height / (uiScale * userScale) / (2.0 * tanHalfFov);
		DistanceMode distanceMode = policy.allows(Feature.DISTANCE_DISPLAY) ? cfg.distance : DistanceMode.OFF;
		double lookHalfWidth = (width / 2.0) * Math.tan(Math.toRadians(LOOK_ANGLE)) / (tanHalfFov * width / height);

		String dimension = client.currentDimension();
		double currentScale = mc.level.dimensionType().coordinateScale();
		boolean crossDimension = cfg.crossDimension && policy.allows(Feature.CROSS_DIMENSION);
		Vec3 player = mc.player.position();

		List<Entry> entries = new ArrayList<>();
		for (Waypoint waypoint : client.waypoints().current()) {
			if (!visible(waypoint, policy)) {
				continue;
			}
			double wx = waypoint.centerX();
			double wz = waypoint.centerZ();
			if (!waypoint.dimension().equals(dimension)) {
				double scale = Dimensions.coordinateScale(waypoint.dimension());
				if (!crossDimension || waypoint.isBlockMarker() || scale == currentScale) {
					continue;
				}
				wx *= scale / currentScale;
				wz *= scale / currentScale;
			}
			double dx = wx - player.x;
			double dz = wz - player.z;
			double horizontal = Math.sqrt(dx * dx + dz * dz);
			boolean exempt = waypoint.global() || waypoint.kind().isDeath() || waypoint.deleteOnArrival();
			if (!exempt && cfg.maxDistance > 0 && horizontal > cfg.maxDistance) {
				continue;
			}
			if (policy.maxRenderDistance() > 0 && horizontal > policy.maxRenderDistance()) {
				continue;
			}
			double dy = waypoint.y() - player.y;
			double distance = Math.sqrt(horizontal * horizontal + dy * dy);
			if (waypoint.isBlockMarker() && distance > BLOCK_MARKER_ICON_RANGE) {
				continue;
			}
			float rx = (float) (wx - cam.x);
			float ry = (float) (waypoint.iconY() - cam.y);
			float rz = (float) (wz - cam.z);
			clip.set(rx, ry, rz, 1.0F).mul(projection);
			if (clip.w <= 0.05F) {
				continue;
			}
			float sx = (clip.x / clip.w * 0.5F + 0.5F) * width;
			float sy = (0.5F - clip.y / clip.w * 0.5F) * height;
			if (sx < -width || sx > width * 2 || sy < -height || sy > height * 2) {
				continue;
			}
			double depth = rx * forward.x() + ry * forward.y() + rz * forward.z();
			boolean looking = Math.abs(sx - width / 2.0F) <= lookHalfWidth;
			boolean hovered = distanceMode == DistanceMode.ALL || distanceMode == DistanceMode.LOOKING && looking;
			entries.add(new Entry(waypoint, sx, sy, depth, distance, looking, hovered));
		}
		long now = System.nanoTime();
		float step = lastFrame == 0 ? 1.0F : Math.min(1.0F, (now - lastFrame) / 1.0E9F * GROW_PER_SECOND);
		lastFrame = now;
		if (entries.isEmpty()) {
			growth.clear();
			return;
		}

		boolean allInfo = mc.player.isShiftKeyDown();
		Entry main = null;
		for (Entry entry : entries) {
			if (entry.hovered && (main == null || Math.abs(entry.x - width / 2) < Math.abs(main.x - width / 2))) {
				main = entry;
			}
		}
		Entry focus = null;
		for (Entry entry : entries) {
			if (entry.looking && (focus == null || Math.abs(entry.x - width / 2) < Math.abs(focus.x - width / 2))) {
				focus = entry;
			}
		}
		Map<UUID, Float> sizes = new HashMap<>();
		for (Entry entry : entries) {
			float target = !cfg.growOnLook || entry == focus ? 1.0F : RESTING_SCALE;
			float current = growth.getOrDefault(entry.waypoint.id(), target);
			sizes.put(entry.waypoint.id(), current + (target - current) * step);
		}
		growth.clear();
		growth.putAll(sizes);
		entries.sort(Comparator.comparingDouble(Entry::depth).reversed());

		Matrix3x2fStack pose = graphics.pose();
		pose.pushMatrix();
		pose.scale(1.0F / guiScale, 1.0F / guiScale);
		for (Entry entry : entries) {
			if (entry != main) {
				draw(graphics, mc.font, cfg, entry, entry.hovered && allInfo, false, uiScale, userScale, clampDepth, growth.get(entry.waypoint.id()));
			}
		}
		if (main != null) {
			draw(graphics, mc.font, cfg, main, true, !allInfo, uiScale, userScale, clampDepth, growth.get(main.waypoint.id()));
		}
		pose.popMatrix();
	}

	private static int uiScale(int width, int height) {
		int size = Math.min(width, height);
		return size >= 1500 ? size / 500 : 2;
	}

	private static boolean visible(Waypoint waypoint, ClientPolicy policy) {
		if (!waypoint.enabled() || !waypoint.display().showsIcon()) {
			return false;
		}
		if (waypoint.kind().isDeath() && !policy.allows(Feature.DEATHPOINTS)) {
			return false;
		}
		return !waypoint.isBlockMarker() || policy.allows(Feature.BLOCK_HIGHLIGHTS);
	}

	private static void draw(GuiGraphicsExtractor graphics, Font font, OwpConfig cfg, Entry entry, boolean highlighted, boolean onlyMain,
		int uiScale, float userScale, double clampDepth, float grow) {
		Waypoint waypoint = entry.waypoint;
		boolean nearby = entry.distance <= NEAR_DISTANCE;
		boolean showDistance = !nearby && highlighted;
		boolean showName = nearby || (showDistance && cfg.namesWithDistance) || (!showDistance && waypoint.kind().isDeath());

		Matrix3x2fStack pose = graphics.pose();
		pose.pushMatrix();
		pose.translate(Math.round(entry.x), Math.round(entry.y));
		if (entry.depth < clampDepth) {
			float scale = (float) (clampDepth / Math.max(0.05, entry.depth));
			pose.scale(scale, scale);
		}

		float iconScale = uiScale * userScale * grow;
		pose.pushMatrix();
		pose.scale(iconScale, iconScale);
		float alpha = ICON_ALPHA * cfg.opacity / 100.0F;
		if (highlighted && onlyMain) {
			alpha = Math.min(1.0F, alpha * 1.5F);
		}
		WaypointIconRenderer.drawWorldIcon(graphics, font, waypoint, alpha);
		pose.popMatrix();

		pose.translate(0, 2);
		if (showName) {
			label(graphics, font, waypoint.name(), Math.max(1, (float) Math.ceil(uiScale * 0.5F)) * userScale);
		}
		pose.translate(0, 2);
		if (showDistance) {
			label(graphics, font, DistanceFormat.format(entry.distance), uiScale * userScale);
		}
		pose.popMatrix();
	}

	private static void label(GuiGraphicsExtractor graphics, Font font, String text, float scale) {
		if (text.isBlank()) {
			return;
		}
		int textWidth = font.width(text);
		int backgroundWidth = textWidth + 3;
		int half = backgroundWidth / 2;
		Matrix3x2fStack pose = graphics.pose();
		pose.pushMatrix();
		if ((backgroundWidth & 1) != 0) {
			pose.translate(-scale / 2.0F, 0);
		}
		pose.scale(scale, scale);
		graphics.fill(-half, 0, -half + backgroundWidth, 9, LABEL_BACKGROUND);
		graphics.text(font, text, -half + 2, 1, 0xFFFFFFFF, false);
		pose.popMatrix();
		pose.translate(0, 9 * scale);
	}
}
