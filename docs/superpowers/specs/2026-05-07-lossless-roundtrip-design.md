# Lossless DITA-MDITA Round-trip Design

## Problem

The current DITA-to-MDITA-to-DITA round-trip is lossy. Topic type specialization, semantic elements, map metadata, character escaping, and table structure degrade through the Markdown intermediate format.

## Goal

Make the round-trip as close to lossless as possible by encoding DITA semantics into the MDITA output using inline attributes (`{.class key=val}`), YAML front matter, and admonition syntax — then reconstructing those semantics in the reverse Java-based transform.

## Constraints

- Both XSLT (DITA→MDITA) and Java (MDITA→DITA) code will be modified
- Tables remain as Markdown pipe tables (accept CALS attribute loss)
- MDITA output should remain valid Markdown, even if busier than before
- Test against a representative subset (~20 files) from the Ansible AAP corpus

## Design

### 1. YAML Front Matter for Topic-Level Metadata

**DITA→MDITA**: The XSLT emits YAML front matter at the top of each `.md` file:

```yaml
---
$schema: urn:oasis:names:tc:dita:xsd:task.xsd
id: troubleshoot-sosreport
---
```

- `$schema`: Maps to topic type via XSD URN — `task.xsd` → `<task>`, `concept.xsd` → `<concept>`, `reference.xsd` → `<reference>`, `topic.xsd` → `<topic>`
- `id`: Topic ID (already partially supported via `ID_FROM_YAML`)
- `shortdesc`: Stays as the first paragraph in the topic body (not in YAML). The reverse parser's existing `SHORTDESC_PARAGRAPH` feature handles first-paragraph → `<shortdesc>` conversion.

**MDITA→DITA**: The Java `MDitaReader`/`TopicRenderer` parses:
- `$schema` → determines DOCTYPE and root element (`<task>`, `<concept>`, `<reference>`)
- `id` → sets topic `@id` (existing behavior)
- First paragraph → `<shortdesc>` (existing `SHORTDESC_PARAGRAPH` behavior)

**Files changed**:
- `src/main/resources/dita2markdownImpl.xsl` — topic template emits YAML front matter with `$schema` and `id`
- `src/main/java/com/elovirta/dita/markdown/renderer/TopicRenderer.java` — parse `$schema` to determine topic type
- `src/main/java/com/elovirta/dita/markdown/MDitaReader.java` — configure YAML parsing for `$schema`

### 2. Semantic Inline Elements

#### 2a. Notes → Admonitions

**Current**: `<note type="warning">` → `**Warning:** text` → parsed back as `<p><b>Warning:</b> text</p>` (note element lost)

**Fix**: Use Flexmark admonition syntax:

```markdown
!!! note
    This is a note.

!!! warning
    Back up your data first.

!!! important
    Verify the configuration.
```

**DITA→MDITA**: Replace the note common-processing template to emit `!!! type` blocks instead of bold labels inside `<div>`.

**MDITA→DITA**: Flexmark's `AdmonitionBlock` extension already maps admonitions to `<note>` elements. The `TopicRenderer` already handles this. Verify it sets the correct `@type` attribute.

**Files changed**:
- `src/main/resources/dita2markdownImpl.xsl` — note templates → admonition output
- `src/main/resources/ast2markdown.xsl` — add `note` AST element → admonition text rendering

#### 2b. UI Domain (menucascade, uicontrol)

**Current**: Already outputs `**File > Save**{.menucascade}` and `**Edit**{.uicontrol}`. The forward direction is adequate.

**Fix**: Ensure the reverse Java parser reconstructs:
- `{.menucascade}` → `<ph class="- topic/ph ui-d/menucascade ">` with nested `<uicontrol>` elements split on ` > `
- `{.uicontrol}` → `<ph class="- topic/ph ui-d/uicontrol ">`
- Preserve `@outputclass` if present

**Files changed**:
- `src/main/java/com/elovirta/dita/markdown/renderer/TopicRenderer.java` — menucascade/uicontrol reconstruction
- `src/main/java/com/elovirta/dita/markdown/SpecializeFilter.java` — may need class ancestry mapping

#### 2c. Other Domain Elements

Elements like `<varname>`, `<filepath>`, `<userinput>`, `<systemoutput>`, `<cmdname>`, `<codeph>`, `<option>`, `<parmname>`, `<apiname>` already use `{.classname}` inline attributes in the forward direction.

**Fix**: Ensure the Java reverse parser maps each class name to its full DITA class ancestry:
- `varname` → `- topic/keyword sw-d/varname `
- `filepath` → `- topic/ph sw-d/filepath `
- `userinput` → `- topic/ph ui-d/userinput `
- `systemoutput` → `- topic/ph ui-d/systemoutput `
- `cmdname` → `- topic/keyword sw-d/cmdname `
- `codeph` → `- topic/ph pr-d/codeph `
- etc.

**Files changed**:
- `src/main/java/com/elovirta/dita/markdown/renderer/TopicRenderer.java` — class ancestry map

### 3. Related Links

