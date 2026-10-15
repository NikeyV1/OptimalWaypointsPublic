package dev.nikey.optimalwaypoints.protocol;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public final class Protocol {
	public static final int VERSION = 1;
	public static final String NAMESPACE = "optimalwaypoints";
	public static final String HELLO_CHANNEL = NAMESPACE + ":hello";
	public static final String POLICY_CHANNEL = NAMESPACE + ":policy";
	public static final int MAX_STRING = 256;

	public record Hello(int protocol, String modVersion) {
	}

	public record Policy(int protocol, ServerPolicy policy) {
	}

	private Protocol() {
	}

	public static byte[] encodeHello(Hello hello) {
		Writer out = new Writer();
		out.varInt(hello.protocol());
		out.string(hello.modVersion());
		return out.bytes();
	}

	public static Hello decodeHello(byte[] data) {
		Reader in = new Reader(data);
		return new Hello(in.varInt(), in.string());
	}

	public static byte[] encodePolicy(ServerPolicy policy) {
		Writer out = new Writer();
		out.varInt(VERSION);
		out.varLong(policy.disabledMask());
		out.varInt(policy.maxRenderDistance());
		out.string(policy.message());
		return out.bytes();
	}

	public static Policy decodePolicy(byte[] data) {
		Reader in = new Reader(data);
		int protocol = in.varInt();
		long mask = in.varLong();
		int distance = in.varInt();
		String message = in.string();
		return new Policy(protocol, new ServerPolicy(mask, distance, message));
	}

	private static final class Writer {
		private final ByteArrayOutputStream out = new ByteArrayOutputStream();

		void varInt(int value) {
			varLong(value & 0xFFFFFFFFL);
		}

		void varLong(long value) {
			while ((value & ~0x7FL) != 0) {
				out.write((int) (value & 0x7F) | 0x80);
				value >>>= 7;
			}
			out.write((int) value);
		}

		void string(String value) {
			String limited = value.length() > MAX_STRING ? value.substring(0, MAX_STRING) : value;
			byte[] bytes = limited.getBytes(StandardCharsets.UTF_8);
			varInt(bytes.length);
			out.writeBytes(bytes);
		}

		byte[] bytes() {
			return out.toByteArray();
		}
	}

	private static final class Reader {
		private final byte[] data;
		private int position;

		Reader(byte[] data) {
			this.data = data;
		}

		private int next() {
			if (position >= data.length) {
				throw new IllegalArgumentException("Truncated Optimal Waypoints packet");
			}
			return data[position++] & 0xFF;
		}

		int varInt() {
			long value = varLong();
			if (value > 0xFFFFFFFFL) {
				throw new IllegalArgumentException("VarInt too big");
			}
			return (int) value;
		}

		long varLong() {
			long value = 0;
			int shift = 0;
			int b;
			do {
				if (shift >= 70) {
					throw new IllegalArgumentException("VarLong too big");
				}
				b = next();
				value |= (long) (b & 0x7F) << shift;
				shift += 7;
			} while ((b & 0x80) != 0);
			return value;
		}

		String string() {
			int length = varInt();
			if (length < 0 || length > MAX_STRING * 4 || position + length > data.length) {
				throw new IllegalArgumentException("Invalid string length");
			}
			String value = new String(data, position, length, StandardCharsets.UTF_8);
			position += length;
			return value;
		}
	}
}
