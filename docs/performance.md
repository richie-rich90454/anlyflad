# Performance
## What is benchmarked
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
- `RasterVectorizer` scans rows, collects contiguous foreground runs, and merges unchanged runs between adjacent rows using reusable arrays.
- `ColorRasterVectorizer` preserves exact ARGB runs, merges only identical color/spans vertically, and bounds both row scratch storage and output paths.
- `ColorMerge`, `Dedupe`, and rectangular `Union` use `SpatialHash` candidate lookup instead of comparing every pair globally.
- `BezierFlattener`, `DouglasPeucker`, `TopologicalSort`, and `MortonCodes` use caller-provided or reusable arrays to avoid per-call collection scaffolding.
- `Pipeline` uses immutable stage lists, immutable configuration values, a bounded 16-entry memoizer, and nanosecond stage timings.
- `SvgCache` is bounded to 32 entries, 8 MiB per entry, and 64 MiB total serialized UTF-8 data; the serialize stage warms it without rewriting the document.
## Cost and concurrency notes
- The default color path is synchronous and memory-resident. It skips destructive stages and creates a bounded list of pixel-aligned paths; noisy images can approach the path budget.
- Binary mode copies pixel arrays in preprocessing, quantization, and thresholding before producing path output.
- `HoleFixStage` checks each candidate path against larger paths directly, so its containment work grows quadratically with path count. It has no spatial index.
- The desktop controller uses one daemon worker, not one worker per stage or per document. `awaitIdle` waits at most 10 seconds.
- The web adapter performs SVG parsing and the full pipeline synchronously in browser callbacks after file reading/decoding. A large image can therefore block the page while it converts.
- Binary mode's fixed stage order means disabling an early stage changes the input seen by later stages; benchmark or measure the exact configuration used by a front end.
## Measurement guidance
Use the same JDK, heap, input, and configuration when comparing changes. Keep the JMH annotation settings for comparable defaults, then pass ordinary JMH options such as `-wi`, `-i`, `-f`, or `-prof` only when intentionally changing the measurement protocol. Treat the synthetic 64x64 setup as a microbenchmark, not as a representative image-size guarantee.
