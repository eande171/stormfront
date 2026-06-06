package com.eande171.stormfront.weather;

import com.eande171.stormfront.api.WeatherCell;
import com.eande171.stormfront.api.WeatherType;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collections;
import java.util.Set;

public class RainfrontType implements WeatherType {

    @Override
    public String getId() { return "stormfront:rain"; }

    @Override
    public int getPriority() { return 1; }

    @Override
    public Set<String> getCompatibleBiomes() { return Collections.emptySet(); }

    @Override
    public void onStart(WeatherCell cell) {}

    @Override
    public void onEnd(WeatherCell cell) {}

    @Override
    public void onTick(WeatherCell cell, Player player) {
        player.setPlayerWeather(org.bukkit.WeatherType.DOWNFALL);
        spawnRainImpacts(cell, player);
        applySlowness(player);
    }

    private void spawnRainImpacts(WeatherCell cell, Player player) {
        int count = (int) (15 * cell.getIntensity());
        player.spawnParticle(Particle.SPLASH,
            player.getLocation(),
            count, 3, 0.1, 3, 0);
    }

    private void applySlowness(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 0, true, false));
    }
}
