package dev.flikas.spring.boot.assistant.idea.plugin.metadata.index.hint.value;

import com.intellij.icons.AllIcons;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.index.hint.Hint;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.source.ConfigurationMetadata;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ValueHint {
  private final ConfigurationMetadata.Hint.ValueHint metadata;


  public ValueHint(ConfigurationMetadata.Hint.ValueHint metadata) {
    this.metadata = metadata;
  }


  /**
   * @see ConfigurationMetadata.Hint.ValueHint#getValue()
   */
  @NotNull
  public Object getValue() {
    return metadata.getValue();
  }


  /**
   * @see ConfigurationMetadata.Hint.ValueHint#getDescription()
   */
  @Nullable
  public String getDescription() {
    return metadata.getDescription();
  }


  public Hint toHint() {
    return new Hint(
        String.valueOf(getValue()),
        getFirstLine(getDescription()),
        getDescription(),
        AllIcons.Nodes.Field);
  }


  private String getFirstLine(@Nullable String paragraph) {
    if (paragraph == null) return null;
    int dot = paragraph.indexOf('.');
    int ls = paragraph.indexOf('\n');
    int end;
    if (dot > 0 && ls > 0) {
      end = Math.min(dot, ls);
    } else if (dot > 0) {
      end = dot;
    } else if (ls > 0) {
      end = ls;
    } else {
      return paragraph;
    }
    return paragraph.substring(0, end);
  }
}
