/*
 * rzVault modifications Copyright (c) 2026 Raze.
 * Derived from Vault; see NOTICE.md for upstream attribution.
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package dev.raze.rzvault.diagnostic;

import dev.raze.rzvault.RzVault;
import dev.raze.rzvault.config.VaultConfig;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;
import java.util.*;

public final class ServiceDiagnostics {
    private record Row(Object registration, String name, String owner, String priority, String state) {}
    private final RzVault plugin;
    private final ServicesManager services;

    public ServiceDiagnostics(RzVault plugin, ServicesManager services) {
        this.plugin = plugin;
        this.services = services;
    }

    public void send(CommandSender sender) {
        sendService(sender, Economy.class);
        sendService(sender, Permission.class);
        sendService(sender, Chat.class);
    }

    private <T> void sendService(CommandSender sender, Class<T> service) {
        VaultConfig config = plugin.configuration();
        if (config == null) return;
        try {
            RegisteredServiceProvider<T> selected = services.getRegistration(service);
            Collection<RegisteredServiceProvider<T>> registrations = services.getRegistrations(service);
            List<Row> rows = registrations == null ? List.of() : List.copyOf(registrations).stream()
                    .map(registration -> inspect(config, registration)).toList();
            Row active = selected == null ? null : rows.stream().filter(row -> row.registration() == selected)
                    .findFirst().orElseGet(() -> inspect(config, selected));
            plugin.send(sender, "service-line", Map.of("service", service.getSimpleName(),
                    "provider", active == null ? label(config, "not-found") : active.name(),
                    "state", label(config, active == null ? "state-missing" : active.state())));
            for (Row row : rows) {
                plugin.send(sender, "provider-line", Map.of("provider", row.name(), "plugin", row.owner(),
                        "priority", row.priority(), "state", label(config, row.state()),
                        "selected", row.registration() == selected ? label(config, "selected") : ""));
            }
        } catch (RuntimeException | LinkageError e) {
            plugin.send(sender, "service-line", Map.of("service", service.getSimpleName(),
                    "provider", label(config, "unknown-provider"), "state", label(config, "state-error")));
        }
    }

    private static Row inspect(VaultConfig config, RegisteredServiceProvider<?> registration) {
        String name = label(config, "unknown-provider"), owner = label(config, "unknown-owner"), priority = "";
        try {
            var owningPlugin = registration.getPlugin();
            owner = owningPlugin.getName();
            priority = registration.getPriority().name();
            Object provider = registration.getProvider();
            name = switch (provider) {
                case Economy economy -> economy.getName();
                case Permission permission -> permission.getName();
                case Chat chat -> chat.getName();
                default -> throw new IllegalArgumentException("Unknown Vault service");
            };
            if (!validName(name))
                throw new IllegalArgumentException("Invalid provider name");
            boolean enabled = owningPlugin.isEnabled() && switch (provider) {
                case Economy economy -> economy.isEnabled();
                case Permission permission -> permission.isEnabled();
                case Chat chat -> chat.isEnabled();
                default -> false;
            };
            return new Row(registration, name, owner, priority, enabled ? "state-ready" : "state-disabled");
        } catch (RuntimeException | LinkageError e) {
            return new Row(registration, !validName(name) ? label(config, "unknown-provider") : name,
                    owner == null ? label(config, "unknown-owner") : owner, priority, "state-error");
        }
    }
    private static boolean validName(String name) {
        return name != null && !name.isBlank() && name.length() <= 128 && name.chars().noneMatch(Character::isISOControl);
    }
    private static String label(VaultConfig config, String key) { return config.message(key).replace("%prefix%", config.prefix()); }
}
