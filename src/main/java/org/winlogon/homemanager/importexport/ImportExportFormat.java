package org.winlogon.homemanager.importexport;

import java.util.Arrays;

public enum ImportExportFormat {
    ESSENTIALSX("essentialsx", "EssentialsX"),
    ULTIMATEHOMES("ultimatehomes", "UltimateHomes"),
    SETHOME("sethome", "SetHome"),
    YAML("yaml", "YAML (Universal)");

    private final String commandName;
    private final String displayName;

    ImportExportFormat(String commandName, String displayName) {
        this.commandName = commandName;
        this.displayName = displayName;
    }

    public String getCommandName() {
        return commandName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static String[] getCommandNames() {
        return Arrays.stream(values())
            .map(ImportExportFormat::getCommandName)
            .toArray(String[]::new);
    }

    public static ImportExportFormat fromCommandName(String name) {
        if (name == null) return null;
        return Arrays.stream(values())
            .filter(f -> f.commandName.equalsIgnoreCase(name))
            .findFirst()
            .orElse(null);
    }
}
