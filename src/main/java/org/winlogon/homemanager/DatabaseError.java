package org.winlogon.homemanager;

public enum DatabaseError {
    CONNECTION_FAILURE("Failed to connect to the database."),
    SQL_EXCEPTION("A database error occurred."),
    HOME_ALREADY_EXISTS("A home with that name already exists."),
    HOME_NOT_FOUND("Home not found."),
    WORLD_NOT_LOADED("The world for this home is not loaded.");

    private final String message;

    DatabaseError(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