**Current**: `<related-links>` → "Related information" heading with plain links → parsed back as body content with bold text and `<xref>`.

**Fix**: Emit as a marked section:

```markdown
## Related information {.related-links}

- [Link text](target.dita){.link}
- [External link](https://example.com){.link format="html" scope="external"}
```

**DITA→MDITA**: The `rel-links.xsl` template emits a section heading with `{.related-links}` class, and each link as a list item with `{.link}` class plus any `format`/`scope` attributes.

**MDITA→DITA**: The Java parser detects `{.related-links}` on a heading and switches to related-links mode — emitting `<related-links>` container with `<link>` children instead of body sections.

**Files changed**:
- `src/main/resources/rel-links.xsl` — related-links output format
- `src/main/resources/ast2markdown.xsl` — link AST rendering
- `src/main/java/com/elovirta/dita/markdown/SpecializeFilter.java` — related-links detection and reconstruction

### 4. Map Metadata

**Current**: `chunk="to-content"` and `toc="no"` attributes lost. `<topichead>` becomes a plain list item.

**Fix**: In MDITA map output:

```yaml
---
$schema: urn:oasis:names:tc:dita:xsd:map.xsd
id: ansible-troubleshoot
---
```

Topicref attributes preserved on list items:

```markdown
- [Topic title](topic.dita){chunk="to-content"}
    - [Child topic](child.dita){toc="no"}
- Troubleshoot {.topichead}
    - [Subtopic](sub.dita)
```

**DITA→MDITA**: `map2markdownImpl.xsl` emits attributes on links and marks topicheads with `{.topichead}`.

**MDITA→DITA**: `MDitamapReader` parses inline attributes on links and reconstructs `@chunk`, `@toc`, `@navtitle` on `<topicref>`. Items with `{.topichead}` become `<topichead>` elements.

**Files changed**:
- `src/main/resources/map2markdownImpl.xsl` — attribute output on links
- `src/main/java/com/elovirta/dita/markdown/MDitamapReader.java` — attribute parsing

### 5. Character Escaping

**Current**: `<varname>` content → italicized with HTML entities → double-escaped on round-trip (`&amp;lt;openshift_url&amp;gt;`).

**Fix**: In the XSLT, emit varname content using code spans with class attribute: `` `<openshift_url>`{.varname} ``. Backtick content is treated as literal by Markdown parsers, avoiding double-encoding of angle brackets and special characters.

**Files changed**:
- `src/main/resources/dita2markdownImpl.xsl` — varname/ph template
- `src/main/resources/sw-d.xsl` or equivalent — software domain output

### 6. Tables

**Current**: Simple pipe tables work. Complex tables (block content in cells) fall back to HTML `<table>` blocks that get parsed back as empty `<table>` stubs with content as `<p>` elements.

**Fix**: Keep pipe tables for simple cases. For the HTML fallback path, fix the Java `TopicRenderer` to properly parse HTML `<table>` blocks back into `<simpletable>` with `<sthead>`/`<strow>`/`<stentry>` structure. The content within cells should be preserved rather than dumped as sequential paragraphs.

**Files changed**:
- `src/main/java/com/elovirta/dita/markdown/renderer/TopicRenderer.java` — HTML table parsing
- `src/main/resources/ast2markdown.xsl` — verify HTML table output is clean

### 7. Task Structure Preservation

**Current**: Task sections (prereq, context, steps, result, postreq) are rendered with section headings. The reverse `SpecializeFilter` detects these by title text matching. This mostly works but is fragile.

**Fix**: Add explicit class markers to task section headings:

```markdown
## Prerequisites {.prereq}

## Procedure {.steps}

1. Step one.{.step}

## Verification {.result}
```

The `SpecializeFilter` can then use class attributes instead of title text matching, making detection reliable across languages.

**Files changed**:
- `src/main/resources/task.xsl` — add classes to section headings
- `src/main/java/com/elovirta/dita/markdown/SpecializeFilter.java` — detect classes instead of/in addition to titles

## Test Strategy

Select ~20 representative files from the Ansible AAP corpus at `/home/aireilly/Downloads/mapexport_ansible-2-6-navigation_1776264333225/content/2/2-6/`:

1. **Tasks**: 3 files with steps, menucascade, substeps, prereq, result
2. **Concepts**: 3 files with sections, notes, tables, code blocks
3. **References**: 3 files with tables, definition lists
4. **Maps**: 2 maps with chunk, toc, topichead, mapref
5. **Mixed**: Files with related-links, varname, codeph, xref
6. **Edge cases**: Files with complex tables, nested topics

Process:
1. DITA → MDITA (forward transform)
2. MDITA → DITA (reverse transform)  
3. Diff original vs. round-tripped DITA
4. Measure: element preservation, attribute preservation, content integrity
5. Iterate until diffs are minimal/acceptable

## Implementation Order

1. YAML front matter (topic type + shortdesc) — highest impact
2. Notes → admonitions — second highest impact
3. Related links reconstruction
4. Map metadata preservation
5. Character escaping fix
6. Table parsing improvement
7. Task structure markers
8. Domain element class ancestry
