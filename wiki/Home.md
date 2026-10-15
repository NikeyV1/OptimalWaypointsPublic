# Optimal Waypoints

Optimal Waypoints is a client-side Fabric mod for Minecraft 26.3. It adds in-world waypoints in the style of Xaero's Minimap and block markers for trapping and building. Servers decide which features players may use.

| I want to | Page |
|---|---|
| Install the mod | [[Installation]] |
| Control the mod on my Paper server | [[Server Setup]] |
| Control the mod from my own plugin | [[Developer API]] |
| Support it on Velocity, Spigot or a custom server | [[Protocol]] |
| Read common questions | [[FAQ]] |

## How server control works

1. A player with the mod joins. The client announces the channels `optimalwaypoints:hello` and `optimalwaypoints:policy`.
2. The server answers with a **policy**: which features are disabled, an optional render distance limit and an optional message.
3. The client applies it instantly and shows one chat line, for example:

```
Waypoints » ⚠ Limited by this server: Through Walls · max. 1000 blocks
```

Servers that never send a policy impose no restrictions.
