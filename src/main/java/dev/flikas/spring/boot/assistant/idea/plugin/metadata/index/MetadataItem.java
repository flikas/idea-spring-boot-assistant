package dev.flikas.spring.boot.assistant.idea.plugin.metadata.index;

import com.intellij.psi.PsiClass;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.source.ConfigurationMetadata;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A spring configuration metadata property or group
 */
public interface MetadataItem {
  /**
   * @see ConfigurationMetadata.Property#getName()
   * @see ConfigurationMetadata.Group#getName()
   */
  @NotNull String getName();

  /**
   * @see ConfigurationMetadata.Property#getType()
   * @see ConfigurationMetadata.Group#getType()
   */
  @Nullable PsiClass getType();

  /**
   * @see ConfigurationMetadata.Property#getSourceType()
   * @see ConfigurationMetadata.Group#getSourceType()
   */
  @Nullable PsiClass getSourceType();
}
