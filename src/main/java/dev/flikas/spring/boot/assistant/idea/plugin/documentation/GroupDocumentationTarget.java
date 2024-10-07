package dev.flikas.spring.boot.assistant.idea.plugin.documentation;

import com.intellij.model.Pointer;
import com.intellij.platform.backend.documentation.DocumentationTarget;
import com.intellij.platform.backend.presentation.TargetPresentation;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.index.MetadataGroup;
import org.jetbrains.annotations.NotNull;

public class GroupDocumentationTarget implements DocumentationTarget {
  private final MetadataGroup group;


  public GroupDocumentationTarget(MetadataGroup group) {
    this.group = group;
  }


  @Override
  public @NotNull Pointer<? extends DocumentationTarget> createPointer() {
    return null;
  }


  @Override
  public @NotNull TargetPresentation computePresentation() {
    return null;
  }
}
