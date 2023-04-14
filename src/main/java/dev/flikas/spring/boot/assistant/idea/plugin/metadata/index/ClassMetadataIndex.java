package dev.flikas.spring.boot.assistant.idea.plugin.metadata.index;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class ClassMetadataIndex extends MetadataIndexBase {


  private ClassMetadataIndex(Project project) {
    super(project);
  }


  @NotNull
  public static Map<String, ClassMetadataIndex> fromType(Project project, PsiType type) {
    return null;
  }


}
