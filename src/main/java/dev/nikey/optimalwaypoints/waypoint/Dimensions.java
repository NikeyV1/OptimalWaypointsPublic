package dev.nikey.optimalwaypoints.waypoint;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.network.chat.Component;

public final class Dimensions {
	public static final String OVERWORLD = "minecraft:overworld";
	public static final String NETHER = "minecraft:the_nether";
	public static final String END = "minecraft:the_end";

	private Dimensions() {
	}

	public static double coordinateScale(String dimension) {
		return NETHER.equals(dimension) ? 8.0 : 1.0;
	}

	public static Component name(String id) {
		return switch (id) {
			case OVERWORLD -> Component.translatable("optimalwaypoints.dimension.overworld");
			case NETHER -> Component.translatable("optimalwaypoints.dimension.the_nether");
			case END -> Component.translatable("optimalwaypoints.dimension.the_end");
			default -> Component.literal(id.substring(id.indexOf(':') + 1));
		};
	}

	public static List<String> known(String... extra) {
		Set<String> dims = new LinkedHashSet<>(List.of(OVERWORLD, NETHER, END));
		dims.addAll(List.of(extra));
		return new ArrayList<>(dims);
	}
}
