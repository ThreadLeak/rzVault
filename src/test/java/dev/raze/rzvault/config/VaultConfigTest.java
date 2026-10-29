package dev.raze.rzvault.config;

import dev.raze.rzvault.TestConfig;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class VaultConfigTest {
    YamlConfiguration yaml, defaults;
    @BeforeEach void prepare() throws Exception { yaml = TestConfig.yaml(); defaults = TestConfig.yaml(); }
    VaultConfig read() { return VaultConfig.load(yaml, defaults); }
    @Test void russianExampleRetainsEveryMessageAndItsPlaceholders() throws Exception {
        var russian=new YamlConfiguration();
        russian.load(java.nio.file.Path.of("docs/config.ru.yml").toFile());
        var english=VaultConfig.load(defaults,defaults);var translated=VaultConfig.load(russian,defaults);
        assertEquals(defaults.getConfigurationSection("messages").getKeys(false),russian.getConfigurationSection("messages").getKeys(false));
        for(String key:english.messages().keySet())assertEquals(tokens(english.message(key)),tokens(translated.message(key)),key);
    }
    private static java.util.Set<String> tokens(String text){
        var values=new java.util.HashSet<String>();var matcher=java.util.regex.Pattern.compile("%[a-z_-]+%").matcher(text);
        while(matcher.find())values.add(matcher.group());return values;
    }
    @Test void absentSettingsUseDefaultsWithoutChangingTheFile() {
        var empty = new YamlConfiguration(); var config = VaultConfig.load(empty, defaults);
        assertFalse(config.message("service-line").isBlank()); assertFalse(config.prefix().isBlank()); assertTrue(empty.getKeys(true).isEmpty());
    }
    @Test void explicitEmptyValuesDisableTheirOwnMessagesAndPrefix() {
        yaml.set("prefix", ""); yaml.set("messages.header", "");
        assertEquals("", read().prefix()); assertEquals("", read().message("header"));
    }
    @Test void publishedMessagesAreImmutableAndIndependentOfYamlChanges() {
        var config = read(); yaml.set("messages.header", "Changed");
        assertNotEquals("Changed", config.message("header")); assertThrows(UnsupportedOperationException.class, () -> config.messages().clear());
    }
    @ParameterizedTest @ValueSource(strings = {"prefix", "messages", "messages.header", "messages.state-ready", "config-version"})
    void mistypedKnownValuesAreRejected(String path) {
        yaml.set(path, List.of("wrong")); assertThrows(IllegalArgumentException.class, this::read);
    }
    @ParameterizedTest @ValueSource(ints = {-1, 0, 2})
    void unsupportedVersionsAreRejected(int version) { yaml.set("config-version", version); assertThrows(IllegalArgumentException.class, this::read); }
    @Test void oversizeMessagesAndNestedUnexpectedMessageSectionsAreRejected() {
        yaml.set("messages.header", "x".repeat(8193)); assertThrows(IllegalArgumentException.class, this::read);
        yaml.set("messages.header", ""); yaml.set("messages.extra.child", "bad"); assertThrows(IllegalArgumentException.class, this::read);
    }
}
