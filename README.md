# Anlyflad
Composition-first raster and SVG vectorization studio. The same core pipeline is exposed through a shaded CLI, a Swing desktop application, and a TeaVM browser build.
## Requirements
- JDK 25 or newer. The parent POM's Enforcer rule requires `[25,)`.
- Maven available as `mvn`. There is no Maven wrapper in this repository.
- Java source and test code is compiled with `release`/`testRelease` 8; that is the bytecode target, not the required build JDK.
## Build
Run commands from the repository root. The native modules build with JDK 25 or newer:
```text
mvn -pl core,cli,desktop-swing -am clean package
mvn test
mvn -pl cli -am package
mvn -pl desktop-swing -am package
```
Build the browser module with the same JDK:
```text
mvn -pl web-teavm -am package
```
`-am` includes `core`, which is required by each application module. The parent reactor's Enforcer rule requires JDK 25. The web module includes TeaVM's class library so the generated JavaScript does not depend on host JDK classfiles.
Expected outputs:
| Module | Output |
|---|---|
| `core` | `core/target/anlyflad-core-1.0.0.jar` and test classes |
| `cli` | `cli/target/anlyflad-cli.jar`, shaded with its dependencies |
| `desktop-swing` | `desktop-swing/target/anlyflad-desktop.jar`, shaded with its dependencies |
| `web-teavm` | `web-teavm/target/webapp/index.html` and `web-teavm/target/webapp/anlyflad.js` |
## CLI quick start
```text
java -jar cli/target/anlyflad-cli.jar input.png --output output.svg
java -jar cli/target/anlyflad-cli.jar input.svg --output output.svg --preset accurate --stage contour.threshold=192 --no-stage smooth
```
`INPUT` accepts PNG, JPEG, or SVG. `--output/-o` is required and must end in `.svg`; missing parent directories are created.
| Option | Behavior |
|---|---|
| `--preset NAME` | `default`, `clean`, `fast`, or `accurate` |
| `--no-clean` | Skip every `CLEANER` stage |
| `--stage STAGE.PARAM=VALUE` | Set one typed stage parameter; repeatable |
| `--no-stage STAGE` | Disable one stage; repeatable; `serialize` is rejected because it produces the output |
| `--help` / `--version` | Print help or version `1.0.0` |
The `fast` preset sets simplify tolerance `2.0`, smooth passes `0`, and dedupe tolerance `0.5`. The `accurate` preset sets simplify tolerance `0.25` and smooth passes `2`. The CLI prints one tab-separated status line per stage and a final path, dimension, and byte count.
## Swing quick start
```text
java -jar desktop-swing/target/anlyflad-desktop.jar
```
A graphical environment is required. The window provides Open image, Export SVG, preset, clean-output, stage enablement, and per-stage parameter controls. The canvas fits a document, zooms with the mouse wheel, and pans by dragging. Pipeline work runs on a single daemon worker and updates the status bar asynchronously.
## Web quick start
```text
mvn -pl web-teavm -am package
```
Host `web-teavm/target/webapp` as static files. The page accepts `.png`, `.jpg`, `.jpeg`, and `.svg`, reads the first selected file, runs the default pipeline, previews the generated SVG, and offers `anlyflad.svg` as a data-URL download. No project server is provided.
## Modules
| Module | Responsibility |
|---|---|
| `core` | Document/path value model, raster algorithms, SVG parsing/writing, geometry, stage registry, pipeline, caches, and JMH benchmark sources |
| `cli` | Picocli command, file loaders, preset/override validation, stage logger, shaded executable |
| `desktop-swing` | Swing frame, canvas, toolbar, stage inspector, background pipeline controller, and desktop loaders |
| `web-teavm` | TeaVM entry point, JSO browser bridge, static browser page, and JavaScript packaging |
## Input and output behavior
- CLI and desktop loaders accept files up to 64 MiB and raster images up to 64 megapixels.
- The browser adapter rejects files larger than 16 MiB and converts decoded RGBA pixels to opaque ARGB values.
- Raster input runs the raster-only stages; SVG input skips them and continues through universal and cleaner stages.
- The final serializer emits fill-based SVG paths; the serialize stage warms the bounded `SvgCache` without changing the document.
## Known limitations
- The current raster vectorizer is run-based: it emits closed rectangles for contiguous black foreground runs rather than tracing arbitrary contours.
- SVG parsing is a focused path/shape subset. The writer emits fill-only paths; strokes, text, images, gradients, masks, filters, and pattern/use content are not preserved.
- The browser UI exposes only the default configuration and runs conversion synchronously on the browser thread.
- Swing preset selection records the preset name but does not apply the CLI fast/accurate parameter presets; use the stage editor for those values.
- The `hole-fix` descriptor advertises a zero minimum, while the implementation requires a positive `maxArea`.
See [architecture](docs/architecture.md), [performance](docs/performance.md), and [TeaVM notes](docs/teavm.md) for implementation details and verification commands.
## License
MIT. See [LICENSE](LICENSE).
