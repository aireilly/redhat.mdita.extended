/*
 * Based on ToHtmlSerializer (C) 2010-2011 Mathias Doenitz
 */
package com.elovirta.dita.markdown.renderer;

import static com.elovirta.dita.markdown.renderer.Utils.buildAtts;
import static javax.xml.XMLConstants.XML_NS_URI;
import static org.dita.dost.util.Constants.*;
import static org.dita.dost.util.XMLUtils.AttributesBuilder;

import com.elovirta.dita.markdown.*;
import com.elovirta.dita.utils.FragmentContentHandler;
import com.vladsch.flexmark.ast.*;
import com.vladsch.flexmark.ext.admonition.AdmonitionBlock;
import com.vladsch.flexmark.ext.attributes.AttributesNode;
import com.vladsch.flexmark.ext.definition.DefinitionItem;
import com.vladsch.flexmark.ext.definition.DefinitionList;
import com.vladsch.flexmark.ext.definition.DefinitionTerm;
import com.vladsch.flexmark.ext.jekyll.tag.JekyllTag;
import com.vladsch.flexmark.ext.jekyll.tag.JekyllTagBlock;
import com.vladsch.flexmark.ext.tables.*;
import com.vladsch.flexmark.ext.yaml.front.matter.AbstractYamlFrontMatterVisitor;
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterBlock;
import com.vladsch.flexmark.util.ast.ContentNode;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.ast.ReferenceNode;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import com.vladsch.flexmark.util.visitor.AstHandler;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.TransformerHandler;
import nu.validator.htmlparser.common.XmlViolationPolicy;
import nu.validator.htmlparser.sax.HtmlParser;
import org.dita.dost.util.DitaClass;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * A renderer for a set of node types.
 */
public class TopicRenderer extends AbstractRenderer {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle("com.elovirta.dita.messages");

  private static final Attributes TOPIC_ATTS = new AttributesBuilder()
    .add(ATTRIBUTE_NAME_CLASS, TOPIC_TOPIC.toString())
    .add(
      DITA_NAMESPACE,
      ATTRIBUTE_NAME_DITAARCHVERSION,
      ATTRIBUTE_PREFIX_DITAARCHVERSION + ":" + ATTRIBUTE_NAME_DITAARCHVERSION,
      "CDATA",
      "2.0"
    )
    //            .add(ATTRIBUTE_NAME_DOMAINS, "(topic hi-d) (topic ut-d) (topic indexing-d) (topic hazard-d) (topic abbrev-d) (topic pr-d) (topic sw-d) (topic ui-d)")
    .build();
  private static final Attributes BODY_ATTS = buildAtts(TOPIC_BODY);
  private static final Attributes NOTE_ATTS = buildAtts(TOPIC_NOTE);
  private static final Attributes LI_ATTS = buildAtts(TOPIC_LI);
  private static final Attributes P_ATTS = buildAtts(TOPIC_P);
  private static final Attributes DD_ATTS = buildAtts(TOPIC_DD);
  private static final Attributes CODEBLOCK_ATTS = buildAtts(PR_D_CODEBLOCK);
  private static final Attributes PRE_ATTS = buildAtts(TOPIC_PRE);
  private static final Attributes DT_ATTS = buildAtts(TOPIC_DT);
  private static final Attributes SHORTDESC_ATTS = buildAtts(TOPIC_SHORTDESC);
  private static final Attributes PROLOG_ATTS = buildAtts(TOPIC_PROLOG);
  private static final Attributes BLOCKQUOTE_ATTS = buildAtts(TOPIC_LQ);
  private static final Attributes UL_ATTS = buildAtts(TOPIC_UL);
  private static final Attributes DL_ATTS = buildAtts(TOPIC_DL);
  private static final Attributes DLENTRY_ATTS = buildAtts(TOPIC_DLENTRY);
  private static final Attributes OL_ATTS = buildAtts(TOPIC_OL);
  private static final Attributes SIMPLETABLE_ATTS = buildAtts(TOPIC_SIMPLETABLE);
  private static final Attributes STHEAD_ATTS = buildAtts(TOPIC_STHEAD);
  private static final Attributes STROW_ATTS = buildAtts(TOPIC_STROW);
  private static final Attributes STENTRY_ATTS = buildAtts(TOPIC_STENTRY);
  private static final Attributes XREF_ATTS = buildAtts(TOPIC_XREF);
  private static final Pattern KEYREF_URL_PATTERN = Pattern.compile("\\{\\{([a-zA-Z0-9_.\\-]+)\\}\\}");
  private static final Attributes FIG_ATTS = buildAtts(TOPIC_FIG);
  private static final Attributes REQUIRED_CLEANUP_ATTS = buildAtts(TOPIC_REQUIRED_CLEANUP);

  public static final String TIGHT_LIST_P = "tight-list-p";

  /**
   * Default heading titles that map to task section elements without
   * requiring explicit class attributes.
   */
  private static final Map<String, String> DEFAULT_TASK_SECTION_TITLES = Map.of(
    "prerequisites",
    TASK_PREREQ.localName,
    "about this task",
    TASK_CONTEXT.localName,
    "verification",
    TASK_RESULT.localName,
    "next steps",
    TASK_POSTREQ.localName,
    "troubleshooting",
    TASK_TASKTROUBLESHOOTING.localName
  );

