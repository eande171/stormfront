package com.eande171.stormfront.services;

import com.eande171.stormfront.PluginMain;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

@RequiredArgsConstructor
public class MessageService {

    private final PluginMain plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private FileConfiguration messages;

    public void load() {
        File file = new File(plugin.getDataFolder(), "messages.yml");

        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        messages = YamlConfiguration.loadConfiguration(file);

        InputStream defaults = plugin.getResource("messages.yml");
        if (defaults != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaults)
            );
            messages.setDefaults(defaultConfig);
        }

        plugin.getLogger().info("Messages loaded.");
    }

    private String getRaw(String path) {
        String value = messages.getString(path);
        if (value == null) {
            plugin.getLogger().warning("Missing message key: " + path);
            return "<red>Missing message: " + path;
        }
        return value;
    }

    private Component parse(String path) {
        return miniMessage.deserialize(getRaw(path));
    }

    private Component parse(String path, String key, Object value) {
        return miniMessage.deserialize(getRaw(path),
            Placeholder.unparsed(key, String.valueOf(value)));
    }

    public Component placeholder() { return parse("commands.placeholder"); }

}
