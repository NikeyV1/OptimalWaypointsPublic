# Protocol

Use this page if you do not run the OptimalWaypoints Paper plugin, for example on Velocity, BungeeCord, Spigot or a modded server. Do not run your own implementation and the plugin at the same time.

Protocol version: **1**

## Channels

| Channel | Direction | Fields, in order |
|---|---|---|
| `optimalwaypoints:hello` | client → server | VarInt protocol, String modVersion |
| `optimalwaypoints:policy` | server → client | VarInt protocol, VarLong disabledMask, VarInt maxRenderDistance, String message |

Types are the standard Minecraft ones: VarInt, VarLong and VarInt-length-prefixed UTF-8 strings (max 256 characters).

## Feature bits

| Bit | Feature |
|---|---|
| 0 | `MOD` (whole mod) |
| 1 | `IN_WORLD_WAYPOINTS` |
| 2 | `BLOCK_HIGHLIGHTS` |
| 3 | `THROUGH_WALLS` |
| 4 | `DEATHPOINTS` |
| 5 | `SHARING` |
| 6 | `CHAT_COORDINATES` |
| 7 | `DISTANCE_DISPLAY` |
| 8 | `CROSS_DIMENSION` |

A set bit disables the feature. Bit indices never change, new features get the next free bit.

## Example

Through Walls disabled, limited to 1000 blocks, no message:

```
01        protocol 1
08        disabledMask, bit 3
E8 07     maxRenderDistance 1000
00        empty message
```

## When to send

- When the client registers `optimalwaypoints:policy` (it is announced in `minecraft:register`).
- When you receive a hello.
- Whenever your rules change. The client applies every policy immediately.

Clients ignore bytes after the known fields, so later versions only append. A packet that cannot be decoded is ignored instead of disconnecting the player.

## Minimal Bukkit implementation

```java
public final class WaypointRules extends JavaPlugin implements Listener, PluginMessageListener {
    static final String HELLO = "optimalwaypoints:hello";
    static final String POLICY = "optimalwaypoints:policy";
    static final long THROUGH_WALLS = 1L << 3;

    @Override
    public void onEnable() {
        getServer().getMessenger().registerIncomingPluginChannel(this, HELLO, this);
        getServer().getMessenger().registerOutgoingPluginChannel(this, POLICY);
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] data) {
        send(player);
    }

    @EventHandler
    public void onRegister(PlayerRegisterChannelEvent event) {
        if (POLICY.equals(event.getChannel())) {
            send(event.getPlayer());
        }
    }

    void send(Player player) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeVar(out, 1);
        writeVar(out, THROUGH_WALLS);
        writeVar(out, 1000);
        byte[] message = "No x-ray markers here.".getBytes(StandardCharsets.UTF_8);
        writeVar(out, message.length);
        out.writeBytes(message);
        player.sendPluginMessage(this, POLICY, out.toByteArray());
    }

    static void writeVar(ByteArrayOutputStream out, long value) {
        while ((value & ~0x7FL) != 0) {
            out.write((int) (value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out.write((int) value);
    }
}
```

On a proxy, register the same channel identifiers, answer the hello and send the policy bytes to the player connection. The client treats the proxy like any other server.
