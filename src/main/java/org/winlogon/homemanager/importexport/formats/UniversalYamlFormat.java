package org.winlogon.homemanager.importexport.formats;

import org.winlogon.homemanager.Home;
import org.winlogon.homemanager.importexport.HomeFormat;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class UniversalYamlFormat implements HomeFormat {
    private static final String EXPORT_FILENAME = "homes.yml";

    @Override
    public String getName() {
        return "yaml";
    }

    @Override
    public List<Home> readHomes(Path dataFolder) throws IOException {
        List<Home> homes = new ArrayList<>();
        
        // Try different possible filenames
        Path yamlFile = dataFolder.resolve(EXPORT_FILENAME);
        if (!Files.exists(yamlFile)) {
            yamlFile = dataFolder.resolve("homes.json"); // Also try JSON
        }
        
        if (!Files.exists(yamlFile)) {
            return homes;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(yamlFile.toFile());

        for (String uuidString : config.getKeys(false)) {
            try {
                UUID playerUuid = UUID.fromString(uuidString);

                if (config.contains(uuidString)) {
                    Map<String, ?> playerHomes = config.getConfigurationSection(uuidString).getValues(false);

                    for (Map.Entry<String, ?> entry : playerHomes.entrySet()) {
                        String homeName = entry.getKey();
                        Map<String, Object> homeData = (Map<String, Object>) entry.getValue();

                        Home home = parseHome(playerUuid, homeName, homeData);
                        if (home != null) {
                            homes.add(home);
                        }
                    }
                }
            } catch (Exception e) {
                // Skip invalid entries
            }
        }

        return homes;
    }

    @Override
    public void writeHomes(Path exportFolder, List<Home> homes) throws IOException {
        Files.createDirectories(exportFolder);
        
        String timestamp = new SimpleDateFormat("yyyy-MM-dd-HHmmss").format(new Date());
        String filename = "homes-" + timestamp + ".yml";
        Path yamlFile = exportFolder.resolve(filename);

        YamlConfiguration config = new YamlConfiguration();

        for (Home home : homes) {
            String uuidKey = home.playerUuid().toString();
            config.set(uuidKey + "." + home.homeName() + ".worldName", home.worldName());
            config.set(uuidKey + "." + home.homeName() + ".x", home.x());
            config.set(uuidKey + "." + home.homeName() + ".y", home.y());
            config.set(uuidKey + "." + home.homeName() + ".z", home.z());
            if (home.yaw() != null) {
                config.set(uuidKey + "." + home.homeName() + ".yaw", home.yaw());
            }
            if (home.pitch() != null) {
                config.set(uuidKey + "." + home.homeName() + ".pitch", home.pitch());
            }
        }

        config.save(yamlFile.toFile());
    }

    @Override
    public Path getDefaultExportPath(Path pluginDataFolder) {
        return pluginDataFolder.resolve("exports");
    }

    private Home parseHome(UUID playerUuid, String homeName, Map<String, Object> data) {
        try {
            String worldName = (String) data.get("worldName");
            if (worldName == null) return null;

            double x = getDoubleOrDefault(data, "x", 0.0);
            double y = getDoubleOrDefault(data, "y", 0.0);
            double z = getDoubleOrDefault(data, "z", 0.0);
            Float yaw = getFloatOrNull(data, "yaw");
            Float pitch = getFloatOrNull(data, "pitch");

            return new Home(playerUuid, homeName, worldName, x, y, z, yaw, pitch);
        } catch (Exception e) {
            return null;
        }
    }

    private double getDoubleOrDefault(Map<String, Object> map, String key, double defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }

    private Float getFloatOrNull(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        return null;
    }
}
