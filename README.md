# redhat.mdita.extended

> [!IMPORTANT]
> A fork of [org.lwdita](https://github.com/jelovirt/org.lwdita) with extended MDITA task support for Red Hat documentation workflows.

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
| Troubleshooting | `<tasktroubleshooting>` |

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

2.  Extract the archive.

    This can take several minutes.

3.  Run the setup script.

    1.  Open a terminal.
    2.  Run `./setup.sh`.

4.  Choose a component:

    -   Base runtime
    -   Developer tools
    -   Full installation

## Verification

The software is now installed.

## Troubleshooting

If the installer fails, check the log file at `/var/log/install.log`.

## Next steps

Configure the license key.
```

The Markdown constructs in this example map to the following DITA
task elements:

| Markdown construct | DITA element |
|--------------------|--------------|
| Ordered list | `<steps>` |
| Ordered list item | `<step>` |
| First paragraph in a step | `<cmd>` |
| Additional paragraphs in a step | `<info>` |
| Nested ordered list in a step | `<substeps>` / `<substep>` |
| Nested unordered list in a step | `<choices>` / `<choice>` |
| Unordered list (at body level) | `<steps-unordered>` |

## Admonitions

Fenced admonitions (Flexmark syntax) are converted to DITA `<note>` elements.
The admonition qualifier maps to the `@type` attribute:

```markdown
!!! warning
    Back up your data before proceeding.

!!! tip "Optional title"
    This tip has a title rendered as a paragraph inside the note.
```

Supported types: `note`, `tip`, `fastpath`, `restriction`, `important`,
`remember`, `attention`, `caution`, `notice`, `danger`, `warning`, `trouble`.
Any other qualifier produces `type="other" othertype="<qualifier>"`.

## Definition lists

Definition lists (Flexmark syntax) are converted to DITA `<dl>`:

```markdown
Term 1
:   Definition of term 1.

Term 2
:   Definition of term 2.
```

## Fenced code blocks

Fenced code blocks become `<codeblock>` elements. The language identifier
is mapped to `@outputclass`:

````markdown
```yaml
apiVersion: v1
kind: Pod
```
````

Extended metadata syntax is also supported:

````markdown
```{.yaml #my-id key=value}
content
```
````

This sets `outputclass="yaml"`, `id="my-id"`, and `key="value"` on the
`<codeblock>` element.

## Images

Images are converted to `<image>` elements. When an image has a title,
it is wrapped in `<fig>` with `<title>`:

```markdown
![Alt text](image.png "Figure title")
```

When an image is the sole child of a paragraph, it gets
`placement="break"` for block-level display.

## Blockquotes

Blockquotes are converted to DITA `<lq>` (long quote):

```markdown
> This becomes a long quote element.
```

## Conditional profiling attributes

Profiling attributes can be added to headings, block elements, and inline
elements using the `{key="value"}` syntax. These attributes are passed
through to the DITA output and can be used with DITA-OT DITAVAL filtering.

Supported profiling attributes: `audience`, `platform`, `product`,
`otherprops`, `deliveryTarget`, `props`, `rev`.

**Heading-level profiling:**

```markdown
## Installing on Linux {platform="linux"}
```

This produces a section (or nested topic) with `platform="linux"` on the
element.

**Block-level profiling (leading attribute paragraph):**

```markdown
{audience="expert"}

* Step one
* Step two
```

The `{audience="expert"}` paragraph before the list applies the attribute
to the `<ul>` element. Supported block elements: lists, definition lists,
blockquotes, and tables.

**Inline profiling:**

```markdown
For **advanced users**{audience="expert"}, compile from source.
```

This produces `<b audience="expert">advanced users</b>` in the DITA output.

**Multiple attributes:**

```markdown
## Cloud Setup {platform="aws" audience="admin"}
```

Non-profiling attributes (such as `{.classname}` or `{custom="value"}`) are
silently ignored.

## Keyword keyrefs

Use `{{key-name}}` double-curly syntax for product name and version
substitution:

```markdown
# About {{product-name}}

{{product-name}} is a container platform.
The current version is {{version}}.
```

Each `{{key}}` produces a `<keyword keyref="key"/>` element in the DITA
output. Key names may contain letters, digits, dots, underscores, and
hyphens. Invalid key names (spaces, empty, special characters) pass through
as literal text.

The `{{keyref}}` syntax coexists with existing keyref syntaxes:

| Syntax | DITA output | Use case |
|--------|-------------|----------|
| `{{key}}` | `<keyword keyref="key"/>` | Product names, versions |
| `[text][key]` | `<xref keyref="key">text</xref>` | Link-style keyrefs |
| `<span data-keyref="key">` | `<ph keyref="key"/>` | HDITA compatibility |

Define key values using YAML key definitions in the map (see below).

## Hard line breaks

A trailing backslash or two spaces at end of line produces a
`<?linebreak?>` processing instruction in DITA output.

## Key and content references

The plug-in processes DITA key references and content references
when they appear in the DITA source (for the DITA-to-Markdown
conversion path):

- `@keyref` on `<ph>`, `<xref>`, and `<image>` elements
- `@conref` on block elements
- `@conkeyref` on block elements

## Link handling

Links are auto-classified based on their URL:

- Absolute URLs or paths starting with `/` get `scope="external"`
- Fragment-only links (`#id`) get `format="markdown"`
- File extension is detected and set as `@format` (e.g., `html`, `pdf`)
- `mailto:` links get `format="email"`
- `.dita` and `.xml` extensions are treated as native DITA (no format attribute)

## Inline HTML

Inline HTML tags in Markdown are transformed to DITA equivalents via
an XSLT stylesheet. Common mappings include `<b>`/`<strong>` to `<b>`,
`<i>`/`<em>` to `<i>`, `<code>` to `<codeph>`, `<sub>`, `<sup>`, and
`<u>` to their DITA highlight-domain counterparts. Unsupported tags
are passed through as `<required-cleanup>`.

## Jekyll include tags

Jekyll-style `{% include file.md %}` tags are converted to
`<required-cleanup conref="file.md"/>`, preserving the reference for
downstream resolution.

## Markdown source formats

Two Markdown source formats are supported:

- [Markdown DITA](https://github.com/jelovirt/org.lwdita/wiki/Markdown-DITA-syntax)
- [MDITA (LwDITA)](https://github.com/jelovirt/org.lwdita/wiki/MDITA-syntax)

## MDITA maps

MDITA map files (`.mditamap`) support the following features.

### Topic references and sub-maps

Bullet list items with links become `<topicref>` elements. Links to
`.ditamap` or `.mditamap` files are automatically emitted as
`<mapref format="ditamap">`, allowing sub-maps to be nested:

```markdown
---
$schema: urn:oasis:names:tc:dita:xsd:map.xsd
---

# Product documentation

- [Overview](overview.md)
- [Installation guide](install.mditamap)
- [API reference](api.ditamap)
```

The referenced `install.mditamap` sub-map is a standalone map with its own
topic references and nesting:

```markdown
---
$schema: urn:oasis:names:tc:dita:xsd:map.xsd
---

# Installation guide

- System requirements
  - [Hardware](install/hardware.md)
  - [Software](install/software.md)
- Installing
  1. [Download the package](install/download.md)
  2. [Run the installer](install/run.md)
  3. [Verify the installation](install/verify.md)
- [Uninstalling](install/uninstall.md)
```

### Ordered lists (sequence)

Ordered list items in maps produce `<topicref collection-type="sequence">`:

```markdown
1. [Step 1](step1.md)
2. [Step 2](step2.md)
3. [Step 3](step3.md)
```

### Topic heads

List items without links become `<topichead>` elements with a `<navtitle>`:

```markdown
- Getting started
  - [Quick start](quickstart.md)
  - [Installation](install.md)
```

### Key definitions

Markdown reference-style links at the bottom of a map file become
`<keydef>` elements:

```markdown
[product-name]: product.md
```

### YAML key definitions

Define keyword keyrefs in the YAML front matter of an MDITA map using a
`keys:` block:

```markdown
---
$schema: urn:oasis:names:tc:dita:xsd:map.xsd
keys:
  product-name: "Red Hat OpenShift Container Platform"
  product-short: "OpenShift"
  version: "4.15"
  product-url: "https://www.redhat.com/openshift"
---

# Installation Guide

* [Prerequisites](prereqs.md)
* [Installing {{product-short}}](install.md)
```

Plain text values produce keyword-content keydefs:

```xml
<keydef keys="product-name">
  <topicmeta>
    <keywords>
      <keyword>Red Hat OpenShift Container Platform</keyword>
    </keywords>
  </topicmeta>
</keydef>
```

URL values (starting with `http://`, `https://`, or ending with `.md`,
`.dita`, `.html`, `.xml`) produce href-based keydefs:

```xml
<keydef keys="product-url" href="https://www.redhat.com/openshift"
        format="html" scope="external"/>
```

YAML key definitions coexist with reference-style key definitions
(`[key]: url`). Both appear in the generated DITA map.

### Relationship tables

A Markdown table placed after the topic list is converted to a DITA
`<reltable>`:

```markdown
| [Overview](overview.md)    | [Configuration](config.md)         |
|----------------------------|------------------------------------|
| [Installation](install.md) | [Troubleshooting](troubleshoot.md) |
```

## Output transtypes

The plug-in registers four DITA-to-Markdown output transtypes:

| Transtype | Description |
|-----------|-------------|
| `markdown` | Standard Markdown output |
| `markdown_github` | GitHub-flavored Markdown |
| `markdown_gitbook` | GitBook format |
| `mdita` | MDITA (Lightweight DITA Markdown) |

Usage:

```shell
dita -i input.ditamap -f markdown_github -o out
```

## Install

### From a release

``` shell
dita install https://github.com/aireilly/redhat.mdita.extended/releases/download/0.0.7/redhat.mdita.extended-0.0.7.zip
```

### From source

1.  Build the distribution:

    ``` shell
    ./gradlew dist
    ```

2.  Install the plug-in:

    ``` shell
    dita install build/distributions/redhat.mdita.extended-0.0.7.zip --force
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
