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

public class HomeExporter {
    private final DataHandler dataHandler;
    private final Path pluginDataFolder;
    private final String exportDirectory;

    public HomeExporter(DataHandler dataHandler, Path pluginDataFolder, String exportDirectory) {
        this.dataHandler = dataHandler;
        this.pluginDataFolder = pluginDataFolder;
        this.exportDirectory = exportDirectory;
    }

    public ExportResult exportHomes(ImportExportFormat format) {
        HomeFormat homeFormat = getFormat(format);
        
        if (homeFormat == null) {
            return new ExportResult(0, List.of("Unknown format: " + format));
        }

        List<Home> allHomes = dataHandler.getAllHomes();
        
        if (allHomes.isEmpty()) {
            return new ExportResult(0, List.of("No homes to export"));
        }

        List<String> errors = new ArrayList<>();
        
        Path exportPath = pluginDataFolder.resolve(exportDirectory).resolve(format.getCommandName());
        
        // Create backup directory path for timestamped backups
        // TODO: "Variable 'backupBasePath' is never used"
        Path backupBasePath = exportPath;
        
        try {
            homeFormat.writeHomes(exportPath, allHomes);
        } catch (IOException e) {
            errors.add("Failed to export homes: " + e.getMessage());
            return new ExportResult(0, errors);
        }

        return new ExportResult(allHomes.size(), errors);
    }

    public Path getExportPath(ImportExportFormat format) {
        HomeFormat homeFormat = getFormat(format);
        if (homeFormat == null) {
            return null;
        }
        return pluginDataFolder.resolve(exportDirectory).resolve(format.getCommandName());
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

    public record ExportResult(int exportedCount, List<String> errors) {
        public boolean hasErrors() {
            return !errors.isEmpty();
        }
    }
}
