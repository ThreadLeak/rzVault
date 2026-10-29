package dev.raze.rzvault;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import dev.raze.rzvault.provider.SuperPermsPermission;

import net.milkbowl.vault.permission.Permission;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.ServicePriority;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.util.List;

class RuntimeTest extends RuntimeSupport {
    @Test
    void startupRegistersOnlyLowestFallbackAndHasNoTasksOrCustomSuccessLogs() throws Exception {
        plugin.onEnable();
        var registration = services.getRegistration(Permission.class);
        assertNotNull(plugin.configuration());
        assertSame(plugin, registration.getPlugin());
        assertEquals(ServicePriority.Lowest, registration.getPriority());
        assertInstanceOf(SuperPermsPermission.class, registration.getProvider());
        verify(plugins).registerEvents((SuperPermsPermission) registration.getProvider(), plugin);
        verify(command).setExecutor(plugin);
        verify(command).setTabCompleter(plugin);
        verify(server, never()).getScheduler();
        verify(logger, never()).info(anyString());
        plugin.onDisable();
        plugin.onDisable();
        assertNull(plugin.configuration());
        assertNull(services.load(Permission.class));
        assertNull(field("fallback"));
        assertNull(field("diagnostics"));
        assertNull(field("services"));
    }

    @Test
    void commandsRequireAdminPermissionAndTabCompletionDoesNotLeakReload() {
        plugin.onEnable();
        when(sender.hasPermission("vault.admin")).thenReturn(false);
        invoke();
        assertEquals(1, messages.size());
        assertTrue(lines().getFirst().contains("do not have permission"));
        assertTrue(plugin.onTabComplete(sender, command, "rzvault", new String[] {""}).isEmpty());
        when(sender.hasPermission("vault.admin")).thenReturn(true);
        assertEquals(
                List.of("reload"),
                plugin.onTabComplete(sender, command, "vault-info", new String[] {"re"}));
        assertTrue(
                plugin.onTabComplete(sender, command, "rzvault", new String[] {"unknown"})
                        .isEmpty());
        messages.clear();
        invoke("reload", "extra");
        assertTrue(lines().getFirst().contains("Use"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"yaml", "type", "missing-file"})
    void invalidReloadPreservesThePreviousSnapshotAndRegisteredProvider(String kind)
            throws Exception {
        plugin.onEnable();
        var before = plugin.configuration();
        var fallback = services.load(Permission.class);
        if (kind.equals("yaml"))
            Files.writeString(directory.resolve("config.yml"), "messages: [broken\n");
        if (kind.equals("type")) {
            var yaml = TestConfig.yaml();
            yaml.set("prefix", true);
            yaml.save(directory.resolve("config.yml").toFile());
        }
        if (kind.equals("missing-file")) Files.delete(directory.resolve("config.yml"));
        invoke("reload");
        assertSame(before, plugin.configuration());
        assertSame(fallback, services.load(Permission.class));
        assertTrue(lines().getFirst().contains("Previous settings"));
        verify(command, times(1)).permissionMessage(any());
    }

    @Test
    void validReloadChangesMessagesWithoutReplacingTheFallback() throws Exception {
        plugin.onEnable();
        var fallback = services.load(Permission.class);
        var yaml = TestConfig.yaml();
        yaml.set("prefix", "&aNew ");
        yaml.set("messages.reload-success", "%prefix%Done\\nNext");
        yaml.save(directory.resolve("config.yml").toFile());
        invoke("RELOAD");
        assertEquals(List.of("New Done\nNext"), lines());
        assertSame(fallback, services.load(Permission.class));
        verify(command, times(2)).permissionMessage(any());
    }

    @Test
    void malformedStartupAndMissingCommandDoNotPublishAService() throws Exception {
        var yaml = TestConfig.yaml();
        yaml.set("messages", false);
        yaml.save(directory.resolve("config.yml").toFile());
        plugin.onEnable();
        assertNull(services.load(Permission.class));
        verify(plugins).disablePlugin(plugin);
        TestConfig.yaml().save(directory.resolve("config.yml").toFile());
        when(plugin.getCommand("rzvault")).thenReturn(null);
        plugin.onEnable();
        assertNull(services.load(Permission.class));
        verify(plugins, times(2)).disablePlugin(plugin);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Vault", "VaultUnlocked"})
    void duplicateVaultImplementationsAreRejectedBeforeRegisteringServices(String name)
            throws Exception {
        var other = owner(name);
        when(other.getDescription())
                .thenReturn(
                        new PluginDescriptionFile(
                                new java.io.StringReader(
                                        "name: "
                                                + name
                                                + "\n"
                                                + "version: test\n"
                                                + "main: example.Plugin\n"
                                                + "provides: [Vault]\n")));
        when(plugins.getPlugins()).thenReturn(new Plugin[] {plugin, other});
        plugin.onEnable();
        verify(plugins).disablePlugin(plugin);
        assertNull(services.load(Permission.class));
        assertNull(plugin.configuration());
    }

    @Test
    void cleanupStillUnregistersServicesIfFallbackCleanupThrows() throws Exception {
        plugin.onEnable();
        var fallback = mock(SuperPermsPermission.class);
        field("fallback", fallback);
        doThrow(new IllegalStateException()).when(fallback).close();
        assertThrows(IllegalStateException.class, plugin::onDisable);
        assertNull(services.load(Permission.class));
        assertNull(field("fallback"));
        assertNull(plugin.configuration());
    }
}
