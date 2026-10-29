package dev.raze.rzvault.diagnostic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import dev.raze.rzvault.RuntimeSupport;
import dev.raze.rzvault.TestConfig;

import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ServiceDiagnosticsTest extends RuntimeSupport {
    Economy economy(String name) {
        var economy = mock(Economy.class);
        when(economy.getName()).thenReturn(name);
        when(economy.isEnabled()).thenReturn(true);
        return economy;
    }

    @Test
    void absentEconomyAndChatAreReportedWithoutInventingProviders() {
        plugin.onEnable();
        invoke();
        assertTrue(
                lines().stream()
                        .anyMatch(
                                line ->
                                        line.startsWith("Economy:")
                                                && line.contains("no provider")));
        assertTrue(
                lines().stream()
                        .anyMatch(
                                line ->
                                        line.startsWith("Permission:")
                                                && line.contains("SuperPerms")
                                                && line.contains("available")));
        assertTrue(
                lines().stream()
                        .anyMatch(
                                line -> line.startsWith("Chat:") && line.contains("no provider")));
        assertNull(services.load(Economy.class));
        assertNull(services.load(Chat.class));
    }

    @Test
    void essentialsStyleProviderIsShownWithoutMakingAnyMonetaryRequests() {
        plugin.onEnable();
        var economy = economy("EssentialsX Economy");
        services.register(Economy.class, economy, owner("Essentials"), ServicePriority.Normal);
        invoke();
        assertTrue(
                lines().stream()
                        .anyMatch(
                                line ->
                                        line.startsWith("Economy:")
                                                && line.contains("EssentialsX Economy")
                                                && line.contains("available")));
        assertTrue(
                lines().stream()
                        .anyMatch(line -> line.contains("Essentials") && line.contains("Normal")));
        verify(economy).getName();
        verify(economy).isEnabled();
        verifyNoMoreInteractions(economy);
    }

    @Test
    void luckPermsPriorityWinsAndItsUnregistrationRestoresFallbackWithoutPolling() {
        plugin.onEnable();
        var fallback = services.load(Permission.class);
        var luckPerms = owner("LuckPerms");
        var permission = mock(Permission.class);
        when(permission.getName()).thenReturn("LuckPerms");
        when(permission.isEnabled()).thenReturn(true);
        var chat = mock(Chat.class);
        when(chat.getName()).thenReturn("LuckPerms");
        when(chat.isEnabled()).thenReturn(true);
        services.register(Permission.class, permission, luckPerms, ServicePriority.High);
        services.register(Chat.class, chat, luckPerms, ServicePriority.High);
        assertSame(permission, services.load(Permission.class));
        assertSame(chat, services.load(Chat.class));
        invoke();
        assertTrue(
                lines().stream()
                        .anyMatch(
                                line ->
                                        line.startsWith("Permission:")
                                                && line.contains("LuckPerms")));
        assertEquals(2, services.getRegistrations(Permission.class).size());
        services.unregisterAll(luckPerms);
        assertSame(fallback, services.load(Permission.class));
        assertNull(services.load(Chat.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"name", "enabled", "linkage"})
    void aBrokenProviderDoesNotPreventOtherServiceRows(String failure) {
        plugin.onEnable();
        var economy = economy("Broken");
        switch (failure) {
            case "name" ->
                    when(economy.getName()).thenThrow(new IllegalStateException("Private detail"));
            case "enabled" ->
                    when(economy.isEnabled())
                            .thenThrow(new IllegalStateException("Private detail"));
            case "linkage" ->
                    when(economy.getName()).thenThrow(new NoClassDefFoundError("Private detail"));
        }
        services.register(Economy.class, economy, owner("EconomyOwner"), ServicePriority.Normal);
        assertDoesNotThrow(() -> invoke());
        assertTrue(
                lines().stream()
                        .anyMatch(
                                line ->
                                        line.startsWith("Economy:")
                                                && line.contains("inspection failed")));
        assertTrue(
                lines().stream()
                        .anyMatch(
                                line ->
                                        line.startsWith("Permission:")
                                                && line.contains("SuperPerms")));
        assertTrue(lines().stream().anyMatch(line -> line.startsWith("Chat:")));
        assertTrue(lines().stream().noneMatch(line -> line.contains("Private detail")));
    }

    @Test
    void disabledSelectedProviderIsNotSilentlyReplacedByAHealthyLowerPriorityProvider() {
        plugin.onEnable();
        var lower = economy("Healthy");
        var higher = economy("Disabled");
        when(higher.isEnabled()).thenReturn(false);
        services.register(Economy.class, lower, owner("Lower"), ServicePriority.Low);
        services.register(Economy.class, higher, owner("Higher"), ServicePriority.High);
        invoke();
        assertSame(higher, services.load(Economy.class));
        assertTrue(
                lines().stream()
                        .anyMatch(
                                line ->
                                        line.startsWith("Economy:")
                                                && line.contains("Disabled")
                                                && line.contains("disabled")));
        assertTrue(
                lines().stream()
                        .anyMatch(line -> line.contains("Healthy") && line.contains("available")));
    }

    @Test
    void disabledOwningPluginMarksTheProviderUnavailableWithoutCallingItsIsEnabled() {
        plugin.onEnable();
        var owner = owner("Essentials");
        when(owner.isEnabled()).thenReturn(false);
        var economy = economy("EssentialsX Economy");
        services.register(Economy.class, economy, owner, ServicePriority.Normal);
        invoke();
        assertTrue(
                lines().stream()
                        .anyMatch(
                                line -> line.startsWith("Economy:") && line.contains("disabled")));
        verify(economy, never()).isEnabled();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "bad\nname", "bad\rname"})
    void invalidProviderNamesCannotInjectAdditionalOutputLines(String name) {
        plugin.onEnable();
        services.register(
                Economy.class, economy(name), owner("EconomyOwner"), ServicePriority.Normal);
        invoke();
        assertTrue(lines().stream().noneMatch(line -> line.contains("\n") || line.contains("\r")));
        assertTrue(lines().stream().anyMatch(line -> line.contains("inspection failed")));
    }

    @Test
    void aServiceRegistryExceptionDoesNotHideTheOtherServices() {
        plugin.onEnable();
        var broken = mock(ServicesManager.class);
        when(broken.getRegistration(Economy.class)).thenThrow(new IllegalStateException());
        when(broken.getRegistration(Permission.class))
                .thenReturn(services.getRegistration(Permission.class));
        new ServiceDiagnostics(plugin, broken).send(sender);
        assertTrue(
                lines().stream()
                        .anyMatch(
                                line ->
                                        line.startsWith("Economy:")
                                                && line.contains("inspection failed")));
        assertTrue(
                lines().stream()
                        .anyMatch(
                                line ->
                                        line.startsWith("Permission:")
                                                && line.contains("SuperPerms")));
    }

    @Test
    void labelsSupportPrefixWithoutForcingItIntoAllDiagnosticRows() throws Exception {
        var yaml = TestConfig.yaml();
        yaml.set("prefix", "Prefix ");
        yaml.set("messages.state-ready", "%prefix%ready");
        yaml.save(directory.resolve("config.yml").toFile());
        plugin.onEnable();
        invoke();
        assertTrue(
                lines().stream()
                        .anyMatch(
                                line ->
                                        line.startsWith("Permission:")
                                                && line.contains("Prefix ready")));
        assertTrue(lines().stream().noneMatch(line -> line.contains("%prefix%")));
    }

    @Test
    void pluginShutdownDoesNotUnregisterServicesOwnedByOtherPlugins() {
        plugin.onEnable();
        var economy = economy("EssentialsX Economy");
        services.register(Economy.class, economy, owner("Essentials"), ServicePriority.Normal);
        plugin.onDisable();
        assertSame(economy, services.load(Economy.class));
        assertNull(services.load(Permission.class));
    }
}
