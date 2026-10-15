package dev.nikey.optimalwaypoints.input;

import com.mojang.blaze3d.platform.InputConstants;
import dev.nikey.optimalwaypoints.OptimalWaypoints;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;

public final class OwpKeybinds {
	public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(OptimalWaypoints.id("main"));

	public static KeyMapping openMenu;
	public static KeyMapping newWaypoint;
	public static KeyMapping markBlock;
	public static KeyMapping tempWaypoint;
	public static KeyMapping toggleVisibility;
	public static KeyMapping clearUnlocked;
	public static KeyMapping switchSet;

	private OwpKeybinds() {
	}

	public static void register() {
		openMenu = key("open_menu", InputConstants.KEY_U);
		newWaypoint = key("new_waypoint", InputConstants.KEY_B);
		markBlock = key("mark_block", InputConstants.KEY_G);
		tempWaypoint = key("temp_waypoint", InputConstants.UNKNOWN.getValue());
		toggleVisibility = key("toggle_visibility", InputConstants.UNKNOWN.getValue());
		clearUnlocked = key("clear_unlocked", InputConstants.UNKNOWN.getValue());
		switchSet = key("switch_set", InputConstants.UNKNOWN.getValue());
	}

	private static KeyMapping key(String name, int code) {
		return KeyMappingHelper.registerKeyMapping(new KeyMapping("key." + OptimalWaypoints.MOD_ID + "." + name, InputConstants.Type.KEYBOARD, code, CATEGORY));
	}
}
