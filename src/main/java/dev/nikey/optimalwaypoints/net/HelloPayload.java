package dev.nikey.optimalwaypoints.net;

import dev.nikey.optimalwaypoints.protocol.Protocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record HelloPayload(Protocol.Hello hello) implements CustomPacketPayload {
	public static final Type<HelloPayload> TYPE = new Type<>(Identifier.parse(Protocol.HELLO_CHANNEL));
	public static final StreamCodec<FriendlyByteBuf, HelloPayload> CODEC = StreamCodec.of(
		(buf, payload) -> buf.writeBytes(Protocol.encodeHello(payload.hello)),
		buf -> new HelloPayload(Protocol.decodeHello(PolicyPayload.readRemaining(buf)))
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
