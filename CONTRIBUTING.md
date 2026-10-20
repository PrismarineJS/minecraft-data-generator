# Contribute to minecraft-data-generator

Contributions can fix behavior, improve documentation, or add focused tests.

## Before you start

Read [the support guide](SUPPORT.md) for questions and issue routing.
Search existing issues and pull requests. Discuss larger API, architecture, or dependency changes before implementation.

Work from `main` and target that branch in your pull request.
Keep each change focused. Avoid unrelated formatting and dependency updates.

## Prepare a checkout

Use JDK 21 for the shared build and Node.js/npm for version tooling. Some Minecraft versions select additional toolchains.

Run the commands below from the repository root unless a command names another directory.
On Windows, use `gradlew.bat` in place of `./gradlew` for Gradle commands.

## Repository layout

- `common/`: shared extraction logic.
- `mc/<version>/`: version-specific source and build.
- `versions.json`: supported versions.
- `tools/`: version tooling.
- `.github/commands/`: automation helpers.

## Verify your change

```bash
npm install
./gradlew :mc:<version>:runServer
```

Replace `<version>` with a directory listed in `versions.json`. The generator runs a server on the client classpath. Review its output under `mc/<version>/run/minecraft-data`. Compare output with the previous version and authoritative game behavior. Do not commit generated run directories. Verify that every `mc/` directory appears in `versions.json`. Use `npm run bump -- <version>` for a new version, then review and fix the copied implementation. For tooling changes, use the StandardJS conventions already configured in `package.json`.

Run the relevant checks before review. State the command and result in the pull request.
If a check cannot run, explain the missing dependency or service. Do not claim it passed.
Keep generated artifacts consistent with their source and review their diff.

## Style and documentation

Follow the existing code conventions and repository formatter. Keep commit hooks enabled.
Add focused tests for changed logic when practical. Avoid tests that only assert source strings.
Update documentation when commands, APIs, configuration, or expected behavior change.
Keep examples small and reproducible. Preserve exact identifiers, commands, and error messages.

## Open a pull request

Explain the problem and resulting behavior. Link related issues without a placeholder issue number.
Include the exact version task and output comparison. Link the corresponding minecraft-data change when applicable.
Include commands and results. State any runtime checks that remain necessary.
Respond to review with a correction or concrete evidence.

Use Conventional Commits: `type(scope): description`, for example `docs(contributing): explain local validation`.
Use a meaningful scope, or omit it. Keep the subject concise and imperative.
Add a body when the reason or compatibility impact is not obvious.

For vulnerabilities, follow [the security reporting instructions](SECURITY.md).
Remove credentials and private data from examples, logs, and screenshots.