  private static final Map<String, String> DEFAULT_SECTION_TITLES = Map.of(
    "related information",
    "related-links",
    "related links",
    "related-links"
  );

  private final boolean shortdescParagraph;
  private final boolean idFromYaml;
  private final boolean tightList;

  private int currentTableColumn;
  private boolean inSection = false;

  private String lastId;

  /**
   * Topic type detected from YAML $schema field (e.g. "task", "concept", "reference").
   * Null if no $schema or unrecognized schema.
   */
  private String schemaType;

  /**
   * Current header level.
   */
  private int headerLevel = 0;

  public TopicRenderer(DataHolder options) {
    super(options);
    shortdescParagraph = DitaRenderer.SHORTDESC_PARAGRAPH.get(options);
    idFromYaml = DitaRenderer.ID_FROM_YAML.get(options);
    tightList = DitaRenderer.TIGHT_LIST.get(options);
  }

  @Override
  public Map<Class<? extends Node>, NodeRenderingHandler<? extends Node>> getNodeRenderingHandlers() {
    final List<NodeRenderingHandler<? extends Node>> res = new ArrayList<>(super.getNodeRenderingHandlers().values());
    res.add(new NodeRenderingHandler<>(TableBlock.class, this::renderSimpleTableBlock));
    res.add(new NodeRenderingHandler<>(TableCaption.class, this::renderSimpleTableCaption));
    res.add(new NodeRenderingHandler<>(TableBody.class, this::renderSimpleTableBody));
    res.add(new NodeRenderingHandler<>(TableHead.class, this::renderSimpleTableHead));
    res.add(new NodeRenderingHandler<>(TableRow.class, this::renderSimpleTableRow));
    res.add(new NodeRenderingHandler<>(TableCell.class, this::renderSimpleTableCell));
    res.add(new NodeRenderingHandler<>(TableSeparator.class, this::renderSimpleTableSeparator));
    if (!mditaCoreProfile) {
      res.add(new NodeRenderingHandler<>(DefinitionList.class, this::render));
      res.add(new NodeRenderingHandler<>(DefinitionTerm.class, this::render));
      res.add(new NodeRenderingHandler<>(DefinitionItem.class, this::render));
    }
    res.add(new NodeRenderingHandler<>(AdmonitionBlock.class, this::render));
    res.add(new NodeRenderingHandler<>(AutoLink.class, this::render));
    res.add(new NodeRenderingHandler<>(YamlFrontMatterBlock.class, this::render));
    res.add(new NodeRenderingHandler<>(BlockQuote.class, this::render));
    res.add(new NodeRenderingHandler<>(BulletList.class, this::render));
    res.add(new NodeRenderingHandler<>(CodeBlock.class, this::render));
    res.add(new NodeRenderingHandler<>(Document.class, this::render));
    res.add(new NodeRenderingHandler<>(FencedCodeBlock.class, this::render));
    res.add(new NodeRenderingHandler<>(HardLineBreak.class, this::render));
    res.add(new NodeRenderingHandler<>(Heading.class, this::render));
    res.add(new NodeRenderingHandler<>(HtmlBlock.class, this::render));
    res.add(new NodeRenderingHandler<>(HtmlCommentBlock.class, this::render));
    res.add(new NodeRenderingHandler<>(HtmlInnerBlock.class, this::render));
    res.add(new NodeRenderingHandler<>(HtmlInnerBlockComment.class, this::render));
    res.add(new NodeRenderingHandler<>(HtmlInline.class, this::render));
    res.add(new NodeRenderingHandler<>(Image.class, this::render));
    res.add(new NodeRenderingHandler<>(ImageRef.class, this::render));
    res.add(new NodeRenderingHandler<>(IndentedCodeBlock.class, this::render));
    res.add(new NodeRenderingHandler<>(Link.class, this::render));
    res.add(new NodeRenderingHandler<>(LinkRef.class, this::render));
    res.add(new NodeRenderingHandler<>(BulletListItem.class, this::render));
    res.add(new NodeRenderingHandler<>(OrderedListItem.class, this::render));
    res.add(new NodeRenderingHandler<>(MailLink.class, this::render));
    res.add(new NodeRenderingHandler<>(OrderedList.class, this::render));
    res.add(new NodeRenderingHandler<>(Paragraph.class, this::render));
    res.add(new NodeRenderingHandler<>(Reference.class, this::render));
    res.add(new NodeRenderingHandler<>(SoftLineBreak.class, this::render));
    res.add(new NodeRenderingHandler<>(Text.class, this::render));
    res.add(new NodeRenderingHandler<>(ThematicBreak.class, this::render));
    res.add(new NodeRenderingHandler<>(JekyllTagBlock.class, this::render));
    res.add(new NodeRenderingHandler<>(JekyllTag.class, this::render));

    final Map<Class<? extends Node>, NodeRenderingHandler<? extends Node>> map = new HashMap<>(
      super.getNodeRenderingHandlers()
    );
    map.putAll(
      res
        .stream()
        .collect(
          Collectors.<
              NodeRenderingHandler<? extends Node>,
              Class<? extends Node>,
              NodeRenderingHandler<? extends Node>
            >toMap(AstHandler::getNodeType, Function.identity())
        )
    );
    return map;
  }

