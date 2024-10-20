package dev.flikas.spring.boot.assistant.idea.plugin.metadata.index;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

public interface MetadataIndex {
  static MetadataIndex empty(Project project) {
    return new MetadataIndex() {
      //region empty implements
      @Override
      public boolean isEmpty() {
        return true;
      }


      @Override
      public @NotNull Project getProject() {
        return project;
      }


      @Override
      public @NotNull String getSource() {
        return "";
      }


      @Override
      public @Nullable MetadataGroup getGroup(String name) {
        return null;
      }


      @Override
      public @NotNull Collection<MetadataGroup> getGroups() {
        return Collections.emptyList();
      }


      @Override
      public MetadataProperty getProperty(String name) {
        return null;
      }


      @Override
      public MetadataProperty getNearestParentProperty(String name) {
        return null;
      }


      @Override
      public @NotNull Collection<MetadataProperty> getProperties() {
        return Collections.emptyList();
      }


      @Override
      public MetadataHint getHint(String name) {
        return null;
      }


      @Override
      public @NotNull Collection<MetadataHint> getHints() {
        return Collections.emptyList();
      }


      @Override
      public MetadataItem getPropertyOrGroup(String name) {
        return null;
      }
      //endregion
    };
  }

  boolean isEmpty();

  @NotNull Project getProject();

  /**
   * Source file url or source type FQN, maybe empty string.
   */
  @NotNull String getSource();

  @NotNull Collection<MetadataGroup> getGroups();

  @NotNull Collection<MetadataProperty> getProperties();

  @NotNull Collection<MetadataHint> getHints();

  @Nullable MetadataGroup getGroup(String name);

  @Nullable MetadataProperty getProperty(String name);

  @Nullable MetadataProperty getNearestParentProperty(String name);

  @Nullable MetadataHint getHint(String name);

  @Nullable MetadataItem getPropertyOrGroup(String name);
}
