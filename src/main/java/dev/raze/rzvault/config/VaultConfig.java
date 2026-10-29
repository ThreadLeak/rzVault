/*
 * rzVault modifications Copyright (c) 2026 Raze.
 * Derived from Vault; see NOTICE.md for upstream attribution.
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package dev.raze.rzvault.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public record VaultConfig(String prefix, Map<String, String> messages) {
    public String message(String key) {
        return messages.getOrDefault(key, "");
    }

    public static VaultConfig load(YamlConfiguration yaml, YamlConfiguration defaults) {
        Object version = yaml.get("config-version");
        if (version != null && (!(version instanceof Integer) || (int) version != 1)) {
            throw new IllegalArgumentException("config-version: expected 1");
        }
        Object section = yaml.get("messages");
        if (section != null && !(section instanceof ConfigurationSection)) {
            throw new IllegalArgumentException("messages: expected section");
        }
        Map<String, String> messages = new HashMap<>();
        ConfigurationSection base =
                Objects.requireNonNull(defaults.getConfigurationSection("messages"));
        for (String key : base.getKeys(false)) {
            messages.put(key, text(yaml, defaults, "messages." + key));
        }
        if (section instanceof ConfigurationSection actual) {
            for (String key : actual.getKeys(false)) {
                messages.put(key, text(yaml, defaults, "messages." + key));
            }
        }
        return new VaultConfig(text(yaml, defaults, "prefix"), Map.copyOf(messages));
    }

    private static String text(YamlConfiguration yaml, YamlConfiguration defaults, String path) {
        Object value = yaml.contains(path) ? yaml.get(path) : defaults.get(path);
        if (!(value instanceof String text) || text.length() > 8192) {
            throw new IllegalArgumentException(path + ": expected text up to 8192 characters");
        }
        return text;
    }
}
