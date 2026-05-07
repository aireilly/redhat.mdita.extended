# Lossless DITA-MDITA Round-trip Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the DITA-to-MDITA-to-DITA round-trip near-lossless by encoding DITA semantics (topic types, notes, related-links, map metadata, varname escaping) into the MDITA output and reconstructing them on the reverse transform.

**Architecture:** The forward transform (XSLT) emits richer MDITA with YAML front matter for topic type (`$schema`), admonition syntax for notes, inline attributes for semantic classes, and map-level metadata. The reverse transform (Java SAX readers + SpecializeFilter) parses these encodings back into proper DITA elements. Changes span both the XSLT pipeline (dita2markdownImpl.xsl, ast2markdown.xsl, task.xsl, map2markdownImpl.xsl, rel-links.xsl, sw-d.xsl) and Java classes (TopicRenderer.java, SpecializeFilter.java, MDitaReader.java, MetadataSerializerImpl.java).

**Tech Stack:** XSLT 2.0 (Saxon), Java 11+ (Flexmark markdown parser, SAX), Gradle, JUnit 5, DITA-OT 3.x

---

### Task 0: Select Representative Test Subset and Baseline

**Files:**
- Create: `test/roundtrip/select-subset.sh` (temporary script, not committed)

This task establishes a baseline by selecting ~20 files from the Ansible corpus and running the current transform to capture the "before" state.

- [ ] **Step 0.1: Select representative files**

Pick files covering all topic types and semantic elements from `/home/aireilly/Downloads/mapexport_ansible-2-6-navigation_1776264333225/content/2/2-6/`:

```bash
mkdir -p /tmp/roundtrip-test/input

# Tasks with menucascade, steps, substeps, notes
cp /home/aireilly/Downloads/mapexport_ansible-2-6-navigation_1776264333225/content/2/2-6/administer/add-permissions-to-inventories.dita /tmp/roundtrip-test/input/
cp /home/aireilly/Downloads/mapexport_ansible-2-6-navigation_1776264333225/content/2/2-6/troubleshoot/proc-troubleshoot-must-gather.dita /tmp/roundtrip-test/input/
cp /home/aireilly/Downloads/mapexport_ansible-2-6-navigation_1776264333225/content/2/2-6/troubleshoot/proc-containerized-troubleshoot-gathering-logs.dita /tmp/roundtrip-test/input/

# Concepts with sections, notes, code blocks
cp /home/aireilly/Downloads/mapexport_ansible-2-6-navigation_1776264333225/content/2/2-6/troubleshoot/assembly-diagnosing-the-problem.dita /tmp/roundtrip-test/input/
cp /home/aireilly/Downloads/mapexport_ansible-2-6-navigation_1776264333225/content/2/2-6/administer/assembly-ag-controller-backup-and-restore.dita /tmp/roundtrip-test/input/

# References with tables
cp /home/aireilly/Downloads/mapexport_ansible-2-6-navigation_1776264333225/content/2/2-6/troubleshoot/ref-troubleshoot-sosreport.dita /tmp/roundtrip-test/input/
cp /home/aireilly/Downloads/mapexport_ansible-2-6-navigation_1776264333225/content/2/2-6/troubleshoot/ref-controller-unable-to-login-http.dita /tmp/roundtrip-test/input/
cp /home/aireilly/Downloads/mapexport_ansible-2-6-navigation_1776264333225/content/2/2-6/troubleshoot/ref-operator-core-aap-resources.dita /tmp/roundtrip-test/input/

# Map with chunk, toc, topichead
cp /home/aireilly/Downloads/mapexport_ansible-2-6-navigation_1776264333225/content/2/2-6/troubleshoot/ansible-2-6-troubleshoot.ditamap /tmp/roundtrip-test/input/

# Files with related-links
cp /home/aireilly/Downloads/mapexport_ansible-2-6-navigation_1776264333225/content/2/2-6/troubleshoot/assembly-operator-troubleshoot.dita /tmp/roundtrip-test/input/
```

- [ ] **Step 0.2: Build and install the current plugin**

```bash
cd /home/aireilly/redhat.mdita.extended
./gradlew dist
dita install build/distributions/redhat.mdita.extended-0.0.4.zip --force
```

- [ ] **Step 0.3: Run baseline forward transform (DITA → MDITA)**

