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
- and extended DITA task element support (substeps, choices, choicetables,
  task sections via heading inference).

## Markdown source document formats

Markdown-based source files must use a subset of Markdown constructs for
compatibility with DITA content models.

Two different Markdown source formats are supported:

- [Markdown DITA](https://github.com/jelovirt/org.lwdita/wiki/Markdown-DITA-syntax)
- [MDITA (LwDITA)](https://github.com/jelovirt/org.lwdita/wiki/MDITA-syntax)

For a comparison of these two formats, see [Format comparison](https://github.com/jelovirt/org.lwdita/wiki/Format-comparison) in the LwDITA Wiki.

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
sections, substeps, choices, and choice tables.

The MDITA map (`demo/src/example.mditamap`):

```markdown
---
$schema: urn:oasis:names:tc:dita:xsd:map.xsd
---

# MDITA Extended Demo

- [Understanding widgets](docs/concept.md)
- [Widget configuration reference](docs/reference.md)
- [Installing the widget](docs/task.md)
```

Build the demo to XHTML:

``` shell
dita -i demo/src/example.mditamap -f xhtml -o out
```

Other useful transtypes:

| Transtype         | Output                     |
|-------------------|----------------------------|
| `xhtml`           | XHTML                      |
| `html5`           | HTML5                      |
| `dita`            | Normalized DITA XML        |
| `pdf`             | PDF (requires PDF plug-in) |

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
