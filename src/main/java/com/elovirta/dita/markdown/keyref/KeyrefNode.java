package com.elovirta.dita.markdown.keyref;

import com.vladsch.flexmark.util.ast.DoNotDecorate;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import org.jetbrains.annotations.NotNull;

public class KeyrefNode extends Node implements DoNotDecorate {

  private String key;

  public KeyrefNode() {}

  public KeyrefNode(BasedSequence chars, String key) {
    super(chars);
    this.key = key;
  }

  public String getKey() {
    return key;
  }

  @NotNull
  @Override
  public BasedSequence[] getSegments() {
    return EMPTY_SEGMENTS;
  }
}
