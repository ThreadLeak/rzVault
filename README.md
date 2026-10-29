# rzVault

[![CodeFactor](https://www.codefactor.io/repository/github/threadleak/rzvault/badge)](https://www.codefactor.io/repository/github/threadleak/rzvault)

[Русский](README.ru.md) · [License](LICENSE) · [Changes](CHANGELOG.md)

A small, classic Vault API bridge for modern Paper servers. Originally created
for HopeWorld and prepared for public use by **Raze / [ThreadLeak](https://github.com/ThreadLeak)**.

rzVault makes the standard `net.milkbowl.vault.*` API available to other plugins,
reports their registered services, and supplies a minimal Bukkit-permission
fallback. **It does not store money, create an economy, or replace LuckPerms.**

## Features

- Classic VaultAPI **1.7**, including deprecated API entry points, with original packages.
- `provides: [Vault]` for modern plugin dependency resolution.
- Economy, Permission and Chat diagnostics showing the actual selected provider,
  its owner, service priority and availability.
- A `Lowest`-priority SuperPerms fallback with owned, temporary permission attachments.
- Editable messages, HEX/legacy colors, optional `%prefix%` and safe config reload.
- No bundled economy adapters, telemetry, update checker, databases or polling tasks.

## Requirements and compatibility

| Component | Target |
| --- | --- |
| Java | **21** |
| Server | **Paper 1.21.11**; other server versions have not been validated |
| Economy | A separate plugin registering the classic Vault `Economy` service |
| Permissions / chat metadata | A provider such as LuckPerms, when those features are needed |

EssentialsX and rzEssentials can register their own economy provider. Without an
Economy provider, rzVault can load but money-dependent plugins still lack an
economy. LuckPerms normally supplies Permission and Chat with a higher priority
than the fallback. Chat here means the Vault metadata API, not a player-chat system.

This is a focused fork, not a bundle of the historical Vault adapters. Older
plugins that require Vault to adapt their private APIs, or that depend on the
internal class `net.milkbowl.vault.Vault`, are outside its compatibility scope.
VaultUnlocked-specific extensions are not implemented. Validate your actual
provider/consumer combination before deployment.

## Installation

1. Stop the server and keep a backup of the existing bridge JAR/configuration.
2. Install **`rzVault-1.0.2.jar`** in `plugins/`.
3. Use **one Vault implementation**: remove the previous Vault, VaultUnlocked or
   rzVault JAR. Do not install `*-sources.jar`.
4. Start the server with your provider plugins, then run `/rzvault` from the
   console or an administrator account.
5. Check the selected providers and test the relevant shop/payment plugins.

Existing money and permission data belong to their provider plugins; rzVault does
not migrate or delete them. A missing/disabled/broken selected service is shown
as such; diagnostics do not silently substitute a lower-priority provider.

## Commands

| Command | Purpose | Permission |
| --- | --- | --- |
| `/rzvault` | Show registered Vault providers and their state | `vault.admin` |
| `/vault-info` | Alias of `/rzvault` | `vault.admin` |
| `/rzvault reload` | Validate and reload messages | `vault.admin` |

`vault.admin` defaults to operators. It grants diagnostics/config reload, not
access to balances or permission-group administration. Reload preserves the last
valid messages on a configuration error and does not re-register providers.

## Configuration

The first start creates `plugins/rzVault/config.yml` with English messages. For
Russian, use [docs/config.ru.yml](docs/config.ru.yml) as your configuration.
Existing configured files are preserved. See [configuration reference](docs/CONFIGURATION.md).

## Build from source

Install **JDK 21**. Maven is pinned and downloaded by the official wrapper on
first use; no globally installed Maven or private dependency cache is required.

```sh
# Linux / macOS
sh ./mvnw --batch-mode --no-transfer-progress verify
```

```powershell
# Windows PowerShell
.\mvnw.cmd --batch-mode --no-transfer-progress verify
```

Outputs:

- `target/rzVault-1.0.2.jar` — installable plugin, with the classic API included.
- `target/rzVault-1.0.2-sources.jar` — source artifact, not a plugin.

The Paper API uses an exact published snapshot revision; Maven/plugins are pinned
and archive timestamps are fixed. This supports repeatable builds with the same
JDK and dependency artifacts. The first build needs access to Maven Central and
the official Paper repository. VaultAPI sources are included in this repository,
so a JitPack lookup is unnecessary.

[GitHub Actions](.github/workflows/build.yml) verifies the build on Linux and
Windows. Workflow permissions are read-only, and actions are pinned to commit SHAs.
Local test success does not claim that hosted Actions have already run.

## For plugin developers

Compile consumers against the classic VaultAPI using `provided` / `compileOnly`;
do not bundle another copy of the API into every consumer. Declare your bridge
dependency on `Vault`, and resolve the actual service through Bukkit:

```java
RegisteredServiceProvider<Economy> registration =
    getServer().getServicesManager().getRegistration(Economy.class);
if (registration == null) {
    // No economy provider is installed/enabled yet.
    return;
}
Economy economy = registration.getProvider();
```

The fallback supports online Bukkit permission queries and temporary additions.
It does not persist permissions, manage real groups, or create world contexts.
Temporary attachments are released on removal, quit and shutdown. Read the
[detailed behavior](docs/ARCHITECTURE.md) before relying on the fallback.

## Contributing and releases

See [CONTRIBUTING.md](CONTRIBUTING.md) and the [release checklist](docs/RELEASING.md).
Report problems with the plugin/server/Java versions, `/rzvault` diagnostics and
relevant errors. See [SECURITY.md](SECURITY.md) for sensitive reports.

## Attribution

Released under **LGPL-3.0-or-later**. Vault / VaultAPI were created by MilkBowl and
contributors. rzVault preserves the classic API's source and attribution; see
[NOTICE.md](NOTICE.md) for the exact upstream revision and included license texts.
