package org.winlogon.homemanager.importexport;

import org.winlogon.homemanager.DataHandler;
import org.winlogon.homemanager.Home;
import org.winlogon.homemanager.importexport.formats.EssentialsXFormat;
import org.winlogon.homemanager.importexport.formats.SetHomeFormat;
import org.winlogon.homemanager.importexport.formats.UltimateHomesFormat;
import org.winlogon.homemanager.importexport.formats.UniversalYamlFormat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HomeImporter {
    private final DataHandler dataHandler;
    private final Path pluginDataFolder;

    public HomeImporter(DataHandler dataHandler, Path pluginDataFolder) {
        this.dataHandler = dataHandler;
        this.pluginDataFolder = pluginDataFolder;
    }

    public ImportResult importHomes(ImportExportFormat format, Path importPath) {
        HomeFormat homeFormat = getFormat(format);

        if (homeFormat == null) {
            return new ImportResult(0, 0, List.of("Unknown format: " + format));
        }

        Path actualPath = importPath;
        if (actualPath == null) {
            actualPath = homeFormat.getDefaultImportPath(pluginDataFolder);
        }

        List<String> errors = new ArrayList<>();
        int imported = 0;
        int skipped = 0;

        try {
            List<Home> homes = homeFormat.readHomes(actualPath);

            for (Home home : homes) {
                String finalName = resolveDuplicateName(home.playerUuid(), home.homeName());
                Home homeToImport = home.withName(finalName);

                var result = dataHandler.importHome(homeToImport);
                if (result.isOk()) {
                    imported++;
                } else {
                    skipped++;
                    errors.add("Failed to import home " + finalName + " for " + home.playerUuid());
                }
            }
        } catch (IOException e) {
            errors.add("Failed to read homes: " + e.getMessage());
        }

        return new ImportResult(imported, skipped, errors);
    }

    private String resolveDuplicateName(UUID playerUuid, String homeName) {
        String finalName = homeName;
        int counter = 1;

        while (dataHandler.homeExists(playerUuid, finalName)) {
            finalName = homeName + "_imported";
            if (counter > 1) {
                finalName = homeName + "_imported" + counter;
            }
            counter++;
        }

        return finalName;
    }

    private HomeFormat getFormat(ImportExportFormat format) {
        return switch (format) {
            case ESSENTIALSX -> new EssentialsXFormat();
            case ULTIMATEHOMES -> new UltimateHomesFormat();
            case SETHOME -> new SetHomeFormat();
            case YAML -> new UniversalYamlFormat();
            default -> null;
        };
    }

    public record ImportResult(int imported, int skipped, List<String> errors) {
        public boolean hasErrors() {
            return !errors.isEmpty();
        }
    }
}
