# Architecture and behavior

The plugin is one Maven module and one installable JAR.

| Source | Responsibility |
| --- | --- |
| `dev.raze.rzvault.RzVault` | Startup, command/config lifecycle, duplicate-bridge guard |
| `config.VaultConfig` | Validated immutable message snapshot |
| `diagnostic.ServiceDiagnostics` | Read-only inspection of Bukkit service registrations |
| `provider.SuperPermsPermission` | Minimal, lowest-priority fallback for online permissions |
| `util.TextUtil` | Legacy/HEX color and placeholder rendering |
| `net.milkbowl.vault.*` | Unmodified vendored classic VaultAPI 1.7 |

## Services

rzVault registers only its own Permission fallback at `ServicePriority.Lowest`.
Economy and Chat come from external providers. Bukkit's normal service priority
chooses the active provider. There is no provider polling loop or adapter scanning.
Diagnostics inspect registration/owner state and provider `getName()` / `isEnabled()`.
They do not read balances, create accounts, transfer money or verify database integrity.
A broken provider is reported without hiding the other service categories.

## Temporary permissions

The fallback owns at most one PermissionAttachment per online player. Removing
the final owned permission, quitting, or shutting down releases it. Other plugins'
attachments are untouched. Mutation requires the main server thread, an enabled
plugin and a live player. OfflinePlayer overloads resolve `getPlayer()` rather
than casting the offline object to Player.

Temporary permissions are global Bukkit attachments; the world argument does not
create a contextual grant. They are not persisted. Permanent grants/removals and
group administration return false; group lists/primary group are empty and
`hasGroupSupport()` is false. The legacy `playerInGroup` convention queries
`groups.<group>`; it is not a query of LuckPerms' actual group graph.

## Lifecycle

Startup validates config and command metadata before registering the fallback.
Reload publishes a new immutable snapshot only after validation, retaining the
working snapshot on failure. Shutdown closes owned attachments and unregisters
only services owned by this plugin. There is no custom executor, background
update/telemetry client or scheduled maintenance loop.

Unit tests cover service selection, permissions/attachments, config reload,
commands, diagnostics, error isolation and cleanup. They are not a real Paper
server integration test with every third-party provider.
