# MDITA Cutdown DTD Plugin — Design Spec

## Goal

Create a DITA-OT DTD plugin (`redhat.mdita.dtd`) that constrains standard DITA to
only the elements and attributes representable in the MDITA extended profile used by
`redhat.mdita.extended`. Any DITA XML valid against these DTDs is guaranteed to
roundtrip through MDITA with no loss of fidelity.

## Approach

Use DITA's constraint module architecture. Shell DTDs reference standard DITA `.mod`
and `.ent` files from the installed DITA-OT plugins but apply constraint modules that
redefine content model parameter entities to remove elements outside the MDITA subset.

Both DITA 2.0 and DITA 1.3 variants are provided.

## Plugin Structure

```
redhat.mdita.dtd/
  plugin.xml
  catalog.xml
  dtd/
    2.0/
      mditaTopic.dtd
      mditaConcept.dtd
      mditaTask.dtd
      mditaReference.dtd
      mditaMap.dtd
      mditaTopicConstraint.mod
      mditaMapConstraint.mod
    1.3/
      mditaTopic.dtd
      mditaConcept.dtd
      mditaTask.dtd
      mditaReference.dtd
      mditaMap.dtd
      mditaTopicConstraint.mod
      mditaMapConstraint.mod
```

## Plugin Registration

### plugin.xml

```xml
<plugin id="redhat.mdita.dtd">
  <require plugin="org.oasis-open.dita.v2_0"/>
  <require plugin="org.oasis-open.dita.techcomm.v2_0"/>
  <require plugin="org.oasis-open.dita.v1_3"/>
  <feature extension="dita.specialization.catalog.relative"
           file="catalog.xml"/>
</plugin>
```

### catalog.xml

Maps PUBLIC identifiers to DTD files:

```xml
<catalog xmlns="urn:oasis:names:tc:entity:xmlns:xml:catalog" prefer="public">
  <!-- DITA 2.0 shells -->
  <public publicId="-//RED HAT//DTD MDITA 2.0 Topic//EN"
          uri="dtd/2.0/mditaTopic.dtd"/>
  <public publicId="-//RED HAT//DTD MDITA 2.0 Concept//EN"
          uri="dtd/2.0/mditaConcept.dtd"/>
  <public publicId="-//RED HAT//DTD MDITA 2.0 Task//EN"
          uri="dtd/2.0/mditaTask.dtd"/>
  <public publicId="-//RED HAT//DTD MDITA 2.0 Reference//EN"
          uri="dtd/2.0/mditaReference.dtd"/>
  <public publicId="-//RED HAT//DTD MDITA 2.0 Map//EN"
          uri="dtd/2.0/mditaMap.dtd"/>

  <!-- DITA 1.3 shells -->
  <public publicId="-//RED HAT//DTD MDITA 1.3 Topic//EN"
          uri="dtd/1.3/mditaTopic.dtd"/>
  <public publicId="-//RED HAT//DTD MDITA 1.3 Concept//EN"
          uri="dtd/1.3/mditaConcept.dtd"/>
  <public publicId="-//RED HAT//DTD MDITA 1.3 Task//EN"
          uri="dtd/1.3/mditaTask.dtd"/>
  <public publicId="-//RED HAT//DTD MDITA 1.3 Reference//EN"
          uri="dtd/1.3/mditaReference.dtd"/>
  <public publicId="-//RED HAT//DTD MDITA 1.3 Map//EN"
          uri="dtd/1.3/mditaMap.dtd"/>
</catalog>
```

## Element Inventory

### Topic-Level Elements (Allowed)

