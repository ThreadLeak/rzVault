# Contributing

1. Use JDK 21 and run `sh ./mvnw verify` or `.\mvnw.cmd verify` on Windows.
2. Keep runtime code in its functional `dev.raze.rzvault` package.
3. Preserve the classic `net.milkbowl.vault.*` ABI, `provides: Vault`, and existing
   config keys/permissions. The vendored API is intentionally unchanged; discuss
   API changes separately and update provenance if the upstream revision changes.
4. Keep provider handling read-only outside the owned permission fallback. No
   balance storage, mandatory economy plugin, telemetry or polling loops.
5. Add a regression test for a behavior change. Do not replace real API behavior
   with a mock merely to make a failing test pass.
6. Explain what changed and how it was checked in the pull request. Keep secrets,
   live server files, build caches and generated JARs out of commits.

## Versions

`pom.xml` is the single source of the plugin version; `plugin.yml` is filtered by
Maven. The current release is 1.0.1. The next changed distributable must use a new
patch version (1.0.2), with README/CHANGELOG/release filenames kept consistent.
Compile/test-only runs do not create a distributable release. See
[docs/RELEASING.md](docs/RELEASING.md).

Contributions are provided under the project's LGPL-3.0-or-later license.
Preserve existing third-party headers and notices.
