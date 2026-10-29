# Recorded build verification — 2026-10-29

- Windows, JDK 21, official Maven Wrapper 3.3.4 / Maven 3.9.11.
- A fresh build used an empty local Maven repository and public Maven Central /
  Paper dependencies. It did not rely on the former private project's cache.
- 60 tests passed, with zero failures, errors or skipped cases.
- Six public VaultAPI types / 222 ABI records match a frozen baseline captured
  from the original 1.7 binary. Five vendored source-file hashes match the recorded
  upstream commit. A compiler-private enum switch helper is outside that ABI.
- A repeated clean packaging build produced identical plugin and source JARs.
  Archive timestamps and text checkout rules are fixed; vendored API bytes are preserved.
- Checked plugin version/alias, packaged licenses and notices, source correspondence,
  English/Russian config consistency, relative documentation links and workflow YAML.
- Publication files contain no legacy adapters, build caches, server data, personal
  workstation paths or detected credential patterns. Paper/test libraries are absent
  from the installable JAR.
- GitHub-hosted Linux/Windows jobs are configured but will run only after publication.
  These are local tests, not an integration run of every real Paper/provider pairing.

Plugin SHA-256: `3050c199bea632470c3aae544de360d1433ae4389143cf00e35482be457ae777`.

Source JAR SHA-256: `07ff869d45a26ee1ea700672615dd9d55a64226fd13831506ed50a91c76dabbb`.