```bash
# Create a simple ditamap pointing to the test files
cat > /tmp/roundtrip-test/input/test.ditamap << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE map PUBLIC "-//OASIS//DTD DITA Map//EN" "map.dtd">
<map id="roundtrip-test">
  <title>Roundtrip Test</title>
  <topicref href="add-permissions-to-inventories.dita"/>
  <topicref href="proc-troubleshoot-must-gather.dita"/>
  <topicref href="proc-containerized-troubleshoot-gathering-logs.dita"/>
  <topicref href="assembly-diagnosing-the-problem.dita"/>
  <topicref href="assembly-ag-controller-backup-and-restore.dita"/>
  <topicref href="ref-troubleshoot-sosreport.dita"/>
  <topicref href="ref-controller-unable-to-login-http.dita"/>
  <topicref href="ref-operator-core-aap-resources.dita"/>
  <topicref href="assembly-operator-troubleshoot.dita"/>
</map>
EOF

dita -i /tmp/roundtrip-test/input/test.ditamap -f mdita -o /tmp/roundtrip-test/mdita-baseline
```

- [ ] **Step 0.4: Run baseline reverse transform (MDITA → DITA)**

```bash
dita -i /tmp/roundtrip-test/mdita-baseline/test.mditamap -f dita -o /tmp/roundtrip-test/dita-baseline
```

- [ ] **Step 0.5: Create baseline diff**

```bash
# Diff original vs round-tripped for each file
for f in /tmp/roundtrip-test/input/*.dita; do
  base=$(basename "$f")
  echo "=== $base ==="
  diff <(xmllint --format "$f" 2>/dev/null || cat "$f") \
       <(xmllint --format "/tmp/roundtrip-test/dita-baseline/$base" 2>/dev/null || echo "MISSING") \
       || true
done > /tmp/roundtrip-test/baseline-diff.txt
wc -l /tmp/roundtrip-test/baseline-diff.txt
```

Expected: large diff showing all the losses documented in the analysis.

---

### Task 1: YAML Front Matter with $schema for Topic Types

**Files:**
- Modify: `src/main/resources/dita2markdownImpl.xsl:116-184` (topic template and title template)
- Modify: `src/main/resources/ast2markdown.xsl:14-48` (pandoc and header templates)
- Modify: `src/main/java/com/elovirta/dita/markdown/renderer/TopicRenderer.java:578-721` (heading render)
- Modify: `src/main/java/com/elovirta/dita/markdown/SpecializeFilter.java:100-119` (type detection)
- Test: `src/test/resources/markdown/task.md`, `src/test/resources/dita/task.dita`

The XSLT forward transform will emit YAML front matter with `$schema` mapping the topic type. The Java reverse transform will read `$schema` from YAML and use it to force the topic type via SpecializeFilter.

- [ ] **Step 1.1: Add YAML front matter output to XSLT topic template**

In `src/main/resources/dita2markdownImpl.xsl`, modify the `chapter-setup` template (called from `root_element` mode at line 129) to emit a YAML block before the topic content. Find the `chapter-setup` template and add YAML output.

First, add a new template in `ast2markdown.xsl` to render a `yaml-frontmatter` AST element:

```xml
<!-- Add after line 16 (before the div template at line 18) in ast2markdown.xsl -->
<xsl:template match="yaml-frontmatter" mode="ast">
  <xsl:text>---</xsl:text>
  <xsl:value-of select="$linefeed"/>
  <xsl:for-each select="yaml-entry">
    <xsl:value-of select="@key"/>
    <xsl:text>: </xsl:text>
    <xsl:choose>
      <xsl:when test="contains(@value, ' ') or contains(@value, ':') or contains(@value, '#')">
        <xsl:text>"</xsl:text>
        <xsl:value-of select="replace(@value, '&quot;', '\\&quot;')"/>
        <xsl:text>"</xsl:text>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="@value"/>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:value-of select="$linefeed"/>
  </xsl:for-each>
  <xsl:text>---</xsl:text>
  <xsl:value-of select="$linefeed"/>
  <xsl:value-of select="$linefeed"/>
</xsl:template>
```

Then in `dita2markdownImpl.xsl`, find the `chapter-setup` template and add YAML front matter generation before the existing content. The chapter-setup template calls topic processing. We need to add a YAML block at the `pandoc` wrapper level.

Find the template that generates the `pandoc` wrapper element. Look for `chapter-setup` or the root processing that wraps content. The root template at line 128 calls `chapter-setup`. We need to add the YAML front matter as the first child of the output.

In `dita2markdownImpl.xsl`, modify the `root_element` mode template to wrap content in a pandoc element with YAML front matter:

