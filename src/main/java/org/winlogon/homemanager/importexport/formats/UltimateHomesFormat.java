package org.winlogon.homemanager.importexport.formats;

import org.winlogon.homemanager.Home;
import org.winlogon.homemanager.importexport.HomeFormat;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class UltimateHomesFormat implements HomeFormat {
    private static final String PLAYERDATA_FOLDER = "playerdata";

    @Override
    public String getName() {
        return "ultimatehomes";
    }

    @Override
    public List<Home> readHomes(Path dataFolder) throws IOException {
        List<Home> homes = new ArrayList<>();
        Path playerdataDir = dataFolder.resolve(PLAYERDATA_FOLDER);

        if (!Files.exists(playerdataDir)) {
            return homes;
        }

        Files.list(playerdataDir)
            .filter(path -> path.toString().endsWith(".yml"))
            .forEach(file -> {
                try {
                    YamlConfiguration config = YamlConfiguration.loadConfiguration(file.toFile());
                    UUID playerUuid = UUID.fromString(file.getFileName().toString().replace(".yml", ""));

                    if (config.contains("homes")) {
                        Map<String, ?> homesSection = config.getConfigurationSection("homes").getValues(false);
                        for (Map.Entry<String, ?> entry : homesSection.entrySet()) {
                            String homeName = entry.getKey();
                            Map<String, Object> homeData = (Map<String, Object>) entry.getValue();

                            Home home = parseHome(playerUuid, homeName, homeData);
                            if (home != null) {
                                homes.add(home);
                            }
                        }
                    }
                } catch (Exception e) {
                    // Skip invalid files
                }
            });

        return homes;
    }

    @Override
    public void writeHomes(Path exportFolder, List<Home> homes) throws IOException {
        Path playerdataDir = exportFolder.resolve(PLAYERDATA_FOLDER);
        Files.createDirectories(playerdataDir);

        homes.stream()
            .collect(Collectors.groupingBy(Home::playerUuid))
            .forEach((uuid, playerHomes) -> {
                try {
                    Path playerFile = playerdataDir.resolve(uuid + ".yml");
                    
                    YamlConfiguration config;
                    if (Files.exists(playerFile)) {
                        config = YamlConfiguration.loadConfiguration(playerFile.toFile());
                    } else {
                        config = new YamlConfiguration();
                    }

                    for (Home home : playerHomes) {
                        // UltimateHomes stores coordinates as integers (block coordinates)
                        config.set("homes." + home.homeName() + ".world", home.worldName());
                        config.set("homes." + home.homeName() + ".x", (int) Math.floor(home.x()));
                        config.set("homes." + home.homeName() + ".y", (int) Math.floor(home.y()));
                        config.set("homes." + home.homeName() + ".z", (int) Math.floor(home.z()));
                        config.set("homes." + home.homeName() + ".yaw", home.yaw() != null ? home.yaw() : 0f);
                        config.set("homes." + home.homeName() + ".pitch", home.pitch() != null ? home.pitch() : 0f);
                    }

                    config.save(playerFile.toFile());
                } catch (IOException e) {
                    throw new RuntimeException("Failed to write homes for " + uuid, e);
                }
            });
    }

    @Override
    public Path getDefaultImportPath(Path pluginDataFolder) {
        return pluginDataFolder.getParent().resolve("UltimateHomes").resolve("playerdata");
    }

    private Home parseHome(UUID playerUuid, String homeName, Map<String, Object> data) {
        try {
            String world = (String) data.get("world");
            if (world == null) return null;

            // UltimateHomes stores coordinates as integers
            double x = getIntOrDoubleOrDefault(data, "x", 0);
            double y = getIntOrDoubleOrDefault(data, "y", 0);
            double z = getIntOrDoubleOrDefault(data, "z", 0);
            float yaw = getFloatOrDefault(data, "yaw", 0f);
            float pitch = getFloatOrDefault(data, "pitch", 0f);

            return new Home(playerUuid, homeName, world, x, y, z, yaw, pitch);
        } catch (Exception e) {
            return null;
        }
    }

    private double getIntOrDoubleOrDefault(Map<String, Object> map, String key, double defaultValue) {
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
