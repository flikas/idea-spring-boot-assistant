package dev.flikas.spring.boot.assistant.idea.plugin.metadata.index;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.source.ConfigurationMetadata;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

public class FileMetadataIndex extends MetadataIndexBase {
  private static final Logger log = Logger.getInstance(FileMetadataIndex.class);
  @Getter
  private final String sourceUrl;


  public FileMetadataIndex(@NotNull Project project, @NotNull String sourceUrl, @NotNull ConfigurationMetadata metadata) {
    super(project);
    this.sourceUrl = sourceUrl;
    if (metadata.isEmpty()) return;

    if (metadata.getGroups() != null) {
      metadata.getGroups().forEach(g -> {
        try {
          add(g);
        } catch (Exception e) {
          log.warn("Invalid group " + g.getName() + " in " + sourceUrl + ", skipped", e);
        }
      });
    }
    if (metadata.getHints() != null) {
      metadata.getHints().forEach(h -> {
        try {
          add(h);
        } catch (Exception e) {
          log.warn("Invalid hint " + h.getName() + " in " + sourceUrl + ", skipped", e);
        }
      });
    }
    metadata.getProperties().forEach(p -> {
      try {
        add(p);
      } catch (Exception e) {
        log.warn("Invalid property " + p.getName() + " in " + sourceUrl + ", skipped", e);
      }
    });
  }
}