```xml
<!-- Replace lines 128-130 -->
<xsl:template match="*" mode="root_element" name="root_element">
  <pandoc>
    <xsl:variable name="topic-type">
      <xsl:choose>
        <xsl:when test="contains(@class, ' task/task ')">task</xsl:when>
        <xsl:when test="contains(@class, ' concept/concept ')">concept</xsl:when>
        <xsl:when test="contains(@class, ' reference/reference ')">reference</xsl:when>
        <xsl:otherwise>topic</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <yaml-frontmatter>
      <yaml-entry key="$schema" value="urn:oasis:names:tc:dita:xsd:{$topic-type}.xsd"/>
      <yaml-entry key="id" value="{@id}"/>
    </yaml-frontmatter>
    <xsl:call-template name="chapter-setup"/>
  </pandoc>
</xsl:template>
```

Note: Check whether `chapter-setup` already wraps in a `pandoc` element. If it does, move the YAML insertion inside that wrapper instead. The key is that `yaml-frontmatter` must be the first child of the top-level pandoc.

- [ ] **Step 1.2: Verify pandoc wrapping in existing code**

Search for `pandoc` element creation in `dita2markdownImpl.xsl`:

```bash
grep -n "pandoc" src/main/resources/dita2markdownImpl.xsl
grep -n "chapter-setup" src/main/resources/dita2markdownImpl.xsl
```

If `chapter-setup` already wraps in `<pandoc>`, adjust Step 1.1 to insert `yaml-frontmatter` as the first child inside the existing `pandoc` rather than adding a new wrapper.

- [ ] **Step 1.3: Update TopicRenderer to read $schema from YAML**

In `src/main/java/com/elovirta/dita/markdown/renderer/TopicRenderer.java`, modify the `getTopicId()` method area (lines 741-752) and the heading render method to also extract `$schema` from YAML front matter.

Add a field and method:

```java
// Add field near line 160
private String schemaType = null;

// Add method near getTopicId()
private String getSchemaType(Node node) {
  if (node.getDocument().getFirstChild() instanceof YamlFrontMatterBlock) {
    AbstractYamlFrontMatterVisitor visitor = new AbstractYamlFrontMatterVisitor();
    visitor.visit(node.getDocument());
    Map<String, List<String>> data = visitor.getData();
    List<String> schema = data.get("$schema");
    if (schema != null && !schema.isEmpty()) {
      String val = schema.get(0);
      // Extract type from URN: urn:oasis:names:tc:dita:xsd:task.xsd -> task
      if (val.contains(":")) {
        String filename = val.substring(val.lastIndexOf(':') + 1);
        if (filename.endsWith(".xsd")) {
          return filename.substring(0, filename.length() - 4);
        }
      }
    }
  }
  return null;
}
```

Then in the `render(Heading node, ...)` method, when processing a level-1 heading, call `getSchemaType()` and set the outputclass accordingly so `SpecializeFilter` picks it up:

```java
// In render(Heading), around line 670-690, after headerLevel is determined
if (headerLevel == 1) {
  String schema = getSchemaType(node);
  if (schema != null && (schema.equals("task") || schema.equals("concept") || schema.equals("reference"))) {
    // Add schema type to outputclass for SpecializeFilter to detect
    // This supplements any existing .task/.concept/.reference class from the heading
    schemaType = schema;
  }
}
```

Then when building topic attributes (around line 703), include the schema type in outputclass:

```java
// When creating the topic element attributes, if schemaType is set and
// no explicit class is already on the heading, add it as outputclass
if (schemaType != null) {
  AttributesImpl topicAtts = new AttributesImpl(atts);
  int idx = topicAtts.getIndex("", "outputclass");
  if (idx >= 0) {
    String existing = topicAtts.getValue(idx);
    if (!existing.contains(schemaType)) {
      topicAtts.setValue(idx, schemaType + " " + existing);
    }
  } else {
    topicAtts.addAttribute("", "outputclass", "outputclass", "CDATA", schemaType);
  }
  // use topicAtts instead of atts
}
```

- [ ] **Step 1.4: Remove the H1 {.task} class from heading since $schema handles it**

In `src/main/resources/dita2markdownImpl.xsl`, the title template at line 166 sets `default-output-class` to `name(..)` when headinglevel is 1 and the parent is not `topic`. This is what generates `{.task}` etc. on the H1. Keep this for backward compatibility (it helps even without YAML), but the YAML `$schema` is now the primary mechanism.

- [ ] **Step 1.5: Run existing tests**

```bash
cd /home/aireilly/redhat.mdita.extended
./gradlew test
```

Expected: Tests pass. The existing test fixtures (like `task.md` which has `{.task}`) still work because the class-based detection path in SpecializeFilter is preserved.

- [ ] **Step 1.6: Add a test for $schema-based type detection**

Create test fixture `src/test/resources/markdown/schema-task.md`:

```markdown
---
$schema: urn:oasis:names:tc:dita:xsd:task.xsd
id: schema-task
---

# Schema Task

Context paragraph.

1.  Do the thing.

    More info.
```

