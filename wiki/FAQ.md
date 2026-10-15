# FAQ

**Can players get around the server rules?**
The rules are enforced by the client. A modified client can ignore them, as with every client-side mod control. They keep honest players and the default experience fair.

**Can I see who uses the mod without the plugin?**
Yes. Fabric announces `optimalwaypoints:policy` in the `minecraft:register` packet, which any plugin that reads registered channels can see.

**What does the player see when rules change?**
One chat line, bundled if several changes arrive within a second. Hovering it lists every feature with its state. Your configured message is appended.

**Does the mod have a minimap, entity radar or teleport?**
No. Optimal Waypoints is waypoints and block markers only.

**Can I keep my Xaero waypoints?**
Yes, **Import** in the waypoint menu (`U`) or `/owp import xaero` imports the waypoints of the current server or world. Waypoints shared in chat by Xaero users are recognized too.

**Which protocol version does a server need?**
The current version is 1. Clients accept newer servers and only log a warning.
