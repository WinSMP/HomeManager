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

public class SetHomeFormat implements HomeFormat {
    private static final String HOMES_FILE = "homes.yml";

    @Override
    public String getName() {
        return "sethome";
    }

    @Override
    public List<Home> readHomes(Path dataFolder) throws IOException {
        List<Home> homes = new ArrayList<>();
        Path homesFile = dataFolder.resolve(HOMES_FILE);

        if (!Files.exists(homesFile)) {
            return homes;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(homesFile.toFile());

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
        Path homesFile = exportFolder.resolve(HOMES_FILE);
        
        // Create backup if file exists
        if (Files.exists(homesFile)) {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd-HHmmss").format(new Date());
            Path backupFile = exportFolder.resolve("homes-" + timestamp + ".yml");
            Files.copy(homesFile, backupFile);
        }

        YamlConfiguration config = new YamlConfiguration();

        for (Home home : homes) {
            String uuidKey = home.playerUuid().toString();
            config.set(uuidKey + "." + home.homeName() + ".world", home.worldName());
            config.set(uuidKey + "." + home.homeName() + ".x", home.x());
            config.set(uuidKey + "." + home.homeName() + ".y", home.y());
            config.set(uuidKey + "." + home.homeName() + ".z", home.z());
            config.set(uuidKey + "." + home.homeName() + ".yaw", home.yaw() != null ? home.yaw() : 0f);
            config.set(uuidKey + "." + home.homeName() + ".pitch", home.pitch() != null ? home.pitch() : 0f);
        }

        config.save(homesFile.toFile());
    }

    @Override
    public Path getDefaultImportPath(Path pluginDataFolder) {
        return pluginDataFolder.getParent().resolve("SetHome").resolve("homes.yml").getParent();
    }

    private Home parseHome(UUID playerUuid, String homeName, Map<String, Object> data) {
        try {
            String world = (String) data.get("world");
            if (world == null) return null;

            double x = getDoubleOrDefault(data, "x", 0.0);
            double y = getDoubleOrDefault(data, "y", 0.0);
            double z = getDoubleOrDefault(data, "z", 0.0);
            float yaw = getFloatOrDefault(data, "yaw", 0f);
            float pitch = getFloatOrDefault(data, "pitch", 0f);

            return new Home(playerUuid, homeName, world, x, y, z, yaw, pitch);
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

    private float getFloatOrDefault(Map<String, Object> map, String key, float defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        return defaultValue;
    }
}