  // Visitor methods

  private void render(final Document node, final NodeRendererContext context, final SaxWriter html) {
    final boolean isCompound = hasMultipleTopLevelHeaders(node);
    if (isCompound) {
      final AttributesBuilder atts = new AttributesBuilder().add(
        DITA_NAMESPACE,
        ATTRIBUTE_NAME_DITAARCHVERSION,
        ATTRIBUTE_PREFIX_DITAARCHVERSION + ":" + ATTRIBUTE_NAME_DITAARCHVERSION,
        "CDATA",
        "2.0"
      );
      if (mditaCoreProfile) {
        atts.add(ATTRIBUTE_NAME_SPECIALIZATIONS, "(topic hi-d)(topic em-d)");
      } else if (mditaExtendedProfile) {
        atts.add(
          ATTRIBUTE_NAME_SPECIALIZATIONS,
          "(topic hi-d)(topic em-d)(topic ui-d)(topic sw-d)(topic pr-d) @props/audience @props/deliveryTarget @props/otherprops @props/platform @props/product"
        );
      } else {
        atts.add(
          ATTRIBUTE_NAME_SPECIALIZATIONS,
          "@props/audience @props/deliveryTarget @props/otherprops @props/platform @props/product"
        );
      }
      html.startElement(node, ELEMENT_NAME_DITA, atts.build());
    }
    context.renderChildren(node);
    if (isCompound) {
      html.endElement();
    }
  }

  private void render(final AdmonitionBlock node, final NodeRendererContext context, final SaxWriter html) {
    final String type = node.getInfo().toString();
    final AttributesBuilder atts = new AttributesBuilder(NOTE_ATTS);
    switch (type.toLowerCase()) {
      case "note":
      case "tip":
      case "fastpath":
      case "restriction":
      case "important":
      case "remember":
      case "attention":
      case "caution":
      case "notice":
      case "danger":
      case "warning":
      case "trouble":
        atts.add("type", type.toLowerCase());
        break;
      default:
        atts.add("type", "other").add("othertype", type);
        break;
    }
    html.startElement(node, TOPIC_NOTE, atts.build());
    if (!node.getTitle().isEmpty()) {
      html.startElement(node, TOPIC_P, P_ATTS);
      html.characters(node.getTitle().toString());
      html.endElement();
    }
    context.renderChildren(node);
    html.endElement();
  }

  private void render(JekyllTagBlock node, final NodeRendererContext context, final SaxWriter html) {
    context.renderChildren(node);
  }

  private void render(JekyllTag node, final NodeRendererContext context, final SaxWriter html) {
    if (node.getTag().toString().equals("include")) {
      final AttributesBuilder atts = new AttributesBuilder(REQUIRED_CLEANUP_ATTS).add(
        ATTRIBUTE_NAME_CONREF,
        node.getParameters().toString()
      );
      html.startElement(node, TOPIC_REQUIRED_CLEANUP, atts.build());
      html.endElement();
    }
  }

  private void render(final AutoLink node, final NodeRendererContext context, final SaxWriter html) {
    if (node.getChars().charAt(0) == '<') {
      final AttributesBuilder atts = getLinkAttributes(node.getText().toString());

      html.startElement(node, TOPIC_XREF, getInlineAttributes(node, atts.build()));
      html.characters(node.getText().toString());
      html.endElement();
    } else {
      context.renderChildren(node);
    }
  }

  private void render(final BlockQuote node, final NodeRendererContext context, final SaxWriter html) {
    if (mditaCoreProfile || mditaExtendedProfile) {
      context.renderChildren(node);
    } else {
      printTag(node, context, html, TOPIC_LQ, getAttributesFromAttributesNode(node, BLOCKQUOTE_ATTS));
    }
  }

  private void render(final BulletList node, final NodeRendererContext context, final SaxWriter html) {
    printTag(node, context, html, TOPIC_UL, getAttributesFromAttributesNode(node, UL_ATTS));
  }

  private void render(final DefinitionList node, final NodeRendererContext context, final SaxWriter html) {
    html.startElement(node, TOPIC_DL, getAttributesFromAttributesNode(node, DL_ATTS));
    DitaClass previous = null;
    //        for (final Node child : node.getChildren()) {
    //            if (previous == null) {
    //                html.startElement(TOPIC_DLENTRY, DLENTRY_ATTS);
    //            }
    //            if (child instanceof DefinitionTermNode) {
    //                if (TOPIC_DD.equals(previous)) {
    //                    html.endElement(); // dlentry
    //                    html.startElement(TOPIC_DLENTRY, DLENTRY_ATTS);
    //                }
    //            }
    context.renderChildren(node);
    //            previous = (child instanceof DefinitionTermNode) ? TOPIC_DT : TOPIC_DD;
    //        }
    //        html.endElement(); // dlentry
    html.endElement(); // dl
  }

