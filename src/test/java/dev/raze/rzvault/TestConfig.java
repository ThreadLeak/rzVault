package dev.raze.rzvault;

import dev.raze.rzvault.config.VaultConfig;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class TestConfig {
    private TestConfig() {}
    public static YamlConfiguration yaml() throws Exception {
        var yaml = new YamlConfiguration();
        try (var reader = new InputStreamReader(TestConfig.class.getResourceAsStream("/config.yml"), StandardCharsets.UTF_8)) {
            yaml.load(reader);
        }
        return yaml;
    }
    public static VaultConfig defaults() throws Exception { return VaultConfig.load(yaml(), yaml()); }
}