Create expected output `src/test/resources/dita/schema-task.dita`:

```xml
<task xmlns:ditaarch="http://dita.oasis-open.org/architecture/2005/"
      ditaarch:DITAArchVersion="2.0"
      specializations="@props/audience @props/deliveryTarget @props/otherprops @props/platform @props/product"
  class="- topic/topic task/task " id="schema-task">
  <title class="- topic/title ">Schema Task</title>
  <taskbody class="- topic/body task/taskbody ">
    <context class="- topic/section task/context ">
      <p class="- topic/p ">Context paragraph.</p>
    </context>
    <steps class="- topic/ol task/steps ">
      <step class="- topic/li task/step ">
        <cmd class="- topic/ph task/cmd ">Do the thing.</cmd>
        <info class="- topic/itemgroup task/info ">
          <p class="- topic/p ">More info.</p>
        </info>
      </step>
    </steps>
  </taskbody>
</task>
```

Add the test case to `MarkdownReaderTest.java` in the `@ValueSource` list.

- [ ] **Step 1.7: Run tests and verify**

```bash
./gradlew test
```

Expected: All tests pass including the new `schema-task` test.

- [ ] **Step 1.8: Commit**

```bash
git add -A
git commit -m "feat: add YAML \$schema front matter for topic type round-trip"
```

---

### Task 2: Notes as Admonitions

**Files:**
- Modify: `src/main/resources/dita2markdownImpl.xsl:352-533` (note templates)
- Modify: `src/main/resources/ast2markdown.xsl` (add note AST element rendering)
- Test: existing `src/test/resources/markdown/admonition.md` and `src/test/resources/dita/admonition.dita`

Currently notes produce `<div class="note"><strong>Note:</strong> text</div>` in the AST, which renders as `**Note:** text` in Markdown. The reverse parser sees bold text, not a note. We need to emit `<note>` AST elements that render as `!!! type` admonition blocks.

- [ ] **Step 2.1: Add a `note` AST element template in ast2markdown.xsl**

Add after the `blockquote` template (around line 212) in `ast2markdown.xsl`:

```xml
<xsl:template match="note" mode="ast">
  <xsl:param name="indent" tunnel="yes" as="xs:string" select="''"/>
  <xsl:variable name="type" select="if (@type) then @type else 'note'" as="xs:string"/>
  <xsl:value-of select="$indent"/>
  <xsl:text>!!! </xsl:text>
  <xsl:value-of select="$type"/>
  <xsl:value-of select="$linefeed"/>
  <xsl:apply-templates mode="ast">
    <xsl:with-param name="indent" tunnel="yes" select="concat($indent, '    ')"/>
  </xsl:apply-templates>
</xsl:template>
```

- [ ] **Step 2.2: Modify note templates in dita2markdownImpl.xsl to emit `note` AST element**

Replace the `process.note.common-processing` template (lines 397-424) to emit a `<note>` AST element instead of `<div><strong>...:` pattern:

```xml
<xsl:template match="*" mode="process.note.common-processing">
  <xsl:param name="type" select="@type"/>
  <note type="{$type}">
    <xsl:apply-templates/>
  </note>
</xsl:template>
```

Also replace the caution template (lines 484-509) and danger template (lines 511-533) to use the same pattern:

```xml
<xsl:template match="*" mode="process.note.caution">
  <note type="caution">
    <xsl:apply-templates/>
  </note>
</xsl:template>

<xsl:template match="*" mode="process.note.danger">
  <note type="danger">
    <xsl:apply-templates/>
  </note>
</xsl:template>
```

- [ ] **Step 2.3: Handle the note AST element in flatten and ast-clean modes**

In `ast2markdown.xsl`, add pass-through templates for `note` in the flatten and ast-clean modes so it doesn't get stripped. Check if there's a generic `*` template in these modes — if so, `note` should be covered. If not, add:

```xml
<xsl:template match="note" mode="flatten">
  <xsl:copy>
    <xsl:copy-of select="@*"/>
    <xsl:apply-templates mode="#current"/>
  </xsl:copy>
</xsl:template>

<xsl:template match="note" mode="ast-clean">
  <xsl:copy>
    <xsl:copy-of select="@*"/>
    <xsl:apply-templates mode="#current"/>
  </xsl:copy>
</xsl:template>
```

Check the existing flatten/ast-clean templates first — there may be a generic identity transform that handles unknown elements.

- [ ] **Step 2.4: Run tests**

```bash
./gradlew test
```

Expected: Existing tests pass. The `admonition.md` test fixture already uses `!!! note` syntax and expects `<note>` elements in the DITA output, so that path is verified. The forward transform now also produces admonition syntax, completing the round-trip.