  private void render(final DefinitionTerm node, final NodeRendererContext context, final SaxWriter html) {
    if (node.getPrevious() == null || !(node.getPrevious() instanceof DefinitionTerm)) {
      html.startElement(node, TOPIC_DLENTRY, DLENTRY_ATTS);
    }
    //        printTag(node, context, html, TOPIC_DT, DT_ATTS);
    html.startElement(node, TOPIC_DT, DT_ATTS);
    Node child = node.getFirstChild();
    while (child != null) {
      Node next = child.getNext();
      context.renderChildren(child);
      child = next;
    }
    html.endElement();
  }

  private void render(final DefinitionItem node, final NodeRendererContext context, final SaxWriter html) {
    printTag(node, context, html, TOPIC_DD, DD_ATTS);
    if (node.getNext() == null || !(node.getNext() instanceof DefinitionItem)) {
      html.endElement();
    }
  }

  private void render(final Image node, final NodeRendererContext context, final SaxWriter html) {
    final AttributesBuilder atts = new AttributesBuilder(getInlineAttributes(node, IMAGE_ATTS)).add(
      ATTRIBUTE_NAME_HREF,
      node.getUrl().toString()
    );
    writeImage(node, node.getTitle().toString(), null, atts, context, html);
  }

  private void writeImage(
    Image node,
    final String title,
    final String alt,
    final AttributesBuilder atts,
    final NodeRendererContext context,
    SaxWriter html
  ) {
    if (!title.isEmpty()) {
      html.startElement(node, TOPIC_FIG, FIG_ATTS);
      html.startElement(node, TOPIC_TITLE, TITLE_ATTS);
      html.characters(title);
      html.endElement();
      html.startElement(node, TOPIC_IMAGE, atts.build());
      if (node.hasChildren()) {
        html.startElement(node, TOPIC_ALT, ALT_ATTS);
        if (alt != null) {
          html.characters(alt);
        } else {
          context.renderChildren(node);
        }
        html.endElement();
      }
      html.endElement();
      html.endElement();
    } else {
      if (onlyImageChild) {
        atts.add("placement", "break");
      }
      html.startElement(node, TOPIC_IMAGE, atts.build());
      if (node.hasChildren()) {
        html.startElement(node, TOPIC_ALT, ALT_ATTS);
        if (alt != null) {
          html.characters(alt);
        } else {
          context.renderChildren(node);
        }
        html.endElement();
      }
      html.endElement();
    }
  }

  private void writeImage(
    ImageRef node,
    final String title,
    final String alt,
    final AttributesBuilder atts,
    final NodeRendererContext context,
    SaxWriter html
  ) {
    if (!title.isEmpty()) {
      html.startElement(node, TOPIC_FIG, FIG_ATTS);
      html.startElement(node, TOPIC_TITLE, TITLE_ATTS);
      html.characters(title);
      html.endElement();
      html.startElement(node, TOPIC_IMAGE, atts.build());
      if (node.hasChildren()) {
        html.startElement(node, TOPIC_ALT, ALT_ATTS);
        if (alt != null) {
          html.characters(alt);
        } else {
          context.renderChildren(node);
        }
        html.endElement();
      }
      html.endElement(); // image
      html.endElement(); // fig
    } else {
      if (onlyImageChild) {
        atts.add("placement", "break");
      }
      html.startElement(node, TOPIC_IMAGE, atts.build());
      if (node.hasChildren()) {
        html.startElement(node, TOPIC_ALT, ALT_ATTS);
        if (alt != null) {
          html.characters(alt);
        } else {
          context.renderChildren(node);
        }
        html.endElement();
      }
      html.endElement(); // image
    }
  }

