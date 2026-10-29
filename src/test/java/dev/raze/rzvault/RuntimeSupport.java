package dev.raze.rzvault;

import static org.mockito.Mockito.*;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.SimpleServicesManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public abstract class RuntimeSupport {
    @TempDir protected Path directory;
    protected RzVault plugin;
    protected Server server;
    protected PluginManager plugins;
    protected SimpleServicesManager services;
    protected PluginCommand command;
    protected CommandSender sender;
    protected Logger logger;
    protected MockedStatic<Bukkit> bukkit;
    protected final List<Component> messages = new ArrayList<>();

    @BeforeEach
    void setupRuntime() throws Exception {
        TestConfig.yaml().save(directory.resolve("config.yml").toFile());
        plugin = mock(RzVault.class);
        server = mock(Server.class);
        plugins = mock(PluginManager.class);
        services = new SimpleServicesManager();
        command = mock(PluginCommand.class);
        sender = mock(CommandSender.class);
        logger = mock(Logger.class);
        when(plugin.getServer()).thenReturn(server);
        when(plugin.isEnabled()).thenReturn(true);
        when(plugin.getName()).thenReturn("rzVault");
        when(plugin.getLogger()).thenReturn(logger);
        var meta =
                new PluginDescriptionFile(
                        "rzVault", System.getProperty("rzvault.version"), RzVault.class.getName());
        when(plugin.getDescription()).thenReturn(meta);
        when(plugin.getPluginMeta()).thenReturn(meta);
        when(plugin.getDataFolder()).thenReturn(directory.toFile());
        when(plugin.getResource("config.yml"))
                .thenAnswer(call -> TestConfig.class.getResourceAsStream("/config.yml"));
        when(plugin.getCommand("rzvault")).thenReturn(command);
        when(server.getPluginManager()).thenReturn(plugins);
        when(server.getServicesManager()).thenReturn(services);
        when(server.isPrimaryThread()).thenReturn(true);
        when(plugins.getPlugins()).thenReturn(new Plugin[] {plugin});
        when(plugins.getDefaultPermissions(anyBoolean())).thenReturn(Set.of());
        when(sender.hasPermission("vault.admin")).thenReturn(true);
        doAnswer(
                        call -> {
                            messages.add(call.getArgument(0));
                            return null;
                        })
                .when(sender)
                .sendMessage(any(Component.class));
        doCallRealMethod().when(plugin).onEnable();
        doCallRealMethod().when(plugin).onDisable();
        doCallRealMethod().when(plugin).reloadPluginConfig();
        when(plugin.configuration()).thenCallRealMethod();
        doCallRealMethod().when(plugin).send(any(), anyString(), anyMap());
        when(plugin.onCommand(any(), any(), anyString(), any())).thenCallRealMethod();
        when(plugin.onTabComplete(any(), any(), anyString(), any())).thenCallRealMethod();
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getServer).thenReturn(server);
        bukkit.when(Bukkit::getPluginManager).thenReturn(plugins);
    }

    @AfterEach
    void closeRuntime() {
        try {
            plugin.onDisable();
        } finally {
            bukkit.close();
        }
    }

    protected Plugin owner(String name) {
        var owner = mock(Plugin.class);
        when(owner.getName()).thenReturn(name);
        when(owner.isEnabled()).thenReturn(true);
        when(owner.getDescription())
                .thenReturn(new PluginDescriptionFile(name, "test", "example.Plugin"));
        return owner;
    }

    protected List<String> lines() {
        return messages.stream().map(PlainTextComponentSerializer.plainText()::serialize).toList();
    }

    protected void invoke(String... args) {
        plugin.onCommand(sender, command, "rzvault", args);
    }

    protected Object field(String name) throws Exception {
        var field = RzVault.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(plugin);
    }

    protected void field(String name, Object value) throws Exception {
        var field = RzVault.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(plugin, value);
    }
}
