# Developer API

The OptimalWaypoints Paper plugin exposes a Java API so your plugin can read and change the rules. Detection, config, commands and packets are handled by the plugin.

## Setup

Download `optimalwaypoints-paper-<version>.jar` from the releases and add it as a compile-only dependency.

```kotlin
// build.gradle.kts
dependencies {
    compileOnly("io.papermc.paper:paper-api:26.3.build.186-stable")
    compileOnly(files("libs/optimalwaypoints-paper-0.1.0.jar"))
}
```

```yaml
# plugin.yml: depend if your plugin needs it, softdepend if it is optional
softdepend: [OptimalWaypoints]
```

With `softdepend`, check `getServer().getPluginManager().isPluginEnabled("OptimalWaypoints")` before touching any API class.

```java
import dev.nikey.optimalwaypoints.paper.api.OptimalWaypointsApi;
import dev.nikey.optimalwaypoints.protocol.Feature;
import dev.nikey.optimalwaypoints.protocol.ServerPolicy;

OptimalWaypointsApi waypoints = OptimalWaypointsApi.get();
```

## Detect players

`WaypointModDetectedEvent` fires once per session when a client announces the mod. The version is `?` if the client registered the channel but has not sent its hello yet.

```java
@EventHandler
public void onDetect(WaypointModDetectedEvent event) {
    getLogger().info(event.getPlayer().getName() + " uses Optimal Waypoints " + event.getModVersion());
}

boolean modded = waypoints.isUsingMod(player);
Optional<String> version = waypoints.modVersion(player);
Collection<Player> all = waypoints.modUsers();
```

## Rules for one player

The override lasts until you clear it or the player leaves.

```java
// entering the event arena: no waypoints at all
waypoints.setPlayerPolicy(player, ServerPolicy.ALLOW_ALL
    .with(Feature.MOD, false)
    .withMessage("Waypoints are off during events."));

// leaving the arena: back to world and global rules
waypoints.clearPlayerPolicy(player);
```

## Rules that depend on game state

`WaypointPolicySendEvent` runs every time a policy is about to be sent. Change the policy there and call `refresh(player)` whenever your state changes. This example disables markers through walls while a player is in combat.

```java
public final class CombatWaypoints implements Listener {
    private final Set<UUID> inCombat = ConcurrentHashMap.newKeySet();
    private final Plugin plugin;

    public CombatWaypoints(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player victim && event.getDamager() instanceof Player) {
            tag(victim);
        }
    }

    private void tag(Player player) {
        if (inCombat.add(player.getUniqueId())) {
            OptimalWaypointsApi.get().refresh(player);
        }
        player.getScheduler().runDelayed(plugin, task -> {
            inCombat.remove(player.getUniqueId());
            OptimalWaypointsApi.get().refresh(player);
        }, null, 20L * 15);
    }

    @EventHandler
    public void onPolicy(WaypointPolicySendEvent event) {
        if (inCombat.contains(event.getPlayer().getUniqueId())) {
            event.setPolicy(event.getPolicy().with(Feature.THROUGH_WALLS, false));
        }
    }
}
```

Keep the listener fast, it runs on every send.

## Global rules

Pass `true` to also save to `config.yml`, `false` to keep the change until the next restart or reload.

```java
ServerPolicy current = waypoints.globalPolicy();
waypoints.setGlobalPolicy(current.with(Feature.SHARING, false).withMaxRenderDistance(2000), false);
```

## Reference

### `OptimalWaypointsApi`

| Method | |
|---|---|
| `isUsingMod(Player)` | Whether the client announced the mod |
| `modVersion(Player)` | Reported mod version, empty if not detected |
| `modUsers()` | All online players using the mod |
| `globalPolicy()` | Current server-wide rules |
| `setGlobalPolicy(policy, save)` | Replace the server-wide rules and resend to everyone |
| `playerPolicy(Player)` | The override for this player, if any |
| `setPlayerPolicy(Player, policy)` | Set an override and resend |
| `clearPlayerPolicy(Player)` | Remove the override and resend |
| `effectivePolicy(Player)` | The policy this player gets right now, after events |
| `refresh(Player)` | Resolve and send the policy again |

### `ServerPolicy`

Immutable record. Start from `ServerPolicy.ALLOW_ALL` and derive new instances:

| Method | |
|---|---|
| `with(Feature, boolean allowed)` | Allow or disable one feature |
| `withMaxRenderDistance(int blocks)` | `0` = unlimited |
| `withMessage(String)` | Text shown to the player |
| `allows(Feature)` | `false` if the feature or the whole mod is disabled |
| `isDisabled(Feature)` | Whether this exact bit is set |

### Events

| Event | |
|---|---|
| `WaypointModDetectedEvent` | `getPlayer()`, `getModVersion()` |
| `WaypointPolicySendEvent` | `getPlayer()`, `getPolicy()`, `setPolicy(ServerPolicy)` |

Both are synchronous player events.