| Element | Class | Notes |
|---------|-------|-------|
| `dita` | (compound wrapper) | Multi-topic MDITA files |
| `topic` | `- topic/topic ` | Nests for H2+ sections |
| `concept` | `- topic/topic concept/concept ` | Via `$schema` |
| `task` | `- topic/topic task/task ` | Via `$schema` |
| `reference` | `- topic/topic reference/reference ` | Via `$schema` |
| `title` | `- topic/title ` | |
| `shortdesc` | `- topic/shortdesc ` | First paragraph |
| `body` / `conbody` / `taskbody` / `refbody` | Respective classes | |
| `prolog` | `- topic/prolog ` | YAML front matter |

### Block Elements (Allowed)

| Element | Class | MDITA Syntax |
|---------|-------|-------------|
| `p` | `- topic/p ` | Paragraph |
| `section` | `- topic/section ` | H2 headings |
| `note` | `- topic/note ` | `!!! type` admonitions |
| `codeblock` | `+ topic/pre pr-d/codeblock ` | Fenced code blocks |
| `fig` | `- topic/fig ` | Image with title |
| `image` | `- topic/image ` | `![alt](src)` |
| `alt` | `- topic/alt ` | Alt text |
| `lq` | `- topic/lq ` | Blockquotes |
| `ul` | `- topic/ul ` | Unordered lists |
| `ol` | `- topic/ol ` | Ordered lists |
| `li` | `- topic/li ` | List items |
| `dl` | `- topic/dl ` | Definition lists |
| `dlentry` | `- topic/dlentry ` | |
| `dt` | `- topic/dt ` | |
| `dd` | `- topic/dd ` | |
| `simpletable` | `- topic/simpletable ` | Pipe tables |
| `sthead` | `- topic/sthead ` | |
| `strow` | `- topic/strow ` | |
| `stentry` | `- topic/stentry ` | |
| `required-cleanup` | `- topic/required-cleanup ` | Jekyll includes |

### Task Elements (Allowed)

| Element | Class | MDITA Syntax |
|---------|-------|-------------|
| `steps` | `- topic/ol task/steps ` | Ordered list |
| `steps-unordered` | `- topic/ul task/steps-unordered ` | Unordered list |
| `step` | `- topic/li task/step ` | List item |
| `cmd` | `- topic/ph task/cmd ` | First paragraph |
| `info` | `- topic/itemgroup task/info ` | Additional paragraphs |
| `substeps` | `- topic/ol task/substeps ` | Nested ordered list |
| `substep` | `- topic/li task/substep ` | |
| `choices` | `- topic/ul task/choices ` | Nested unordered list |
| `choice` | `- topic/li task/choice ` | |
| `prereq` | `- topic/section task/prereq ` | H2 "Prerequisites" |
| `context` | `- topic/section task/context ` | H2 "About this task" |
| `result` | `- topic/section task/result ` | H2 "Verification" |
| `postreq` | `- topic/section task/postreq ` | H2 "Next steps" |
| `tasktroubleshooting` | `- topic/section task/tasktroubleshooting ` | H2 "Troubleshooting" |

### Inline Elements (Allowed)

| Element | Class | MDITA Syntax |
|---------|-------|-------------|
| `b` | `+ topic/ph hi-d/b ` | `**bold**` |
| `i` | `+ topic/ph hi-d/i ` | `*italic*` |
| `u` | `+ topic/ph hi-d/u ` | `<u>` |
| `line-through` | `+ topic/ph hi-d/line-through ` | `~~strike~~` |
| `sup` | `+ topic/ph hi-d/sup ` | `^super^` |
| `sub` | `+ topic/ph hi-d/sub ` | `~sub~` |
| `tt` | `+ topic/ph hi-d/tt ` | `<tt>` |
| `ph` | `- topic/ph ` | Generic phrase |
| `codeph` | `+ topic/ph pr-d/codeph ` | `` `code` `` |
| `keyword` | `- topic/keyword ` | `{{keyref}}` |
| `cite` | `- topic/cite ` | `*text*{.cite}` |
| `xref` | `- topic/xref ` | `[text](url)` |
| `uicontrol` | `+ topic/ph ui-d/uicontrol ` | `{.uicontrol}` |
| `wintitle` | `+ topic/keyword ui-d/wintitle ` | `{.wintitle}` |
| `menucascade` | `+ topic/ph ui-d/menucascade ` | `{.menucascade}` |
| `filepath` | `+ topic/ph sw-d/filepath ` | `{.filepath}` |
| `cmdname` | `+ topic/keyword sw-d/cmdname ` | `{.cmdname}` |
| `varname` | `+ topic/keyword sw-d/varname ` | `{.varname}` |
| `msgph` | `+ topic/ph sw-d/msgph ` | `{.msgph}` |
| `userinput` | `+ topic/ph sw-d/userinput ` | `{.userinput}` |
| `systemoutput` | `+ topic/ph sw-d/systemoutput ` | `{.systemoutput}` |
| `option` | `+ topic/keyword pr-d/option ` | `{.option}` |
| `parmname` | `+ topic/keyword pr-d/parmname ` | `{.parmname}` |
| `apiname` | `+ topic/keyword pr-d/apiname ` | `{.apiname}` |

