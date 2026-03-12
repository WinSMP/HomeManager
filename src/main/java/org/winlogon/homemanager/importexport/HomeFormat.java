package org.winlogon.homemanager.importexport;

import org.winlogon.homemanager.Home;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface HomeFormat {
    String getName();

    List<Home> readHomes(Path dataFolder) throws IOException;

    void writeHomes(Path exportFolder, List<Home> homes) throws IOException;

    default Path getDefaultImportPath(Path pluginDataFolder) {
        return pluginDataFolder;
    }

    default Path getDefaultExportPath(Path pluginDataFolder) {
        return pluginDataFolder.resolve("exports").resolve(getName());
    }
}
