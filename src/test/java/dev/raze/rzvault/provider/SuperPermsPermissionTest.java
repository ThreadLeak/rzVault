package dev.raze.rzvault.provider;

import dev.raze.rzvault.PermissionSupport;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SuperPermsPermissionTest extends PermissionSupport {
    @ParameterizedTest @ValueSource(ints = {0, 1, 2, 3})
    void everyTransientOverloadWorksWithoutRecursionOrCastingAnOfflineWrapper(int overload) {
        var user = new User(); var offline = mock(OfflinePlayer.class);
        when(offline.isOnline()).thenReturn(true); when(offline.getPlayer()).thenReturn(user.player);
        boolean added = switch (overload) {
            case 0 -> provider.playerAddTransient(user.player, "test.use");
            case 1 -> provider.playerAddTransient(offline, "test.use");
            case 2 -> provider.playerAddTransient("world", offline, "test.use");
            default -> provider.playerAddTransient("world", user.player, "test.use");
        };
        assertTrue(added); assertTrue(user.player.hasPermission("test.use")); assertEquals(1, user.live.size());
        boolean removed = switch (overload) {
            case 0 -> provider.playerRemoveTransient(user.player, "test.use");
            case 1 -> provider.playerRemoveTransient(offline, "test.use");
            case 2 -> provider.playerRemoveTransient("world", offline, "test.use");
            default -> provider.playerRemoveTransient("world", user.player, "test.use");
        };
        assertTrue(removed); assertFalse(user.player.hasPermission("test.use")); assertTrue(user.live.isEmpty());
    }
    @Test void aThousandAddRemoveCyclesNeverRetainEmptyAttachments() {
        var user = new User();
        for (int i = 0; i < 1000; i++) {
            assertTrue(provider.playerAddTransient(user.player, "test.use")); assertEquals(1, user.live.size());
            assertTrue(provider.playerRemoveTransient(user.player, "test.use")); assertTrue(user.live.isEmpty());
        }
        assertTrue(user.permissions.getEffectivePermissions().isEmpty());
    }
    @Test void severalNodesShareOneAttachmentUntilTheLastPermissionIsRemoved() {
        var user = new User();
        assertTrue(provider.playerAddTransient(user.player, "TEST.ONE"));
        assertTrue(provider.playerAddTransient(user.player, "test.two")); assertEquals(1, user.created.size());
        assertTrue(provider.playerRemoveTransient(user.player, "test.one")); assertEquals(1, user.live.size());
        assertTrue(user.player.hasPermission("test.two"));
        assertFalse(provider.playerRemoveTransient(user.player, "missing"));
        assertTrue(provider.playerRemoveTransient(user.player, "TEST.TWO")); assertTrue(user.live.isEmpty());
    }
    @Test void quitRemovesOnlyTheDepartingPlayersAttachment() {
        var first = new User(); var second = new User();
        provider.playerAddTransient(first.player, "test.use"); provider.playerAddTransient(second.player, "test.use");
        var quit = mock(PlayerQuitEvent.class); when(quit.getPlayer()).thenReturn(first.player); provider.onQuit(quit);
        assertTrue(first.live.isEmpty()); assertEquals(1, second.live.size());
        provider.onQuit(quit); assertTrue(first.live.isEmpty());
    }
    @Test void closeIsIdempotentAndRejectsStaleProviderReferences() {
        var first = new User(); var second = new User();
        provider.playerAddTransient(first.player, "test.use"); provider.playerAddTransient(second.player, "test.use");
        provider.close(); provider.close(); assertTrue(first.live.isEmpty()); assertTrue(second.live.isEmpty());
        assertFalse(provider.isEnabled()); assertFalse(provider.playerAddTransient(first.player, "test.new"));
    }
    @Test void reconnectAndAnOldRemovalCallbackCannotDeleteTheNewSession() {
        var old = new User(); provider.playerAddTransient(old.player, "test.old");
        var attachment = old.created.getFirst();
        var next = new User(old.player.getUniqueId()); provider.playerAddTransient(next.player, "test.new");
        assertTrue(old.live.isEmpty()); assertEquals(1, next.live.size());
        attachment.getRemovalCallback().attachmentRemoved(attachment);
        var quit = mock(PlayerQuitEvent.class); when(quit.getPlayer()).thenReturn(old.player); provider.onQuit(quit);
        assertTrue(provider.playerRemoveTransient(next.player, "test.new")); assertTrue(next.live.isEmpty());
    }
    @Test void externalAttachmentRemovalReleasesTheProvidersTrackingEntry() {
        var user = new User(); provider.playerAddTransient(user.player, "test.use");
        user.player.removeAttachment(user.created.getFirst()); assertTrue(user.live.isEmpty());
        assertTrue(provider.playerAddTransient(user.player, "test.new")); assertEquals(1, user.live.size());
        provider.close(); assertTrue(user.live.isEmpty());
    }
    @Test void unrelatedPluginsAttachmentsAreNeverModifiedOrRemoved() {
        var user = new User(); var other = mock(Plugin.class); when(other.isEnabled()).thenReturn(true);
        var foreign = user.player.addAttachment(other); foreign.setPermission("foreign.use", true);
        provider.playerAddTransient(user.player, "test.use");
        assertFalse(provider.playerRemoveTransient(user.player, "foreign.use")); provider.close();
        assertEquals(1, user.live.size()); assertTrue(user.player.hasPermission("foreign.use"));
        user.player.removeAttachment(foreign);
    }
    @Test void disabledPluginOrAsyncCallCannotMutatePermissions() {
        var user = new User(); when(plugin.isEnabled()).thenReturn(false);
        assertFalse(provider.playerAddTransient(user.player, "test.use")); when(plugin.isEnabled()).thenReturn(true);
        provider.playerAddTransient(user.player, "test.use"); when(server.isPrimaryThread()).thenReturn(false);
        assertFalse(provider.playerAddTransient(user.player, "test.new")); assertFalse(provider.playerRemoveTransient(user.player, "test.use"));
        assertTrue(user.player.hasPermission("test.use")); when(server.isPrimaryThread()).thenReturn(true);
    }
    @Test void offlineAndInvalidArgumentsFailWithoutCreatingAttachments() {
        var offline = mock(OfflinePlayer.class);
        assertFalse(provider.playerAddTransient(offline, "test.use")); assertFalse(provider.playerRemoveTransient(offline, "test.use"));
        assertFalse(provider.playerAddTransient((OfflinePlayer) null, "test.use"));
        assertFalse(provider.playerAddTransient((Player) null, "test.use"));
        var user = new User(); assertFalse(provider.playerAddTransient(user.player, null));
        assertFalse(provider.playerAddTransient(user.player, " ")); assertTrue(user.created.isEmpty());
    }
    @Test void ordinaryPermissionQueriesKeepTheClassicOnlineOnlyFallbackContract() {
        var user = new User(); when(server.getPlayerExact("Player")).thenReturn(user.player);
        provider.playerAddTransient(user.player, "groups.staff");
        assertTrue(provider.playerHas("ignored-world", "Player", "groups.staff"));
        assertTrue(provider.playerHas(null, (OfflinePlayer) user.player, "groups.staff"));
        assertTrue(provider.playerInGroup((String) null, "Player", "staff")); assertFalse(provider.hasGroupSupport());
        assertFalse(provider.playerAdd((String) null, "Player", "test.use")); assertFalse(provider.playerRemove((String) null, "Player", "test.use"));
        assertFalse(provider.groupHas((String) null, "staff", "test.use")); assertEquals(0, provider.getGroups().length);
        assertEquals("", provider.getPrimaryGroup((String) null, "Player"));
    }
    @Test void failedAttachmentCreationDoesNotAffectOtherPlayersOrRetainAnEntry() {
        var broken = new User(); var healthy = new User();
        doThrow(new IllegalStateException()).when(broken.player).addAttachment(plugin);
        assertFalse(provider.playerAddTransient(broken.player, "test.use"));
        assertTrue(provider.playerAddTransient(healthy.player, "test.use")); provider.close();
        assertTrue(healthy.live.isEmpty()); assertTrue(broken.live.isEmpty());
    }
    @Test void disableDuringAttachmentCreationCannotLeaveATransientPermissionBehind() {
        var user = new User();
        doAnswer(call -> {
            var attachment = user.permissions.addAttachment(plugin); user.created.add(attachment); user.live.add(attachment);
            provider.close(); return attachment;
        }).when(user.player).addAttachment(plugin);
        assertFalse(provider.playerAddTransient(user.player, "test.use")); assertTrue(user.live.isEmpty());
    }
    @Test void quitDuringAttachmentCreationCannotLeaveAnUntrackedPlayerReference() {
        var user = new User();
        doAnswer(call -> {
            var attachment = user.permissions.addAttachment(plugin); user.created.add(attachment); user.live.add(attachment);
            when(user.player.isOnline()).thenReturn(false); return attachment;
        }).when(user.player).addAttachment(plugin);
        assertFalse(provider.playerAddTransient(user.player, "test.use")); assertTrue(user.live.isEmpty());
    }
    @Test void oneFailedAttachmentRemovalDoesNotPreventCleanupOfOtherPlayers() {
        var first = new User(); var second = new User();
        provider.playerAddTransient(first.player, "test.use"); provider.playerAddTransient(second.player, "test.use");
        doThrow(new IllegalStateException()).when(first.player).removeAttachment(any());
        assertDoesNotThrow(provider::close); assertTrue(second.live.isEmpty());
        assertFalse(provider.playerAddTransient(second.player, "test.new"));
        doAnswer(call -> { first.permissions.removeAttachment(call.getArgument(0)); first.live.clear(); return null; })
                .when(first.player).removeAttachment(any());
        first.created.getFirst().remove(); assertTrue(first.live.isEmpty());
    }
}
