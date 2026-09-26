# Contributing to Anlyflad

Thanks for helping improve Anlyflad. This guide covers the workflow, standards,
and verification commands used by the project.

By participating you agree to follow the [Code of Conduct](CODE_OF_CONDUCT.md).
Security issues must be reported privately as described in [SECURITY.md](SECURITY.md).

## Ways to contribute

- Report reproducible bugs with the bug report issue form.
- Propose features and scope changes with the feature request form.
- Improve the documentation, examples, and fixtures.
- Fix issues, add tests, or improve performance and quality.
- Triage issues and review pull requests.

## Development setup

Requirements:

- JDK 25 or newer (the parent POM enforces `[25,)`).
- Maven available as `mvn` (no Maven wrapper is provided).
- A graphical environment for the Swing module; browser testing needs a browser.

Build and test from the repository root:

```text
mvn -o clean verify
```

`-o` runs fully offline; all dependencies are expected in the local repository.
Individual modules:

```text
mvn -o -pl core test
mvn -o -pl cli -am package
mvn -o -pl desktop-swing -am package
mvn -o -pl web-teavm -am package
mvn -o -pl app -am package
```

The main artifact is `app/target/anlyflad.jar`: no arguments prints the CLI
help, `--desktop` opens the Swing UI, and `--web [--port N]` starts the embedded
static server.

## Project layout

| Module | Responsibility |
|---|---|
| `core` | Value model, raster algorithms, geometry, SVG parser/writer, stages, pipeline, docs renderer |
| `cli` | Picocli command, loaders, configuration validation, shaded CLI |
| `desktop-swing` | Swing frame, canvas, toolbar, stage inspector, documentation window, fonts, icons |
| `web-teavm` | TeaVM browser app, docs page, fonts, favicons, manifest |
| `app` | Launcher dispatch and the shaded all-in-one JAR |

## Coding standards

- Java 8 source and bytecode target; do not use newer language features.
- No new runtime dependencies without discussion; prefer the JDK standard library.
- Follow the existing style: compact declarations, fully qualified names only
  where the surrounding file already uses them, no comments unless the reason is
  non-obvious.
- Keep code TeaVM-compatible in `core` when it can be reached by the web module;
  avoid AWT, ImageIO, threads, and `java.util.concurrent` in shared code.
- Keep output deterministic. Parallel and sequential paths must produce
  byte-identical SVG for the same input and configuration.
- Core algorithms belong in `core`; do not duplicate vectorization logic in
  front ends.
- Update the relevant documentation in `docs/` when behavior, parameters, or
  limits change.
- Never commit generated build outputs, temporary files, personal images such as
  `sad_barry.png`, or `test.svg`.

## Tests

Add focused tests for behavior changes:

- Core logic and algorithms: `core/src/test`.
- CLI behavior: `cli/src/test`.
- Swing panels and controllers: `desktop-swing/src/test`.
- Web behavior: verify through the built `web-teavm/target/webapp` bundle.

Run the full reactor before opening a pull request:

```text
mvn -o clean verify
node --check web-teavm/target/webapp/anlyflad.js
```

## Commits

Use conventional commit messages, one logical change per commit:

```text
feat(core): add adaptive color quantization
fix(desktop): seal raster preview seams
perf(core): fit raster rings in parallel
docs: document fat jar and docs viewer
```

Keep the subject line imperative, lower case, and under 72 characters. Reference
issues in the body or pull request when relevant.

## Pull requests

1. Fork the repository and create a topic branch from `main`.
2. Make the change and add tests.
3. Run `mvn -o clean verify` and `node --check` when the web bundle changed.
4. Fill in the pull request template, including tests, screenshots for UI
   changes, and any breaking changes.
5. Keep pull requests focused; split unrelated changes into separate PRs.
6. Be responsive to review feedback and push follow-up commits rather than
   rewriting history once review has started.

Small, focused, well-tested pull requests are reviewed fastest.

## Reporting bugs

Use the bug report form and include:

- Operating system and JDK version.
- Module: CLI, desktop, web, or library.
- Exact command or UI steps.
- Input format, dimensions, and settings (raster mode, vector mode, quality).
- Expected and actual output, including logs or error messages.
- The smallest input that reproduces the problem when possible.

## License

By contributing you agree that your contributions are licensed under the
[MIT License](LICENSE).
