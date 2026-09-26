# Anlyflad
Composition-first raster and SVG vectorization studio. The same core pipeline is exposed through a shaded CLI, a Swing desktop application, and a TeaVM browser build.
## Requirements
- JDK 25 or newer. The parent POM's Enforcer rule requires `[25,)`.
- Maven available as `mvn`. There is no Maven wrapper in this repository.
- Java source and test code is compiled with `release`/`testRelease` 8; that is the bytecode target, not the required build JDK.
## Build
Run commands from the repository root:
```text
mvn -o clean verify
mvn -pl cli -am package
mvn -pl desktop-swing -am package
mvn -pl web-teavm -am package
```
`-am` includes `core`, which is required by each application module. Use `-o` for fully offline Maven builds; all dependencies are expected in the local repository.
Expected outputs:
| Module | Output |
|---|---|
| `core` | `core/target/anlyflad-core-1.0.0.jar` and test classes |
| `cli` | `cli/target/anlyflad-cli.jar`, shaded with its dependencies |
| `desktop-swing` | `desktop-swing/target/anlyflad-desktop.jar`, shaded with its dependencies |
| `web-teavm` | `web-teavm/target/webapp/` with `index.html`, `anlyflad.js`, favicons, and the web manifest |
## CLI quick start
```text
java -jar cli/target/anlyflad-cli.jar input.png --output output.svg
java -jar cli/target/anlyflad-cli.jar input.png --output output.svg --mode binary
java -jar cli/target/anlyflad-cli.jar input.png --output output.svg --vector-mode curve --scale 75
java -jar cli/target/anlyflad-cli.jar input.png --output output.svg --scale draft --stage quantize.maxColors=4
java -jar cli/target/anlyflad-cli.jar input.png --output output.svg --stage vectorize.outputScale=2
```
`INPUT` accepts PNG, JPEG, or SVG. `--output/-o` is required and must end in `.svg`; missing parent directories are created. `--scale` accepts `0` to `100` or the aliases `draft` (0), `balanced` (50), and `max` (100).
| Option | Behavior |
|---|---|
| `--mode MODE` | `color` or `binary` raster mode; default `color` |
| `--vector-mode MODE` | `exact`, `contour`, or `curve`; default `curve` |
| `--scale NAME` | Quality scale `0`-`100` or `draft`, `balanced`, `max` |
| `--preset NAME` | `default`, `clean`, `fast`, or `accurate`; presets tune cleanup stages |
| `--no-clean` | Skip every `CLEANER` stage in binary mode |
| `--stage STAGE.PARAM=VALUE` | Set one typed stage parameter; repeatable |
| `--no-stage STAGE` | Disable one stage; `validate`, `vectorize`, and `serialize` are protected |
| `--help` / `--version` | Print help or version `1.0.0` |
Color mode quantizes to an adaptive palette and vectorizes with the selected vector mode. Exact mode emits one rectangle per visible ARGB run, with no artificial path cap and no vertex budget; contour mode emits simplified polygon regions; curve mode supersamples, traces, and fits cubic curves. Binary mode remains available for compact monochrome output. The CLI prints one tab-separated status line per stage and a final path, dimension, and byte count.
## Swing quick start
```text
java -jar desktop-swing/target/anlyflad-desktop.jar
```
A graphical environment is required. The window provides a menu bar with Open/Export/Fit/1:1/Zoom shortcuts, a grouped toolbar with raster mode, vector mode, a 0-100 quality slider, cleanup toggle, and view controls, plus a vertical stage inspector with per-parameter hints, validation, and reset-to-defaults. The canvas renders vector results live through `Path2D`, so zooming shows resolution-independent geometry instead of a scaled bitmap. Long pipeline runs show an animated progress bar and elapsed time, and control changes are debounced before rerunning. Sample inputs up to 1 GiB and 100 megapixels are supported. The desktop JAR bundles 16-256 px application icons for window and taskbar display, and forces English locale for dialogs and formatted values.
## Web quick start
```text
mvn -pl web-teavm -am package
```
Host `web-teavm/target/webapp` as static files. The page accepts `.png`, `.jpg`, `.jpeg`, and `.svg`, provides raster mode, vector mode, a 0-100 quality slider, a pipeline preset, a cleanup toggle, and an output scale, previews the generated SVG in an object URL, and offers `anlyflad.svg` as a download. `index.html` imports the generated ES module and calls `main()`; the page also ships `favicon.svg`, a multi-size `favicon.ico`, PNG favicons, an Apple touch icon, manifest icons, and `site.webmanifest`. No project server is provided; any static host works.
## Modules
| Module | Responsibility |
|---|---|
| `core` | Document/path value model, raster algorithms, SVG parsing/writing, geometry, stage registry, pipeline, caches, and JMH benchmark sources |
| `cli` | Picocli command, file loaders, scale/preset validation, stage logger, shaded executable |
| `desktop-swing` | Swing frame, live canvas, toolbar, stage inspector, background pipeline controller, icons, and desktop loaders |
| `web-teavm` | TeaVM entry point, JSO browser bridge, static browser page with favicons/manifest, and JavaScript packaging |
## Vector modes and quality scale
| Vector mode | Output |
|---|---|
| `exact` | One rectangle per visible color run; pixel-faithful, largest output, no path or vertex budget beyond available memory |
| `contour` | Simplified polygonal color regions traced on a supersampled label grid |
| `curve` | Supersampled regions fitted to cubic Bezier curves; smooth, resolution-independent output |
The quality scale drives palette size, supersampling, and curve fitting:
| Scale | Colors | Supersample | Curve tolerance | Example size (356x359 cartoon) |
|---|---:|---:|---:|---:|
| Draft (0) | 6 | 1x | 1.2 px | ~77 KB |
| Balanced (50, default) | 16 | automatic 1-4x | 0.35 px | ~324 KB |
| Max (100) | 32 | 4x | 0.05 px | ~573 KB |
`vectorize.supersample` (0 auto, 1-4) and `vectorize.outputScale` (0.1-16) are also available as stage parameters. `outputScale` scales the exported SVG width/height while keeping the original coordinate system. The writer quantizes coordinates to two decimals, omits default attributes, minifies path commands, packs exact rectangles into `h`/`v` runs, and adds same-color seam strokes to traced regions.
## Input and output behavior
- CLI and desktop loaders accept files up to 1 GiB and raster images up to 100 megapixels.
- The browser adapter rejects files larger than 1 GiB and decoded raster images larger than 100 megapixels, and preserves the source alpha byte.
- SVG input in color mode is validated against a safe element/reference allowlist and serialized from its original source for lossless passthrough; binary mode parses and runs the cleaner stages.
- Fully transparent pixels are omitted because they have no visible contribution; hidden RGB values below zero alpha are not serialized.
- The serialize stage warms the bounded `SvgCache`; CLI, desktop, and web read the cached string for export.
## Concurrency
Core stages are synchronous and deterministic. Raster supersampling rows and per-ring curve fitting run through a pluggable `ParallelRunner`: the CLI and desktop install a JVM thread-pool runner, while TeaVM builds keep the sequential runner. Parallel output is byte-identical to sequential output. `parallel` copies use per-thread palette-match caches, and a hidden `anlyflad.sequential` system property forces sequential execution for debugging.
## Known limitations
- Exact color mode remains path-heavy for noisy photographs because each visible run is preserved. It no longer fails at a fixed 250k path cap; it grows as far as available memory allows.
- Binary SVG conversion uses the focused parser/writer subset and cannot preserve every SVG feature. Default color mode passes only allowlisted, reference-safe source SVG through unchanged.
- Browser conversion is synchronous on the browser thread; the page has no Web Worker protocol, though the progress UI covers native runs.
- The `hole-fix` descriptor advertises a zero minimum, while the implementation requires a positive `maxArea`.
See [architecture](docs/architecture.md), [performance](docs/performance.md), and [TeaVM notes](docs/teavm.md) for implementation details and verification commands.
## License
MIT. Author: Richard Jiang. See [LICENSE](LICENSE).
