# redhat.mdita.dtd

A DITA-OT DTD plugin that constrains standard DITA to only the elements representable in the MDITA extended profile. Any DITA file that validates against these DTDs is guaranteed to round-trip losslessly through MDITA conversion.

## Why

Standard DITA has hundreds of elements. The MDITA extended profile supports a useful subset. If a writer uses an element outside that subset, the MDITA converter must drop or approximate it — a lossy conversion. This plugin catches that at authoring time: if it validates, it round-trips.

## Supported document types

| Type | DITA 2.0 | DITA 1.3 |
|-----------|----------|----------|
| Topic | `mditaTopic.dtd` | `mditaTopic.dtd` |
| Concept | `mditaConcept.dtd` | `mditaConcept.dtd` |
| Task | `mditaTask.dtd` | `mditaTask.dtd` |
| Reference | `mditaReference.dtd` | `mditaReference.dtd` |
| Map | `mditaMap.dtd` | `mditaMap.dtd` |

## Usage

### DOCTYPE declarations

Use the appropriate PUBLIC identifier in your DITA files:

```xml
<!-- Topic -->
<!DOCTYPE topic PUBLIC "-//RED HAT//DTD MDITA 2.0 Topic//EN" "mditaTopic.dtd">

<!-- Concept -->
<!DOCTYPE concept PUBLIC "-//RED HAT//DTD MDITA 2.0 Concept//EN" "mditaConcept.dtd">

<!-- Task -->
<!DOCTYPE task PUBLIC "-//RED HAT//DTD MDITA 2.0 Task//EN" "mditaTask.dtd">

<!-- Reference -->
<!DOCTYPE reference PUBLIC "-//RED HAT//DTD MDITA 2.0 Reference//EN" "mditaReference.dtd">

<!-- Map -->
<!DOCTYPE map PUBLIC "-//RED HAT//DTD MDITA 2.0 Map//EN" "mditaMap.dtd">
```

For DITA 1.3, replace `2.0` with `1.3` in the PUBLIC identifier.

### DITA-OT installation

```sh
dita install redhat.mdita.dtd
```

The plugin registers its catalog entries automatically via `plugin.xml`.

### Standalone validation with xmllint

```sh
export XML_CATALOG_FILES=redhat.mdita.dtd/test/catalog.xml
xmllint --valid --noout --max-ampl 100 your-topic.dita
```

The `--max-ampl 100` flag is required — libxml2 2.12.10+ enforces entity amplification limits that DITA DTDs exceed at the default threshold.

## What's allowed

The constraint permits the MDITA extended profile elements:

**Inline:** b, i, u, sub, sup, codeph, apiname, cmdname, filepath, msgnum, option, parmname, varname, wintitle, uicontrol, menucascade, systemoutput, userinput, cite, keyword, ph, xref, term, image, data

**Block:** p, ol, ul, dl, pre, codeblock, simpletable, fig, note, lq, section, div, draft-comment, required-cleanup

**Topic structure:** title, titlealts, shortdesc, prolog, body, related-links, topic (nesting)

**Task-specific:** taskbody, prereq, context, steps, steps-unordered, step, cmd, info, substeps, substep, choices, choice, stepresult, result, postreq

**Map-specific:** topicref, topichead, topicgroup, mapref, keydef, reltable, relrow, relcell, topicmeta

### DITA 2.0 vs 1.3 differences

- **substeps/substep:** Removed in DITA 2.0 but re-declared by this plugin via `mditaSubstepsMod.mod` (the MDITA spec retains them). In 1.3 they are natively available.
- **navtitle element:** Available in 1.3 only. The 2.0 DTDs use `titlealt` with `title-role="navigation"` instead.
- **Programming domain:** In 1.3, `synph` and `syntaxdiagram` are excluded via domain sub-entity overrides.

## What's rejected

Elements outside the MDITA extended profile are rejected at validation time. Examples:

- CALS `table` (use `simpletable` instead)
- `choicetable` in task steps
- `screen` (use `codeblock` instead)
- `hazardstatement`
- `fn` (footnote)
- `abstract` (use `shortdesc` directly)
- `navref` in maps

## Plugin structure

```
redhat.mdita.dtd/
├── plugin.xml                  # DITA-OT plugin descriptor
├── catalog.xml                 # OASIS XML catalog (15 PUBLIC IDs)
├── dtd/
│   ├── 2.0/
│   │   ├── mditaTopic.dtd          # Topic shell DTD
│   │   ├── mditaConcept.dtd        # Concept shell DTD
│   │   ├── mditaTask.dtd           # Task shell DTD
│   │   ├── mditaReference.dtd      # Reference shell DTD
│   │   ├── mditaMap.dtd            # Map shell DTD
│   │   ├── mditaTopicConstraint.mod # Shared topic constraint module
│   │   ├── mditaMapConstraint.mod   # Map constraint module
│   │   └── mditaSubstepsMod.mod     # Re-declares substeps for 2.0
│   └── 1.3/
│       ├── mditaTopic.dtd
│       ├── mditaConcept.dtd
│       ├── mditaTask.dtd
│       ├── mditaReference.dtd
│       ├── mditaMap.dtd
│       ├── mditaTopicConstraint.mod
│       └── mditaMapConstraint.mod
└── test/
    ├── catalog.xml             # Test catalog (resolves to local DTDs)
    ├── validate.sh             # Validation suite
    ├── test_topic.dita         # Curated positive tests
    ├── test_concept.dita
    ├── test_task.dita
    ├── test_reference.dita
    ├── test_map.ditamap
    ├── test_1.3_topic.dita
    ├── test_1.3_task.dita
    └── negative/               # Negative tests (must fail)
        ├── neg_abstract.dita
        ├── neg_cals_table.dita
        ├── neg_choicetable.dita
        ├── neg_fn.dita
        ├── neg_hazard.dita
        └── neg_screen.dita
```

## Running tests

```sh
bash redhat.mdita.dtd/test/validate.sh
```

The suite validates curated test files and converter output fixtures (42 positive tests) and confirms that 6 negative test files are correctly rejected. Requires `xmllint` (libxml2).
