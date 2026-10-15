package dev.nikey.optimalwaypoints.api;

import dev.nikey.optimalwaypoints.OwpClient;
import dev.nikey.optimalwaypoints.config.option.OptionRegistry;
import net.minecraft.client.Minecraft;

public interface ClientModule {
	String id();

	default void init(OwpClient client) {
	}

	default void registerOptions(OptionRegistry registry) {
	}

	default void onJoin(OwpClient client, Minecraft mc) {
	}

	default void onLeave(OwpClient client, Minecraft mc) {
	}

	default void tick(OwpClient client, Minecraft mc) {
	}
}