- [ ] **Step 2.5: Create a round-trip note test**

Create `src/test/resources/markdown/note-roundtrip.md`:

```markdown
# Note Round-trip

Paragraph before note.

!!! note
    This is a note.

!!! warning
    Back up your data first.

!!! important
    Verify the configuration.

!!! tip
    Use keyboard shortcuts.
```

Create `src/test/resources/dita/note-roundtrip.dita` with the expected DITA output containing `<note type="note">`, `<note type="warning">`, etc.

- [ ] **Step 2.6: Run tests and commit**

```bash
./gradlew test
git add -A
git commit -m "feat: emit notes as admonitions for lossless round-trip"
```

---

### Task 3: Related Links Preservation

**Files:**
- Modify: `src/main/resources/rel-links.xsl:279-293` (related-links output)
- Modify: `src/main/resources/dita2markdownImpl.xsl:192-202` (body template related-links call)
- Modify: `src/main/resources/ast2markdown.xsl` (add link list rendering)
- Verify: `src/main/java/com/elovirta/dita/markdown/SpecializeFilter.java:614-692` (already handles `{.related-links}`)

The SpecializeFilter already detects `outputclass="related-links"` on sections and converts them to `<related-links>` with `<link>` elements. The issue is the forward XSLT doesn't emit the right markers.

- [ ] **Step 3.1: Modify rel-links.xsl to emit structured related-links section**

In `rel-links.xsl`, the `related-links:group-result.` template (around line 280) creates a `<linklist>` with `<title>Related information</title>`. This needs to produce AST that renders as a heading with `{.related-links}` class and a bullet list of links with attributes.

Find the template that processes the linklist into the AST. The linklist element flows through `dita2markdownImpl.xsl` — check how `linklist` is rendered. Likely it's treated as a section. We need to ensure:

1. The heading gets `class="related-links"` 
2. Each link gets format/scope attributes preserved

In `rel-links.xsl`, modify the `related-links:group-result.` template:

```xml
<xsl:template match="*[contains(@class, ' topic/link ')]" mode="related-links:result-group"
              name="related-links:group-result.">
  <xsl:param name="links"/>
  <linklist class="- topic/linklist " outputclass="related-links">
    <title class="- topic/title ">
      <xsl:call-template name="getVariable">
        <xsl:with-param name="id" select="'Related information'"/>
      </xsl:call-template>
    </title>
    <xsl:copy-of select="$links"/>
  </linklist>
</xsl:template>
```

This should already produce `outputclass="related-links"` (check if it already does). The key fix may be in how the `link` elements inside the linklist are rendered to the AST — they need `format` and `scope` attributes preserved on the markdown links.

- [ ] **Step 3.2: Ensure link attributes are preserved in AST output**

In `rel-links.xsl`, find how individual `<link>` elements are rendered. They should produce `<link>` AST elements with `href`, `format`, and `scope` attributes. Check the `link` template in rel-links.xsl.

In `ast2markdown.xsl`, the `link[@href]` template (line 448) only outputs `[text](href)`. Modify to include format/scope as inline attributes:

```xml
<xsl:template match="link[@href]" mode="ast">
  <xsl:text>[</xsl:text>
  <xsl:apply-templates mode="ast"/>
  <xsl:text>]</xsl:text>
  <xsl:text>(</xsl:text>
  <xsl:value-of select="@href"/>
  <xsl:text>)</xsl:text>
  <xsl:if test="@format or @scope or @class">
    <xsl:text>{</xsl:text>
    <xsl:if test="@class">
      <xsl:text>.</xsl:text>
      <xsl:value-of select="tokenize(@class, '\s+')[last()]"/>
    </xsl:if>
    <xsl:if test="@format">
      <xsl:text> format="</xsl:text>
      <xsl:value-of select="@format"/>
      <xsl:text>"</xsl:text>
    </xsl:if>
    <xsl:if test="@scope">
      <xsl:text> scope="</xsl:text>
      <xsl:value-of select="@scope"/>
      <xsl:text>"</xsl:text>
    </xsl:if>
    <xsl:text>}</xsl:text>
  </xsl:if>
</xsl:template>
```

- [ ] **Step 3.3: Run tests and verify**

```bash
./gradlew test
```

- [ ] **Step 3.4: Commit**

```bash
git add -A
git commit -m "feat: preserve related-links structure in round-trip"
```

---

### Task 4: Map Metadata Preservation