### Prolog/Metadata Elements (Allowed)

| Element | YAML Key |
|---------|----------|
| `metadata` | (container) |
| `keywords` | (container) |
| `keyword` | `keyword:` |
| `author` | `author:` |
| `source` | `source:` |
| `publisher` | `publisher:` |
| `permissions` | `permissions:` |
| `audience` | `audience:` |
| `category` | `category:` |
| `resourceid` | `resourceid:` |
| `data` | (catch-all) |

### Map Elements (Allowed)

| Element | Class |
|---------|-------|
| `map` | `- map/map ` |
| `topicref` | `- map/topicref ` |
| `topicmeta` | `- map/topicmeta ` |
| `navtitle` | `- topic/navtitle ` |
| `topichead` | `+ map/topicref mapgroup-d/topichead ` |
| `keydef` | `+ map/topicref mapgroup-d/keydef ` |
| `mapref` | `+ map/topicref mapgroup-d/mapref ` |
| `reltable` | `- map/reltable ` |
| `relheader` | `- map/relheader ` |
| `relcolspec` | `- map/relcolspec ` |
| `relrow` | `- map/relrow ` |
| `relcell` | `- map/relcell ` |

### Attributes (Allowed)

- Structural: `id`, `class`, `outputclass`, `specializations`, `ditaarch:DITAArchVersion`
- Linking: `href`, `keyref`, `keys`, `conref`, `format`, `scope`, `type`
- Profiling: `audience`, `platform`, `product`, `otherprops`, `deliveryTarget`, `props`, `rev`
- Image: `height`, `width`, `placement`
- Table: `colspan`
- Code: `xml:space="preserve"`
- Note: `type`, `othertype`
- Map: `toc`, `collection-type`
- Permissions: `view`

### Excluded Elements

The following standard DITA elements are NOT allowed:

- `abstract`, `titlealts`, `related-links`, `link`, `linklist`, `linkpool`, `linkinfo`, `linktext`
- `table` (CALS), `tgroup`, `colspec`, `spanspec`, `thead`, `tbody`, `row`, `entry`
- `div`, `bodydiv`, `sectiondiv`, `lines`, `pre` (base)
- `fn`, `indexterm`, `indextermref`
- `object`, `param`, `foreign`, `unknown`, `draft-comment`, `desc`, `longdescref`
- `example` (use `section` instead)
- `choicetable`, `chhead`, `chrow`, `choption`, `chdesc`, `choptionhd`, `chdeschd`
- `stepsection`, `stepxmp`, `stepresult`, `tutorialinfo`
- `screen`, `shortcut` (ui-d)
- All hazard-d, equation-d, svg-d, mathml-d, syntaxdiagram-d, markup-d, xml-d, hw-d, relmgmt-d, abbrev-d elements
- `sl`, `sli`, `itemgroup` (standalone)
- `dlhead`, `dthd`, `ddhd`
- `boolean`, `state`, `q`, `term`, `tm`
- `figgroup`, `no-topic-nesting`, `text`

