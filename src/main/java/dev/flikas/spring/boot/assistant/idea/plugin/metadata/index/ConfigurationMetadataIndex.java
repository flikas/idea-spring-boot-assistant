package dev.flikas.spring.boot.assistant.idea.plugin.metadata.index;

import com.intellij.openapi.project.Project;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.source.ConfigurationMetadata;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * An index created from a {@link ConfigurationMetadata}
 */
@SuppressWarnings("LombokGetterMayBeUsed")
public class ConfigurationMetadataIndex extends MetadataIndexBase {
  @Getter
  private final String source;


  public ConfigurationMetadataIndex(
      @NotNull Project project, @NotNull String source, @NotNull ConfigurationMetadata metadata
  ) {
    super(project);
    this.source = source;
    add(source, metadata);
  }
}
