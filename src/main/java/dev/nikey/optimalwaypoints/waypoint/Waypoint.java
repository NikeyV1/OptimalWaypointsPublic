package dev.nikey.optimalwaypoints.waypoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;

public final class Waypoint {
	private UUID id = UUID.randomUUID();
	private String name = "";
	private String initials = "";
	private int x;
	private int y;
	private int z;
	private String dimension = Dimensions.OVERWORLD;
	private int color = 0xFFFFFF;
	private WaypointKind kind = WaypointKind.STANDARD;
	private DisplayMode display = DisplayMode.ICON_AND_BLOCK;
	private boolean enabled = true;
	private boolean global;
	private boolean deleteOnArrival;
	private boolean locked;
	private long created = System.currentTimeMillis();
	private List<Long> blocks = new ArrayList<>();

	private transient Set<Long> blockIndex;
	private transient double centerX;
	private transient double centerZ;
	private transient int revision;

	public Waypoint() {
	}

	public Waypoint(String name, int x, int y, int z, String dimension, int color, WaypointKind kind) {
		this.name = name;
		this.x = x;
		this.y = y;
		this.z = z;
		this.dimension = dimension;
		this.color = color & 0xFFFFFF;
		this.kind = kind;
	}

	public static String deriveInitials(String name) {
		String trimmed = name == null ? "" : name.trim();
		if (trimmed.isEmpty()) {
			return "?";
		}
		return trimmed.substring(0, Character.charCount(trimmed.codePointAt(0))).toUpperCase(Locale.ROOT);
	}

	public Waypoint copy() {
		Waypoint copy = new Waypoint();
		copy.id = id;
		copy.created = created;
		copy.copyFrom(this);
		return copy;
	}

	public void copyFrom(Waypoint other) {
		name = other.name;
		initials = other.initials;
		x = other.x;
		y = other.y;
		z = other.z;
		dimension = other.dimension;
		color = other.color;
		kind = other.kind;
		display = other.display;
		enabled = other.enabled;
		global = other.global;
		deleteOnArrival = other.deleteOnArrival;
		locked = other.locked;
		blocks = new ArrayList<>(other.blocks());
		invalidateBlocks();
	}

	public UUID id() {
		return id;
	}

	public String name() {
		return name == null ? "" : name;
	}

	public void setName(String name) {
		this.name = name == null ? "" : name;
	}

	public String initials() {
		return initials == null || initials.isBlank() ? deriveInitials(name) : initials;
	}

	public String rawInitials() {
		return initials == null ? "" : initials;
	}

	public void setInitials(String initials) {
		this.initials = initials == null ? "" : initials;
	}

	public int x() {
		return x;
	}

	public int y() {
		return y;
	}

	public int z() {
		return z;
	}

	public void setPosition(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public String dimension() {
		return dimension == null ? Dimensions.OVERWORLD : dimension;
	}

	public void setDimension(String dimension) {
		this.dimension = dimension;
	}

	public int color() {
		return color & 0xFFFFFF;
	}

	public void setColor(int color) {
		this.color = color & 0xFFFFFF;
	}

	public WaypointKind kind() {
		return kind == null ? WaypointKind.STANDARD : kind;
	}

	public void setKind(WaypointKind kind) {
		this.kind = kind;
	}

	public DisplayMode display() {
		return display == null ? DisplayMode.ICON_AND_BLOCK : display;
	}

	public void setDisplay(DisplayMode display) {
		this.display = display;
	}

	public boolean enabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean global() {
		return global;
	}

	public void setGlobal(boolean global) {
		this.global = global;
	}

	public boolean deleteOnArrival() {
		return deleteOnArrival;
	}

	public void setDeleteOnArrival(boolean deleteOnArrival) {
		this.deleteOnArrival = deleteOnArrival;
	}

	public boolean locked() {
		return locked;
	}

	public void setLocked(boolean locked) {
		this.locked = locked;
	}

	public long created() {
		return created;
	}

	public boolean isBlockMarker() {
		return kind() == WaypointKind.BLOCK;
	}

	public List<Long> blocks() {
		if (blocks == null) {
			blocks = new ArrayList<>();
		}
		return Collections.unmodifiableList(blocks);
	}

	public int blockRevision() {
		ensureIndex();
		return revision;
	}

	public boolean containsBlock(BlockPos pos) {
		ensureIndex();
		return blockIndex.contains(pos.asLong());
	}

	public boolean addBlock(BlockPos pos) {
		ensureIndex();
		if (!blockIndex.add(pos.asLong())) {
			return false;
		}
		blocks.add(pos.asLong());
		onBlocksChanged();
		return true;
	}

	public boolean removeBlock(BlockPos pos) {
		ensureIndex();
		if (!blockIndex.remove(pos.asLong())) {
			return false;
		}
		blocks.remove(Long.valueOf(pos.asLong()));
		onBlocksChanged();
		return true;
	}

	public double centerX() {
		if (!isBlockMarker() || blocks().isEmpty()) {
			return x + 0.5;
		}
		ensureIndex();
		return centerX;
	}

	public double centerZ() {
		if (!isBlockMarker() || blocks().isEmpty()) {
			return z + 0.5;
		}
		ensureIndex();
		return centerZ;
	}

	public double iconY() {
		return y + 1.0;
	}

	private void invalidateBlocks() {
		blockIndex = null;
		revision++;
	}

	private void onBlocksChanged() {
		revision++;
		updateCenter();
		if (!blocks.isEmpty()) {
			x = (int) Math.floor(centerX);
			z = (int) Math.floor(centerZ);
			int maxY = Integer.MIN_VALUE;
			for (long packed : blocks) {
				maxY = Math.max(maxY, BlockPos.getY(packed));
			}
			y = maxY;
		}
	}

	private void ensureIndex() {
		if (blockIndex != null) {
			return;
		}
		if (blocks == null) {
			blocks = new ArrayList<>();
		}
		blockIndex = new HashSet<>(blocks);
		updateCenter();
	}

	private void updateCenter() {
		double sx = 0;
		double sz = 0;
		for (long packed : blocks) {
			sx += BlockPos.getX(packed) + 0.5;
			sz += BlockPos.getZ(packed) + 0.5;
		}
		int count = Math.max(1, blocks.size());
		centerX = sx / count;
		centerZ = sz / count;
	}
}
