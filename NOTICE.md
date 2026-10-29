# Third-party notices

rzVault modifications: Copyright (c) 2026 Raze (ThreadLeak).
The project is distributed under **LGPL-3.0-or-later**; see [LICENSE](LICENSE)
and the incorporated [GNU GPL v3 text](LICENSES/GPL-3.0.txt).

## Vault and VaultAPI

This project is derived from [Vault](https://github.com/MilkBowl/Vault) and
includes the classic [VaultAPI](https://github.com/MilkBowl/VaultAPI).
VaultAPI's README credits Copyright (C) 2011–2018 Morgan Humes and the MilkBowl
contributors. Original license headers are retained in all vendored API files.

The five API source files under `src/main/java/net/milkbowl/vault/` are copied
without modification from tag **1.7**, commit
`68f14eca202d9b7f1a0fe5a4dcfcfbe26d4a6a40`.
Exact paths and SHA-256 hashes are recorded in
[docs/vault-api-provenance.json](docs/vault-api-provenance.json).
Their upstream license is included in [LICENSES/VaultAPI-LGPL-3.0.txt](LICENSES/VaultAPI-LGPL-3.0.txt).
The API is compiled into the plugin without package relocation. It is not
represented as newly authored rzVault code.

## Maven Wrapper

`mvnw` and `mvnw.cmd` are unmodified Apache Maven Wrapper 3.3.4 scripts,
Copyright the Apache Software Foundation and contributors, licensed under
[Apache License 2.0](LICENSES/Apache-2.0.txt).
They are build tools and are not loaded by the Minecraft server.

## Provided and test dependencies

Paper API and its Adventure dependencies are supplied by the server. JUnit and
Mockito are test dependencies. None is bundled in the plugin JAR. Their dependency
metadata and upstream licenses remain available through the Maven coordinates in
[pom.xml](pom.xml). The installable JAR contains only rzVault, the classic VaultAPI
classes, plugin resources, build metadata, and these licensing notices.
