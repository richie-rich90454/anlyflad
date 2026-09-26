# Architecture
## System shape
Anlyflad is a Maven reactor with one shared Java core and three delivery modules. Every front end creates a `VectorDocument`, runs the same ordered `Pipeline`, and serializes the result through `SvgWriter`/`SvgCache`.
```text
PNG/JPEG/SVG -> module loader -> VectorDocument -> Pipeline -> VectorDocument -> SvgCache/SvgWriter -> SVG
```
The parent project owns the module list, compiler settings, dependency versions, and the JDK 25 Enforcer rule. `core` is the dependency of `cli`, `desktop-swing`, and `web-teavm`; the front ends do not duplicate vectorization logic.
## Module boundaries
| Package or module | Contents |
|---|---|
| `core/model` | `VectorDocument`, `VectorPath`, `Color`, origins, rectangles, points, path IDs, descriptors, parameters, and metrics |
| `core/raster` | ARGB frame ownership, adaptive color quantization, supersampling, exact/contour/curve vectorizers, grayscale/brightness/contrast, palette lookup, luminance thresholding, and bounded binary run vectorization |
| `core/geometry` | Polygon math, point-in-polygon tests, iterative Douglas-Peucker simplification, cubic Bezier fitting, Bezier flattening, and marching-squares cell contours |
| `core/svg` | XML-like SVG scanner, path command parser, affine transforms, compact deterministic writer, and bounded identity cache |
| `core/stage` | Immutable configuration, fixed standard registry, stage contracts, execution, logging, memoization, quality scale, and the 15 standard stages |
| `core/perf` | Color lookup tables, squared color deltas, spatial hashing, Morton codes, and array-based topological sorting |
| `cli` | Picocli options, file validation, image/SVG loading, scale/preset parsing, and machine-readable stage logging |
| `desktop-swing` | Swing workspace, live vector canvas, stage inspector, background pipeline controller, bundled icons, and desktop loaders |
| `web-teavm` | TeaVM-compiled `VectoriumWeb`, JSO DOM bridge, static page with favicons/manifest, and module bootstrap |
## Document model
`VectorDocument` stores an immutable path list, width, height, origin, output scale, and a copied `int[]` pixel buffer at construction. The owned pixel array is exposed by `getOwnedPixels` for internal pipeline work. A raster origin carries decoded ARGB pixels and normally starts with no paths; an SVG origin carries parsed paths and the parser still creates a zero-filled buffer whose length matches the document dimensions. `withOutputScale` records an export-size multiplier without touching the coordinate system or pixel buffer. `VectorPath` owns finite coordinate pairs, a path ID, closed/open state, fill color, and opacity; bounds, node count, and polygon area are derived at construction.
The origin controls applicability: raster stages report `RASTER_ONLY` and are skipped for SVG documents, while `UNIVERSAL` stages can inspect either origin. `StageDescriptor` supplies labels, descriptions, defaults, and parameter metadata used by the CLI and Swing editor.
## Pipeline construction and execution
`StageRegistry` owns a `LinkedHashMap` of the standard stage factories. `buildPipeline` creates one fresh instance of every stage in registry order; the supplied `PipelineConfig` is applied at run time by `ConfigurableStage` instances. The registry is fixed to the standard set; there is no public registration method for arbitrary stage factories.
`Pipeline.run`:
1. Rejects null input or configuration, disabled validation, and disabled raster vectorization.
2. Looks up `(input object identity, config hash, pipeline identity)` in the memoizer and returns an exact cached output when present.
3. In `COLOR` mode, skips stages implementing `ColorTransformStage` except `QuantizeStage` when the mode is not `EXACT`; validation, raster vectorization, serialization, and custom non-transform stages remain available. Raster `vectorize` honors the configured vector mode; allowlisted SVG input retains its validated source SVG through serialization.
4. Skips a `CLEANER` stage when `config.isClean()` is false in binary mode.
5. Skips a stage when `stage.enabled` is false in the configuration.
6. Applies configuration, checks `appliesTo`, executes `apply`, and rejects a null result.
7. Reports `APPLIED`, `SKIPPED`, or `FAILED` to the logger; `StageException` and runtime failures are propagated.
8. Stores the final document in the memoizer and returns it.
`PipelineConfig` is immutable. It includes `RasterMode.COLOR` and `VectorMode.CURVE` by default; `withRasterMode(BINARY)` selects the monochrome pipeline and `withVectorMode` selects a raster vector mode. Overrides use `stage.parameter` keys; typed getters parse booleans, integers, finite doubles, and comma-separated integer arrays. `withStageDisabled` adds `stage.enabled=false`; `withoutStage` removes only that stage's overrides. The default clean flag is true.
## Standard pipeline
| Order | Stage | Tag | Parameters and defaults | Actual operation |
|---:|---|---|---|---|
| 1 | `validate` | `UNIVERSAL` | none | Checks nonnegative dimensions, pixel-buffer length, null paths, and positive raster dimensions |
| 2 | `preprocess` | `RASTER_ONLY` | `grayscale=true`, `brightness=0`, `contrast=1.0` | Copies pixels, optionally converts to luminance, adjusts brightness, and applies contrast |
| 3 | `quantize` | `RASTER_ONLY` | `palette=0,16777215,65280,16711680`, `maxColors=12` | In exact color mode the stage is skipped; otherwise maps pixels to the nearest adaptive or explicit palette color while preserving alpha |
| 4 | `contour` | `RASTER_ONLY` | `threshold=128` | Converts pixels below the luminance threshold to black and all others to white for binary input |
| 5 | `vectorize` | `RASTER_ONLY` | `mode=curve`, `maxPaths=2147483647`, `maxVertices=4000000`, `curveTolerance=0.5`, `quality=balanced`, `supersample=0`, `outputScale=1.0` | Exact mode emits ARGB run rectangles with no fixed cap; contour mode traces supersampled regions; curve mode supersamples, traces, and fits cubic Bezier curves; `maxPaths` is clamped to 1,000,000 for contour/curve; `outputScale` records an export-size multiplier |
| 6 | `speck-filter` | `CLEANER` | `minArea=1.0` | Removes paths whose area is below the threshold |
| 7 | `color-merge` | `CLEANER` | `distance=8.0` | Uses a spatial hash to recolor nearby paths with similar RGB distance when alpha matches |
| 8 | `union` | `CLEANER` | `distance=0.0` | Unions same-style axis-aligned rectangles that share an exact edge; diagonal and differently colored rectangles remain separate |
| 9 | `simplify` | `CLEANER` | `tolerance=1.0` | Runs iterative Douglas-Peucker simplification for open and closed paths |
| 10 | `smooth` | `CLEANER` | `passes=1` | Inserts rounded-corner line points; the corner ratio is fixed at `0.25` |
| 11 | `hole-fix` | `CLEANER` | `maxArea=100.0` | Removes small closed paths whose points are contained by a larger closed path |
| 12 | `layer-sort` | `CLEANER` | `descending=true` | Sorts by area, then path ID |
| 13 | `dedupe` | `CLEANER` | `tolerance=0.1` | Removes paths equivalent in closure, node count, style, opacity, and coordinates within tolerance |
| 14 | `normalize` | `UNIVERSAL` | `enabled=true` | Translates paths to a nonnegative origin, resizes the document, and repositions raster pixels when needed |
| 15 | `serialize` | `UNIVERSAL` | none | Calls `SvgCache.get(document)` and returns the same document |
For a raster document in default `COLOR` mode, quantization runs unless the vector mode is `EXACT`, destructive cleaners are skipped, and the result is a set of non-overlapping region paths. For an SVG document in `COLOR` mode, the validated source SVG is serialized unchanged after the safe validation/serialization stages. `BINARY` mode considers the full cleanup pipeline and cleaner stages only when `config.isClean()` is true.
## Raster vectorization
`AdaptiveColorQuantizer` builds a deterministic weighted median-cut palette from a 6-bit RGB histogram, with a default of 12 colors and a maximum of 256. `RasterSupersampler` bilinearly upsamples the quantized label grid with premultiplied alpha and snaps every interpolated pixel back to the palette; scaling is 4x, 2x, or 1x chosen from a 33.5M-pixel budget. `ColorContourVectorizer` performs the flood-fill and boundary trace, and `ColorCurveVectorizer` fits the traced rings.
- Exact mode: `ColorRasterVectorizer` scans horizontal runs, merges vertically identical spans, and emits axis-aligned rectangles. It has no path cap or vertex budget; the writer later compresses each rectangle into an `M`/`h`/`v`/`z` path.
- Contour mode: `ColorContourVectorizer.vectorizeSupersampled` traces the supersampled labels back into source coordinates.
- Curve mode: the same trace feeds Douglas-Peucker simplification and `CubicBezierFitter`. Rings are scaled back before fitting, and the traced-contour path uses the fitter's unchecked mode because shared boundaries cannot self-intersect.
The parallel runner fits independent rings on a JVM thread pool in the CLI and desktop, and sequentially in TeaVM. Results are deterministic across task order and thread count.
## Quality scale
`QualityScale` maps a 0-100 value or the aliases `draft`/`balanced`/`max` to palette size, curve tolerance, and supersampling:
| Value | Colors | Supersample | Tolerance |
|---:|---:|---:|---:|
| 0 | 6 | 1 | 1.2 |
| 50 | 16 | auto | 0.35 |
| 100 | 32 | 4 | 0.05 |
Values below 50 interpolate between 0 and 50; values above 50 interpolate between 50 and 100. `VectorizeStage` applies the quality defaults only when `curveTolerance` or `supersample` is not explicitly configured, so explicit stage overrides always win. `outputScale` is independent of quality and is stored on the document for the writer.
## Input and output paths
The CLI and desktop loaders decode PNG/JPEG with ImageIO into `RasterOrigin` documents and read SVG text through `SvgParser`. The web adapter uses `FileReader`, a canvas, and `ImageData` before constructing the same document types, preserving the source alpha byte. `SvgParser` recognizes `svg`, nested `g`/`svg`, `path`, `rect`, `circle`, `ellipse`, `line`, `polygon`, and `polyline`; it applies supported affine transforms, fill/color, opacity, and inline style declarations. Curves and arcs are flattened into line coordinates before becoming `VectorPath` objects. Parsed SVG documents retain the original source for color-mode passthrough only after a conservative element, attribute, URL, and path-count safety check. `SvgWriter` emits an SVG root and fill-based paths; coordinates are quantized to two decimals, default attributes are omitted, command separators are minified, axis-aligned rectangles become compact `h`/`v` runs, and raster-origin traced regions receive a same-color 1 px stroke to seal rendering seams. The writer scales the root `width`/`height` by the document `outputScale` while keeping the original `viewBox`.
The default `SerializeStage` only warms the cache. CLI and desktop explicitly read the cached string for export; the web adapter creates a browser object URL and revokes the previous URL when replacing a result. `index.html` loads the generated ES module and calls its exported `main()` function, which registers the DOM listeners and initializes the controls.
## Caching and concurrency boundaries
`BoundedPipelineMemoizer` holds at most 16 entries and matches input by object identity plus configuration hash and pipeline identity. `SvgCache` holds at most 32 entries, 8 MiB per entry, and 64 MiB total serialized UTF-8 data; `get` matches document identity, while replacement lookup uses document ID. The desktop controller clears both caches when loading a new file or changing configuration, preventing old source documents from accumulating in the interactive session. Both use bounded snapshots; `SvgCache` synchronizes cache mutation. Core stages are synchronous and deterministic. The desktop controller moves loading, pipeline execution, and export to one daemon executor, shows an indeterminate progress bar with elapsed time, and debounces configuration changes before rerunning. The browser adapter performs the same work synchronously inside its event handlers.
## Deliberate constraints
- Stage order and standard stage membership are fixed by `StageRegistry`.
- Contour and curve modes are bounded by `maxPaths` and `maxVertices`; exact mode is bounded only by memory and the SVG byte limit.
- Fully transparent pixels are omitted because they have no visible contribution; hidden RGB values below zero alpha are not serialized.
- Binary SVG conversion uses the focused parser/writer subset and cannot preserve every SVG feature; default color mode passes only allowlisted, reference-safe source SVG through unchanged and rejects active or unsupported source constructs.
- Binary mode remains the compact monochrome pipeline; color cleaners are skipped because they would alter visible pixels.
