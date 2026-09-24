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
| `core/raster` | ARGB frame ownership, grayscale/brightness/contrast, palette lookup, luminance thresholding, and run-based vectorization |
| `core/geometry` | Polygon math, point-in-polygon tests, iterative Douglas-Peucker simplification, Bezier flattening, and marching-squares cell contours |
| `core/svg` | XML-like SVG scanner, path command parser, affine transforms, deterministic writer, and bounded identity cache |
| `core/stage` | Immutable configuration, fixed standard registry, stage contracts, execution, logging, memoization, and the 15 standard stages |
| `core/perf` | Color lookup tables, squared color deltas, spatial hashing, Morton codes, and array-based topological sorting |
| `cli` | Picocli options, file validation, image/SVG loading, preset parsing, and machine-readable stage logging |
| `desktop-swing` | Swing workspace, renderer, stage inspector, and single-worker pipeline controller |
| `web-teavm` | TeaVM-compiled `VectoriumWeb`, JSO DOM bridge, and the static page copied into the build output |
## Document model
`VectorDocument` stores an immutable path list, width, height, origin, and a copied `int[]` pixel buffer at construction. The owned pixel array is exposed by `getOwnedPixels` for internal pipeline work. A raster origin carries decoded ARGB pixels and normally starts with no paths; an SVG origin carries parsed paths and the parser still creates a zero-filled buffer whose length matches the document dimensions. `VectorPath` owns finite coordinate pairs, a path ID, closed/open state, fill color, and opacity; bounds, node count, and polygon area are derived at construction.
The origin controls applicability: raster stages report `RASTER_ONLY` and are skipped for SVG documents, while `UNIVERSAL` stages can inspect either origin. `StageDescriptor` supplies labels, descriptions, defaults, and parameter metadata used by the CLI and Swing editor.
## Pipeline construction and execution
`StageRegistry` owns a `LinkedHashMap` of the standard stage factories. `buildPipeline` creates one fresh instance of every stage in registry order; the supplied `PipelineConfig` is applied at run time by `ConfigurableStage` instances. The registry is fixed to the standard set; there is no public registration method for arbitrary stage factories.
`Pipeline.run`:
1. Rejects null input or configuration.
2. Looks up `(input object identity, config hash)` in the memoizer and returns an exact cached output when present.
3. Skips a `CLEANER` stage when `config.isClean()` is false.
4. Skips a stage when `stage.enabled` is false in the configuration.
5. Applies configuration, checks `appliesTo`, executes `apply`, and rejects a null result.
6. Reports `APPLIED`, `SKIPPED`, or `FAILED` to the logger; `StageException` and runtime failures are propagated.
7. Stores the final document in the memoizer and returns it.
`PipelineConfig` is immutable. Overrides use `stage.parameter` keys; typed getters parse booleans, integers, finite doubles, and comma-separated integer arrays. `withStageDisabled` adds `stage.enabled=false`; `withoutStage` removes only that stage's overrides. The default clean flag is true.
## Standard pipeline
| Order | Stage | Tag | Parameters and defaults | Actual operation |
|---:|---|---|---|---|
| 1 | `validate` | `UNIVERSAL` | none | Checks nonnegative dimensions, pixel-buffer length, null paths, and positive raster dimensions |
| 2 | `preprocess` | `RASTER_ONLY` | `grayscale=true`, `brightness=0`, `contrast=1.0` | Copies pixels, optionally converts to luminance, adjusts brightness, and applies contrast |
| 3 | `quantize` | `RASTER_ONLY` | `palette=0,16777215,65280,16711680` | Replaces each RGB value with the nearest palette representative while preserving alpha |
| 4 | `contour` | `RASTER_ONLY` | `threshold=128` | Converts pixels below the luminance threshold to black and all others to white |
| 5 | `vectorize` | `RASTER_ONLY` | none | Finds black horizontal runs and emits closed rectangular paths |
| 6 | `speck-filter` | `CLEANER` | `minArea=1.0` | Removes paths whose area is below the threshold |
| 7 | `color-merge` | `CLEANER` | `distance=8.0` | Uses a spatial hash to recolor nearby paths with similar RGB distance |
| 8 | `union` | `CLEANER` | `distance=1.0` | Unions touching or near four-node closed rectangular paths and reindexes output IDs |
| 9 | `simplify` | `CLEANER` | `tolerance=1.0` | Runs iterative Douglas-Peucker simplification for open and closed paths |
| 10 | `smooth` | `CLEANER` | `passes=1` | Inserts rounded-corner line points; the corner ratio is fixed at `0.25` |
| 11 | `hole-fix` | `CLEANER` | `maxArea=100.0` | Removes small closed paths whose points are contained by a larger closed path |
| 12 | `layer-sort` | `CLEANER` | `descending=true` | Sorts by area, then path ID |
| 13 | `dedupe` | `CLEANER` | `tolerance=0.1` | Removes paths equivalent in closure, node count, style, opacity, and coordinates within tolerance |
| 14 | `normalize` | `UNIVERSAL` | `enabled=true` | Translates paths to a nonnegative origin, resizes the document, and repositions raster pixels when needed |
| 15 | `serialize` | `UNIVERSAL` | none | Calls `SvgCache.get(document)` and returns the same document |
For a raster document all raster stages are considered. For an SVG document those five raster stages are skipped, so the SVG path list moves through validation, the cleaner group, normalization, and serialization. `clean=false` skips only the cleaner group; it does not disable universal stages.
## Input and output paths
The CLI and desktop loaders decode PNG/JPEG with ImageIO into `RasterOrigin` documents and read SVG text through `SvgParser`. The web adapter uses `FileReader`, a canvas, and `ImageData` before constructing the same document types. `SvgParser` recognizes `svg`, nested `g`/`svg`, `path`, `rect`, `circle`, `ellipse`, `line`, `polygon`, and `polyline`; it applies supported affine transforms, fill/color, opacity, and inline style declarations. Curves and arcs are flattened into line coordinates before becoming `VectorPath` objects. `SvgWriter` emits an SVG root and fill-based `M`/`L`/`Z` paths, combining path opacity and fill alpha into `fill-opacity`.
The default `SerializeStage` only warms the cache. CLI and desktop explicitly read the cached string for export; the web adapter uses the cached string as a data URL.
## Caching and concurrency boundaries
`BoundedPipelineMemoizer` holds at most 16 entries and matches input by object identity plus configuration hash. `SvgCache` holds at most 32 entries; `get` matches document identity, while replacement lookup uses document ID. Both use atomic array snapshots and bounded replacement. Core stages are synchronous. The desktop controller moves loading, pipeline execution, and export to one daemon executor; the browser adapter performs the same work synchronously inside its event handlers.
## Deliberate constraints
- Stage order and standard stage membership are fixed by `StageRegistry`.
- The default rasterizer is run-based rectangle extraction; `MarchingSquares` is a standalone geometry utility and is not selected by the default `VectorizeStage`.
- The SVG writer has no stroke or text model, so round-tripping is not lossless.
- The CLI and Swing front ends expose different configuration breadth: the CLI applies fast/accurate preset overrides, while the Swing preset control only changes the preset name.