  private void render(final Heading node, final NodeRendererContext context, final SaxWriter html) {
    if (inSection) {
      html.endElement(); // section or example
      inSection = false;
    }
    final DitaClass cls;
    final boolean isSection;
    final String headingText = node.getText().toString().trim().toLowerCase();
    if ((mditaCoreProfile || mditaExtendedProfile) && node.getLevel() == 2) {
      isSection = true;
      cls = TOPIC_SECTION;
    } else if (!mditaCoreProfile) {
      if (DEFAULT_TASK_SECTION_TITLES.containsKey(headingText) || DEFAULT_SECTION_TITLES.containsKey(headingText)) {
        isSection = true;
        cls = TOPIC_SECTION;
      } else {
        isSection = false;
        cls = null;
      }
    } else {
      isSection = false;
      cls = null;
    }
    if (isSection) {
      if (node.getLevel() <= headerLevel) {
        throw new ParseException(
          String.format(
            "Level %d section title must be higher level than parent topic title %d",
            node.getLevel(),
            headerLevel
          )
        );
      }
      final AttributesBuilder atts = new AttributesBuilder().add(ATTRIBUTE_NAME_CLASS, cls.toString());
      final String id = getSectionId(node);
      if (id != null) {
        atts.add(ATTRIBUTE_NAME_ID, id);
      }
      if (!mditaCoreProfile) {
        final List<String> classes = new ArrayList<>();
        final String defaultTaskClass = DEFAULT_TASK_SECTION_TITLES.get(headingText);
        if (defaultTaskClass != null) {
          classes.add(defaultTaskClass);
        } else {
          final String defaultSectionClass = DEFAULT_SECTION_TITLES.get(headingText);
          if (defaultSectionClass != null) {
            classes.add(defaultSectionClass);
          }
        }
        if (!classes.isEmpty()) {
          atts.add("outputclass", String.join(" ", classes));
        }
      }
      if (!mditaCoreProfile) {
        final Title profilingHeader = Title.getFromChildren(node);
        readProfilingAttributes(profilingHeader, atts);
      }
      html.startElement(node, cls, atts.build());
      inSection = true;
      html.startElement(node, TOPIC_TITLE, TITLE_ATTS);
      context.renderChildren(node);
      html.endElement(); // title
    } else {
      if (headerLevel > 0) {
        html.endElement(); // body
      }
      for (; node.getLevel() <= headerLevel; headerLevel--) {
        html.endElement(); // topic
      }
      headerLevel = node.getLevel();

      final AttributesBuilder atts;
      if (mditaCoreProfile) {
        atts = new AttributesBuilder(TOPIC_ATTS).add(ATTRIBUTE_NAME_SPECIALIZATIONS, "(topic hi-d)(topic em-d)");
      } else if (mditaExtendedProfile) {
        atts = new AttributesBuilder(TOPIC_ATTS).add(
          ATTRIBUTE_NAME_SPECIALIZATIONS,
          "(topic hi-d)(topic em-d)(topic ui-d)(topic sw-d)(topic pr-d) @props/audience @props/deliveryTarget @props/otherprops @props/platform @props/product"
        );
      } else {
        atts = new AttributesBuilder(TOPIC_ATTS).add(
          ATTRIBUTE_NAME_SPECIALIZATIONS,
          "@props/audience @props/deliveryTarget @props/otherprops @props/platform @props/product"
        );
      }

      final String id = getTopicId(node);
      if (id != null) {
        lastId = id;
        atts.add(ATTRIBUTE_NAME_ID, id);
      }
      if (!mditaCoreProfile && node.getLevel() == 1) {
        final String yamlType = getSchemaType(node);
        if (yamlType != null) {
          atts.add(ATTRIBUTE_NAME_OUTPUTCLASS, yamlType);
        }
        schemaType = yamlType;
      }
      if (!mditaCoreProfile) {
        final Title profilingHeader = Title.getFromChildren(node);
        readProfilingAttributes(profilingHeader, atts);
      }
      html.startElement(node, TOPIC_TOPIC, atts.build());
      html.startElement(node, TOPIC_TITLE, TITLE_ATTS);
      context.renderChildren(node);
      html.endElement(); // title
      if (shortdescParagraph && node.getNext() instanceof Paragraph) {
        html.startElement(node.getNext(), TOPIC_SHORTDESC, SHORTDESC_ATTS);
        context.renderChildren(node.getNext());
        html.endElement(); // shortdesc
      }
      if (node.getLevel() == 1) {
        final Node firstChild = node.getDocument().getFirstChild();
        if (firstChild instanceof YamlFrontMatterBlock) {
          html.startElement(firstChild, TOPIC_PROLOG, PROLOG_ATTS);
          metadataSerializer.render((YamlFrontMatterBlock) firstChild, context, html);
          html.endElement();
        }
      }
      html.startElement(node, TOPIC_BODY, BODY_ATTS);
    }
  }

  private String getSectionId(Heading node) {
    if (node.getAnchorRefId() != null) {
      return node.getAnchorRefId();
    }
    return null;
  }

  private String getTopicId(final Heading node) {
    if (idFromYaml && node.getLevel() == 1 && node.getDocument().getChildOfType(YamlFrontMatterBlock.class) != null) {
      final AbstractYamlFrontMatterVisitor v = new AbstractYamlFrontMatterVisitor();
      v.visit(node.getDocument());
      final Map<String, List<String>> metadata = v.getData();
      final List<String> ids = metadata.get("id");
      if (ids != null && !ids.isEmpty()) {
        return ids.get(0);
      }
    }
    if (node.getAnchorRefId() != null) {
      return node.getAnchorRefId();
    }
    return getId(node.getText().toString());
  }

  /**
   * Extract topic type from YAML front matter {@code $schema} field.
   * Expects values like {@code urn:oasis:names:tc:dita:xsd:task.xsd}.
   *
   * @return topic type string (e.g. "task", "concept", "reference") or null
   */
  private String getSchemaType(final Node node) {
    final Document doc = node instanceof Document ? (Document) node : node.getDocument();
    if (doc.getChildOfType(YamlFrontMatterBlock.class) == null) {
      return null;
    }
    final AbstractYamlFrontMatterVisitor v = new AbstractYamlFrontMatterVisitor();
    v.visit(doc);
    final Map<String, List<String>> metadata = v.getData();
    final List<String> schemas = metadata.get("$schema");
    if (schemas == null || schemas.isEmpty()) {
      return null;
    }
    final String schema = schemas.get(0);
    // Parse type from urn:oasis:names:tc:dita:xsd:{type}.xsd
    final java.util.regex.Matcher m = java.util.regex.Pattern.compile(
      "urn:oasis:names:tc:dita:xsd:(\\w+)\\.xsd"
    ).matcher(schema);
    if (m.matches()) {
      final String type = m.group(1);
      // Only return recognized specialization types
      if ("task".equals(type) || "concept".equals(type) || "reference".equals(type)) {
        return type;
      }
    }
    return null;
  }

