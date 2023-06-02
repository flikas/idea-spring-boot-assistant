package dev.flikas.spring.boot.assistant.idea.plugin.metadata.service;

import com.google.gson.GsonBuilder;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.psi.PsiType;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.index.*;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.source.ConfigurationMetadata;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.source.PropertyTypeUtil;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public final class ProjectMetadataService {
  public static final String METADATA_FILE = "META-INF/spring-configuration-metadata.json";
  public static final String ADDITIONAL_METADATA_FILE = "META-INF/additional-spring-configuration-metadata.json";

  private final Logger log = Logger.getInstance(ProjectMetadataService.class);

  private final Project project;

  private final ConcurrentMap<String, MetadataFileRoot> metadataFiles = new ConcurrentHashMap<>();


  public ProjectMetadataService(Project project) {
    this.project = project;
    project.getMessageBus().connect().subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
      @Override
      public void after(@NotNull List<? extends VFileEvent> events) {
        List<MetadataFileRoot> toReload = new ArrayList<>();
        for (VFileEvent event : events) {
          if (event.getFile() == null) continue;
          for (MetadataFileRoot fileRoot : metadataFiles.values()) {
            if (!VfsUtilCore.isAncestor(fileRoot.root, event.getFile(), true)) continue;
            String relativePath = VfsUtilCore.getRelativePath(event.getFile(), fileRoot.root);
            if (METADATA_FILE.equals(relativePath) || ADDITIONAL_METADATA_FILE.equals(relativePath)) {
              toReload.add(fileRoot);
            }
          }
        }
        toReload.forEach(MetadataFileRoot::reload);
      }
    });
  }


  public MetadataIndex getMetadata(VirtualFile root) {
    return metadataFiles
        .computeIfAbsent(root.getUrl(), url -> new MetadataFileRoot(root))
        .getMetadata();
  }


  @Data
  private class MetadataFileRoot {
    private final VirtualFile root;
    private MetadataIndex metadata;


    public MetadataFileRoot(VirtualFile root) {
      this.root = root;
      reload();
    }


    public void reload() {
      FileMetadataIndex fmi = findMetadata(root, METADATA_FILE);
      if (fmi == null) {
        // Some package has additional metadata file only, so we have to load it,
        // otherwise, spring-configuration-processor should merge additional metadata to the main one,
        // thus, the additional metadata file should not be load.
        fmi = findMetadata(root, ADDITIONAL_METADATA_FILE);
      }
      if (fmi != null) {
        AggregatedMetadataIndex index = new AggregatedMetadataIndex(Collections.singleton(fmi));
        // Spring does not create metadata for types in collections, we should create it by ourselves and expand our index,
        // to better support code-completion, documentation, navigation, etc.
        for (MetadataProperty property : this.metadata.getProperties()) {
          resolvePropertyType(property).ifPresent(index::addFirst);
        }
        this.metadata = index;
      }
    }


    /**
     * @see ConfigurationMetadata.Property#getType()
     */
    @NotNull
    private Optional<MetadataIndex> resolvePropertyType(MetadataProperty property) {
      return Optional.ofNullable(property)
          .map(MetadataProperty::getFullType)
          .filter(PsiType::isValid)
          .filter(t -> PropertyTypeUtil.isCollection(project, t))
          .flatMap(t -> project.getService(ProjectClassMetadataService.class).getMetadata(t))
          .map(idx -> new MetadataIndexBaseNameWrapper(property.getName(), idx));
    }


    @Nullable
    private FileMetadataIndex findMetadata(VirtualFile root, String metaFile) {
      VirtualFile file = VfsUtilCore.findRelativeFile(metaFile, root);
      if (file != null) {
        try {
          return new FileMetadataIndex(project, file.getUrl(), readJson(file));
        } catch (IOException e) {
          log.warn("Read metadata file " + file + " failed", e);
        }
      }
      return null;
    }


    private ConfigurationMetadata readJson(VirtualFile file) throws IOException {
      return ReadAction.compute(() -> {
        try (Reader reader = new InputStreamReader(file.getInputStream(), file.getCharset())) {
          return new GsonBuilder()
              .create()
              .fromJson(reader, ConfigurationMetadata.class);
        }
      });
    }
  }
}
