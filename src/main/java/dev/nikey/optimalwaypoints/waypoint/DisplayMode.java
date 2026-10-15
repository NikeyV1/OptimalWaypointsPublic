package dev.nikey.optimalwaypoints.waypoint;

public enum DisplayMode {
	ICON_AND_BLOCK(true, true),
	ICON_ONLY(true, false),
	BLOCK_ONLY(false, true);

	private final boolean icon;
	private final boolean block;

	DisplayMode(boolean icon, boolean block) {
		this.icon = icon;
		this.block = block;
	}

	public boolean showsIcon() {
		return icon;
	}

	public boolean showsBlock() {
		return block;
	}

	public String translationKey() {
		return "optimalwaypoints.display." + name().toLowerCase(java.util.Locale.ROOT);
	}
}
