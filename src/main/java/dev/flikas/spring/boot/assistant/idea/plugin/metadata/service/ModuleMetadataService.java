package dev.flikas.spring.boot.assistant.idea.plugin.metadata.service;

import dev.flikas.spring.boot.assistant.idea.plugin.metadata.index.MetadataIndex;

public interface ModuleMetadataService {
  /**
   * @return Merged spring configuration metadata in this module and its libraries, or {@linkplain MetadataIndex#EMPTY EMPTY}.
   */
  MetadataIndex getIndex();
}
