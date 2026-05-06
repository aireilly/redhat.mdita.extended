# redhat.mdita.extended

A fork of [org.lwdita](https://github.com/jelovirt/org.lwdita) with extended
MDITA task support for Red Hat documentation workflows.

This plug-in incorporates the following upstream PRs that have not yet been
merged:

- [#249 — Add fuller DITA task element support to MDITA task topic type](https://github.com/jelovirt/org.lwdita/pull/249)
- [#242 — Fix duplicate class attribute error in task info elements](https://github.com/jelovirt/org.lwdita/pull/242)

It contains:

- a custom SAX parser for Markdown and HTML to allow using Markdown and HDITA
  as source document formats,
- a transtype to generate Markdown from DITA source,
- conditional processing attribute support for MDITA extended profile,
- relationship table support in MDITA maps,
- and extended DITA task element support (substeps, choices, choicetables,
  task sections via heading inference).

## Markdown source document formats

Markdown-based source files must use a subset of Markdown constructs for
compatibility with DITA content models.

Two different Markdown source formats are supported:

- [Markdown DITA](https://github.com/jelovirt/org.lwdita/wiki/Markdown-DITA-syntax)
- [MDITA (LwDITA)](https://github.com/jelovirt/org.lwdita/wiki/MDITA-syntax)

For a comparison of these two formats, see [Format comparison](https://github.com/jelovirt/org.lwdita/wiki/Format-comparison) in the LwDITA Wiki.

## Conditional processing (DITAVAL filtering)

The MDITA extended profile supports DITA conditional processing attributes.
You can add profiling attributes such as `audience`, `platform`, `product`,
`otherprops`, `deliveryTarget`, `props`, and `rev` to headings, block elements,
and inline elements using the flexmark `{key="value"}` attribute syntax.

DITA-OT then uses standard `.ditaval` files to include, exclude, or flag
content based on these attributes.

### Attributes on headings (sections)

Append `{key="value"}` after a heading to apply attributes to the
generated `<section>` or `<topic>` element:

```markdown
## Installing on Linux {platform="linux"}

Use the package manager to install.

## Installing on macOS {platform="macos"}

Use Homebrew to install.
```

This produces `<section platform="linux">` and `<section platform="macos">`
in the DITA output. Multiple attributes can be combined:

```markdown
## Advanced setup {platform="linux" audience="expert"}
```

### Attributes on block elements

Place `{key="value"}` on a standalone line immediately before a block element
(list, table, definition list) to apply attributes to that element:

```markdown
{audience="novice"}

- Step one
- Step two
```

This produces `<ul audience="novice">` in the DITA output.

The same syntax works for ordered lists, definition lists, and tables:

```markdown
{platform="linux"}

| Package   | Version |
|-----------|---------|
| glibc     | 2.38    |
| openssl   | 3.1     |
```

For code blocks, add the attributes directly on the opening fence:

````markdown
``` {.yaml platform="kubernetes"}
apiVersion: v1
kind: ConfigMap
```
````

### Attributes on inline elements

Append `{key="value"}` immediately after an inline element (bold, italic,
code) with no space between the closing marker and the opening brace:

```markdown
For **expert users**{audience="expert"}, compile from source.
```

This produces `<b audience="expert">expert users</b>` in the DITA output.

You can also use `outputclass` via the `.classname` shorthand and `id`
via `#name`:

```markdown
This is **important**{.highlight}.

See the *overview*{#intro-section}.
```

### Filtering with DITAVAL

Create a standard `.ditaval` file to control which profiled content appears
in the output:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<val>
  <prop action="exclude" att="platform" val="kubernetes"/>
  <prop action="include" att="platform" val="openshift"/>
</val>
```

Apply the filter at build time with the `--filter` flag:

```shell
dita -i example.mditamap -f html5 -o out --filter=openshift.ditaval
```

Content marked with `platform="kubernetes"` is excluded from the output,
while `platform="openshift"` content is included.

### Supported profiling attributes

Any attribute can be passed through, but the following DITA profiling
attributes are declared in the `specializations` string and recognized
by DITA-OT conditional processing:

| Attribute        | Purpose                                 |
|------------------|-----------------------------------------|
| `audience`       | Target audience (e.g., novice, expert)  |
| `platform`       | Target platform (e.g., linux, macos)    |
| `product`        | Product name or variant                 |
| `otherprops`     | Custom profiling values                 |
| `deliveryTarget` | Output format (e.g., html, pdf)         |
| `props`          | Generic profiling attribute             |
| `rev`            | Revision identifier for flagging        |

### Roundtrip support

Profiling attributes are preserved in both directions:

- **Markdown to DITA**: `{platform="linux"}` in MDITA becomes
  `platform="linux"` on the corresponding DITA XML element.
- **DITA to Markdown**: `platform="linux"` in DITA XML becomes
  `{platform="linux"}` in the generated Markdown output.

## Relationship tables in MDITA maps

MDITA map files (`.mditamap`) support relationship tables. A Markdown table
placed after the topic list in a map is converted to a DITA `<reltable>`.

Relationship tables define links between topics that are not part of the
hierarchical table of contents. DITA-OT uses them to generate "Related
information" links in the output.

### Syntax

Write a standard Markdown table in your `.mditamap` file. Each cell
contains a link to a topic. The table header row becomes `<relheader>`
with `<relcolspec>` entries, and body rows become `<relrow>` with
`<relcell>` entries:

```markdown
---
$schema: urn:oasis:names:tc:dita:xsd:map.xsd
---

# Product documentation

- [Overview](overview.md)
- [Configuration](config.md)
- [Installation](install.md)
- [Troubleshooting](troubleshoot.md)

| [Overview](overview.md)      | [Configuration](config.md)       |
|------------------------------|----------------------------------|
| [Installation](install.md)   | [Troubleshooting](troubleshoot.md) |
```

This produces the following DITA structure:

```xml
<reltable toc="no">
  <relheader>
    <relcolspec toc="no">
      <topicref href="overview.md" format="md">...</topicref>
    </relcolspec>
    <relcolspec toc="no">
      <topicref href="config.md" format="md">...</topicref>
    </relcolspec>
  </relheader>
  <relrow>
    <relcell>
      <topicref href="install.md" format="md">...</topicref>
    </relcell>
    <relcell>
      <topicref href="troubleshoot.md" format="md">...</topicref>
    </relcell>
  </relrow>
</reltable>
```

### How relationship tables work

Each column in the reltable defines a group of related topics. DITA-OT
links each topic in a row to all other topics in the same row but in
different columns:

- In the example above, `install.md` gets a related link to
  `troubleshoot.md`, and vice versa.
- The header row defines the column types. Topics listed in the header
  cells establish the default relationship pattern for that column.

You can add multiple body rows to define additional relationships:

```markdown
| Concepts                     | Tasks                           |
|------------------------------|---------------------------------|
| [Overview](overview.md)      | [Installation](install.md)      |
| [Architecture](arch.md)      | [Configuration](config.md)      |
```

### Reference links

You can use Markdown reference-style links in reltable cells:

```markdown
- [Install guide]
- [Config ref]

| [Install guide] | [Config ref] |
|-----------------|--------------|

[Install guide]: install.md
[Config ref]: config.md
```

## Task topic support

When a topic is identified as a task (via `{.task}` on the H1 heading or
`$schema: urn:oasis:names:tc:dita:xsd:task.xsd` in YAML front matter), the
plug-in automatically maps Markdown constructs to DITA task elements.

### Body-level structure

Content before the first ordered/unordered list becomes `<context>`. The
list itself becomes `<steps>` (ordered) or `<steps-unordered>` (unordered).
Content after the list becomes `<result>`.

Task sections can be created using H2 headings with specific default titles
or explicit class attributes:

| Default title    | Class attribute          | DITA element           |
|------------------|--------------------------|------------------------|
| Prerequisites    | `{.prereq}`              | `<prereq>`             |
| About this task  | `{.context}`             | `<context>`            |
| Verification     | `{.result}`              | `<result>`             |
| Next steps       | `{.postreq}`             | `<postreq>`            |
| *(none)*         | `{.tasktroubleshooting}` | `<tasktroubleshooting>` |

Default titles are matched case-insensitively and require no attributes.
Explicit class attributes can be used with any heading text.

```markdown
# Install the software {.task}

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

The same task can also use explicit class attributes with custom titles:

```markdown
## Before you begin {.prereq}

## Troubleshooting {.tasktroubleshooting}
```

### Step-level structure

Within each step, the first paragraph becomes `<cmd>` and subsequent
paragraphs are wrapped in `<info>`. Nested constructs within a step are
automatically specialized:

- **Substeps**: A nested ordered list becomes `<substeps>`, with each item
  as a `<substep>` containing its own `<cmd>` and `<info>`.
- **Choices**: A nested unordered list becomes `<choices>`, with each item
  as a `<choice>`.
- **Choice table**: A table within a step becomes a `<choicetable>` with
  `<choption>` and `<chdesc>` columns.

```markdown
1.  Configure the server:

    1.  Edit the config file.
    2.  Set the port number.

2.  Select the installation type:

    -   Minimal
    -   Standard
    -   Full

3.  Select an option:

    | Option | Description   |
    |--------|---------------|
    | Fast   | Quick setup   |
    | Full   | Complete install |
```

## Demo

A ready-to-build demo is included under `demo/src/`. It contains an MDITA map
and three topics (concept, reference, task) that exercise the plug-in's key
features: YAML front matter, admonitions, tables, fenced code blocks, task
sections, substeps, choices, choice tables, conditional processing attributes,
and relationship tables.

The MDITA map (`demo/src/example.mditamap`):

```markdown
---
$schema: urn:oasis:names:tc:dita:xsd:map.xsd
---

# MDITA Extended Demo

- [Understanding widgets](docs/concept.md)
- [Widget configuration reference](docs/reference.md)
- [Installing the widget](docs/task.md)

| [Understanding widgets](docs/concept.md)             | [Widget configuration reference](docs/reference.md) |
|------------------------------------------------------|------------------------------------------------------|
| [Installing the widget](docs/task.md)                |                                                      |
```

The relationship table at the bottom links concepts to references (header
row) and the task topic to other topics (body row), so DITA-OT generates
"Related information" links in the output.

The demo topics use conditional processing attributes to mark
platform-specific content. A sample DITAVAL file is included at
`demo/src/openshift.ditaval` to filter for OpenShift content:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<val>
  <prop action="exclude" att="platform" val="kubernetes"/>
  <prop action="include" att="platform" val="openshift"/>
</val>
```

Build the demo to HTML5:

```shell
dita -i demo/src/example.mditamap -f html5 -o out
```

Build with DITAVAL filtering to exclude Kubernetes-specific content:

```shell
dita -i demo/src/example.mditamap -f html5 -o out --filter=demo/src/openshift.ditaval
```

Other useful transtypes:

| Transtype         | Output                     |
|-------------------|----------------------------|
| `xhtml`           | XHTML                      |
| `html5`           | HTML5                      |
| `dita`            | Normalized DITA XML        |
| `pdf`             | PDF (requires PDF plug-in) |

## Testing the demo

After installing the plug-in, run through these steps to verify
conditional processing, relationship tables, and the DITA-to-Markdown
roundtrip.

### Build and install

``` shell
./gradlew dist
dita install build/distributions/redhat.mdita.extended-*.zip
```

### Full build (no filtering)

``` shell
dita -i demo/src/example.mditamap -f html5 -o out/full
```

All three topics should build without errors. Open `out/full/docs/concept.html`
and confirm that both the "Kubernetes-specific considerations" and
"OpenShift-specific considerations" sections are present.

### Filtered build (DITAVAL)

``` shell
dita -i demo/src/example.mditamap -f html5 -o out/openshift \
  --filter=demo/src/openshift.ditaval
```

Open `out/openshift/docs/concept.html` and confirm that the
"Kubernetes-specific considerations" section is absent while
"OpenShift-specific considerations" remains. The task topic should
include the "Troubleshooting" section (which is OpenShift-only).

### Verify profiling attributes in normalized DITA

``` shell
dita -i demo/src/example.mditamap -f dita -o out/dita
```

Inspect the generated DITA XML to confirm that profiling attributes
landed on the correct elements:

``` shell
grep -E 'platform=|audience=' out/dita/docs/*.dita
```

You should see `platform="kubernetes"`, `platform="openshift"`, and
`audience="expert"` on the corresponding `<section>`, `<b>`, and
other elements.

### Verify relationship table links

``` shell
grep -l "Related information" out/full/docs/*.html
```

The reltable in the map should cause DITA-OT to generate "Related
information" links in the HTML output, connecting the concept,
reference, and task topics to each other.

### Compare filtered vs. unfiltered output

``` shell
grep -c "Kubernetes" out/full/docs/concept.html
grep -c "Kubernetes" out/openshift/docs/concept.html
```

The first command should return a non-zero count; the second should
return `0`.

### Run the unit tests

``` shell
./gradlew test
```

All tests should pass. The test suite covers both the Markdown-to-DITA
and DITA-to-Markdown directions, including profiling attribute roundtrip.

## Install

### From a release

Install the plug-in directly from a GitHub release:

``` shell
dita install https://github.com/aireilly/redhat.mdita.extended/releases/download/0.0.1/redhat.mdita.extended-0.0.1.zip
```

### From source

1.  Build the distribution:

    ``` shell
    ./gradlew dist
    ```

2.  Install the plug-in from the built ZIP:

    ``` shell
    dita install build/distributions/redhat.mdita.extended-*.zip
    ```

### Run a build

``` shell
dita -i demo/src/example.mditamap -f xhtml -o out
```

## Release

To create a new release, tag the commit and push. The GitHub Actions
workflow builds the distribution ZIP and attaches it to a release
automatically.

``` shell
git tag 0.0.2
git push origin 0.0.2
```

## Upstream

This is a fork of [jelovirt/org.lwdita](https://github.com/jelovirt/org.lwdita).
Support the upstream project and its author
[@jelovirt](https://github.com/jelovirt) via [GitHub
Sponsors](https://github.com/sponsors/jelovirt).

## License

DITA-OT redhat.mdita.extended is licensed for use under the [Apache License
2.0](http://www.apache.org/licenses/LICENSE-2.0).