  private void render(final YamlFrontMatterBlock node, final NodeRendererContext context, final SaxWriter html) {
    // YAML header is pulled by Heading renderer
  }

  private static String getId(final String contents) {
    return contents.toLowerCase().replaceAll("[^\\w]", "").trim().replaceAll("\\s+", "_");
  }

  /**
   * Render HTML block into DITA.
   */
  private void render(final HtmlBlock node, final NodeRendererContext context, final SaxWriter html) {
    final String text = node.getChars().toString();
    final FragmentContentHandler fragmentFilter = new FragmentContentHandler();
    fragmentFilter.setContentHandler(html);
    final TransformerHandler h;
    try {
      h = transformerFactorySupplier.get().newTransformerHandler(templatesSupplier.get());
    } catch (final TransformerConfigurationException e) {
      throw new RuntimeException(e);
    }
    final Transformer transformer = h.getTransformer();
    transformer.setParameter("formats", String.join(",", formats));
    transformer.setParameter("raw-dita", rawDita);
    h.setResult(new SAXResult(fragmentFilter));
    final HtmlParser parser = new HtmlParser();
    parser.setNamePolicy(XmlViolationPolicy.ALLOW);
    final NamespaceFilter filter = new NamespaceFilter(parser);
    filter.setContentHandler(h);
    try {
      html.setLocation(node);
      filter.parse(new InputSource(new StringReader(text)));
    } catch (IOException | SAXException e) {
      throw new ParseException(String.format(MESSAGES.getString("error.html_parse_fail"), e.getMessage()), e);
    }
    html.setDocumentLocator();
  }

  private void render(final HtmlInline node, final NodeRendererContext context, final SaxWriter html) {
    final String text = node.getChars().toString();
    final TransformerHandler h;
    try {
      h = transformerFactorySupplier.get().newTransformerHandler(templatesSupplier.get());
    } catch (final TransformerConfigurationException e) {
      throw new RuntimeException(e);
    }
    h.getTransformer().setParameter("formats", String.join(",", formats));
    final HtmlParser parser = new HtmlParser(XmlViolationPolicy.ALLOW);
    parser.setContentHandler(h);
    html.setLocation(node);
    if (text.startsWith("</")) {
      h.setResult(new SAXResult(new EndElementHandler(html)));
      final String data = text.replaceAll("/", "") + text;
      try (final StringReader in = new StringReader(data)) {
        parser.parse(new InputSource(in));
      } catch (IOException | SAXException e) {
        throw new ParseException(String.format(MESSAGES.getString("error.html_parse_fail"), e.getMessage()), e);
      }
    } else {
      h.setResult(new SAXResult(new StartElementHandler(html)));
      try (final StringReader in = new StringReader(text)) {
        parser.parse(new InputSource(in));
      } catch (IOException | SAXException e) {
        throw new ParseException(String.format(MESSAGES.getString("error.html_parse_fail"), e.getMessage()), e);
      }
      if (text.endsWith("/>")) {
        html.endElement();
      }
    }
    html.setDocumentLocator();
  }

  private void render(final ListItem node, final NodeRendererContext context, final SaxWriter html) {
    printTag(node, context, html, TOPIC_LI, LI_ATTS);
  }

  private void render(final MailLink node, final NodeRendererContext context, final SaxWriter html) {
    final AttributesBuilder atts = getLinkAttributes("mailto:" + node.getText());
    atts.add(ATTRIBUTE_NAME_FORMAT, "email");

    html.startElement(node, TOPIC_XREF, getInlineAttributes(node, atts.build()));
    context.renderChildren(node);
    html.endElement();
  }

  private void render(final OrderedList node, final NodeRendererContext context, final SaxWriter html) {
    printTag(node, context, html, TOPIC_OL, getAttributesFromAttributesNode(node, OL_ATTS));
  }

  private boolean onlyImageChild = false;

  private void render(final Paragraph node, final NodeRendererContext context, final SaxWriter html) {
    if (isAttributesParagraph(node)) {
      return;
    }
    if (shortdescParagraph && !inSection && node.getPrevious() instanceof Heading) {
      // Pulled by Heading
    } else if (containsImage(node)) {
      onlyImageChild = true;
      context.renderChildren(node);
      onlyImageChild = false;
    } else {
      final Node parent = node.getParent();
      if (
        tightList &&
        parent.isOrDescendantOfType(BulletListItem.class, OrderedListItem.class) &&
        !parent.isOrDescendantOfType(BlockQuote.class) &&
        ListItem.class.isAssignableFrom(parent.getClass()) &&
        ((ListItem) parent).isTight()
      ) {
        html.startElement(node, TIGHT_LIST_P, P_ATTS);
        context.renderChildren(node);
        html.endElement();
        return;
      }
      printTag(node, context, html, TOPIC_P, P_ATTS);
    }
  }

  /**
   * Contains only single image
   */
  private boolean containsImage(final ContentNode node) {
    final Node first = node.getFirstChild();
    if (first instanceof Image || first instanceof ImageRef) {
      return first.getNext() == null;
    }
    return false;
  }

  private void render(final ReferenceNode node, final NodeRendererContext context, final SaxWriter html) {
    throw new RuntimeException();
  }

  private void render(final Reference node, final NodeRendererContext context, final SaxWriter html) {
    // Ignore
  }

