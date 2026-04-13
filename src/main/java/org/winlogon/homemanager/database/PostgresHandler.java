package org.winlogon.homemanager.database;

import java.sql.Types;
import java.util.logging.Logger;

public class PostgresHandler extends AbstractDatabaseHandler {
    public PostgresHandler(QueryRunner dbManager, Logger logger) {
        super(dbManager, logger);
    }

    @Override
    protected int getNullType() {
        return Types.DOUBLE;
    }

    @Override
    protected String getDuplicateErrorCode() {
        return "23505";
    }

    @Override
    protected String getUpsertSql() {
        return """
            INSERT INTO homes (player_uuid, home_name, world_name, x, y, z, yaw, pitch)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (player_uuid, home_name) DO UPDATE SET
            world_name = EXCLUDED.world_name, x = EXCLUDED.x, y = EXCLUDED.y, z = EXCLUDED.z, yaw = EXCLUDED.yaw, pitch = EXCLUDED.pitch""";
    }

    @Override
    protected String getCreateTableSql() {
        return """
            CREATE TABLE IF NOT EXISTS homes (
                player_uuid VARCHAR(36) NOT NULL,
                home_name VARCHAR(255) NOT NULL,
                world_name VARCHAR(255),
                x DOUBLE PRECISION,
                y DOUBLE PRECISION,
                z DOUBLE PRECISION,
                yaw DOUBLE PRECISION,
                pitch DOUBLE PRECISION,
                PRIMARY KEY (player_uuid, home_name)
            );
            """;
    }
}