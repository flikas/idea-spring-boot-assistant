package dev.flikas.spring.boot.assistant.idea.plugin.metadata.index;

import dev.flikas.spring.boot.assistant.idea.plugin.metadata.index.hint.provider.ValueProvider;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.index.hint.value.ValueHint;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.source.ConfigurationMetadata.Hint;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface MetadataHint {
  Hint getMetadata();

  @NotNull
  List<ValueHint> getValues();

  @NotNull
  List<ValueProvider> getProviders();
}
