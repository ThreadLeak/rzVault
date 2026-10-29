# Changelog

## 1.0.2 — 2026-10-29

- Resolve all 210 locally reproduced CodeFactor Java findings by placing each
  statement on its own line in runtime code and tests.
- Expand package imports, add braces to runtime control flow, and consistently
  format owned Java sources without changing plugin behavior.
- Run pinned Checkstyle during every Maven build and the existing CI workflow.
  The checks include tests and the vendored API, with no rule suppressions.
- Preserve the five original VaultAPI source files byte for byte and retain the
  existing classic API, configuration, commands and permissions.

## 1.0.1 — 2026-10-29

Public distribution preparation:

- Include unchanged VaultAPI 1.7 sources with exact commit/hash provenance.
- Replace JitPack shading with a normal, self-contained API/plugin build.
- Pin Maven Wrapper/distribution checksum, build plugins and Paper API revision;
  set repeatable archive timestamps and derive plugin.yml version from the POM.
- Add read-only, commit-pinned Linux/Windows GitHub Actions builds.
- Provide English/Russian documentation, an English default configuration and a
  Russian example. Existing configured server files remain unchanged.
- Preserve the classic API namespace, Vault alias, permission nodes and runtime behavior.
- Include upstream attribution and LGPL/GPL license texts with source and binary.

## 1.0.0 — internal release

- Focused Vault-compatible bridge, service diagnostics, safe message reload and
  owned SuperPerms transient permission fallback for the HopeWorld server.
