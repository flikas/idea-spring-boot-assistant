package dev.flikas.spring.boot.assistant.idea.plugin.metadata.index;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Deque;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;

public class AggregatedMetadataIndex implements MetadataIndex {
  private final Deque<MetadataIndex> indexes = new ConcurrentLinkedDeque<>();


  public AggregatedMetadataIndex() {
  }


  public AggregatedMetadataIndex(Collection<MetadataIndex> indexes) {
    this.indexes.addAll(indexes);
  }


  public void addLast(MetadataIndex index) {
    this.indexes.addLast(index);
  }


  public void addFirst(MetadataIndex index) {
    this.indexes.addFirst(index);
  }


  @Override
  public boolean isEmpty() {
    return indexes.stream().allMatch(MetadataIndex::isEmpty);
  }


  @Override
  public @Nullable MetadataGroup getGroup(String name) {
    return this.indexes.stream().map(index -> index.getGroup(name)).filter(Objects::nonNull).findFirst().orElse(null);
  }


  @Override
  public Collection<MetadataGroup> getGroups() {
    return this.indexes.stream().flatMap(index -> index.getGroups().stream()).toList();
  }


  @Override
  public MetadataProperty getProperty(String name) {
    return this.indexes.stream().map(index -> index.getProperty(name)).filter(Objects::nonNull).findFirst().orElse(null);
  }


  @Override
  public MetadataProperty getNearestParentProperty(String name) {
    return this.indexes.stream().map(index -> index.getNearestParentProperty(name)).filter(Objects::nonNull).findFirst().orElse(null);
  }


  @Override
  public Collection<MetadataProperty> getProperties() {
    return this.indexes.stream().flatMap(index -> index.getProperties().stream()).toList();
  }


  @Override
  public MetadataHint getHint(String name) {
    return this.indexes.stream().map(index -> index.getHint(name)).filter(Objects::nonNull).findFirst().orElse(null);
  }


  @Override
  public Collection<MetadataHint> getHints() {
    return this.indexes.stream().flatMap(index -> index.getHints().stream()).toList();
  }


  @Override
  public MetadataItem getPropertyOrGroup(String name) {
    return this.indexes.stream().map(index -> index.getPropertyOrGroup(name)).filter(Objects::nonNull).findFirst().orElse(null);
  }
}
