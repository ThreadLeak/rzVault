# Recorded build verification — 2026-10-29

## 1.0.2 — Java style cleanup

- Reproduced exactly 210 `OneStatementPerLine` findings using CodeFactor's
  published Java rules and Checkstyle 14.3.0 before editing owned source code.
- The same complete source scan now reports zero findings, with no exclusions or
  suppressed rules. The build runs the equivalent checked-in rules against both
  runtime and test sources using Maven Checkstyle Plugin 3.6.0.
- A clean Maven 3.9.11 / JDK 21 build passed all 60 tests, including the frozen
  classic API ABI comparison. All five vendored API source hashes are unchanged.
- `javap -c -p` output for all seven owned runtime classes matches version 1.0.1:
  formatting and added braces did not change compiled executable instructions.
- Hosted CI and the CodeFactor grade for this new version still need to run
  after the changes are pushed. A local clean lint result does not guarantee a
  particular hosted grade or replace real-server integration tests.

## 1.0.1 — Public distribution preparation

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
- The local preparation checks below were followed by successful GitHub Actions
  Build #2 on Linux and Windows for commit `99a995c` (confirmed in the workflow
  summary). These do not replace an integration run of every Paper/provider pairing.

Plugin SHA-256: `3050c199bea632470c3aae544de360d1433ae4389143cf00e35482be457ae777`.

Source JAR SHA-256: `07ff869d45a26ee1ea700672615dd9d55a64226fd13831506ed50a91c76dabbb`.
