package org.winlogon.homemanager.database;

public record AdvancedDatabaseConfig(
    boolean enabled,
    String host,
    int port,
    String database,
    String user,
    String password
) {}

