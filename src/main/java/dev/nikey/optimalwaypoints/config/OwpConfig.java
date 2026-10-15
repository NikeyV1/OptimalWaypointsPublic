package dev.nikey.optimalwaypoints.config;

public final class OwpConfig {
	public boolean showWaypoints = true;
	public boolean hiddenByKey = false;
	public int scale = 100;
	public int opacity = 80;
	public boolean growOnLook = true;
	public DistanceMode distance = DistanceMode.LOOKING;
	public boolean namesWithDistance = true;
	public int maxDistance = 0;
	public boolean crossDimension = true;

	public boolean throughWalls = true;
	public int fillOpacity = 25;
	public int outlineWidth = 25;

	public boolean deathpoints = true;
	public boolean keepOldDeathpoints = true;
	public boolean deleteReachedDeathpoints = true;

	public boolean chatCoordinates = true;
}
