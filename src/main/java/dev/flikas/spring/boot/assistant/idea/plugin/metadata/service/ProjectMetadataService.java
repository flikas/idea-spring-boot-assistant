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
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.ConfigurationMetadata;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.index.MetadataIndex;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

@Service
public final class ProjectMetadataService {
  public static final String METADATA_FILE = "META-INF/spring-configuration-metadata.json";
  public static final String ADDITIONAL_METADATA_FILE = "META-INF/additional-spring-configuration-metadata.json";

  private static final Logger LOG = Logger.getInstance(ProjectMetadataService.class);

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
    private Collection<VirtualFile> metadataFiles;
    private MetadataIndex metadata;


    public MetadataFileRoot(VirtualFile root) {
      this.root = root;
      reload();
    }


    public void reload() {
      MetadataIndex meta = new MetadataIndex(project);
      this.metadataFiles = Stream.of(
              findMetadata(root, METADATA_FILE, meta),
              findMetadata(root, ADDITIONAL_METADATA_FILE, meta))
          .filter(Objects::nonNull)
          .toList();
      this.metadata = meta;
    }


    private VirtualFile findMetadata(VirtualFile root, String metaFile, MetadataIndex metadata) {
      VirtualFile file = VfsUtilCore.findRelativeFile(metaFile, root);
      if (file != null) {
        try {
          metadata.merge(file.getUrl(), readJson(file));
        } catch (IOException e) {
          LOG.warn("Read metadata file " + file + " failed", e);
        }
      }
      return file;
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
