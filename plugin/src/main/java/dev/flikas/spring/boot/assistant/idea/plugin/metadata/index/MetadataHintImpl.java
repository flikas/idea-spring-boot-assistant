package dev.flikas.spring.boot.assistant.idea.plugin.metadata.index;

import com.intellij.openapi.diagnostic.Logger;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.index.hint.provider.HandleAsValueProvider;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.index.hint.provider.ValueProvider;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.index.hint.value.ValueHint;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.source.ConfigurationMetadata;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@EqualsAndHashCode(of = "metadata")
@ToString(of = "metadata")
@Getter
class MetadataHintImpl implements MetadataHint {
  private static final Logger LOG = Logger.getInstance(HandleAsValueProvider.class);
  private final ConfigurationMetadata.Hint metadata;
  private final List<ValueHint> values;
  private final List<ValueProvider> providers;


  public MetadataHintImpl(ConfigurationMetadata.Hint metadata) {
    this.metadata = metadata;

    ConfigurationMetadata.Hint.ValueHint[] values = metadata.getValues();
    if (values == null) {
      this.values = List.of();
    } else {
      this.values = Stream.of(values).map(ValueHint::new).toList();
    }

    ConfigurationMetadata.Hint.ValueProvider[] providers = metadata.getProviders();
    if (providers == null) {
      this.providers = List.of();
    } else {
      this.providers = Stream.of(providers).map(m -> {
        try {
          return ValueProvider.create(m);
        } catch (Exception e) {
          LOG.warn("Invalid hint configuration, ignored: " + metadata.getName(), e);
          return null;
        }
      }).filter(Objects::nonNull).toList();
    }
  }
}
