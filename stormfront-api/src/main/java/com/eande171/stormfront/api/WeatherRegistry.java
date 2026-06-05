package com.eande171.stormfront.api;

import java.util.Collection;
import java.util.Optional;

public interface WeatherRegistry {

    void register(WeatherType type);

    void unregister(String id);

    Optional<WeatherType> get(String id);

    Collection<WeatherType> getAll();
}
