package org.winlogon.homemanager.database;

import java.sql.Types;
import java.util.logging.Logger;

public class SQLiteHandler extends AbstractDatabaseHandler {
    public SQLiteHandler(QueryRunner dbManager, Logger logger) {
        super(dbManager, logger);
    }

    @Override
    protected int getNullType() {
        return Types.REAL;
    }

    @Override
    protected String getDuplicateErrorCode() {
        return "19";
    }

    @Override
    protected String getUpsertSql() {
        return "INSERT OR REPLACE INTO homes (player_uuid, home_name, world_name, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    }

    @Override
    protected String getCreateTableSql() {
        return
            """
            CREATE TABLE IF NOT EXISTS homes (
                player_uuid TEXT,
                home_name TEXT,
                world_name TEXT,
                x REAL,
                y REAL,
                z REAL,
                yaw REAL,
                pitch REAL,
                PRIMARY KEY (player_uuid, home_name)
            );
            """;
    }
}