package com.elovirta.dita.markdown;

import static org.junit.jupiter.api.Assertions.*;

import com.elovirta.dita.utils.AbstractReaderTest;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class MDitaReaderExtendedTest extends AbstractReaderTest {

  private MarkdownReader r = new MDitaReader();

  @Override
  public MarkdownReader getReader() {
    return r;
  }

  @Override
  public String getExp() {
    return "xdita-ext/";
  }

  @Override
  public String getSrc() {
    return "markdown/";
  }

  @ParameterizedTest
  @ValueSource(
    strings = {
      "codeblock.md",
      "comment.md",
      "concept.md",
      "conkeyref.md",
      "conref.md",
      "dl.md",
      "entity.md",
      "escape.md",
      "dita_block.md",
      "html.md",
      "html_unsupported.md",
      "image.md",
      "inline.md",
      "jekyll.md",
      "keyref.md",
      "keys.md",
      "linebreak.md",
      "link.md",
      "multiple_top_level.md",
      "multiple_top_level_specialized.md",
      "note.md",
      "ol.md",
      "profiling.md",
      "quote.md",
      "reference.md",
      "short.md",
      "shortdesc.md",
      "table-width.md",
      "table.md",
      "task.md",
      "taskTight.md",
      "taskOneStep.md",
      "testBOM.md",
      "testNoBOM.md",
      "ul.md",
      "yaml.md",
      "related_links.md",
      "keyref_variable.md",
    }
  )
  public void test(String file) throws Exception {
    run(file);
  }

  @ParameterizedTest
  @ValueSource(strings = { "header.md", "invalid_header.md", "invalid_header_third.md" })
  public void test_fail(String file) {
    assertThrows(SAXException.class, () -> {
      final String input = "/" + getSrc() + file;
      try (final InputStream in = getClass().getResourceAsStream(input)) {
        final InputSource i = new InputSource(in);
        reader.parse(i);
      }
    });
  }

  @Test
  public void testLocator() throws IOException, SAXException {
    testLocatorParsing(
      Arrays.asList(
        new Event("startDocument", 1, 1),
        new Event("startElement", "topic", 1, 1),
        new Event("startElement", "title", 1, 1),
        new Event("characters", "Shortdesc", 1, 1),
        new Event("endElement", "title", 1, 1),
        new Event("startElement", "shortdesc", 3, 1),
        new Event("characters", "Shortdesc.", 3, 1),
        new Event("endElement", "shortdesc", 3, 1),
        new Event("startElement", "body", 1, 1),
        new Event("startElement", "p", 5, 1),
        new Event("characters", "Paragraph.", 5, 1),
        new Event("endElement", "p", 5, 1),
        new Event("endElement", "body", 5, 1),
        new Event("endElement", "topic", 5, 1),
        new Event("endDocument", 5, 1)
      ),
      "shortdesc.md"
    );
  }

  @Test
  public void taskOneStep() throws IOException, SAXException {
    testLocatorParsing(
      Arrays.asList(
        new Event("startDocument", 1, 1),
        new Event("startElement", "task", 4, 1),
        new Event("startElement", "title", 4, 1),
        new Event("characters", "Task", 4, 1),
        new Event("endElement", "title", 4, 1),
        new Event("startElement", "shortdesc", 6, 1),
        new Event("characters", "Context", 6, 1),
        new Event("endElement", "shortdesc", 6, 1),
        new Event("startElement", "prolog", 1, 1),
        new Event("endElement", "prolog", 1, 1),
        new Event("startElement", "taskbody", 4, 1),
        new Event("startElement", "steps", 8, 1),
        new Event("startElement", "step", 8, 1),
        new Event("startElement", "cmd", 8, 5),
        new Event("characters", "Command", 8, 5),
        new Event("endElement", "cmd", 8, 5),
        new Event("startElement", "info", 10, 5),
        new Event("startElement", "p", 10, 5),
        new Event("characters", "Info.", 10, 5),
        new Event("endElement", "p", 10, 5),
        new Event("endElement", "info", 10, 5),
        new Event("endElement", "step", 10, 5),
        new Event("endElement", "steps", 10, 5),
        new Event("endElement", "taskbody", 10, 5),
        new Event("endElement", "task", 10, 5),
        new Event("endDocument", 10, 5)
      ),
      "taskOneStep.md"
    );
  }
}
