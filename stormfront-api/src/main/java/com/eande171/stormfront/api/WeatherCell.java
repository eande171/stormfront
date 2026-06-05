package com.eande171.stormfront.api;

import lombok.Builder;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.UUID;

@Getter
@Builder
public class WeatherCell {

    private final UUID id;
    private final WeatherType type;
    private Location center;
    private final int radius;
    private float intensity;
    private Vector movementVector;
    private final long startedAt;
    private final long duration;

    public void setCenter(Location center) { this.center = center; }
    public void setIntensity(float intensity) { this.intensity = intensity; }
    public void setMovementVector(Vector movementVector) { this.movementVector = movementVector; }

    public boolean isIndefinite() { return duration == -1; }
}
