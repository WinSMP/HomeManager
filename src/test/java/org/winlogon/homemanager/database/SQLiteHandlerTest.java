package org.winlogon.homemanager.database;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.sqlite.SQLiteDataSource;

import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class SQLiteHandlerTest {
    private SQLiteHandler handler;

    @SuppressWarnings("unchecked")
    private <T> T createProxy(Class<T> clazz, Map<String, Object> methods) {
        return (T) Proxy.newProxyInstance(
            clazz.getClassLoader(),
            new Class<?>[]{clazz},
            (_, method, _) -> {
                if (methods.containsKey(method.getName())) {
                    return methods.get(method.getName());
                }
                if (method.getReturnType().equals(Void.TYPE)) {
                    return null;
                }
                // Return default values for common types
                if (method.getReturnType().equals(String.class)) return "";
                if (method.getReturnType().equals(Boolean.TYPE)) return false;
                if (method.getReturnType().equals(Integer.TYPE)) return 0;
                return null;
            }
        );
    }

    @Test
    void testCreateAndGetHome() throws SQLException {
        Plugin plugin = createProxy(Plugin.class, Map.of(
            "getLogger", Logger.getLogger("TestLogger"),
            "getName", "TestPlugin"
        ));

        var ds = new SQLiteDataSource();
        ds.setUrl("jdbc:sqlite::memory:");

        var queryRunner = new QueryRunner(plugin, ds, 1);
        var asyncExecutor = new AsyncDatabaseExecutor(plugin);
        handler = new SQLiteHandler(queryRunner, asyncExecutor, plugin.getLogger());

        var playerUuid = UUID.randomUUID();
        Player mockPlayer = createProxy(Player.class, Map.of(
            "getUniqueId", playerUuid,
            "getName", "TestPlayer"
        ));

        World mockWorld = createProxy(World.class, Map.of(
            "getName", "world"
        ));

        var loc = new Location(mockWorld, 10, 64, 20, 90f, 0f);

        var created = handler.createHome(mockPlayer, "test-home", loc);
        assertTrue(created.isPresent());
        assertTrue(created.get());

        var createdDuplicate = handler.createHome(mockPlayer, "test-home", loc);
        assertTrue(createdDuplicate.isPresent());
        assertFalse(createdDuplicate.get());
    }

    @Test
    void testHomeExists() throws SQLException {
        Plugin plugin = createProxy(Plugin.class, Map.of(
            "getLogger", Logger.getLogger("TestLogger"),
            "getName", "TestPlugin"
        ));

        var ds = new SQLiteDataSource();
        ds.setUrl("jdbc:sqlite::memory:");

        var queryRunner = new QueryRunner(plugin, ds, 1);
        var asyncExecutor = new AsyncDatabaseExecutor(plugin);
        handler = new SQLiteHandler(queryRunner, asyncExecutor, plugin.getLogger());

        var playerUuid = UUID.randomUUID();
        Player mockPlayer = createProxy(Player.class, Map.of(
            "getUniqueId", playerUuid
        ));

        World mockWorld = createProxy(World.class, Map.of(
            "getName", "world"
        ));
        var loc = new Location(mockWorld, 10, 64, 20);

        handler.createHome(mockPlayer, "existing", loc);
        assertTrue(handler.homeExists(playerUuid, "existing"));
        assertFalse(handler.homeExists(playerUuid, "non-existent"));
    }
}
