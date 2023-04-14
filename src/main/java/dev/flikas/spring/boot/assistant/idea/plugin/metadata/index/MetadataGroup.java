package dev.flikas.spring.boot.assistant.idea.plugin.metadata.index;

import com.intellij.psi.PsiMethod;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.source.ConfigurationMetadata.Group;
import org.jetbrains.annotations.Nullable;

public interface MetadataGroup extends MetadataItem {
  @Nullable PsiMethod getSourceMethod();

  Group getMetadata();
}
