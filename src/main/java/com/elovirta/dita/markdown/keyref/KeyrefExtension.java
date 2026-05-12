package com.elovirta.dita.markdown.keyref;

import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataHolder;
import org.jetbrains.annotations.NotNull;

public class KeyrefExtension implements Parser.ParserExtension {

  private KeyrefExtension() {}

  public static KeyrefExtension create() {
    return new KeyrefExtension();
  }

  @Override
  public void parserOptions(MutableDataHolder options) {}

  @Override
  public void extend(Parser.Builder parserBuilder) {
    parserBuilder.customInlineParserExtensionFactory(new KeyrefInlineParser.Factory());
  }
}
