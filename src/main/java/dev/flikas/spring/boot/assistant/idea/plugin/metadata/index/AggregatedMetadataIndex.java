package dev.flikas.spring.boot.assistant.idea.plugin.metadata.index;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Deque;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;

public class AggregatedMetadataIndex implements MetadataIndex {
  private final Deque<MetadataIndex> indexes = new ConcurrentLinkedDeque<>();


  public AggregatedMetadataIndex() {
  }


  public AggregatedMetadataIndex(MetadataIndex... indexes) {
    for (MetadataIndex index : indexes) {
      addLast(index);
    }
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
  public @NotNull Project getProject() {
    return this.indexes.stream().map(MetadataIndex::getProject).reduce((p1, p2) -> {
      if (p1 == p2) {
        return p1;
      } else {
        throw new IllegalStateException("Not the same project");
      }
    }).orElseThrow();
  }


  @Override
  public @Nullable MetadataGroup getGroup(String name) {
    return this.indexes.stream().map(index -> index.getGroup(name)).filter(Objects::nonNull).findFirst().orElse(null);
  }


  @Override
  public @NotNull Collection<MetadataGroup> getGroups() {
    return this.indexes.stream().flatMap(index -> index.getGroups().stream()).toList();
  }


  @Override
  public MetadataProperty getProperty(String name) {
    return this.indexes.stream()
        .map(index -> index.getProperty(name))
        .filter(Objects::nonNull)
        .findFirst().orElse(null);
  }


  @Override
  public MetadataProperty getNearestParentProperty(String name) {
    return this.indexes.stream()
        .map(index -> index.getNearestParentProperty(name))
        .filter(Objects::nonNull)
        .findFirst().orElse(null);
  }


  @Override
  public @NotNull Collection<MetadataProperty> getProperties() {
    return this.indexes.stream().flatMap(index -> index.getProperties().stream()).toList();
  }


  @Override
  public MetadataHint getHint(String name) {
    return this.indexes.stream()
        .map(index -> index.getHint(name))
        .filter(Objects::nonNull)
        .findFirst().orElse(null);
  }


  @Override
  public @NotNull Collection<MetadataHint> getHints() {
    return this.indexes.stream().flatMap(index -> index.getHints().stream()).toList();
  }


  @Override
  public MetadataItem getPropertyOrGroup(String name) {
    return this.indexes.stream()
        .map(index -> index.getPropertyOrGroup(name))
        .filter(Objects::nonNull)
        .findFirst().orElse(null);
  }
}
