package dev.flikas.spring.boot.assistant.idea.plugin.metadata.index;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

public interface MetadataIndex {
  MetadataIndex EMPTY = new MetadataIndex() {
    //region empty implements
    @Override
    public boolean isEmpty() {
      return true;
    }


    @Override
    public @Nullable MetadataGroup getGroup(String name) {
      return null;
    }


    @Override
    public Collection<MetadataGroup> getGroups() {
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
    public Collection<MetadataProperty> getProperties() {
      return Collections.emptyList();
    }


    @Override
    public MetadataHint getHint(String name) {
      return null;
    }


    @Override
    public Collection<MetadataHint> getHints() {
      return Collections.emptyList();
    }


    @Override
    public MetadataItem getPropertyOrGroup(String name) {
      return null;
    }
    //endregion
  };

  boolean isEmpty();

  @Nullable MetadataGroup getGroup(String name);

  Collection<MetadataGroup> getGroups();

  @Nullable
  MetadataProperty getProperty(String name);

  @Nullable
  MetadataProperty getNearestParentProperty(String name);

  Collection<MetadataProperty> getProperties();

  @Nullable
  MetadataHint getHint(String name);

  Collection<MetadataHint> getHints();

  @Nullable
  MetadataItem getPropertyOrGroup(String name);
}