## Constraint Module Implementation

### mditaTopicConstraint.mod (DITA 2.0)

Redefines content model entities before `topic.mod` is included:

```dtd
<!-- Restrict body content to MDITA-representable blocks -->
<!ENTITY % body.cnt
    "%dl; | %fig; | %image; | %lq; | %note; | %ol; |
     %p; | %pre; | %simpletable; | %ul; | required-cleanup"
>

<!-- Restrict section content -->
<!ENTITY % section.cnt
    "#PCDATA | %dl; | %fig; | %image; | %lq; | %note; | %ol; |
     %p; | %ph; | %pre; | %simpletable; | %ul; | %keyword; |
     %xref; | %cite; | required-cleanup"
>

<!-- Restrict section-no-title content -->
<!ENTITY % section.notitle.cnt
    "#PCDATA | %dl; | %fig; | %image; | %lq; | %note; | %ol; |
     %p; | %ph; | %pre; | %simpletable; | %ul; | %keyword; |
     %xref; | %cite; | required-cleanup"
>
```

### Domain Sub-Entity Overrides (BEFORE .ent file inclusion)

Standard domain `.ent` files include elements outside the MDITA subset. Because
DTD uses "first declaration wins," we declare restricted versions of the domain
sub-entities BEFORE including the `.ent` files:

```dtd
<!-- pr-d: exclude synph (not representable in MDITA) -->
<!ENTITY % pr-d-ph      "codeph">
<!-- pr-d: exclude kwd (not representable in MDITA) -->
<!ENTITY % pr-d-keyword  "option | parmname | apiname">

<!-- sw-d: exclude msgnum (not representable in MDITA) -->
<!ENTITY % sw-d-keyword  "cmdname | varname">

<!-- ui-d: exclude shortcut (not representable in MDITA) -->
<!ENTITY % ui-d-keyword  "wintitle">
```

### Domain Extension Overrides (AFTER .ent, BEFORE .mod)

```dtd
<!-- Only MDITA-subset specializations of ph -->
<!ENTITY % ph  "ph | %hi-d-ph; | %pr-d-ph; | %sw-d-ph; | %ui-d-ph;">

<!-- Only MDITA-subset specializations of keyword -->
<!ENTITY % keyword "keyword | %pr-d-keyword; | %sw-d-keyword; | %ui-d-keyword;">

<!-- Only codeblock from pre -->
<!ENTITY % pre  "pre | %pr-d-pre;">

<!-- No domain specializations of these -->
<!ENTITY % fig  "fig">
<!ENTITY % note "note">
<!ENTITY % data "data">
<!ENTITY % dl   "dl">
<!ENTITY % term "term">
<!ENTITY % div  "div">
<!ENTITY % foreign "foreign">
<!ENTITY % metadata "metadata">
```

The full domain `.mod` files are still included (declaring all elements), but
unreachable elements (like `synph`, `kwd`, `parml`, `shortcut`, `msgnum`, `screen`)
cannot appear in any content model and will cause validation errors if used.

### Shell DTD Pattern (mditaTopic.dtd example)

1. Domain sub-entity overrides (restrict `%pr-d-ph;`, `%pr-d-keyword;`, etc.)
2. Domain entity declarations (hi-d, pr-d, sw-d, ui-d `.ent` files only)
3. Domain attribute declarations (audience, deliveryTarget, platform, product, otherprops)
4. Domain extension overrides (`%ph;`, `%keyword;`, `%pre;`, etc. — restricted)
5. Domain attribute extensions
6. Topic nesting override (`%topic-info-types = "topic"`)
7. Domains/specializations attribute override
8. Constraint module inclusion (`mditaTopicConstraint.mod`)
9. Topic element integration (`topic.mod`)
10. Domain element integration (hi-d.mod, programmingDomain.mod, softwareDomain.mod, uiDomain.mod)

