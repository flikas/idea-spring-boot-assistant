package dev.flikas.spring.boot.assistant.idea.plugin.metadata.index;

import dev.flikas.spring.boot.assistant.idea.plugin.metadata.source.PropertyName;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class MetadataIndexBaseNameWrapper implements MetadataIndex {
  private final MetadataIndex index;
  private final PropertyName basename;


  public MetadataIndexBaseNameWrapper(String basename, MetadataIndex index) {
    this.index = index;
    this.basename = PropertyName.of(basename);
  }


  @Override
  public boolean isEmpty() {
    return false;
  }


  @Override
  public @Nullable MetadataGroup getGroup(String name) {
    return null;
  }


  @Override
  public Collection<MetadataGroup> getGroups() {
    return null;
  }


  @Override
  public @Nullable MetadataProperty getProperty(String name) {
    return null;
  }


  @Override
  public @Nullable MetadataProperty getNearestParentProperty(String name) {
    return null;
  }


  @Override
  public Collection<MetadataProperty> getProperties() {
    return null;
  }


  @Override
  public @Nullable MetadataHint getHint(String name) {
    return null;
  }


  @Override
  public Collection<MetadataHint> getHints() {
    return null;
  }


  @Override
  public @Nullable MetadataItem getPropertyOrGroup(String name) {
    return null;
  }
}
