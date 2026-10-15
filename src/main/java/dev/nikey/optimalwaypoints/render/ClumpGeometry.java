package dev.nikey.optimalwaypoints.render;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ClumpGeometry {
	private static final int[][] DIRS = {
		{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}
	};
	private static final int[] AXIS_COORD = {1, 2, 0};

	public record Edge(float x1, float y1, float z1, float x2, float y2, float z2) {
	}

	public record Face(int dir, float[] corners) {
	}

	private final List<Edge> edges = new ArrayList<>();
	private final List<Face> faces = new ArrayList<>();

	public List<Edge> edges() {
		return edges;
	}

	public List<Face> faces() {
		return faces;
	}

	public static ClumpGeometry build(Iterable<int[]> blocks, float inflate) {
		Set<Key> set = new HashSet<>();
		for (int[] b : blocks) {
			set.add(new Key(b[0], b[1], b[2]));
		}
		ClumpGeometry geometry = new ClumpGeometry();
		for (Key b : set) {
			for (int f = 0; f < 6; f++) {
				if (set.contains(b.offset(f))) {
					continue;
				}
				geometry.faces.add(new Face(f, faceCorners(b, f, inflate)));
				for (int e = 0; e < 6; e++) {
					if (axis(e) == axis(f)) {
						continue;
					}
					Key n = b.offset(e);
					boolean neighborInClump = set.contains(n);
					boolean draw;
					if (!neighborInClump) {
						draw = f < e;
					} else if (set.contains(n.offset(f))) {
						draw = f < opposite(e);
					} else {
						draw = false;
					}
					if (draw) {
						geometry.edges.add(edge(b, f, e, inflate));
					}
				}
			}
		}
		return geometry;
	}

	private static int axis(int dir) {
		return AXIS_COORD[dir / 2];
	}

	private static int opposite(int dir) {
		return dir ^ 1;
	}

	private static float side(int dir, float inflate) {
		return (dir & 1) == 1 ? 1.0F + inflate : -inflate;
	}

	private static Edge edge(Key b, int f, int e, float inflate) {
		float[] lo = new float[3];
		float[] hi = new float[3];
		int fa = axis(f);
		int ea = axis(e);
		int free = 3 - fa - ea;
		lo[fa] = hi[fa] = side(f, inflate);
		lo[ea] = hi[ea] = side(e, inflate);
		lo[free] = -inflate;
		hi[free] = 1.0F + inflate;
		return new Edge(b.x + lo[0], b.y + lo[1], b.z + lo[2], b.x + hi[0], b.y + hi[1], b.z + hi[2]);
	}

	private static float[] faceCorners(Key b, int f, float inflate) {
		int fa = axis(f);
		int u = (fa + 1) % 3;
		int v = (fa + 2) % 3;
		float[] out = new float[12];
		float[][] uv = {{-inflate, -inflate}, {1 + inflate, -inflate}, {1 + inflate, 1 + inflate}, {-inflate, 1 + inflate}};
		for (int i = 0; i < 4; i++) {
			float[] p = new float[3];
			p[fa] = side(f, inflate);
			p[u] = uv[i][0];
			p[v] = uv[i][1];
			out[i * 3] = b.x + p[0];
			out[i * 3 + 1] = b.y + p[1];
			out[i * 3 + 2] = b.z + p[2];
		}
		return out;
	}

	private record Key(int x, int y, int z) {
		Key offset(int dir) {
			return new Key(x + DIRS[dir][0], y + DIRS[dir][1], z + DIRS[dir][2]);
		}
	}
}
