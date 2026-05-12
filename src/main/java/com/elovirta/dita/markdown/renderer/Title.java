package com.elovirta.dita.markdown.renderer;

import com.vladsch.flexmark.ext.anchorlink.AnchorLink;
import com.vladsch.flexmark.ext.attributes.AttributeNode;
import com.vladsch.flexmark.ext.attributes.AttributesNode;
import com.vladsch.flexmark.util.ast.Node;
import java.util.*;

class Title {

  final Collection<String> classes;
  final Map<String, String> attributes;
  final Optional<String> id;

  private Title(List<AttributesNode> attributesNodes) {
    classes = getClasses(attributesNodes);
    attributes = getAttributes(attributesNodes);
    id = getId(attributesNodes);
  }

  public static Title getFromNext(final Node node) {
    return new Title(getNextAttributesNodes(node));
  }

  public static Title getFromChildren(final Node node) {
    //        return new Title(getAttributesNodes(node));
    return new Title(getPreviousAttributesNodes(node));
  }

  private static List<AttributesNode> getNextAttributesNodes(Node current) {
    final List<AttributesNode> res = new ArrayList<>();
    Node node = current.getNext();
    while (node instanceof AttributesNode) {
      res.add((AttributesNode) node);
      node = node.getNext();
    }
    return res;
  }

  private static List<AttributesNode> getPreviousAttributesNodes(Node current) {
    final List<AttributesNode> res = new ArrayList<>();
    // When AnchorLinkExtension is active, heading content is wrapped in
    // an AnchorLink node. Look inside it for trailing AttributesNode children.
    Node startNode = current.getLastChild();
    if (startNode instanceof AnchorLink) {
      startNode = startNode.getLastChild();
    }
    Node node = startNode;
    while (node != null) {
      if (node instanceof AttributesNode) {
        res.add((AttributesNode) node);
      } else {
        break;
      }
      node = node.getPrevious();
    }
    Collections.reverse(res);
    return res;
  }

  private static Map<String, String> getAttributes(List<AttributesNode> attributesNodes) {
    final Map<String, String> res = new HashMap<>();
    for (AttributesNode attributesNode : attributesNodes) {
      for (Node child : attributesNode.getChildren()) {
        if (child instanceof AttributeNode) {
          final AttributeNode attributeNode = (AttributeNode) child;
          if (!isClass(attributeNode) && !isId(attributeNode)) {
            res.put(attributeNode.getName().toString(), attributeNode.getValue().toString());
          }
        }
      }
    }
    return res;
  }

  private static Optional<String> getId(List<AttributesNode> attributesNodes) {
    for (AttributesNode attributesNode : attributesNodes) {
      for (Node child : attributesNode.getChildren()) {
        if (child instanceof AttributeNode) {
          final AttributeNode attributeNode = (AttributeNode) child;
          if (isId(attributeNode)) {
            return Optional.of(attributeNode.getValue().toString());
          }
        }
      }
    }
    return Optional.empty();
  }

  private static boolean isId(AttributeNode attributeNode) {
    return attributeNode.getName().toString().equals("#") || attributeNode.getName().toString().equals("id");
  }

  private List<String> getClasses(List<AttributesNode> attributesNodes) {
    final List<String> res = new ArrayList<>();
    for (AttributesNode attributesNode : attributesNodes) {
      for (Node child : attributesNode.getChildren()) {
        if (child instanceof AttributeNode) {
          final AttributeNode attributeNode = (AttributeNode) child;
          if (isClass(attributeNode)) {
            res.add(attributeNode.getValue().toString());
          }
        }
      }
    }
    return res;
  }

  private static boolean isClass(AttributeNode attributeNode) {
    return attributeNode.getName().toString().equals(".") || attributeNode.getName().toString().equals("class");
  }
}
