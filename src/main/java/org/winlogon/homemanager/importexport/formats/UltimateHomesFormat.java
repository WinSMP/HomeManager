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
import java.util.stream.Stream;

/**
 * Implements <a href="https://www.spigotmc.org/resources/%E3%80%90ultimatehomes%E3%80%91-highly-configurable-player-sethome-system-1-8-1-21-11.64210/">UltimateHomes</a>'s home format.
 */
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

        try (Stream<Path> paths = Files.list(playerdataDir)) {
            paths.filter(path -> path.toString().endsWith(".yml"))
                .forEach(file -> {
                try {
                    var config = YamlConfiguration.loadConfiguration(file.toFile());
                    var playerUuid = UUID.fromString(file.getFileName().toString().replace(".yml", ""));

                    if (config.contains("homes")) {
                        Map<String, ?> homesSection = config.getConfigurationSection("homes").getValues(false);
                        for (Map.Entry<String, ?> entry : homesSection.entrySet()) {
                            String homeName = entry.getKey();
                            var homeData = (Map<String, Object>) entry.getValue();

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
        }

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

            double x = (int) Math.floor(HomeFormatUtils.getNumber(data, "x", 0));
            double y = (int) Math.floor(HomeFormatUtils.getNumber(data, "y", 0));
            double z = (int) Math.floor(HomeFormatUtils.getNumber(data, "z", 0));
            float yaw = HomeFormatUtils.getFloat(data, "yaw", 0f);
            float pitch = HomeFormatUtils.getFloat(data, "pitch", 0f);

            return new Home(playerUuid, homeName, world, x, y, z, yaw, pitch);
        } catch (Exception e) {
            return null;
        }
    }
}