  private void render(final ImageRef node, final NodeRendererContext context, final SaxWriter html) {
    final String text = node.getText().toString();
    final String key = node.getReference() != null ? node.getReference().toString() : text;
    final Reference refNode = node.getReferenceNode(node.getDocument());
    if (refNode == null) {
      // "fake" reference image link
      final AttributesBuilder atts = new AttributesBuilder(IMAGE_ATTS).add(ATTRIBUTE_NAME_KEYREF, key);
      if (onlyImageChild) {
        atts.add("placement", "break");
      }
      html.startElement(node, TOPIC_IMAGE, getInlineAttributes(node, atts.build()));
      html.endElement();
    } else {
      final AttributesBuilder atts = new AttributesBuilder(getInlineAttributes(node, IMAGE_ATTS)).add(
        ATTRIBUTE_NAME_HREF,
        refNode.getUrl().toString()
      );
      if (key != null) {
        atts.add(ATTRIBUTE_NAME_KEYREF, key);
      }
      writeImage(node, refNode.getTitle().toString(), text, atts, context, html);
    }
  }

  private void render(final RefNode node, final NodeRendererContext context, final SaxWriter html) {
    final String text = node.getText().toString();
    final String key = node.getReference() != null ? node.getReference().toString() : text;
    final Reference refNode = node.getReferenceNode(node.getDocument());
    if (refNode == null) {
      // "fake" reference link
      final AttributesBuilder atts = new AttributesBuilder(XREF_ATTS).add(ATTRIBUTE_NAME_KEYREF, key);
      html.startElement(node, TOPIC_XREF, atts.build());
      if (!node.getText().toString().isEmpty()) {
        html.characters(node.getText().toString());
      }
      html.endElement();
    } else {
      final AttributesBuilder atts = getLinkAttributes(refNode.getUrl().toString());
      html.startElement(node, TOPIC_XREF, atts.build());
      if (!refNode.getTitle().toString().isEmpty()) {
        html.characters(refNode.getTitle().toString());
      } else {
        context.renderChildren(node);
      }
      html.endElement();
    }
  }

  private void render(final Link node, final NodeRendererContext context, final SaxWriter html) {
    final String url = node.getUrl().toString();
    final Matcher m = KEYREF_URL_PATTERN.matcher(url);
    if (m.matches()) {
      final AttributesBuilder atts = new AttributesBuilder(XREF_ATTS)
        .add(ATTRIBUTE_NAME_KEYREF, m.group(1));
      html.startElement(node, TOPIC_XREF, getInlineAttributes(node, atts.build()));
    } else {
      final AttributesBuilder atts = getLinkAttributes(url);
      html.startElement(node, TOPIC_XREF, getInlineAttributes(node, atts.build()));
    }
    context.renderChildren(node);
    html.endElement();
  }

  // Simple table

  private void renderSimpleTableBlock(final TableBlock node, final NodeRendererContext context, final SaxWriter html) {
    html.startElement(node, TOPIC_SIMPLETABLE, getAttributesFromAttributesNode(node, SIMPLETABLE_ATTS));
    final Node caption = node.getChildOfType(TableCaption.class);
    if (caption != null) {
      html.startElement(caption, TOPIC_TITLE, TITLE_ATTS);
      context.renderChildren(caption);
      html.endElement();
    }

    context.renderChildren(node);
    html.endElement(); // table
    //    currentTableNode = null;
  }

  private void renderSimpleTableCaption(
    final TableCaption node,
    final NodeRendererContext context,
    final SaxWriter html
  ) {
    // Pull processed by TableBlock
  }

  private void renderSimpleTableHead(final TableHead node, final NodeRendererContext context, final SaxWriter html) {
    context.renderChildren(node);
  }

  private void renderSimpleTableBody(final TableBody node, final NodeRendererContext context, final SaxWriter html) {
    context.renderChildren(node);
  }

  private void renderSimpleTableSeparator(TableSeparator node, NodeRendererContext context, SaxWriter html) {
    // Ignore
  }

  private void renderSimpleTableRow(final TableRow node, final NodeRendererContext context, final SaxWriter html) {
    currentTableColumn = 0;
    if (node.getParent() instanceof TableHead) {
      printTag(node, context, html, TOPIC_STHEAD, STHEAD_ATTS);
    } else {
      printTag(node, context, html, TOPIC_STROW, STROW_ATTS);
    }
  }

  private void renderSimpleTableCell(final TableCell node, final NodeRendererContext context, final SaxWriter html) {
    final AttributesBuilder atts = new AttributesBuilder(STENTRY_ATTS);
    if (node.getSpan() > 1) {
      atts.add("colspan", Integer.toString(node.getSpan()));
    }
    html.startElement(node, TOPIC_STENTRY, atts.build());
    if (isInline(node.getFirstChild())) {
      html.startElement(node, TOPIC_P, P_ATTS);
      context.renderChildren(node);
      html.endElement();
    } else {
      context.renderChildren(node);
    }
    html.endElement();

    currentTableColumn += node.getSpan();
  }

  private boolean isInline(Node node) {
    return node instanceof Text || node instanceof Emphasis || node instanceof StrongEmphasis;
  }

  // Code block

