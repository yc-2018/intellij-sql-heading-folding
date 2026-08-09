# Repository Guidelines

## Scope

This repository contains an IntelliJ Platform plugin that adds hierarchical headings, folding, reading-mode labels, and outline navigation to SQL consoles and SQL files.

Primary verification targets are IntelliJ IDEA Ultimate and DataGrip. Keep the plugin available to every JetBrains IDE that provides `com.intellij.database`; Marketplace determines product availability from that dependency. Do not introduce dependencies on Java-specific IDEA modules or unnecessarily narrow product compatibility.

## Toolchain

- Use JDK 17 for Gradle and compilation.
- Use the checked-in Gradle Wrapper. Do not require a globally installed Gradle.
- Keep JVM bytecode at version 17.
- The minimum IntelliJ Platform build is `232` (2023.2).
- Keep the upper platform build unrestricted unless a verified incompatibility requires a temporary cap.
- Do not add developer-machine JDK paths or other absolute local paths.

## Architecture

- `model/`: heading data and the pure text parser.
- `folding/`: IntelliJ folding descriptors for heading labels and SQL sections.
- `editor/`: caret-aware reading-mode presentation.
- `toolwindow/`: heading outline, navigation, and bulk folding controls.
- `src/main/resources/META-INF/plugin.xml`: plugin metadata, dependencies, and extension registrations.

Keep parsing independent from IntelliJ APIs where possible. UI and editor integrations may consume parser results but should not duplicate heading syntax rules.

## Heading Contract

- Recognize only full-line SQL comments in the form `-- # Title` through `-- ##### Title`.
- Preserve the document text. Reading mode must be visual only.
- A section ends at the next heading with the same or a higher level.
- Keep the section folding marker on the heading line.
- When the caret is outside a heading line, show a compact `H1` to `H5` label and bold title.
- When the caret enters a heading line, restore the original comment presentation.
- Do not let reading-mode label backgrounds include indentation or padding spaces.

## Coding Style

- Follow existing Kotlin style and IntelliJ Platform APIs.
- Prefer small, focused classes and pure parser functions.
- Keep comments succinct and only for non-obvious behavior.
- Preserve theme compatibility. Avoid hard-coded editor foreground or background colors.
- Dispose listeners, alarms, highlighters, and editor resources correctly.
- Avoid work on every caret offset change when a line-level update is sufficient.

## Verification

Run before committing behavior changes:

```powershell
.\gradlew.bat test buildPlugin
```

Parser behavior changes require focused unit tests in `SqlHeadingParserTest`.

For compatibility changes, test the packaged ZIP in both IntelliJ IDEA Ultimate and DataGrip, then sample other JetBrains IDEs that bundle Database Tools when practical. Use JetBrains Plugin Verifier for representative IDE releases before Marketplace publication.

The Community test sandbox does not bundle `com.intellij.database`; its missing-plugin warning during searchable-options generation is expected and must not be confused with a compilation or unit-test failure.

## Documentation And Releases

- Keep README content bilingual in English and Simplified Chinese.
- Update Marketplace metadata and change notes for user-visible releases.
- Bump the plugin version for every distributable behavior change.
- Pushes to `main` refresh the `continuous` prerelease.
- Version tags matching `v*` create immutable GitHub Releases.
- Never commit `.gradle/`, `.intellijPlatform/`, `.idea/`, `build/`, or local credentials.
