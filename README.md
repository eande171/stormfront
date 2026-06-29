# Stormfront

Stormfront replaces Minecraft's global vanilla weather system with dynamic, per-player weather cells. Each cell has its own type, position, radius, and intensity, meaning two players in the same world can experience completely different weather simultaneously.

## Features

- **Rainfront:** rain impacts, movement slowdown, campfire extinguishing
- **Thunderstorm:** everything in Rainfront plus lightning strikes and Enderman damage in the rain
- **Blizzard:** snowflakes, ground drift, wind particles, freeze buildup, fire extinguishing. Cold biomes only.
- **Heatwave:** heat shimmer, dust haze, hunger drain. Hot biomes only.
- **Miasma:** swamp gas, rising smoke, bubble pops, spore drift, and pulsing nausea. Swamp and mangrove biomes only.
- **Fully configurable:** enable or disable individual weather types in config.yml
- **API included:** register custom weather types from your own plugin

## Requirements

- **Paper** 1.21.11 or newer
- **PacketEvents**

## Installation

1. Install [PacketEvents](https://modrinth.com/plugin/packetevents) if not already present
2. Drop `stormfront-api-x.x.x.jar` and `stormfront-x.x.x.jar` into your server's `plugins/` folder
3. Restart the server. `plugins/Stormfront/config.yml` will be generated.
4. Enable or disable weather types as needed.
5. Use `/stormfront spawn` to test.

## Commands

All commands require the `stormfront.admin` permission.

| Command | Description |
|---|---|
| `/stormfront spawn <type> [radius] [intensity] [duration]` | Spawns a weather cell at your location |
| `/stormfront stop` | Removes all active weather cells |
| `/stormfront list` | Lists all active weather cells with their position and status |

**Spawn defaults:** radius=50, intensity=1.0, duration=indefinite

## Permissions

| Permission | Description | Default |
|---|---|---|
| `stormfront.admin` | Access to all `/stormfront` commands | OP |

## Configuration

Generated at `plugins/Stormfront/config.yml` on first run.

```yaml
# Enable or disable individual built-in weather types
types:
  rainfront: true
  thunderstorm: true
  miasma: true
  blizzard: true
  heatwave: true
```

## API

Stormfront exposes an API for registering custom weather types from other plugins. Depend on `stormfront-api` and implement the `WeatherType` interface.

```java
public class MyWeatherType implements WeatherType {
    @Override
    public String getId() { return "myplugin:myweather"; }

    @Override
    public void onTick(WeatherCell cell, Player player) {
        // your particle/effect logic here
    }

    // ... other required methods
}
```

Register it on enable:

```java
StormfrontAPI.get().getRegistry().register(new MyWeatherType());
```

**Maven dependency:**
```xml
<dependency>
    <groupId>com.eande171</groupId>
    <artifactId>stormfront-api</artifactId>
    <version>VERSION</version>
    <scope>provided</scope>
</dependency>
```
