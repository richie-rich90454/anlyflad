# TeaVM
## Build contract
The web module compiles the shared `anlyflad-core` code and the `VectoriumWeb` entry point to JavaScript. The build uses TeaVM `0.12.0` and depends on `teavm-core`, `teavm-jso`, and `teavm-classlib`.
From the repository root:
```text
mvn -pl web-teavm -am package
```
The POM binds the TeaVM `compile` goal and the web-resource copy to `process-classes`. Its configured entry point is `com.anlyflad.web.VectoriumWeb`; the target is `JAVASCRIPT`, module format `ES2015`, optimization `ADVANCED`, and minification enabled. The output directory contains:
```text
web-teavm/target/webapp/index.html
web-teavm/target/webapp/anlyflad.js
web-teavm/target/webapp/favicon.svg
web-teavm/target/webapp/favicon.ico
web-teavm/target/webapp/favicon-16.png
web-teavm/target/webapp/favicon-32.png
web-teavm/target/webapp/favicon-48.png
web-teavm/target/webapp/apple-touch-icon.png
web-teavm/target/webapp/icon-192.png
web-teavm/target/webapp/icon-512.png
web-teavm/target/webapp/site.webmanifest
```
`index.html` loads `anlyflad.js` as an ES module and calls the exported `main()` function:
```html
<script type="module">
import { main } from "./anlyflad.js";
main();
</script>
```
The TeaVM module does not auto-run its entry point, so the explicit call is required. The page provides the file input, Color/Binary raster mode, vector mode, 0-100 quality slider, Vectorize button, preview, download link, and status element. The generated output is static content; the project does not include a web server or backend API.
## JDK compatibility
| Setting | Configured value | Meaning |
|---|---|---|
| Maven Enforcer | `[25,)` | The reactor rejects build JDKs below 25 |
| `maven.compiler.source` / `target` | `1.8` | Source/target compatibility properties |
| `maven.compiler.release` | `8` | Main Java bytecode release |
| `maven.compiler.testRelease` | `8` | Test Java bytecode release |
| `teavm.version` | `0.12.0` | TeaVM dependencies and Maven plugin |
The web module declares `teavm-classlib`, allowing TeaVM to use its Java runtime replacements instead of host JDK classfiles. With that dependency present, the module builds under the normal JDK 25 Enforcer rule. Java 8 is the compilation target, not a supported alternative for the build tool. The browser receives generated JavaScript and does not need a JVM.
## Shared-code boundary
`VectoriumWeb` uses JSO interfaces in `WebDom` for the DOM, file input, `FileReader`, canvas image data, object URLs, event listeners, and the mode controls. The web path constructs `VectorDocument` values and calls the same core `SvgParser`, `StageRegistry`, `Pipeline`, and `SvgCache` classes as the native front ends. The web module does not use AWT, ImageIO, Swing, or a server API.
The core SVG parser is a manual scanner over the source string, and the path parser uses the core Bezier flattener. This keeps browser-side parsing on the TeaVM-compiled core path; there is no separate JavaScript SVG parser in the repository. The web build keeps the sequential `ParallelRunner`; JavaScript has no thread pool, so native parallelism is disabled there while output remains identical.
## Browser behavior
- The file input advertises `.png`, `.jpg`, `.jpeg`, and `.svg`; the adapter uses the first selected file, invalidates stale asynchronous selections, and enforces a 1 GiB file-size limit.
- PNG/JPEG data is decoded through a browser image and canvas, then stored with its source alpha byte; SVG is read as text and parsed by `SvgParser`. Decoded raster images are limited to 100 megapixels.
- Color mode is selected by default and vector mode defaults to Curves. The quality slider is passed through as `vectorize.quality` plus a matching `quantize.maxColors` value, so 0 gives the smallest SVG and 100 the most detailed.
- The adapter creates a fresh `SvgCache` and bounded pipeline memoizer for each conversion, serializes the result, previews it through a browser object URL, and assigns the same URL to the `anlyflad.svg` download.
- Conversion and serialization are synchronous inside the change/click handlers; the page has no Web Worker or progress protocol.
- Favicons and the manifest are shipped from `src/main/webapp`, copied unchanged into `target/webapp`, and linked from the page head with legacy ICO, SVG, PNG, Apple touch, and manifest entries.
## Web limitations
- The generated page is a static ES2015 module. The build emits files only; host the `target/webapp` directory with a static file host because no application server is part of the build.
- The browser adapter checks the 1 GiB file limit, positive dimensions, the 100-megapixel decode limit, and the Java `int` pixel-buffer limit.
- The browser UI exposes raster mode, vector mode, and the quality slider; individual stage parameters are not editable there, so cleanup settings correspond to the selected mode defaults.
- Browser image decoding and synchronous JavaScript execution impose browser memory and responsiveness limits that are not measured by the native JMH benchmark.
- The TeaVM target is JavaScript, not a JVM distribution; the Java 8 release settings should not be interpreted as permission to build with JDK 8.
