package dev.raze.rzvault;

import static org.mockito.Mockito.*;

import dev.raze.rzvault.provider.SuperPermsPermission;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

public abstract class PermissionSupport {
    protected RzVault plugin;
    protected Server server;
    protected PluginManager plugins;
    protected SuperPermsPermission provider;
    protected MockedStatic<Bukkit> bukkit;

    @BeforeEach
    void preparePermissions() {
        plugin = mock(RzVault.class);
        server = mock(Server.class);
        plugins = mock(PluginManager.class);
        when(plugin.isEnabled()).thenReturn(true);
        when(plugin.getServer()).thenReturn(server);
        when(plugin.getLogger()).thenReturn(mock(Logger.class));
        when(server.isPrimaryThread()).thenReturn(true);
        when(server.getPluginManager()).thenReturn(plugins);
        when(plugins.getDefaultPermissions(anyBoolean())).thenReturn(Set.of());
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getPluginManager).thenReturn(plugins);
        bukkit.when(Bukkit::getServer).thenReturn(server);
        provider = new SuperPermsPermission(plugin);
    }

    @AfterEach
    void closePermissions() {
        try {
            provider.close();
        } finally {
            bukkit.close();
        }
    }

    protected final class User {
        public final Player player = mock(Player.class);
        public final PermissibleBase permissions;
        public final List<PermissionAttachment> created = new ArrayList<>();
        public final Set<PermissionAttachment> live = new HashSet<>();

        public User() {
            this(UUID.randomUUID());
        }

        public User(UUID uuid) {
            when(player.getUniqueId()).thenReturn(uuid);
            when(player.isOnline()).thenReturn(true);
            when(player.getName()).thenReturn("Player");
            when(player.getPlayer()).thenReturn(player);
            permissions = new PermissibleBase(player);
            when(player.getEffectivePermissions())
                    .thenAnswer(call -> permissions.getEffectivePermissions());
            when(player.hasPermission(anyString()))
                    .thenAnswer(call -> permissions.hasPermission((String) call.getArgument(0)));
            doAnswer(
                            call -> {
                                permissions.recalculatePermissions();
                                return null;
                            })
                    .when(player)
                    .recalculatePermissions();
            when(player.addAttachment(any(Plugin.class)))
                    .thenAnswer(
                            call -> {
                                var attachment = permissions.addAttachment(call.getArgument(0));
                                created.add(attachment);
                                live.add(attachment);
                                return attachment;
                            });
            doAnswer(
                            call -> {
                                PermissionAttachment attachment = call.getArgument(0);
                                permissions.removeAttachment(attachment);
                                live.remove(attachment);
                                return null;
                            })
                    .when(player)
                    .removeAttachment(any(PermissionAttachment.class));
        }
    }
}
