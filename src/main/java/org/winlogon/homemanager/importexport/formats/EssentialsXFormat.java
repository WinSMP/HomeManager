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

public class EssentialsXFormat implements HomeFormat {
    private static final String USERDATA_FOLDER = "userdata";

    @Override
    public String getName() {
        return "essentialsx";
    }

    @Override
    public List<Home> readHomes(Path dataFolder) throws IOException {
        List<Home> homes = new ArrayList<>();
        Path userdataDir = dataFolder.resolve(USERDATA_FOLDER);

        if (!Files.exists(userdataDir)) {
            return homes;
        }

        try (Stream<Path> paths = Files.list(userdataDir)) {
            paths.filter(path -> path.toString().endsWith(".yml"))
                .forEach(file -> {
                    try {
                        var config = YamlConfiguration.loadConfiguration(file.toFile());
                        var uuid = UUID.fromString(file.getFileName().toString().replace(".yml", ""));
                        homes.addAll(loadPlayerHomes(uuid, config));
                    } catch (Exception e) {
                        // Skip invalid files
                    }
                });
        }

        return homes;
    }

    @Override
    public void writeHomes(Path exportFolder, List<Home> homes) throws IOException {
        Path userdataDir = exportFolder.resolve(USERDATA_FOLDER);
        Files.createDirectories(userdataDir);

        homes.stream()
            .collect(Collectors.groupingBy(Home::playerUuid))
            .forEach((uuid, playerHomes) -> {
                try {
                    Path playerFile = userdataDir.resolve(uuid + ".yml");
                    
                    YamlConfiguration config;
                    if (Files.exists(playerFile)) {
                        config = YamlConfiguration.loadConfiguration(playerFile.toFile());
                    } else {
                        config = new YamlConfiguration();
                    }

                    for (var home : playerHomes) {
                        config.set("homes." + home.homeName() + ".world", home.worldName());
                        config.set("homes." + home.homeName() + ".x", home.x());
                        config.set("homes." + home.homeName() + ".y", home.y());
                        config.set("homes." + home.homeName() + ".z", home.z());
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
        return pluginDataFolder.getParent().resolve("Essentials").resolve("userdata");
    }

    private Home parseHome(UUID playerUuid, String homeName, Map<String, Object> data) {
        try {
            var world = (String) data.get("world");
            if (world == null) return null;

            double x = HomeFormatUtils.getNumber(data, "x", 0.0);
            double y = HomeFormatUtils.getNumber(data, "y", 0.0);
            double z = HomeFormatUtils.getNumber(data, "z", 0.0);
            float yaw = HomeFormatUtils.getFloat(data, "yaw", 0f);
            float pitch = HomeFormatUtils.getFloat(data, "pitch", 0f);

            return new Home(playerUuid, homeName, world, x, y, z, yaw, pitch);
        } catch (Exception e) {
            return null;
        }
    }

    private List<Home> loadPlayerHomes(UUID playerUuid, YamlConfiguration config) {
        if (!config.contains("homes")) return List.of();

        var homesSection = config.getConfigurationSection("homes");
        if (homesSection == null) return List.of();

        Map<String, ?> homesMap = homesSection.getValues(false);
        List<Home> homes = new ArrayList<>();

        for (Map.Entry<String, ?> entry : homesMap.entrySet()) {
            if (entry.getValue() instanceof Map) {
                var home = parseHome(playerUuid, entry.getKey(), (Map<String, Object>) entry.getValue());
                if (home != null) homes.add(home);
            }
        }
        return homes;
    }
}
