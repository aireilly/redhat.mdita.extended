#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
export XML_CATALOG_FILES="$SCRIPT_DIR/catalog.xml"

PASS=0
FAIL=0
SKIP=0

validate() {
  local file="$1"
  if xmllint --valid --noout --max-ampl 100 "$file" 2>/dev/null; then
    echo "  PASS: $(basename "$file")"
    PASS=$((PASS + 1))
  else
    echo "  FAIL: $(basename "$file")"
    xmllint --valid --noout --max-ampl 100 "$file" 2>&1 | head -3 || true
    FAIL=$((FAIL + 1))
  fi
}

validate_fixture() {
  local doctype="$1"
  local public_id="$2"
  local dtd_file="$3"
  local file="$4"
  local tmp
  tmp=$(mktemp)

  echo "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" > "$tmp"
  echo "<!DOCTYPE $doctype PUBLIC \"$public_id\" \"$dtd_file\">" >> "$tmp"
  # Strip any existing XML declaration from the fixture
  sed '/^<?xml /d' "$file" >> "$tmp"

  if xmllint --valid --noout --max-ampl 100 "$tmp" 2>/dev/null; then
    echo "  PASS: $(basename "$file")"
    PASS=$((PASS + 1))
  else
    echo "  FAIL: $(basename "$file")"
    xmllint --valid --noout --max-ampl 100 "$tmp" 2>&1 | head -3 || true
    FAIL=$((FAIL + 1))
  fi
  rm -f "$tmp"
}

echo "=== DITA 2.0 MDITA DTD Validation ==="
echo ""

echo "--- Curated Test Files ---"
for f in "$SCRIPT_DIR"/test_topic.dita \
         "$SCRIPT_DIR"/test_concept.dita \
         "$SCRIPT_DIR"/test_task.dita \
         "$SCRIPT_DIR"/test_reference.dita \
         "$SCRIPT_DIR"/test_map.ditamap; do
  if [ -f "$f" ]; then
    validate "$f"
  else
    echo "  SKIP: $(basename "$f") (not found)"
    SKIP=$((SKIP + 1))
  fi
done

echo ""
echo "--- Converter Output Fixtures (topics) ---"
# domain_elements.dita excluded: contains menucascade with text content
# instead of uicontrol children (invalid per DITA spec, converter bug)
TOPIC_FIXTURES=(
  inline inline_extended note dl codeblock
  header header_attributes shortdesc profiling
  keyref keyref_link keyref_variable link
  ol ul yaml linebreak escape entity admonition conref conkeyref
  body_attributes nested short thematic_break comment jekyll
  image image-size quote topic
)

for f in "${TOPIC_FIXTURES[@]}"; do
  src="$REPO_ROOT/src/test/resources/dita/${f}.dita"
  if [ -f "$src" ]; then
    validate_fixture "topic" "-//RED HAT//DTD MDITA 2.0 Topic//EN" "mditaTopic.dtd" "$src"
  else
    echo "  SKIP: ${f}.dita (not found)"
    SKIP=$((SKIP + 1))
  fi
done

echo ""
echo "--- Converter Output Fixtures (concepts) ---"
for f in concept; do
  src="$REPO_ROOT/src/test/resources/dita/${f}.dita"
  if [ -f "$src" ]; then
    validate_fixture "concept" "-//RED HAT//DTD MDITA 2.0 Concept//EN" "mditaConcept.dtd" "$src"
  else
    echo "  SKIP: ${f}.dita (not found)"
    SKIP=$((SKIP + 1))
  fi
done

echo ""
echo "--- Converter Output Fixtures (tasks) ---"
# task_default_titles.dita excluded: places title inside prereq which uses
# section.notitle.cnt — title in prereq is invalid per DITA spec
TASK_FIXTURES=(task taskOneStep taskTight)
for f in "${TASK_FIXTURES[@]}"; do
  src="$REPO_ROOT/src/test/resources/dita/${f}.dita"
  if [ -f "$src" ]; then
    validate_fixture "task" "-//RED HAT//DTD MDITA 2.0 Task//EN" "mditaTask.dtd" "$src"
  else
    echo "  SKIP: ${f}.dita (not found)"
    SKIP=$((SKIP + 1))
  fi
done

echo ""
echo "--- Converter Output Fixtures (references) ---"
for f in reference; do
  src="$REPO_ROOT/src/test/resources/dita/${f}.dita"
  if [ -f "$src" ]; then
    validate_fixture "reference" "-//RED HAT//DTD MDITA 2.0 Reference//EN" "mditaReference.dtd" "$src"
  else
    echo "  SKIP: ${f}.dita (not found)"
    SKIP=$((SKIP + 1))
  fi
done

echo ""
echo "--- Converter Output Fixtures (maps) ---"
# All map fixtures use navtitle element (DITA 1.3, removed in 2.0).
# Map validation is covered by the curated test_map.ditamap above.
echo "  SKIP: converter map fixtures use DITA 1.3 navtitle element"

echo ""
echo "--- Negative Tests (must FAIL validation) ---"
NEG_PASS=0
NEG_FAIL=0
validate_negative() {
  local file="$1"
  if xmllint --valid --noout --max-ampl 100 "$file" 2>/dev/null; then
    echo "  UNEXPECTED PASS: $(basename "$file") — should have been rejected"
    NEG_FAIL=$((NEG_FAIL + 1))
  else
    echo "  CORRECTLY REJECTED: $(basename "$file")"
    NEG_PASS=$((NEG_PASS + 1))
  fi
}

for f in "$SCRIPT_DIR"/negative/neg_*.dita; do
  if [ -f "$f" ]; then
    validate_negative "$f"
  fi
done

echo ""
echo "=== Results ==="
echo "  Positive: $PASS passed, $FAIL failed, $SKIP skipped"
echo "  Negative: $NEG_PASS correctly rejected, $NEG_FAIL unexpected passes"
[ "$FAIL" -eq 0 ] && [ "$NEG_FAIL" -eq 0 ] || exit 1
