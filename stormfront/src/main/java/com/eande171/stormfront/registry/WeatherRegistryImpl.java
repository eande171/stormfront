package com.eande171.stormfront.registry;

import com.eande171.stormfront.api.WeatherRegistry;
import com.eande171.stormfront.api.WeatherType;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class WeatherRegistryImpl implements WeatherRegistry {

    private final Map<String, WeatherType> types = new HashMap<>();

    @Override
    public void register(WeatherType type) {
        types.put(type.getId(), type);
    }

    @Override
    public void unregister(String id) {
        types.remove(id);
    }

    @Override
    public Optional<WeatherType> get(String id) {
        return Optional.ofNullable(types.get(id));
    }

    @Override
    public Collection<WeatherType> getAll() {
        return Collections.unmodifiableCollection(types.values());
    }
}
