# Pull Request

## Summary

Describe the change and why it is needed.

## Related issues

Closes #

## Type of change

- [ ] Bug fix
- [ ] New feature
- [ ] Performance improvement
- [ ] Refactor
- [ ] Documentation
- [ ] Build or release tooling

## Component

- [ ] Core library
- [ ] CLI
- [ ] Desktop (Swing)
- [ ] Web (TeaVM)
- [ ] All-in-one JAR
- [ ] Documentation

## How was this tested?

Describe the exact commands or UI steps used, and include the relevant output.

```text
mvn -o clean verify
node --check web-teavm/target/webapp/anlyflad.js
```

## Visual changes

Include before/after screenshots or rendered output when the contribution
changes the desktop or web UI.

## Checklist

- [ ] The change is focused and does not include unrelated edits.
- [ ] New behavior has focused tests.
- [ ] `mvn -o clean verify` passes.
- [ ] `node --check` passes when the web bundle changed.
- [ ] Documentation in `docs/` or the README is updated when needed.
- [ ] Output remains deterministic between sequential and parallel execution.
- [ ] No secrets, private data, or generated build outputs are committed.
- [ ] I have read and followed [CONTRIBUTING.md](../CONTRIBUTING.md) and the
      [Code of Conduct](../CODE_OF_CONDUCT.md).

## Breaking changes

Describe any breaking change, migration step, or compatibility note. Write
"None" when there are none.
