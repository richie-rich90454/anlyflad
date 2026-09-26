# Fixtures
Small, deterministic assets for smoke tests and manual front-end checks.
| File | Coverage |
|---|---|
| `minimal.svg` | One nested path stack for parser, writer, and export smoke tests |
| `shape-suite.svg` | Groups, transforms, opacity, basic shapes, curves, and open paths |
| `cleaner-suite.svg` | Duplicate, nested, speck, hole, and open-path cases for cleaner stages |
| `checkerboard.png` | 64 by 64 opaque black-and-white raster for preprocessing, thresholding, run-based vectorization, and quality-scale smoke tests |
All files are committed as intentionally small inputs. Run the CLI from the repository root after building it:
```text
java -jar cli/target/anlyflad-cli.jar fixtures/minimal.svg --output target/fixtures/minimal.svg
java -jar cli/target/anlyflad-cli.jar fixtures/shape-suite.svg --output target/fixtures/shape-suite.svg
java -jar cli/target/anlyflad-cli.jar fixtures/cleaner-suite.svg --output target/fixtures/cleaner-suite.svg
java -jar cli/target/anlyflad-cli.jar fixtures/checkerboard.png --output target/fixtures/checkerboard.svg
java -jar cli/target/anlyflad-cli.jar fixtures/checkerboard.png --output target/fixtures/checkerboard-exact.svg --vector-mode exact
java -jar cli/target/anlyflad-cli.jar fixtures/checkerboard.png --output target/fixtures/checkerboard-max.svg --scale max
```
The default color mode preserves allowlisted, reference-safe SVG source during export. Use `--mode binary` to run the focused SVG subset through the monochrome cleaner pipeline; unsupported SVG elements may be rejected or reduced to fill paths. Use `--vector-mode exact|contour|curve` and `--scale 0-100` (or `draft`, `balanced`, `max`) to compare output size and geometry.
