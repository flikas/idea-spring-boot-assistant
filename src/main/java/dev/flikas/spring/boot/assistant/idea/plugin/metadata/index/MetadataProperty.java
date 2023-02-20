package dev.flikas.spring.boot.assistant.idea.plugin.metadata.index;

import com.intellij.psi.PsiField;
import com.intellij.psi.PsiType;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.source.ConfigurationMetadata;
import org.jetbrains.annotations.Nullable;

/**
 * A spring configuration metadata property
 */
public interface MetadataProperty extends MetadataItem {
  /**
   * The PsiType include type arguments, for example, {@code Map<String, String>}.
   *
   * @see ConfigurationMetadata.Property#getType()
   */
  @Nullable PsiType getFullType();

  /**
   * @return the field that this property will be bound to, null if not present.
   */
  @Nullable PsiField getSourceField();
}
