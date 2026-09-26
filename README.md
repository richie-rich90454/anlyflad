# Anlyflad: Raster and SVG Vectorization for Java 8+

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Runtime: Java 8+](https://img.shields.io/badge/Runtime-Java%208%2B-orange.svg)](#runtime-requirements)
[![Build: JDK 25+](https://img.shields.io/badge/Build-JDK%2025%2B-blue.svg)](#build)

Anlyflad is a fast, multithreaded raster-to-SVG vectorization studio written in
pure Java. It converts PNG, JPEG, and SVG files into resolution-independent SVG
with three vector modes, a continuous quality scale, adaptive color
quantization, subpixel supersampling, and parallel curve fitting. The same core
pipeline is exposed through a command-line interface, a Swing desktop
application, and a TeaVM browser build, plus an all-in-one executable JAR.

Keywords: raster to SVG, image to SVG, PNG to SVG, JPEG to SVG, vectorization,
vectorizer, image tracing, contour tracing, curve fitting, vector graphics,
official Java library, CLI, Swing desktop, browser, TeaVM, WebAssembly-ready
tooling, multithreaded image processing, adaptive color quantization.

## Table of contents

- [Runtime requirements](#runtime-requirements)
- [Build](#build)
- [All-in-one JAR](#all-in-one-jar)
- [CLI quick start](#cli-quick-start)
- [Desktop quick start](#desktop-quick-start)
- [Web quick start](#web-quick-start)
- [Vector modes](#vector-modes)
- [Quality scale](#quality-scale)
- [Documentation and fonts](#documentation-and-fonts)
- [Modules](#modules)
- [Concurrency](#concurrency)
- [Input and output behavior](#input-and-output-behavior)
- [Known limitations](#known-limitations)
- [Community](#community)
- [License](#license)

## Runtime requirements

Anlyflad **runs on every Java 8 or newer runtime**. The published classes are
compiled with `-release 8` and use only Java 8 APIs, so the CLI, the Swing
desktop application, and the all-in-one JAR work on Java 8, Java 11, Java 17,
Java 21, Java 25, and later releases. The browser build runs without any JVM.

A newer JDK such as JDK 25 is only needed to **build** the project from source.
The parent POM enforces JDK 25 for the Maven build, not for running the result.

## Build

Run commands from the repository root:

```text
mvn -o clean verify
mvn -pl cli -am package
mvn -pl desktop-swing -am package
mvn -pl web-teavm -am package
mvn -pl app -am package
```

`-am` includes `core`, which every application module depends on. `-o` runs
fully offline from the local Maven repository.

Expected outputs:

| Module | Output |
|---|---|
| `core` | `core/target/anlyflad-core-1.0.0.jar` and test classes |
| `cli` | `cli/target/anlyflad-cli.jar`, shaded with its dependencies |
| `desktop-swing` | `desktop-swing/target/anlyflad-desktop.jar`, shaded with its dependencies |
| `web-teavm` | `web-teavm/target/webapp/` with `index.html`, `docs.html`, `anlyflad.js`, fonts, docs, favicons, and the web manifest |
| `app` | `app/target/anlyflad.jar`, the all-in-one fat JAR |

## All-in-one JAR

```text
mvn -pl app -am package

java -jar app/target/anlyflad.jar
java -jar app/target/anlyflad.jar input.png -o output.svg
java -jar app/target/anlyflad.jar --desktop
java -jar app/target/anlyflad.jar --web
java -jar app/target/anlyflad.jar --web --port 9000
```

- With no arguments, the JAR prints the CLI help.
- With input and output arguments, it runs the CLI conversion.
- `--desktop` launches the Swing desktop application.
- `--web` starts an embedded static server on `http://127.0.0.1:8080/`, with
  the browser application at `/` and the documentation at `/docs.html`.
- `--port N` selects a different port.

The fat JAR bundles the core, CLI, desktop, web assets, documentation, and Noto
Sans fonts. It has no external runtime dependencies and makes no CDN requests.

## CLI quick start

```text
java -jar app/target/anlyflad.jar input.png --output output.svg
java -jar app/target/anlyflad.jar input.jpg --output output.svg --mode binary
java -jar app/target/anlyflad.jar input.png --output output.svg --vector-mode curve --scale 75
java -jar app/target/anlyflad.jar input.png --output output.svg --scale draft
java -jar app/target/anlyflad.jar input.png --output output.svg --stage vectorize.outputScale=2
java -jar cli/target/anlyflad-cli.jar input.svg --output output.svg --preset accurate
```

`INPUT` accepts PNG, JPEG, or SVG. `--output` or `-o` is required and must end
in `.svg`; missing parent directories are created.

| Option | Behavior |
|---|---|
| `--mode MODE` | `color` or `binary` raster mode; default `color` |
| `--vector-mode MODE` | `exact`, `contour`, or `curve`; default `curve` |
| `--scale NAME` | Quality scale `0` to `100`, or `draft`, `balanced`, `max` |
| `--preset NAME` | `default`, `clean`, `fast`, or `accurate` |
| `--no-clean` | Skip cleaner stages in binary mode |
| `--stage STAGE.PARAM=VALUE` | Set one typed stage parameter; repeatable |
| `--no-stage STAGE` | Disable one stage; `validate`, `vectorize`, and `serialize` are protected |
| `--help` / `--version` | Print help or version `1.0.0` |

## Desktop quick start

```text
java -jar app/target/anlyflad.jar --desktop
```

A graphical environment is required. The window provides a menu bar, a grouped
toolbar with raster mode, vector mode, a 0 to 100 quality slider, a cleanup
toggle and view controls, a vertical stage inspector with parameter hints,
validation and reset, and a live `Path2D` canvas that stays
resolution-independent when zoomed. Long runs show an animated progress bar and
elapsed time. The desktop JAR bundles 16 to 256 pixel icons and forces English
locale for dialogs and formatted values.

## Web quick start

```text
mvn -pl web-teavm -am package
```

Host `web-teavm/target/webapp` as static files, or run `java -jar
app/target/anlyflad.jar --web`. The browser application mirrors the desktop
controls: raster mode, vector mode, quality slider, preset, cleanup toggle,
output scale, run and view controls, a stage inspector, a live preview, and a
download link. The generated ES module is loaded and initialized with
`import { main } from "./anlyflad.js"; main();`. The page ships a scalable
favicon, a multi-size ICO, PNG and Apple touch icons, a web manifest, and a
documentation page at `docs.html`.

## Vector modes

| Mode | Output | Best for |
|---|---|---|
| `exact` | One rectangle per visible color run, pixel-faithful, no artificial path cap | Lossless pixel-accurate color |
| `contour` | Simplified polygonal color regions traced on a supersampled label grid | Crisp edges and moderate file sizes |
| `curve` | Supersampled regions fitted to cubic Bezier curves | Smooth, resolution-independent output |

## Quality scale

The quality scale drives palette size, supersampling, and curve fitting. Low
values produce tiny SVGs; high values produce the most detailed result.

| Scale | Colors | Supersample | Curve tolerance | Example size (356x359 cartoon) |
|---|---:|---:|---:|---:|
| Draft, 0 | 6 | 1x | 1.2 px | about 77 KB |
| Balanced, 50, default | 16 | automatic 1x to 4x | 0.35 px | about 324 KB |
| Max, 100 | 32 | 4x | 0.05 px | about 573 KB |

`vectorize.supersample` accepts 0 for automatic or 1 to 4.
`vectorize.outputScale` accepts 0.1 to 16 and scales the exported width and
height while keeping the original coordinate system. The writer quantizes
coordinates to two decimals, omits default attributes, minifies commands,
encodes rectangles compactly, and seals traced-region seams.

## Documentation and fonts

- Desktop: Help, then Documentation, or press F1. A separate window lists README,
  Architecture, Performance, and TeaVM pages and renders them as formatted HTML.
- Web: `docs.html` renders the same bundled pages with client-side Markdown
  rendering, and is linked from Help, then Documentation.
- Fonts: every module uses bundled Noto Sans exclusively, with Regular, Medium,
  and Bold weights and the SIL OFL 1.1 license included. The web copies are
  Latin-subset WOFF2 files; the desktop copies are full TTFs registered as Swing
  UI defaults. No CDN or network font is used.

See [docs/architecture.md](docs/architecture.md),
[docs/performance.md](docs/performance.md), and [docs/teavm.md](docs/teavm.md).

## Modules

| Module | Responsibility |
|---|---|
| `core` | Document and path model, raster algorithms, geometry, adaptive quantization, supersampling, contour and curve vectorizers, SVG parser and writer, stages, pipeline, Markdown renderer |
| `cli` | Picocli command, file loaders, scale and preset validation, stage logger, shaded executable |
| `desktop-swing` | Swing frame, live canvas, toolbar, stage inspector, documentation window, background pipeline controller, Noto Sans fonts, icons |
| `web-teavm` | TeaVM entry point, JSO browser bridge, static app and docs pages, fonts, favicons, manifest |
| `app` | Launcher dispatch for CLI, desktop, and web, embedded static server, shaded all-in-one JAR |

## Concurrency

Core stages are synchronous and deterministic. Raster supersampling rows and
per-ring curve fitting run through a pluggable `ParallelRunner`. The CLI and
desktop install a JVM thread-pool runner; TeaVM builds keep the sequential
runner. Parallel output is byte-identical to sequential output. Pass
`-Danlyflad.sequential=1` to force sequential execution for debugging or
representative timing.

## Input and output behavior

- CLI and desktop loaders accept files up to 1 GiB and raster images up to 100
  megapixels.
- The browser adapter rejects files larger than 1 GiB and decoded raster images
  larger than 100 megapixels, and preserves the source alpha byte.
- SVG input in color mode is validated against a safe element and reference
  allowlist and serialized from its original source for lossless passthrough.
- Fully transparent pixels are omitted because they have no visible
  contribution; hidden RGB values below zero alpha are not serialized.
- The serialize stage warms the bounded `SvgCache`; CLI, desktop, and web read
  the cached string for export.

## Known limitations

- Exact mode remains path-heavy for noisy photographs because every visible run
  is preserved. It has no fixed 250,000 path cap, but memory is the practical
  bound.
- Binary SVG conversion uses a focused parser and writer subset and cannot
  preserve every SVG feature.
- Browser conversion is synchronous on the browser thread; there is no Web
  Worker protocol.
- The `hole-fix` descriptor advertises a zero minimum while the implementation
  requires a positive `maxArea`.

## Community

- [Contributing guide](CONTRIBUTING.md)
- [Code of Conduct](CODE_OF_CONDUCT.md)
- [Security policy](SECURITY.md) and
  [private vulnerability reports](https://github.com/richie-rich90454/Anlyflad/security/advisories/new)
- [Bug reports](https://github.com/richie-rich90454/Anlyflad/issues/new?template=bug_report.yml)
- [Feature requests](https://github.com/richie-rich90454/Anlyflad/issues/new?template=feature_request.yml)
- [Discussions](https://github.com/richie-rich90454/Anlyflad/discussions)

Maintainer: [@richie-rich90454](https://github.com/richie-rich90454)

## License

MIT. Author: Richard Jiang. See [LICENSE](LICENSE).