The concept, task, and reference shells add their respective `.mod` files after
`topic.mod`. The task shell uses the general task content model (not
`strictTaskbodyConstraint.mod`) because MDITA task body ordering is determined
by Markdown heading order, which is more flexible than strict task sequencing.

### mditaMapConstraint.mod

Restricts map content models similarly:

- Only `topicref`, `topichead`, `keydef`, `mapref`, `reltable` in map body
- No `topicgroup`, `anchorref`, `ditavalref`, `glossref`, `subjectScheme` elements

## DITA 1.3 Differences

The DITA 1.3 constraint mechanism is similar but:

- Uses `constraints` attribute entity instead of `specializations`
- `.mod` files are from `org.oasis-open.dita.v1_3` plugin paths
- Domain `.ent` file paths differ
- `line-through` does not exist in hi-d 1.3 (use `outputclass="line-through"`)
- Some attribute group entity names differ

The 1.3 shells follow the same pattern but reference `org.oasis-open.dita.v1_3`
PUBLIC identifiers.

## Testing

### Positive validation

Validate all `.dita` test fixtures from `src/test/resources/dita/` against the
cutdown DTDs. Every file the MDITA reader produces must be valid.

Test files to validate (topics): `task.dita`, `task_default_titles.dita`,
`taskOneStep.dita`, `taskTight.dita`, `concept.dita`, `reference.dita`,
`topic.dita`, `inline.dita`, `inline_extended.dita`, `domain_elements.dita`,
`note.dita`, `dl.dita`, `table.dita`, `image.dita`, `image-size.dita`,
`quote.dita`, `codeblock.dita`, `header.dita`, `header_attributes.dita`,
`shortdesc.dita`, `profiling.dita`, `keyref.dita`, `keyref_link.dita`,
`keyref_variable.dita`, `link.dita`, `ol.dita`, `ul.dita`, `yaml.dita`,
`linebreak.dita`, `escape.dita`, `entity.dita`, `admonition.dita`,
`conref.dita`, `conkeyref.dita`, `related_links.dita`, `body_attributes.dita`,
`multiple_top_level.dita`, `multiple_top_level_specialized.dita`,
`nested.dita`, `short.dita`, `thematic_break.dita`, `comment.dita`,
`html.dita`, `jekyll.dita`.

Test files to validate (maps): `map/map.dita`, `map/map_keys.dita`,
`map/map_ol.dita`, `map/map_reltable.dita`, `map/map_title.dita`,
`map/map_topichead.dita`, `map/map_without_title.dita`, `map/map_yaml.dita`.

### Negative validation

Create test files using excluded elements and verify validation failure:

- `neg_cals_table.dita` — uses `<table><tgroup>` CALS table
- `neg_choicetable.dita` — uses `<choicetable>` in task
- `neg_screen.dita` — uses `<screen>` from ui-d
- `neg_hazard.dita` — uses `<hazardstatement>`
- `neg_fn.dita` — uses `<fn>` footnote
- `neg_abstract.dita` — uses `<abstract>` instead of `<shortdesc>`

### Roundtrip proof

For selected fixtures, run the DITA→MDITA→DITA cycle and diff against originals
to verify no loss.

## Usage

After installation:

```xml
<!DOCTYPE topic PUBLIC "-//RED HAT//DTD MDITA 2.0 Topic//EN" "mditaTopic.dtd">
<topic id="example">
  <title>Example</title>
  <body>
    <p>Only MDITA-representable elements are valid here.</p>
  </body>
</topic>
```

```xml
<!DOCTYPE task PUBLIC "-//RED HAT//DTD MDITA 2.0 Task//EN" "mditaTask.dtd">
```

```xml
<!DOCTYPE map PUBLIC "-//RED HAT//DTD MDITA 2.0 Map//EN" "mditaMap.dtd">
```
