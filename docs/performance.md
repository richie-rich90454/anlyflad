# Performance
## Benchmarks
`core/src/test/java/com/anlyflad/core/benchmark/VectorizationBenchmark.java` is a JMH 1.37 benchmark class. Its annotations request average time in milliseconds, one fork, one 100 ms warmup iteration, and two 100 ms measurement iterations. The setup uses a 64x64 checkerboard raster, a four-color palette, 1024 Morton values, and a 1024-point open path.
The benchmark methods are:
| Method | Work measured |
|---|---|
| `colorLookup` | Periodic `ColorLut.lookupArgb` calls |
| `mortonSort` | Packing and sorting 1024 Morton codes |
| `pipelineRun` | The default `COLOR` `StageRegistry` pipeline on the prepared raster |
| `rasterVectorize` | Binary `RasterVectorizer` on the prepared frame |
| `simplify` | Douglas-Peucker on the prepared point buffer |
| `svgParse` | Parsing a small inline SVG document |
| `svgWrite` | Serializing the prepared document |
`pipelineRun` creates a new `VectorDocument` on each invocation, so the identity-based pipeline memoizer is not intentionally reused by that measurement.
## Invocation
There is no JMH Maven execution or shaded benchmark jar configured in the POM. Compile the test classes, generate a dependency classpath, and invoke JMH's `Main` class. PowerShell from the repository root:
```powershell
mvn -pl core -DskipTests test-compile
mvn -pl core dependency:build-classpath "-Dmdep.includeScope=test" "-Dmdep.outputFile=target/jmh-classpath.txt"
$deps = Get-Content -LiteralPath "core\target\jmh-classpath.txt" -Raw
$cp = "core\target\test-classes;core\target\classes;$deps"
java -cp $cp org.openjdk.jmh.Main -l
java -cp $cp org.openjdk.jmh.Main VectorizationBenchmark
```
The equivalent POSIX classpath form is:
```text
mvn -pl core -DskipTests test-compile
mvn -pl core dependency:build-classpath -Dmdep.includeScope=test -Dmdep.outputFile=target/jmh-classpath.txt
CP="core/target/test-classes:core/target/classes:$(cat core/target/jmh-classpath.txt)"
java -cp "$CP" org.openjdk.jmh.Main VectorizationBenchmark
```
`-l` lists benchmarks without running them. No benchmark result files are checked into the repository; measurements must be collected on the target machine.
## Implemented performance choices
- `ColorLut` uses 32 levels per RGB channel and 32768 lookup entries; `SquaredDeltaTable` precomputes channel deltas for palette construction.
- `ColorRasterVectorizer` preserves exact ARGB runs, merges only identical color/spans vertically, and has no fixed path cap; each rectangle is written as a compact `M`/`h`/`v`/`z` path.
- `RasterSupersampler` parallelizes independent rows, uses premultiplied-alpha bilinear interpolation, and keeps an exact per-thread palette-match cache instead of a shared approximate cache.
- `ColorCurveVectorizer` parallelizes independent ring fits through `ParallelRunner`; the CLI and desktop install a JVM thread pool, TeaVM keeps the sequential runner. Per-ring fitting uses the fitter's unchecked path because traced rings are simple by construction.
- `CubicBezierFitter` uses caller-provided candidate sampling, bounded work counters, and a rectilinear fast path; `fitUnchecked` skips the O(n^2) self-intersection validation that is only needed for arbitrary SVG rings.
- `ColorMerge`, `Dedupe`, and rectangular `Union` use `SpatialHash` candidate lookup instead of comparing every pair globally.
- `BezierFlattener`, `DouglasPeucker`, `TopologicalSort`, and `MortonCodes` use caller-provided or reusable arrays to avoid per-call collection scaffolding.
- `Pipeline` uses immutable stage lists, immutable configuration values, a bounded 16-entry memoizer, and nanosecond stage timings.
- `SvgWriter` quantizes coordinates to two decimals, omits default attributes, minifies commands, and encodes rectangles compactly; `SvgCache` is bounded to 32 entries, 8 MiB per entry, and 64 MiB total serialized UTF-8 data.
## Measured reference
On the 356x359 `sad_barry.png` cartoon with the default curve mode, an eight-core desktop JVM produced:
| Scale | Colors | Supersample | SVG size | Vectorize time |
|---:|---:|---:|---:|---:|
| 0 (draft) | 6 | 1x | ~77 KB | shortest |
| 50 (balanced) | 16 | auto | ~324 KB | ~0.8 s |
| 100 (max) | 32 | 4x | ~573 KB | longest |
The 1424x1436 upscaled input traced and fitted in about 2.3 s. Times vary with CPU, heap, and JIT state; treat them as smoke measurements, not guarantees.
## Cost and concurrency notes
- Supersampling multiplies pixel work by up to 16x on small images. The scale budget keeps the supersampled frame at or below 33.5M pixels, so large inputs trace at 2x or 1x.
- Contour and curve modes are bounded by `maxPaths` (1,000,000) and `maxVertices` (4,000,000). Exact mode has no fixed cap and grows with the run count.
- Parallel output is byte-identical to sequential output; `-Danlyflad.sequential=1` forces sequential execution for debugging and representative timing.
- `HoleFixStage` checks each candidate path against larger paths directly, so its containment work grows quadratically with path count. It has no spatial index.
- The desktop controller uses one daemon worker for the pipeline plus a JVM thread pool for ring fitting, and shows an indeterminate progress bar plus elapsed time while a run is active. Configuration changes are debounced before rerunning.
- The web adapter performs SVG parsing and the full pipeline synchronously in browser callbacks; the TeaVM runner is sequential because JavaScript has no true thread pool.
## Measurement guidance
Use the same JDK, heap, input, and configuration when comparing changes. Keep the JMH annotation settings for comparable defaults, then pass ordinary JMH options such as `-wi`, `-i`, `-f`, or `-prof` only when intentionally changing the measurement protocol. Treat the synthetic 64x64 setup as a microbenchmark, not as a representative image-size guarantee.
