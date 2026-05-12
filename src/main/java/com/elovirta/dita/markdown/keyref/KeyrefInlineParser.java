package com.elovirta.dita.markdown.keyref;

import com.vladsch.flexmark.parser.InlineParser;
import com.vladsch.flexmark.parser.InlineParserExtension;
import com.vladsch.flexmark.parser.InlineParserExtensionFactory;
import com.vladsch.flexmark.parser.LightInlineParser;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import java.util.Set;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class KeyrefInlineParser implements InlineParserExtension {

  private static final Pattern KEYREF_PATTERN = Pattern.compile(
    "\\{\\{([a-zA-Z0-9_.\\-]+)\\}\\}"
  );

  public KeyrefInlineParser(LightInlineParser inlineParser) {}

  @Override
  public void finalizeDocument(@NotNull InlineParser inlineParser) {}

  @Override
  public void finalizeBlock(@NotNull InlineParser inlineParser) {}

  @Override
  public boolean parse(@NotNull LightInlineParser inlineParser) {
    BasedSequence input = inlineParser.getInput();
    int index = inlineParser.getIndex();

    if (index + 1 >= input.length() || input.charAt(index + 1) != '{') {
      return false;
    }

    int closeStart = input.indexOf("}}", index + 2);
    if (closeStart < 0) {
      return false;
    }

    BasedSequence keySeq = input.subSequence(index + 2, closeStart);
    String key = keySeq.toString();

    if (key.isEmpty() || !KEYREF_PATTERN.matcher("{{" + key + "}}").matches()) {
      return false;
    }

    BasedSequence fullSeq = input.subSequence(index, closeStart + 2);
    KeyrefNode node = new KeyrefNode(fullSeq, key);
    inlineParser.flushTextNode();
    inlineParser.getBlock().appendChild(node);
    inlineParser.setIndex(closeStart + 2);
    return true;
  }

  public static class Factory implements InlineParserExtensionFactory {

    @Nullable
    @Override
    public Set<Class<?>> getAfterDependents() {
      return null;
    }

    @NotNull
    @Override
    public CharSequence getCharacters() {
      return "{";
    }

    @Nullable
    @Override
    public Set<Class<?>> getBeforeDependents() {
      return null;
    }

    @Override
    public boolean affectsGlobalScope() {
      return false;
    }

    @NotNull
    @Override
    public InlineParserExtension apply(@NotNull LightInlineParser lightInlineParser) {
      return new KeyrefInlineParser(lightInlineParser);
    }
  }
}
