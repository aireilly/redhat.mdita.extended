package com.elovirta.dita.utils;

import com.elovirta.dita.markdown.MDitaReader;
import com.elovirta.dita.markdown.MarkdownReader;
import java.io.*;
import java.net.URI;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

public class GenerateExpected {

  public static void main(String[] args) throws Exception {
    final TransformerFactory tf = TransformerFactory.newInstance();
    tf.setURIResolver(new ClasspathURIResolver(tf.getURIResolver()));
    final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    final DocumentBuilder db = dbf.newDocumentBuilder();

    String[] files = {
      "concept",
      "reference",
      "task",
      "taskTight",
      "taskOneStep",
      "multiple_top_level_specialized",
      "header",
      "profiling",
      "related_links",
      "html_unsupported",
      "table",
    };

    for (String file : files) {
      generateFile(tf, db, new MarkdownReader(), "markdown/" + file + ".md", "dita/" + file + ".dita");
      generateFile(tf, db, new MDitaReader(), "markdown/" + file + ".md", "xdita-ext/" + file + ".dita");
    }

    String[] astFiles = {
      "admonition",
      "ast",
      "codeblock",
      "comment",
      "concept",
      "conkeyref",
      "conref",
      "dl",
      "entity",
      "escape",
      "hdita",
      "header",
      "html",
      "html_unsupported",
      "image",
      "inline",
      "inline_extended",
      "jekyll",
      "keyref",
      "keys",
      "linebreak",
      "link",
      "missing_root_header",
      "missing_root_header_with_yaml",
      "multiple_top_level",
      "multiple_top_level_specialized",
      "note",
      "ol",
      "pandoc_header",
      "quote",
      "reference",
      "short",
      "shortdesc",
      "table-block",
      "table-width",
      "table",
      "task",
      "taskOneStep",
      "taskTight",
      "testBOM",
      "testNoBOM",
      "topic",
      "ul",
      "yaml",
    };

    for (String file : astFiles) {
      generateAst(tf, db, "dita/" + file + ".dita", "output/ast/" + file + ".xml");
    }

    for (String file : astFiles) {
      generateMarkdownFromAst(tf, "output/ast/" + file + ".xml", "output/markdown/" + file + ".md");
    }
  }

  private static void generateFile(
    TransformerFactory tf,
    DocumentBuilder db,
    XMLReader reader,
    String srcPath,
    String expPath
  ) throws Exception {
    try (InputStream in = GenerateExpected.class.getResourceAsStream("/" + srcPath)) {
      if (in == null) {
        System.err.println("SKIP (no source): " + srcPath);
        return;
      }
      Document act = db.newDocument();
      Transformer t = tf.newTransformer();
      InputSource is = new InputSource(in);
      is.setSystemId(URI.create("classpath:/" + srcPath).toString());
      t.transform(new SAXSource(reader, is), new DOMResult(act));

      File outFile = new File("src/test/resources/" + expPath);
      t = tf.newTransformer();
      t.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
      t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
      t.setOutputProperty(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "yes");
      t.transform(new DOMSource(act), new StreamResult(outFile));
      System.out.println("Generated: " + expPath);
    } catch (Exception e) {
      System.err.println("ERROR generating " + expPath + ": " + e.getMessage());
    }
  }

  private static void generateMarkdownFromAst(TransformerFactory tf, String astPath, String mdPath) throws Exception {
    File astFile = new File("src/test/resources/" + astPath);
    if (!astFile.exists()) {
      System.err.println("SKIP (no AST): " + astPath);
      return;
    }
    try (InputStream style = GenerateExpected.class.getResourceAsStream("/ast.xsl")) {
      File outFile = new File("src/test/resources/" + mdPath);
      outFile.getParentFile().mkdirs();
      Transformer t = tf.newTransformer(new StreamSource(style, "classpath:///ast.xsl"));
      t.transform(new StreamSource(astFile), new StreamResult(outFile));
      System.out.println("Generated MD: " + mdPath);
    } catch (Exception e) {
      System.err.println("ERROR generating MD " + mdPath + ": " + e.getMessage());
    }
  }

  private static void generateAst(TransformerFactory tf, DocumentBuilder db, String ditaPath, String astPath)
    throws Exception {
    File ditaFile = new File("src/test/resources/" + ditaPath);
    if (!ditaFile.exists()) {
      System.err.println("SKIP (no DITA): " + ditaPath);
      return;
    }
    try (InputStream style = GenerateExpected.class.getResourceAsStream("/dita2ast.xsl")) {
      Document output = db.newDocument();
      Transformer t = tf.newTransformer(new StreamSource(style, "classpath:///dita2ast.xsl"));
      t.transform(new StreamSource(ditaFile), new DOMResult(output));

      File outFile = new File("src/test/resources/" + astPath);
      outFile.getParentFile().mkdirs();
      t = tf.newTransformer();
      t.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
      t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
      t.setOutputProperty(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "yes");
      t.transform(new DOMSource(output), new StreamResult(outFile));
      System.out.println("Generated AST: " + astPath);
    } catch (Exception e) {
      System.err.println("ERROR generating AST " + astPath + ": " + e.getMessage());
    }
  }
}
