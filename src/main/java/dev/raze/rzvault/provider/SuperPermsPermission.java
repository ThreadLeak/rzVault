/*
 * rzVault modifications Copyright (c) 2026 Raze.
 * Derived from Vault; see NOTICE.md for upstream attribution.
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package dev.raze.rzvault.provider;

import net.milkbowl.vault.permission.Permission;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@SuppressWarnings("deprecation")
public final class SuperPermsPermission extends Permission implements Listener, AutoCloseable {

    private static final String NAME = "SuperPerms";

    private record Owned(Player player, PermissionAttachment attachment) {}

    private final Map<UUID, Owned> attachments = new HashMap<>();
    private volatile boolean closed;

    public SuperPermsPermission(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean isEnabled() {
        return !closed && plugin.isEnabled();
    }

    @Override
    public boolean hasSuperPermsCompat() {
        return true;
    }

    @Override
    public boolean hasGroupSupport() {
        return false;
    }

    @Override
    public boolean playerHas(String world, String player, String permission) {
        if (!isEnabled() || player == null || permission == null) {
            return false;
        }
        Player online = plugin.getServer().getPlayerExact(player);
        return online != null && online.isOnline() && online.hasPermission(permission);
    }

    @Override
    public boolean playerHas(String world, OfflinePlayer player, String permission) {
        if (!isEnabled() || player == null || permission == null) {
            return false;
        }
        Player online = player.getPlayer();
        return online != null && online.isOnline() && online.hasPermission(permission);
    }

    @Override
    public boolean playerAddTransient(String world, OfflinePlayer player, String permission) {
        return playerAddTransient(player, permission);
    }

    @Override
    public boolean playerAddTransient(String world, Player player, String permission) {
        return playerAddTransient(player, permission);
    }

    @Override
    public boolean playerRemoveTransient(String world, OfflinePlayer player, String permission) {
        return playerRemoveTransient(player, permission);
    }

    @Override
    public boolean playerRemoveTransient(String world, Player player, String permission) {
        return playerRemoveTransient(player, permission);
    }

    @Override
    public boolean playerAddTransient(OfflinePlayer player, String permission) {
        return canMutate() && player != null && playerAddTransient(player.getPlayer(), permission);
    }

    @Override
    public boolean playerRemoveTransient(OfflinePlayer player, String permission) {
        return canMutate()
                && player != null
                && playerRemoveTransient(player.getPlayer(), permission);
    }

    @Override
    public boolean playerAddTransient(Player player, String permission) {
        if (!canMutate()
                || player == null
                || !player.isOnline()
                || permission == null
                || permission.isBlank()) {
            return false;
        }
        UUID uuid = player.getUniqueId();
        Owned owned = attachments.get(uuid);
        if (owned != null && owned.player() != player) {
            remove(uuid);
            owned = null;
        }
        try {
            if (owned == null) {
                owned = new Owned(player, player.addAttachment(plugin));
                Owned expected = owned;
                attachments.put(uuid, owned);
                owned.attachment()
                        .setRemovalCallback(ignored -> attachments.remove(uuid, expected));
            }
            owned.attachment().setPermission(permission.toLowerCase(Locale.ROOT), true);
            if (!isEnabled() || !player.isOnline() || attachments.get(uuid) != owned) {
                remove(uuid, owned);
                return false;
            }
            return true;
        } catch (RuntimeException e) {
            remove(uuid, owned);
            warn(e);
            return false;
        }
    }

    @Override
    public boolean playerRemoveTransient(Player player, String permission) {
        if (!canMutate()
                || player == null
                || !player.isOnline()
                || permission == null
                || permission.isBlank()) {
            return false;
        }
        UUID uuid = player.getUniqueId();
        Owned owned = attachments.get(uuid);
        if (owned == null || owned.player() != player) {
            return false;
        }
        String node = permission.toLowerCase(Locale.ROOT);
        if (!owned.attachment().getPermissions().containsKey(node)) {
            return false;
        }
        try {
            owned.attachment().unsetPermission(node);
            if (owned.attachment().getPermissions().isEmpty()) {
                remove(uuid, owned);
            }
            return true;
        } catch (RuntimeException e) {
            warn(e);
            return false;
        }
    }

    private boolean canMutate() {
        return isEnabled() && plugin.getServer().isPrimaryThread();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        Owned owned = attachments.get(uuid);
        if (owned != null && owned.player() == event.getPlayer()) {
            remove(uuid);
        }
    }

    private void remove(UUID uuid) {
        remove(uuid, attachments.get(uuid));
    }

    private void remove(UUID uuid, Owned owned) {
        if (owned == null || !attachments.remove(uuid, owned)) {
            return;
        }
        try {
            owned.attachment().remove();
        } catch (RuntimeException e) {
            warn(e);
        }
    }

    @Override
    public void close() {
        closed = true;
        for (UUID uuid : List.copyOf(attachments.keySet())) {
            remove(uuid);
        }
    }

    private void warn(RuntimeException e) {
        plugin.getLogger()
                .warning(
                        "Could not update a SuperPerms transient attachment: "
                                + e.getClass().getSimpleName());
    }

    @Override
    public boolean playerAdd(String world, String player, String permission) {
        return false;
    }

    @Override
    public boolean playerAdd(String world, OfflinePlayer player, String permission) {
        return false;
    }

    @Override
    public boolean playerRemove(String world, String player, String permission) {
        return false;
    }

    @Override
    public boolean playerRemove(String world, OfflinePlayer player, String permission) {
        return false;
    }

    @Override
    public boolean groupHas(String world, String group, String permission) {
        return false;
    }

    @Override
    public boolean groupAdd(String world, String group, String permission) {
        return false;
    }

    @Override
    public boolean groupRemove(String world, String group, String permission) {
        return false;
    }

    @Override
    public boolean playerInGroup(String world, String player, String group) {
        return group != null && !group.isBlank() && playerHas(world, player, "groups." + group);
    }

    @Override
    public boolean playerInGroup(String world, OfflinePlayer player, String group) {
        return group != null && !group.isBlank() && playerHas(world, player, "groups." + group);
    }

    @Override
    public boolean playerAddGroup(String world, String player, String group) {
        return false;
    }

    @Override
    public boolean playerAddGroup(String world, OfflinePlayer player, String group) {
        return false;
    }

    @Override
    public boolean playerRemoveGroup(String world, String player, String group) {
        return false;
    }

    @Override
    public boolean playerRemoveGroup(String world, OfflinePlayer player, String group) {
        return false;
    }

    @Override
    public String[] getPlayerGroups(String world, String player) {
        return new String[0];
    }

    @Override
    public String[] getPlayerGroups(String world, OfflinePlayer player) {
        return new String[0];
    }

    @Override
    public String getPrimaryGroup(String world, String player) {
        return "";
    }

    @Override
    public String getPrimaryGroup(String world, OfflinePlayer player) {
        return "";
    }

    @Override
    public String[] getGroups() {
        return new String[0];
    }
}
