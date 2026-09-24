# TeaVM
## Build contract
The web module compiles the shared `anlyflad-core` code and the `VectoriumWeb` entry point to JavaScript. The build uses TeaVM `0.12.0` and depends on `teavm-core`, `teavm-jso`, and `teavm-classlib`.
From the repository root:
```text
mvn -pl web-teavm -am package
```
The POM binds the TeaVM `compile` goal and the web-resource copy to `process-classes`. Its configured entry point is `com.vectorium.web.VectoriumWeb`; the target is `JAVASCRIPT`, module format `ES2015`, optimization `ADVANCED`, and minification enabled. The output directory and filenames are:
```text
web-teavm/target/webapp/index.html
web-teavm/target/webapp/anlyflad.js
```
`index.html` loads `anlyflad.js` as a module and provides the file input, Vectorize button, preview, download link, and status element. The generated output is static content; the project does not include a web server or backend API.
## JDK compatibility
| Setting | Configured value | Meaning |
|---|---|---|
| Maven Enforcer | `[25,)` | The reactor rejects build JDKs below 25 |
| `maven.compiler.source` / `target` | `1.8` | Source/target compatibility properties |
| `maven.compiler.release` | `8` | Main Java bytecode release |
| `maven.compiler.testRelease` | `8` | Test Java bytecode release |
| `teavm.version` | `0.12.0` | TeaVM dependencies and Maven plugin |
The web module declares `teavm-classlib`, allowing TeaVM to use its Java runtime replacements instead of host JDK classfiles. With that dependency present, the module builds under the normal JDK 25 Enforcer rule:
```text
mvn -pl web-teavm -am package
```
Java 8 is the compilation target, not a supported alternative for the build tool. The browser receives generated JavaScript and does not need a JVM.
## Shared-code boundary
`VectoriumWeb` uses JSO interfaces in `WebDom` for the DOM, file input, `FileReader`, canvas image data, event listeners, and URI encoding. The web path constructs `VectorDocument` values and calls the same core `SvgParser`, `StageRegistry`, `Pipeline`, and `SvgCache` classes as the native front ends. The web module does not use AWT, ImageIO, Swing, or a server API.
The core SVG parser is a manual scanner over the source string, and the path parser uses the core Bezier flattener. This keeps browser-side parsing on the TeaVM-compiled core path; there is no separate JavaScript SVG parser in the repository.
## Browser behavior
- The file input advertises `.png`, `.jpg`, `.jpeg`, and `.svg`; the adapter uses the first selected file and enforces a 16 MiB file-size limit.
- PNG/JPEG data is decoded through a browser image and canvas, then stored as opaque ARGB (`alpha=255`); SVG is read as text and parsed by `SvgParser`.
- The page always uses `PipelineConfig.defaults()`. There are no web controls for presets, stage toggles, or parameter overrides.
- The adapter creates a fresh `SvgCache` and bounded pipeline memoizer for each conversion, serializes the result, previews it as a `data:image/svg+xml` URL, and assigns the same URL to the `anlyflad.svg` download.
- Conversion and serialization are synchronous inside the change/click handlers; the page has no Web Worker or progress protocol.
## Web limitations
- The generated page is a static ES2015 module. The build emits files only; host the `target/webapp` directory with a static file host because no application server is part of the build.
- The browser adapter has no equivalent of the CLI's 64 MiB/64-megapixel input policy. It checks the 16 MiB file limit, positive dimensions, and the Java `int` pixel-buffer limit after decoding.
- The browser UI cannot configure the 15-stage pipeline, so browser output corresponds to the default configuration.
- Browser image decoding and synchronous JavaScript execution impose browser memory and responsiveness limits that are not measured by the native JMH benchmark.
- The TeaVM target is JavaScript, not a JVM distribution; the Java 8 release settings should not be interpreted as permission to build with JDK 8.