**Files:**
- Modify: `src/main/resources/map2markdownImpl.xsl:14-115` (map and topicref templates)
- Modify: `src/main/resources/map2markdown-coverImpl.xsl` (map cover/wrapper template)
- Modify: `src/main/resources/ast2markdown.xsl` (link attribute output)
- Verify: `src/main/java/com/elovirta/dita/markdown/MDitamapReader.java`

- [ ] **Step 4.1: Add YAML front matter to map output**

In `map2markdown-coverImpl.xsl` (or whichever template generates the map wrapper), add YAML front matter with map ID and schema:

```xml
<yaml-frontmatter>
  <yaml-entry key="$schema" value="urn:oasis:names:tc:dita:xsd:map.xsd"/>
  <yaml-entry key="id" value="{/*[contains(@class, ' map/map ')]/@id}"/>
</yaml-frontmatter>
```

- [ ] **Step 4.2: Preserve chunk and toc attributes on topicref links**

In `map2markdownImpl.xsl`, modify the topicref template (lines 28-104) to add `chunk` and `toc` attributes to the `<link>` AST element:

```xml
<!-- In the <link> element creation around line 43, add: -->
<link>
  <xsl:attribute name="href">...</xsl:attribute>
  <xsl:if test="@chunk">
    <xsl:attribute name="chunk" select="@chunk"/>
  </xsl:if>
  <xsl:if test="@toc">
    <xsl:attribute name="toc" select="@toc"/>
  </xsl:if>
  <xsl:value-of select="$title"/>
</link>
```

Then in `ast2markdown.xsl`, update the `link[@href]` template to also output `chunk` and `toc` as inline attributes:

```xml
<!-- Extend the existing link template to include chunk/toc -->
<xsl:if test="@chunk">
  <xsl:text> chunk="</xsl:text>
  <xsl:value-of select="@chunk"/>
  <xsl:text>"</xsl:text>
</xsl:if>
<xsl:if test="@toc">
  <xsl:text> toc="</xsl:text>
  <xsl:value-of select="@toc"/>
  <xsl:text>"</xsl:text>
</xsl:if>
```

- [ ] **Step 4.3: Mark topichead elements**

In `map2markdownImpl.xsl`, topicheads (elements with navtitle but no href) currently output as plain text in the list item (line 83). Modify to add a class attribute:

```xml
<!-- Around line 82-84, when there's no href: -->
<xsl:otherwise>
  <span class="topichead">
    <xsl:value-of select="$title"/>
  </span>
</xsl:otherwise>
```

Then in `ast2markdown.xsl`, add a `span[@class='topichead']` template or ensure the generic span template outputs the class:

```xml
<xsl:template match="span[@class = 'topichead']" mode="ast">
  <xsl:apply-templates mode="ast"/>
  <xsl:text> {.topichead}</xsl:text>
</xsl:template>
```

- [ ] **Step 4.4: Include toc="no" topicrefs in map output**

Currently, topicrefs with `toc="no"` are excluded from the map output (line 107-115 only passes through to children). For round-trip fidelity, we need to include them but mark them:

Modify the `toc="no"` template to include these items with a `toc="no"` attribute on the link, but still nested under their parent:

```xml
<xsl:template match="*[contains(@class, ' map/topicref ')]
                      [@toc = 'no']
                      [not(@processing-role = 'resource-only')]"
              mode="toc">
  <xsl:param name="pathFromMaplist"/>
  <xsl:variable name="title">
    <xsl:apply-templates select="." mode="get-navtitle"/>
  </xsl:variable>
  <xsl:if test="normalize-space($title) and normalize-space(@href)">
    <li>
      <link toc="no">
        <xsl:attribute name="href">
          <xsl:call-template name="replace-extension">
            <xsl:with-param name="filename" select="@href"/>
            <xsl:with-param name="extension" select="$OUTEXT"/>
          </xsl:call-template>
        </xsl:attribute>
        <xsl:if test="@chunk">
          <xsl:attribute name="chunk" select="@chunk"/>
        </xsl:if>
        <xsl:value-of select="$title"/>
      </link>
    </li>
  </xsl:if>
  <!-- Also process children that may have toc="yes" -->
  <xsl:apply-templates select="*[contains(@class, ' map/topicref ')]" mode="toc">
    <xsl:with-param name="pathFromMaplist" select="$pathFromMaplist"/>
  </xsl:apply-templates>
</xsl:template>
```

- [ ] **Step 4.5: Run tests and commit**

```bash
./gradlew test
git add -A
git commit -m "feat: preserve map metadata (chunk, toc, topichead) in round-trip"
```

---

### Task 5: Fix Character Escaping for varname

**Files:**
- Modify: `src/main/resources/sw-d.xsl:53-59` (varname template)
- Modify: `src/main/resources/ast2markdown.xsl:510-521` (text escaping)

