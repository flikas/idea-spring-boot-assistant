package dev.flikas.spring.boot.assistant.idea.plugin.metadata.index;

import com.intellij.psi.PsiMethod;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.source.ConfigurationMetadata.Group;

import java.util.Optional;

public interface MetadataGroup extends MetadataItem {
  Optional<PsiMethod> getSourceMethod();

  Group getMetadata();
}
