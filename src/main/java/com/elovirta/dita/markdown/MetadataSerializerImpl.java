package com.elovirta.dita.markdown;

import static com.elovirta.dita.markdown.renderer.Utils.buildAtts;
import static org.dita.dost.util.Constants.*;

import com.elovirta.dita.markdown.renderer.NodeRendererContext;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.vladsch.flexmark.ext.yaml.front.matter.AbstractYamlFrontMatterVisitor;
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterBlock;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.dita.dost.util.DitaClass;
import org.dita.dost.util.XMLUtils;

public class MetadataSerializerImpl implements MetadataSerializer {

  private final Set<String> knownKeys;

  public MetadataSerializerImpl(Boolean idFromYaml) {
    final ImmutableSet.Builder<String> keys = ImmutableSet.<String>builder().add(
      TOPIC_AUTHOR.localName,
      TOPIC_SOURCE.localName,
      TOPIC_PUBLISHER.localName,
      TOPIC_PERMISSIONS.localName,
      TOPIC_AUDIENCE.localName,
      TOPIC_CATEGORY.localName,
      TOPIC_RESOURCEID.localName,
      TOPIC_KEYWORD.localName,
      "keys",
      "$schema"
    );
    if (idFromYaml) {
      keys.add(ATTRIBUTE_NAME_ID);
    }
    knownKeys = keys.build();
  }

  @Override
  public void render(final YamlFrontMatterBlock node, final NodeRendererContext context, final SaxWriter html) {
    final AbstractYamlFrontMatterVisitor v = new AbstractYamlFrontMatterVisitor();
    v.visit(node);
    final Map<String, List<String>> header = v.getData();

    write(header, TOPIC_AUTHOR, html);
    write(header, TOPIC_SOURCE, html);
    write(header, TOPIC_PUBLISHER, html);
    // copyright
    // critdates
    write(header, TOPIC_PERMISSIONS, "view", html);
    if (
      header.containsKey(TOPIC_AUDIENCE.localName) ||
      header.containsKey(TOPIC_CATEGORY.localName) ||
      header.containsKey(TOPIC_KEYWORD.localName)
    ) {
      html.startElement(node, TOPIC_METADATA, buildAtts(TOPIC_METADATA));
      write(header, TOPIC_AUDIENCE, ATTRIBUTE_NAME_TYPE, html);
      write(header, TOPIC_CATEGORY, html);
      if (header.containsKey(TOPIC_KEYWORD.localName)) {
        html.startElement(node, TOPIC_KEYWORDS, buildAtts(TOPIC_KEYWORDS));
        write(header, TOPIC_KEYWORD, html);
        html.endElement();
      }
      // prodinfo
      // othermeta
      html.endElement();
    }
    write(header, TOPIC_RESOURCEID, "appid", html);
    // Exclude keys that are nested under 'keys:' in the YAML front matter,
    // since flexmark flattens nested YAML into top-level entries.
    final Set<String> dynamicExclusions = getNestedKeysNames(node);
    final Set<String> allExclusions = dynamicExclusions.isEmpty()
      ? knownKeys
      : ImmutableSet.<String>builder().addAll(knownKeys).addAll(dynamicExclusions).build();
    final List<String> keys = Sets.difference(header.keySet(), allExclusions)
      .stream()
      .sorted()
      .collect(Collectors.toList());
    for (String key : keys) {
      for (String val : header.get(key)) {
        html.startElement(
          node,
          TOPIC_DATA.localName,
          new XMLUtils.AttributesBuilder()
            .add(ATTRIBUTE_NAME_CLASS, TOPIC_DATA.toString())
            .add(ATTRIBUTE_NAME_NAME, key)
            .add(ATTRIBUTE_NAME_VALUE, val)
            .build()
        );
        html.endElement();
      }
    }
  }

  private void write(final Map<String, List<String>> header, final DitaClass elem, SaxWriter html) {
    if (header.containsKey(elem.localName)) {
      for (String v : header.get(elem.localName)) {
        html.startElement(null, elem, buildAtts(elem));
        if (v != null) {
          html.characters(v);
        }
        html.endElement();
      }
    }
  }

  /**
   * Parse the raw YAML front matter text to find key names nested under {@code keys:}.
   * Flexmark flattens these into top-level entries, so we need to identify and
   * exclude them from the generic {@code <data>} output.
   */
  private static Set<String> getNestedKeysNames(YamlFrontMatterBlock node) {
    final String yamlText = node.getChars().toString();
    final Set<String> result = new java.util.HashSet<>();
    final String[] lines = yamlText.split("\\r?\\n");
    boolean inKeysBlock = false;
    for (String line : lines) {
      if (line.equals("---")) {
        continue;
      }
      if (!inKeysBlock) {
        if (line.matches("^keys:\\s*$")) {
          inKeysBlock = true;
        }
        continue;
      }
      if (line.matches("^\\s+.*")) {
        final String trimmed = line.trim();
        final int colonPos = trimmed.indexOf(':');
        if (colonPos > 0) {
          result.add(trimmed.substring(0, colonPos).trim());
        }
      } else {
        break;
      }
    }
    return result;
  }

  private void write(final Map<String, List<String>> header, final DitaClass elem, final String attr, SaxWriter html) {
    if (header.containsKey(elem.localName)) {
      for (String v : header.get(elem.localName)) {
        html.startElement(
          null,
          elem,
          new XMLUtils.AttributesBuilder().add(ATTRIBUTE_NAME_CLASS, elem.toString()).add(attr, v).build()
        );
        html.endElement();
      }
    }
  }
}