- [ ] **Step 5.1: Change varname to use code span instead of emph**

In `sw-d.xsl`, change the varname template from `emph` to `code` to avoid the double-encoding problem with angle brackets:

```xml
<!-- Replace lines 53-59 -->
<xsl:template match="*[contains(@class,' sw-d/varname ')]" name="topic.sw-d.varname">
  <code class="varname">
    <xsl:call-template name="commonattributes"/>
    <xsl:call-template name="setidaname"/>
    <xsl:apply-templates/>
  </code>
</xsl:template>
```

This changes varname output from `*content*{.varname}` (italic) to `` `content`{.varname} `` (code). Code spans don't escape their contents, so `<openshift_url>` stays as `<openshift_url>` rather than becoming `&lt;openshift\_url&gt;`.

- [ ] **Step 5.2: Update test fixtures if needed**

Check if any existing test fixtures reference varname output format. Update them to expect code spans instead of italic:

```bash
grep -rn "varname" src/test/resources/
```

Update any affected expected output files.

- [ ] **Step 5.3: Run tests and commit**

```bash
./gradlew test
git add -A
git commit -m "fix: use code spans for varname to prevent double-encoding"
```

---

### Task 6: Task Section Class Markers

**Files:**
- Modify: `src/main/resources/task.xsl:59-97` (section heading templates)
- Modify: `src/main/resources/dita2markdownImpl.xsl:277-290` (section template)

- [ ] **Step 6.1: Add class attributes to task section headings**

In `task.xsl`, the `generate-task-label` mode template (line 121-138) generates `<header>` elements with `default-output-class` set to `name(..)` (the parent element name like `taskbody`). We need to instead set the class to the section name (prereq, context, result, postreq).

The section heading templates (lines 59-97) are called in mode `dita2html:section-heading`. They delegate to `generate-task-label` with a label string. We need to ensure the heading gets the right class. Modify each section heading template to pass the section name as a class:

```xml
<!-- Modify prereq template (lines 59-67) -->
<xsl:template match="*[contains(@class,' task/prereq ')]" mode="dita2html:section-heading">
  <xsl:apply-templates select="." mode="generate-task-label">
    <xsl:with-param name="use-label">
      <xsl:call-template name="getVariable">
        <xsl:with-param name="id" select="'task_prereq'"/>
      </xsl:call-template>
    </xsl:with-param>
    <xsl:with-param name="section-class" select="'prereq'"/>
  </xsl:apply-templates>
</xsl:template>
```

Apply same pattern for context → `'context'`, result → `'result'`, postreq → `'postreq'`.

Then modify `generate-task-label` to accept and use the class:

```xml
<xsl:template match="*" mode="generate-task-label">
  <xsl:param name="use-label"/>
  <xsl:param name="section-class" select="''"/>
  <xsl:param name="headLevel" as="xs:integer">
    <xsl:variable name="headCount" select="count(ancestor::*[contains(@class, ' topic/topic ')]) + 1"/>
    <xsl:choose>
      <xsl:when test="$headCount > 6">6</xsl:when>
      <xsl:otherwise><xsl:value-of select="$headCount"/></xsl:otherwise>
    </xsl:choose>
  </xsl:param>
  <xsl:if test="$GENERATE-TASK-LABELS = 'YES'">
    <header level="{$headLevel}">
      <xsl:call-template name="commonattributes">
        <xsl:with-param name="default-output-class" select="if ($section-class != '') then $section-class else name(..)"/>
      </xsl:call-template>
      <xsl:value-of select="$use-label"/>
    </header>
  </xsl:if>
</xsl:template>
```

- [ ] **Step 6.2: Run tests and commit**

```bash
./gradlew test
git add -A
git commit -m "feat: add class markers to task section headings"
```

---

### Task 7: Fix HTML Table Parsing on Reverse Transform

**Files:**
- Modify: `src/main/java/com/elovirta/dita/markdown/renderer/TopicRenderer.java:765-790` (HtmlBlock render)
- Modify: `src/main/resources/hdita2dita-common.xsl` (HTML-to-DITA table mapping)

Complex tables (with block content in cells like codeblocks or lists) fall back to HTML `<table>` blocks in the MDITA output. On the reverse transform, these HTML tables are not properly parsed — content ends up as sequential `<p>` elements instead of proper `<simpletable>` structure.

- [ ] **Step 7.1: Investigate current HTML table parsing**

Read `TopicRenderer.java` lines 765-790 to understand how `HtmlBlock` nodes are processed. The method uses an XSLT transform (`hdita2dita-common.xsl`) to convert HTML to DITA. Read `hdita2dita-common.xsl` to see how HTML `<table>` elements are mapped.

