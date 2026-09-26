# Security Policy

## Supported versions

| Version | Supported |
|---|---|
| 1.0.x | Yes |
| Older versions | No |

## Reporting a vulnerability

Do not open a public issue for security problems. Report vulnerabilities
privately through GitHub's
[private security advisory](https://github.com/richie-rich90454/Anlyflad/security/advisories/new)
form. If the form is unavailable, contact the maintainer
[@richie-rich90454](https://github.com/richie-rich90454) directly on GitHub.

Please include as much of the following as possible:

- A clear description of the issue and its impact.
- Affected component: core, CLI, desktop, web, or the all-in-one JAR.
- Affected version or commit.
- Reproduction steps, proof-of-concept input, or a minimal test case.
- Any suggested fix or mitigation.
- Whether you wish to be credited.

## What to expect

- Acknowledgement of the report as soon as possible, normally within a few days.
- An assessment of severity and affected versions.
- A coordinated fix and release when the report is confirmed.
- Credit in the advisory if you want it.

Please give the project reasonable time to release a fix before public
disclosure.

## Scope

In scope:

- The `anlyflad.jar` all-in-one JAR, CLI, Swing desktop app, and TeaVM web app.
- SVG parsing and serialization, XML entity handling, and URL/reference
  validation.
- The embedded `--web` static file server, including path traversal and content
  type handling.
- Supply chain concerns in Maven dependencies and bundled assets.

Out of scope:

- Denial of service caused only by deliberately huge inputs within the
  documented 1 GiB / 100-megapixel limits.
- Issues in third-party dependencies that are already fixed upstream; report
  those upstream and open a dependency update issue here.
- Reports requiring physical access to an unlocked machine.
- Social engineering, spam, or missing optional hardening headers on static
  hosting.

## Safe harbor

Good-faith security research is welcome. Do not access, modify, or exfiltrate
data that does not belong to you, do not degrade the service for others, and
stop testing as soon as you have enough evidence to report the issue.
