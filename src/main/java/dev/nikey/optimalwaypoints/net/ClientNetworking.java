package dev.nikey.optimalwaypoints.net;

import dev.nikey.optimalwaypoints.OptimalWaypoints;
import dev.nikey.optimalwaypoints.OwpClient;
import dev.nikey.optimalwaypoints.protocol.Protocol;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ServerboundPlayChannelEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class ClientNetworking {
	private static boolean helloSent;

	private ClientNetworking() {
	}

	public static void register(OwpClient client) {
		PayloadTypeRegistry.serverboundPlay().register(HelloPayload.TYPE, HelloPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(PolicyPayload.TYPE, PolicyPayload.CODEC);

		ClientPlayNetworking.registerGlobalReceiver(PolicyPayload.TYPE, (payload, context) -> {
			if (payload.policy() == null) {
				OptimalWaypoints.LOGGER.warn("Ignoring malformed server policy");
				return;
			}
			if (payload.policy().protocol() > Protocol.VERSION) {
				OptimalWaypoints.LOGGER.warn("Server uses newer Optimal Waypoints protocol {}", payload.policy().protocol());
			}
			client.policy().apply(payload.policy().policy());
		});

		ClientPlayConnectionEvents.INIT.register((handler, mc) -> helloSent = false);
		ClientPlayConnectionEvents.JOIN.register((handler, sender, mc) -> trySendHello());
		ServerboundPlayChannelEvents.REGISTER.register((handler, sender, mc, channels) -> {
			if (channels.contains(HelloPayload.TYPE.id())) {
				trySendHello();
			}
		});
	}

	private static void trySendHello() {
		if (!helloSent && ClientPlayNetworking.canSend(HelloPayload.TYPE)) {
			helloSent = true;
			ClientPlayNetworking.send(new HelloPayload(new Protocol.Hello(Protocol.VERSION, OptimalWaypoints.version())));
		}
	}
}