```bash
grep -n "table\|simpletable\|sthead\|strow\|stentry" src/main/resources/hdita2dita-common.xsl
```

- [ ] **Step 7.2: Fix the HTML-to-DITA table XSLT**

In `hdita2dita-common.xsl`, ensure the HTML table template properly maps:
- `<table>` → `<simpletable>`
- `<thead>/<tr>` → `<sthead>`
- `<tbody>/<tr>` → `<strow>`
- `<th>`, `<td>` → `<stentry>`
- Nested block content (paragraphs, code blocks) preserved within `<stentry>`

The fix depends on what's currently broken — it may be that the XSLT template isn't matching HTML table elements, or that nested block content isn't being processed.

- [ ] **Step 7.3: Run tests and commit**

```bash
./gradlew test
git add -A
git commit -m "fix: properly parse HTML tables back to DITA simpletable"
```

---

### Task 8: Build, Install, and Iterative Round-trip Testing

**Files:**
- No source changes — this task runs transforms against the test corpus

- [ ] **Step 8.1: Build and install the updated plugin**

```bash
cd /home/aireilly/redhat.mdita.extended
./gradlew dist
dita install build/distributions/redhat.mdita.extended-0.0.4.zip --force
```

- [ ] **Step 8.2: Run forward transform (DITA → MDITA)**

```bash
dita -i /tmp/roundtrip-test/input/test.ditamap -f mdita -o /tmp/roundtrip-test/mdita-v1
```

- [ ] **Step 8.3: Inspect MDITA output for correctness**

Check that the MDITA files now contain:
- YAML front matter with `$schema` and `id`
- Admonition syntax (`!!! note`, `!!! warning`) instead of bold labels
- `{.related-links}` section headings
- Code spans for varname content

```bash
for f in /tmp/roundtrip-test/mdita-v1/*.md; do
  echo "=== $(basename $f) ==="
  head -20 "$f"
  echo "..."
done
```

- [ ] **Step 8.4: Run reverse transform (MDITA → DITA)**

```bash
dita -i /tmp/roundtrip-test/mdita-v1/test.mditamap -f dita -o /tmp/roundtrip-test/dita-v1
```

- [ ] **Step 8.5: Diff round-tripped output against original**

```bash
for f in /tmp/roundtrip-test/input/*.dita; do
  base=$(basename "$f")
  echo "=== $base ==="
  diff <(xmllint --format "$f" 2>/dev/null || cat "$f") \
       <(xmllint --format "/tmp/roundtrip-test/dita-v1/$base" 2>/dev/null || echo "MISSING") \
       || true
done > /tmp/roundtrip-test/v1-diff.txt

echo "Baseline diff lines: $(wc -l < /tmp/roundtrip-test/baseline-diff.txt)"
echo "V1 diff lines: $(wc -l < /tmp/roundtrip-test/v1-diff.txt)"
```

Expected: V1 diff should be significantly smaller than baseline diff.

- [ ] **Step 8.6: Analyze remaining differences and iterate**

Review the diff output. Common remaining issues may include:
- DTD/DOCTYPE differences (acceptable — system vs. project-relative)
- Whitespace differences (acceptable — normalize in comparison)
- Attribute ordering differences (acceptable — XML doesn't guarantee order)
- DITA class attribute formatting differences (cosmetic)

For each substantive issue found, create a fix in the appropriate file and re-run the build/install/transform cycle.

- [ ] **Step 8.7: Start ralph loop for iterative refinement**

Use the ralph loop to continuously:
1. Fix an issue in XSLT or Java
2. Rebuild: `./gradlew dist && dita install build/distributions/redhat.mdita.extended-0.0.4.zip --force`
3. Re-run transforms
4. Check diff
5. Repeat until acceptable

```bash
# Each iteration:
./gradlew dist && \
dita install build/distributions/redhat.mdita.extended-0.0.4.zip --force && \
dita -i /tmp/roundtrip-test/input/test.ditamap -f mdita -o /tmp/roundtrip-test/mdita-latest && \
dita -i /tmp/roundtrip-test/mdita-latest/test.mditamap -f dita -o /tmp/roundtrip-test/dita-latest && \
for f in /tmp/roundtrip-test/input/*.dita; do
  base=$(basename "$f")
  diff <(xmllint --format "$f" 2>/dev/null) \
       <(xmllint --format "/tmp/roundtrip-test/dita-latest/$base" 2>/dev/null) || true
done 2>&1 | head -100
```

- [ ] **Step 8.8: Final commit when round-trip is acceptable**

```bash
git add -A
git commit -m "fix: iterative round-trip improvements"
```