  private void render(final CodeBlock node, final NodeRendererContext context, final SaxWriter html) {
    final AttributesBuilder atts = new AttributesBuilder(mditaExtendedProfile ? PRE_ATTS : CODEBLOCK_ATTS).add(
      XML_NS_URI,
      "space",
      "xml:space",
      "CDATA",
      "preserve"
    );
    html.startElement(node, mditaExtendedProfile ? TOPIC_PRE : PR_D_CODEBLOCK, atts.build());
    String text = node.getChars().toString();
    if (text.endsWith("\n")) {
      text = text.substring(0, text.length() - 1);
    }
    html.characters(text);
    html.endElement();
  }

  private void render(final IndentedCodeBlock node, final NodeRendererContext context, final SaxWriter html) {
    final AttributesBuilder atts = new AttributesBuilder(mditaExtendedProfile ? PRE_ATTS : CODEBLOCK_ATTS).add(
      XML_NS_URI,
      "space",
      "xml:space",
      "CDATA",
      "preserve"
    );
    html.startElement(node, mditaExtendedProfile ? TOPIC_PRE : PR_D_CODEBLOCK, atts.build());
    // FIXME: For compatibility with HTML pre/code, should be removed
    if (mditaExtendedProfile) {
      html.startElement(node, HI_D_TT, TT_ATTS);
    }
    String text = node.getContentChars().toString();
    if (text.endsWith("\n")) {
      text = text.substring(0, text.length() - 1);
    }
    html.characters(text);
    if (mditaExtendedProfile) {
      html.endElement();
    }
    html.endElement();
  }

  private void render(final FencedCodeBlock node, final NodeRendererContext context, final SaxWriter html) {
    final AttributesBuilder atts = new AttributesBuilder(mditaExtendedProfile ? PRE_ATTS : CODEBLOCK_ATTS).add(
      XML_NS_URI,
      "space",
      "xml:space",
      "CDATA",
      "preserve"
    );

    BasedSequence info = node.getInfo();
    if (info.startsWith("{") && info.endsWith("}")) {
      final Metadata metadata = Metadata.parse(info.subSequence(1, info.length() - 1).toString());
      if (!metadata.classes.isEmpty()) {
        atts.add("outputclass", String.join(" ", metadata.classes));
      }
      if (metadata.id != null) {
        atts.add(ATTRIBUTE_NAME_ID, metadata.id);
      }
      for (Map.Entry<String, String> entry : metadata.attrs.entrySet()) {
        atts.add(entry.getKey(), entry.getValue());
      }
    } else if (info.isNotNull() && !info.isBlank()) {
      int space = info.indexOf(' ');
      BasedSequence language;
      if (space == -1) {
        language = info;
      } else {
        language = info.subSequence(0, space);
      }
      atts.add("outputclass", context.getDitaOptions().languageClassPrefix + language.unescape());
    } else {
      String noLanguageClass = context.getDitaOptions().noLanguageClass.trim();
      if (!noLanguageClass.isEmpty()) {
        atts.add("outputclass", noLanguageClass);
      }
    }

    html.startElement(node, mditaExtendedProfile ? TOPIC_PRE : PR_D_CODEBLOCK, atts.build());
    // FIXME: For compatibility with HTML pre/code, should be removed
    if (mditaExtendedProfile) {
      html.startElement(node, HI_D_TT, TT_ATTS);
    }
    String text = node.getContentChars().normalizeEOL();
    if (text.endsWith("\n")) {
      text = text.substring(0, text.length() - 1);
    }
    html.characters(text);
    if (mditaExtendedProfile) {
      html.endElement();
    }
    html.endElement();
  }

  private void render(final Text node, final NodeRendererContext context, final SaxWriter html) {
    if (node.getParent() instanceof Code) {
      html.characters(node.getChars().toString());
    } else {
      html.characters(node.getChars().unescapeNoEntities());
    }
  }

  private void render(final ContentNode node, final NodeRendererContext context, final SaxWriter html) {
    context.renderChildren(node);
  }

  private void render(final SoftLineBreak node, final NodeRendererContext context, final SaxWriter html) {
    html.characters('\n');
  }

  private void render(final HardLineBreak node, final NodeRendererContext context, final SaxWriter html) {
    html.processingInstruction("linebreak", null);
  }

  private void render(final Node node, final NodeRendererContext context, final SaxWriter html) {
    throw new RuntimeException(
      "No renderer configured for " + node.getNodeName() + " = " + node.getClass().getCanonicalName()
    );
  }

  private boolean isAttributesParagraph(final Node node) {
    if (node == null) {
      return false;
    }
    final Node firstChild = node.getFirstChild();
    return firstChild instanceof AttributesNode && firstChild.getNext() == null;
  }

  private Attributes getAttributesFromAttributesNode(Node node, Attributes base) {
    if (isAttributesParagraph(node.getPrevious())) {
      final Title header = Title.getFromChildren(node.getPrevious());
      final AttributesBuilder builder = new AttributesBuilder(base);
      return readProfilingAttributes(header, builder).build();
    } else {
      return base;
    }
  }

  // helpers

  @Override
  protected AttributesBuilder getLinkAttributes(final String href) {
    return getLinkAttributes(href, XREF_ATTS);
  }
}
