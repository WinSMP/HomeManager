package org.winlogon.homemanager.database;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.sqlite.SQLiteDataSource;

import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class SQLiteHandlerTest {
    private ServerMock server;
    private TestPlugin plugin;
    private SQLiteHandler handler;
    private Player player;
    private World world;

    @BeforeEach
    void setUp() throws SQLException {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(TestPlugin.class);

        Logger logger = plugin.getLogger();
        SQLiteDataSource ds = new SQLiteDataSource();
        ds.setUrl("jdbc:sqlite::memory:");

        QueryRunner queryRunner = new QueryRunner(plugin, ds, 1);
        AsyncDatabaseExecutor asyncExecutor = new AsyncDatabaseExecutor(plugin);
        handler = new SQLiteHandler(queryRunner, asyncExecutor, logger);

        world = server.addSimpleWorld("test_world");
        player = server.addPlayer();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void testCreateAndGetHome() {
        Location loc = new Location(world, 10, 64, 20, 90f, 0f);

        // Test creation
        var created = handler.createHome(player, "my-home", loc);
        assertTrue(created.isPresent(), "Result should be present");
        assertTrue(created.get(), "Home should be created");

        // Test duplicate
        var createdDuplicate = handler.createHome(player, "my-home", loc);
        assertTrue(createdDuplicate.isPresent(), "Result should be present");
        assertFalse(createdDuplicate.get(), "Duplicate home should not be created");

        // Test retrieval
        var locOpt = handler.getHomeLocation(player, "my-home");
        assertTrue(locOpt.isPresent(), "Home location should be found");
        Location foundLoc = locOpt.get();
        assertEquals(loc.getX(), foundLoc.getX(), 0.001);
        assertEquals(loc.getY(), foundLoc.getY(), 0.001);
        assertEquals(loc.getZ(), foundLoc.getZ(), 0.001);
        assertEquals(loc.getYaw(), foundLoc.getYaw(), 0.001);
        assertEquals(loc.getPitch(), foundLoc.getPitch(), 0.001);
        assertEquals(world.getName(), foundLoc.getWorld().getName());
    }

    @Test
    void testHomeExists() {
        Location loc = new Location(world, 10, 64, 20);
        handler.createHome(player, "exists", loc);

        assertTrue(handler.homeExists(player.getUniqueId(), "exists"));
        assertFalse(handler.homeExists(player.getUniqueId(), "not-exists"));
        assertFalse(handler.homeExists(UUID.randomUUID(), "exists"));
    }

    @Test
    void testGetHomes() {
        Location loc = new Location(world, 10, 64, 20);
        handler.createHome(player, "home1", loc);
        handler.createHome(player, "home2", loc);

        var homes = handler.getHomes(player);
        assertEquals(2, homes.size());
        assertTrue(homes.contains("home1"));
        assertTrue(homes.contains("home2"));
    }
}
