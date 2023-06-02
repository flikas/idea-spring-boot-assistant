package dev.flikas.spring.boot.assistant.idea.plugin.metadata.index;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiType;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.source.ConfigurationMetadata;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class ClassMetadataIndex extends MetadataIndexBase {

  public ClassMetadataIndex(Project project, ConfigurationMetadata metadata) {
    super(project);
  }

}
