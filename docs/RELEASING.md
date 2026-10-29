# Releasing rzVault

This repository does not automatically publish releases or push code.

1. Set a new version in `pom.xml`; never replace a published JAR with changed
   contents under the same version. Update CHANGELOG and documented filenames.
2. Use JDK 21 and run the wrapper's `clean verify` command. For a first public
   release, also build with an empty local Maven repository to catch private-cache
   dependencies. Confirm the Linux/Windows workflow succeeds after pushing.
3. Check `target/rzVault-VERSION.jar`: correct descriptor version, complete classic
   Vault API, root LICENSE, included license texts and NOTICE.md. Paper, Mockito
   and JUnit classes must not be bundled.
4. Test startup/diagnostics with the intended provider plugins on Paper. Exercise
   temporary permissions and a consumer's real economic operation where relevant.
5. Attach the installable JAR, source JAR, full source archive and SHA-256 checksums
   to the same GitHub release/tag. The full source archive must include the POM,
   wrapper scripts/properties, notices and vendored API source, not just class files.
6. Ensure a clean checkout of that tag can build the distributed artifact. Record
   the JDK/Maven versions and dependency provenance. Keep source available alongside
   the binary; retain LGPL/GPL texts and all upstream attribution.

The only server-installable file is `rzVault-VERSION.jar`. `*-sources.jar` and the
full source ZIP are for development/license correspondence, not plugin installation.

Before the first push, check that only this project is selected as the repository
root. The historical working folder, server backups and other HopeWorld plugins
are not part of the publication. If using a different GitHub repository name, update
project URLs in the POM, descriptor and README before building the release.
