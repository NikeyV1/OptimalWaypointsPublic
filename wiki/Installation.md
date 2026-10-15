# Installation

## Players

1. Install [Fabric Loader](https://fabricmc.net/use/) 0.19.5 or newer for Minecraft 26.3.
2. Put [Fabric API](https://modrinth.com/mod/fabric-api) and `optimalwaypoints-<version>.jar` into your `mods` folder.
3. Optional: [Mod Menu](https://modrinth.com/mod/modmenu) to open the settings from the mod list.

### Default keys

| Key | Action |
|---|---|
| `U` | Waypoint menu |
| `B` | New waypoint at your position |
| `G` | Mark the block you look at. Hold and sweep to mark several blocks as one marker. Sneak + `G` extends your last marker. `G` on a marked block removes it. |
| unbound | Toggle waypoints, temporary waypoint, clear unlocked markers, switch waypoint set |

### Commands

Everything below is also available in the waypoint menu (`U`): **Import** imports Xaero waypoints and shared waypoint codes, and **Server Rules** in the top right shows what the current server allows.

| Command | |
|---|---|
| `/owp` | Open the waypoint menu |
| `/owp settings` | Open the settings |
| `/owp here <name>` | Waypoint at your position |
| `/owp add <x> <y> <z> <name>` | Waypoint at coordinates |
| `/owp addshared <code>` | Add a shared waypoint from its code |
| `/owp import xaero [folder]` | Import waypoints from Xaero's Minimap |
| `/owp status` | Show what the current server allows |

## Server owners

The mod itself never goes on the server. Install the **OptimalWaypoints** Paper plugin instead, see [[Server Setup]].
