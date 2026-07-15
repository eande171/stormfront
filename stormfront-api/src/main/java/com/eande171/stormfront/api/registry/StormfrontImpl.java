package com.eande171.stormfront.api.registry;

import com.eande171.stormfront.api.CellManager;
import com.eande171.stormfront.api.StormfrontAPI;
import com.eande171.stormfront.api.WeatherRegistry;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class StormfrontImpl extends StormfrontAPI {

    @Getter private final WeatherRegistry registry = new WeatherRegistryImpl();
    @Getter private final CellManager cellManager;
}
