package dev.nikey.optimalwaypoints.net;

import dev.nikey.optimalwaypoints.protocol.Protocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record PolicyPayload(Protocol.Policy policy) implements CustomPacketPayload {
	public static final Type<PolicyPayload> TYPE = new Type<>(Identifier.parse(Protocol.POLICY_CHANNEL));
	public static final StreamCodec<FriendlyByteBuf, PolicyPayload> CODEC = StreamCodec.of(
		(buf, payload) -> buf.writeBytes(Protocol.encodePolicy(payload.policy.policy())),
		buf -> {
			try {
				return new PolicyPayload(Protocol.decodePolicy(readRemaining(buf)));
			} catch (IllegalArgumentException e) {
				return new PolicyPayload(null);
			}
		}
	);

	static byte[] readRemaining(FriendlyByteBuf buf) {
		byte[] data = new byte[buf.readableBytes()];
		buf.readBytes(data);
		return data;
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
