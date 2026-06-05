package com.eande171.stormfront.api;

public abstract class StormfrontAPI {

    private static StormfrontAPI instance;

    public static StormfrontAPI get() {
        if (instance == null) {
            throw new IllegalStateException("StormfrontAPI has not been initialized.");
        }
        return instance;
    }

    public static void setInstance(StormfrontAPI api) {
        if (instance != null) {
            throw new IllegalStateException("StormfrontAPI is already initialized.");
        }
        instance = api;
    }

    public abstract WeatherRegistry getRegistry();
}
