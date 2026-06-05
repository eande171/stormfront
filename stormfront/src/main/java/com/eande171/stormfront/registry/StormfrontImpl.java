package com.eande171.stormfront.registry;

import com.eande171.stormfront.api.StormfrontAPI;
import com.eande171.stormfront.api.WeatherRegistry;
import lombok.Getter;

public class StormfrontImpl extends StormfrontAPI {

    @Getter private final WeatherRegistry registry = new WeatherRegistryImpl();

}
