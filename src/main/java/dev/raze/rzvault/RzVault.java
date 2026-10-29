/*
 * rzVault modifications Copyright (c) 2026 Raze.
 * Derived from Vault; see NOTICE.md for upstream attribution.
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package dev.raze.rzvault;

import dev.raze.rzvault.config.VaultConfig;
import dev.raze.rzvault.diagnostic.ServiceDiagnostics;
import dev.raze.rzvault.provider.SuperPermsPermission;
import dev.raze.rzvault.util.TextUtil;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.command.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.*;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;

public final class RzVault extends JavaPlugin implements TabExecutor {
    private volatile VaultConfig configuration;
    private ServicesManager services;
    private SuperPermsPermission fallback;
    private ServiceDiagnostics diagnostics;

    @Override public void onEnable() {
        try {
            rejectDuplicateVault();
            saveDefaultConfig();
            configuration = readConfiguration();
            services = getServer().getServicesManager();
            fallback = new SuperPermsPermission(this);
            diagnostics = new ServiceDiagnostics(this, services);
            var command = Objects.requireNonNull(getCommand("rzvault"), "Missing /rzvault declaration");
            command.setExecutor(this);
            command.setTabCompleter(this);
            updatePermissionMessage(configuration);
            getServer().getPluginManager().registerEvents(fallback, this);
            services.register(Permission.class, fallback, this, ServicePriority.Lowest);
        } catch (Exception | LinkageError e) {
            getLogger().log(Level.SEVERE, "rzVault could not start. Check configuration and duplicate Vault plugins.", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void rejectDuplicateVault() {
        for (Plugin other : getServer().getPluginManager().getPlugins()) {
            if (other == this) continue;
            var description = other.getDescription();
            if (description.getName().equalsIgnoreCase("Vault")
                    || description.getProvides().stream().anyMatch(name -> name.equalsIgnoreCase("Vault")))
                throw new IllegalStateException("Remove the other Vault provider before starting rzVault: " + description.getName());
        }
    }

    public void reloadPluginConfig() {
        VaultConfig next = readConfiguration();
        updatePermissionMessage(next);
        configuration = next;
    }

    private VaultConfig readConfiguration() {
        try (InputStream input = getResource("config.yml")) {
            if (input == null) throw new IOException("Missing bundled config.yml");
            var defaults = new YamlConfiguration();
            defaults.load(new InputStreamReader(input, StandardCharsets.UTF_8));
            var yaml = new YamlConfiguration();
            yaml.load(new File(getDataFolder(), "config.yml"));
            return VaultConfig.load(yaml, defaults);
        } catch (Exception e) { throw new IllegalArgumentException("Invalid rzVault configuration: " + e.getMessage(), e); }
    }

    private void updatePermissionMessage(VaultConfig config) {
        var command = Objects.requireNonNull(getCommand("rzvault"));
        command.permissionMessage(TextUtil.render(config.message("no-permission"), config.prefix(), Map.of()));
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("vault.admin")) { send(sender, "no-permission", Map.of()); return true; }
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            try { reloadPluginConfig(); send(sender, "reload-success", Map.of()); }
            catch (IllegalArgumentException e) {
                send(sender, "reload-error", Map.of());
                getLogger().warning("rzVault reload rejected; previous settings retained: " + e.getMessage());
            }
        } else if (args.length == 0) {
            send(sender, "header", Map.of("version", getPluginMeta().getVersion()));
            if (diagnostics != null) diagnostics.send(sender);
        } else send(sender, "usage", Map.of());
        return true;
    }

    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return sender.hasPermission("vault.admin") && args.length == 1
                && "reload".startsWith(args[0].toLowerCase(Locale.ROOT)) ? List.of("reload") : List.of();
    }

    public void send(CommandSender sender, String key, Map<String, String> values) {
        VaultConfig config = configuration;
        if (config != null && !config.message(key).isEmpty())
            sender.sendMessage(TextUtil.render(config.message(key), config.prefix(), values));
    }

    @Override public void onDisable() {
        try { if (fallback != null) fallback.close(); }
        finally {
            try { if (services != null) services.unregisterAll(this); }
            finally { fallback = null; services = null; diagnostics = null; configuration = null; }
        }
    }

    public VaultConfig configuration() { return configuration; }
}
