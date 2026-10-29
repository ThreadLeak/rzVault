package dev.raze.rzvault;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class MetadataTest {
    @Test void metadataRetainsTheClassicVaultAliasPermissionAndStartupOrder() throws Exception {
        var yaml = new YamlConfiguration();
        try (var reader = new InputStreamReader(getClass().getResourceAsStream("/plugin.yml"), StandardCharsets.UTF_8)) { yaml.load(reader); }
        assertEquals("rzVault", yaml.getString("name")); assertEquals(System.getProperty("rzvault.version"), yaml.getString("version"));
        assertEquals(RzVault.class.getName(), yaml.getString("main")); assertEquals("STARTUP", yaml.getString("load"));
        assertEquals(List.of("Vault"), yaml.getStringList("provides"));
        assertTrue(yaml.getStringList("loadbefore").containsAll(List.of("Essentials", "LuckPerms")));
        assertEquals(List.of("vault-info"), yaml.getStringList("commands.rzvault.aliases"));
        assertEquals("vault.admin", yaml.getString("commands.rzvault.permission"));
        assertEquals("op", yaml.getString("permissions.vault.admin.default"));
        assertNotNull(getClass().getResource("/LICENSE"));
        assertNotNull(getClass().getResource("/LICENSES/GPL-3.0.txt"));
        assertNotNull(getClass().getResource("/NOTICE.md"));
        assertEquals("https://github.com/ThreadLeak/rzVault", yaml.getString("website"));
    }
}
