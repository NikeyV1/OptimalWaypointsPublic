package dev.nikey.optimalwaypoints.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.nikey.optimalwaypoints.OwpClient;
import dev.nikey.optimalwaypoints.config.OwpConfig;
import dev.nikey.optimalwaypoints.protocol.Feature;
import dev.nikey.optimalwaypoints.waypoint.Waypoint;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class BlockHighlightRenderer {
	private static final float INFLATE = 0.002F;
	private static final float[] FACE_SHADE = {0.6F, 1.0F, 0.8F, 0.8F, 0.7F, 0.7F};

	private Map<UUID, Cached> cache = new HashMap<>();

	private record Cached(int revision, ClumpGeometry geometry, int originX, int originY, int originZ) {
	}

	private record Batch(ClumpGeometry geometry, int color, int originX, int originY, int originZ) {
	}

	public boolean active() {
		OwpClient client = OwpClient.get();
		OwpConfig cfg = client.config();
		return !cfg.hiddenByKey && client.policy().allows(Feature.BLOCK_HIGHLIGHTS);
	}

	public void collect(LevelRenderContext context) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null || !active()) {
			return;
		}
		OwpClient client = OwpClient.get();
		OwpConfig cfg = client.config();
		Vec3 cam = context.levelState().cameraRenderState.pos;
		String dimension = client.currentDimension();
		double maxDistance = Math.max(64.0, mc.options.renderDistance().get() * 16.0 + 32.0);
		int policyMax = client.policy().maxRenderDistance();
		if (policyMax > 0) {
			maxDistance = Math.min(maxDistance, policyMax);
		}
		double maxSq = maxDistance * maxDistance;

		Map<UUID, Cached> used = new HashMap<>();
		List<Batch> batches = new ArrayList<>();
		for (Waypoint waypoint : client.waypoints().current()) {
			if (!waypoint.isBlockMarker() || !waypoint.enabled() || !waypoint.display().showsBlock()
				|| !waypoint.dimension().equals(dimension) || waypoint.blocks().isEmpty()) {
				continue;
			}
			double dx = waypoint.centerX() - cam.x;
			double dy = waypoint.y() + 0.5 - cam.y;
			double dz = waypoint.centerZ() - cam.z;
			if (dx * dx + dy * dy + dz * dz > maxSq) {
				continue;
			}
			batches.add(batch(waypoint, used));
		}
		cache = used;
		if (batches.isEmpty()) {
			return;
		}

		boolean seeThrough = cfg.throughWalls && client.policy().allows(Feature.THROUGH_WALLS);
		float lineWidth = Math.max(0.5F, cfg.outlineWidth / 10.0F);
		int fillAlpha = Math.round(cfg.fillOpacity * 2.55F);
		SubmitNodeCollector collector = context.submitNodeCollector();

		if (fillAlpha > 0) {
			collector.submitCustomGeometry(new PoseStack(), OwpRenderTypes.fill(seeThrough), (pose, consumer) -> {
				for (Batch batch : batches) {
					float ox = (float) (batch.originX - cam.x);
					float oy = (float) (batch.originY - cam.y);
					float oz = (float) (batch.originZ - cam.z);
					for (ClumpGeometry.Face face : batch.geometry.faces()) {
						int argb = shade(batch.color, FACE_SHADE[face.dir()], fillAlpha);
						float[] c = face.corners();
						for (int i = 0; i < 12; i += 3) {
							consumer.addVertex(pose, c[i] + ox, c[i + 1] + oy, c[i + 2] + oz).setColor(argb);
						}
					}
				}
			});
		}
		collector.submitCustomGeometry(new PoseStack(), OwpRenderTypes.lines(seeThrough), (pose, consumer) -> {
			for (Batch batch : batches) {
				float ox = (float) (batch.originX - cam.x);
				float oy = (float) (batch.originY - cam.y);
				float oz = (float) (batch.originZ - cam.z);
				int argb = 0xFF000000 | batch.color;
				for (ClumpGeometry.Edge edge : batch.geometry.edges()) {
					line(consumer, pose, edge, ox, oy, oz, argb, lineWidth);
				}
			}
		});
	}

	private static void line(VertexConsumer consumer, PoseStack.Pose pose, ClumpGeometry.Edge edge, float ox, float oy, float oz, int argb, float width) {
		float nx = edge.x2() - edge.x1();
		float ny = edge.y2() - edge.y1();
		float nz = edge.z2() - edge.z1();
		float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
		if (len > 0) {
			nx /= len;
			ny /= len;
			nz /= len;
		}
		consumer.addVertex(pose, edge.x1() + ox, edge.y1() + oy, edge.z1() + oz).setColor(argb).setNormal(pose, nx, ny, nz).setLineWidth(width);
		consumer.addVertex(pose, edge.x2() + ox, edge.y2() + oy, edge.z2() + oz).setColor(argb).setNormal(pose, nx, ny, nz).setLineWidth(width);
	}

	private static int shade(int rgb, float factor, int alpha) {
		int r = Math.min(255, Math.round(((rgb >> 16) & 0xFF) * factor));
		int g = Math.min(255, Math.round(((rgb >> 8) & 0xFF) * factor));
		int b = Math.min(255, Math.round((rgb & 0xFF) * factor));
		return (alpha << 24) | (r << 16) | (g << 8) | b;
	}

	private Batch batch(Waypoint waypoint, Map<UUID, Cached> used) {
		int revision = waypoint.blockRevision();
		Cached cached = cache.get(waypoint.id());
		if (cached == null || cached.revision != revision) {
			long origin = waypoint.blocks().getFirst();
			int ox = BlockPos.getX(origin);
			int oy = BlockPos.getY(origin);
			int oz = BlockPos.getZ(origin);
			List<int[]> blocks = new ArrayList<>(waypoint.blocks().size());
			for (long packed : waypoint.blocks()) {
				blocks.add(new int[]{BlockPos.getX(packed) - ox, BlockPos.getY(packed) - oy, BlockPos.getZ(packed) - oz});
			}
			cached = new Cached(revision, ClumpGeometry.build(blocks, INFLATE), ox, oy, oz);
		}
		used.put(waypoint.id(), cached);
		return new Batch(cached.geometry, waypoint.color(), cached.originX, cached.originY, cached.originZ);
	}

	public boolean allowVanillaOutline() {
		OwpClient client = OwpClient.get();
		if (!active()) {
			return true;
		}
		Minecraft mc = Minecraft.getInstance();
		if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
			return true;
		}
		Waypoint marker = client.waypoints().blockMarkerAt(hit.getBlockPos(), client.currentDimension());
		return marker == null || !marker.enabled() || !marker.display().showsBlock();
	}
}
