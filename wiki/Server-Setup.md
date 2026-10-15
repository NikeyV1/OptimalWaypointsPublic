# Server Setup

Put `optimalwaypoints-paper-<version>.jar` into the `plugins` folder of a Paper 26.3 server and restart. The plugin creates `plugins/OptimalWaypoints/config.yml`.

## config.yml

```yaml
# Set a feature to false to disable it for players using Optimal Waypoints.
features:
  mod: true
  in_world_waypoints: true
  block_highlights: true
  through_walls: true
  deathpoints: true
  sharing: true
  chat_coordinates: true
  distance_display: true
  cross_dimension: true

# Waypoint render distance limit in blocks, 0 = unlimited.
max-distance: 0

# Shown to players together with the restrictions.
message: ""

# Per-world overrides. Only the listed values replace the global ones.
worlds:
  world_pvp:
    features:
      through_walls: false
    max-distance: 300

notify-staff: true
log-users: true
```

## Features

| Key | When disabled |
|---|---|
| `mod` | Turns the whole mod off: no rendering, menus or keybinds |
| `in_world_waypoints` | No waypoint icons in the world |
| `block_highlights` | No block markers, the mark key is blocked |
| `through_walls` | Marked blocks only render where they are visible |
| `deathpoints` | No deathpoints |
| `sharing` | No sharing or receiving waypoints in chat |
| `chat_coordinates` | No waypoint offers for coordinates posted in chat |
| `distance_display` | No distance labels |
| `cross_dimension` | No Overworld waypoints in the Nether (1:8) and vice versa |

The distance limit is measured horizontally and applies to every waypoint, including global ones and deathpoints.

## Commands

`/optimalwaypoints` or `/owpadmin`, permission `optimalwaypoints.admin`.

| Command | |
|---|---|
| `users` | Online players using the mod, with version |
| `check <player>` | Whether one player uses the mod |
| `policy` | Show the global rules |
| `policy <feature> <true\|false>` | Allow or disable a feature, applied live and saved |
| `maxdistance <blocks>` | Set the distance limit, `0` removes it |
| `message [text]` | Set or clear the message |
| `reload` | Reload `config.yml` and resend the rules |

## Permissions

| Permission | Default | |
|---|---|---|
| `optimalwaypoints.admin` | op | Use `/optimalwaypoints` |
| `optimalwaypoints.notify` | op | Chat notice when a player joins with the mod |
| `optimalwaypoints.bypass` | false | Ignore all restrictions |

## Which rules a player gets

The first match wins:

1. Player has `optimalwaypoints.bypass`: everything allowed.
2. A rule set for this player by another plugin, see [[Developer API]].
3. The player's world under `worlds:`.
4. The global rules.

Plugins listening to `WaypointPolicySendEvent` can change the result last.
