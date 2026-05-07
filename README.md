# redhat.mdita.extended

A fork of [org.lwdita](https://github.com/jelovirt/org.lwdita) with extended
MDITA task support for Red Hat documentation workflows.

This plug-in simplifies the Markdown-to-DITA conversion by focusing on text
fidelity over format fidelity. Complex DITA elements are downsampled to
bold, italic, or code.

## What it does

- Parses Markdown into DITA XML (SAX-based reader).
- Generates Markdown from DITA source (XSLT transtype).
- Detects topic type (concept, reference, task) from `$schema` in YAML
  front matter.
- Specializes task topics: steps, substeps, choices, and task sections
  (prerequisites, context, result, postreq) via default heading titles.
- Supports pipe tables (simpletable), admonitions, fenced code blocks,
  definition lists, and key/content references.
- Supports relationship tables in MDITA maps.

## Topic type detection

Use `$schema` in YAML front matter to declare the topic type:

```markdown
---
$schema: 'urn:oasis:names:tc:dita:xsd:task.xsd'
---
# Install the software

1. Download the installer.
2. Run the installer.
```

Supported schema values:

| Schema URN | Topic type |
|------------|------------|
| `urn:oasis:names:tc:dita:xsd:concept.xsd` | Concept |
| `urn:oasis:names:tc:dita:xsd:reference.xsd` | Reference |
| `urn:oasis:names:tc:dita:xsd:task.xsd` | Task |
| `urn:oasis:names:tc:dita:xsd:topic.xsd` | Generic topic |

## YAML front matter

Beyond `$schema` and `id`, the plugin maps standard DITA prolog fields
from YAML front matter into `<prolog>` elements. Unrecognized fields fall
through as `<data name="..." value="..."/>`.

```markdown
---
$schema: urn:oasis:names:tc:dita:xsd:concept.xsd
id: metrics-prerequisites
author: vLLM Documentation Team
category: AI Inference
keyword:
  - metrics
  - prerequisites
  - benchmarking
  - vllm
workflow: review
---
# Metrics prerequisites
```

Supported fields:

| YAML key | DITA element |
|----------|--------------|
| `$schema` | Topic type detection (not emitted) |
| `id` | Topic `@id` attribute |
| `author` | `<author>` |
| `source` | `<source>` |
| `publisher` | `<publisher>` |
| `permissions` | `<permissions @view="...">` |
| `audience` | `<audience @type="...">` |
| `category` | `<category>` |
| `keyword` | `<keyword>` (inside `<keywords>`) |
| `resourceid` | `<resourceid @appid="...">` |
| *(anything else)* | `<data name="..." value="..."/>` |

## Task topics

When a topic is identified as a task, the plug-in maps Markdown constructs
to DITA task elements:

- Content before the first list becomes `<context>`.
- An ordered list becomes `<steps>`, unordered becomes `<steps-unordered>`.
- Content after the list becomes `<result>`.
- Nested ordered lists in steps become `<substeps>`.
- Nested unordered lists in steps become `<choices>`.

### Task sections

H2 headings with recognized default titles produce specialized task sections:

| Heading text | DITA element |
|--------------|--------------|
| Prerequisites | `<prereq>` |
| About this task | `<context>` |
| Verification | `<result>` |
| Next steps | `<postreq>` |

```markdown
---
$schema: 'urn:oasis:names:tc:dita:xsd:task.xsd'
---
# Install the software

## Prerequisites

You need administrator access.

## About this task

This procedure installs the base package.

1.  Download the installer.
2.  Run the installer.

## Verification

The software is now installed.

## Next steps

Configure the license key.
```

## Markdown source formats

Two Markdown source formats are supported:

- [Markdown DITA](https://github.com/jelovirt/org.lwdita/wiki/Markdown-DITA-syntax)
- [MDITA (LwDITA)](https://github.com/jelovirt/org.lwdita/wiki/MDITA-syntax)

## Relationship tables in MDITA maps

MDITA map files (`.mditamap`) support relationship tables. A Markdown table
placed after the topic list is converted to a DITA `<reltable>`:

```markdown
---
$schema: urn:oasis:names:tc:dita:xsd:map.xsd
---

# Product documentation

- [Overview](overview.md)
- [Configuration](config.md)
- [Installation](install.md)

| [Overview](overview.md)    | [Configuration](config.md)         |
|----------------------------|------------------------------------|
| [Installation](install.md) | [Troubleshooting](troubleshoot.md) |
```

## Install

### From a release

``` shell
dita install https://github.com/aireilly/redhat.mdita.extended/releases/download/0.0.6/redhat.mdita.extended-0.0.6.zip
```

### From source

1.  Build the distribution:

    ``` shell
    ./gradlew dist
    ```

2.  Install the plug-in:

    ``` shell
    dita install build/distributions/redhat.mdita.extended-0.0.6.zip --force
    ```

### Run a build

``` shell
dita -i example.mditamap -f html5 -o out
```

## Testing

``` shell
./gradlew test
```

## Upstream

Fork of [jelovirt/org.lwdita](https://github.com/jelovirt/org.lwdita).
Support the upstream author [@jelovirt](https://github.com/jelovirt) via
[GitHub Sponsors](https://github.com/sponsors/jelovirt).

## License

Licensed under the [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0).
