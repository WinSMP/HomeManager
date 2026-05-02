package org.winlogon.homemanager.database;

import java.sql.Types;
import java.util.logging.Logger;

public class SQLiteHandler extends AbstractDatabaseHandler {
    public SQLiteHandler(QueryRunner dbManager, AsyncDatabaseExecutor asyncExecutor, Logger logger) {
        super(dbManager, asyncExecutor, logger);
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
        return super.getCreateTableSql();
    }
}
