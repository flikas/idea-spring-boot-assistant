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
import com.intellij.psi.util.PsiUtil;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.index.ClassMetadataIndex;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.index.FileMetadataIndex;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.index.MetadataIndex;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.index.MetadataProperty;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.source.ConfigurationMetadata;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.source.PropertyTypeUtil;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
        this.metadata = fmi;
        // Spring does not create metadata for types in collections, we should create it by ourselves and expand our index,
        // to better support code-completion, documentation, navigation, etc.
        for (MetadataProperty property : this.metadata.getProperties()) {
          resolvePropertyType(property);
        }
      }
    }


    /**
     * @see ConfigurationMetadata.Property#getType()
     */
    @Nullable
    private void resolvePropertyType(MetadataProperty property) {
      @Nullable PsiType type = property.getFullType();
      if (type == null || !type.isValid()) return;
      if (PropertyTypeUtil.isCollection(project, type)) {
        //TODO add ProjectClassMetadataService，缓存和监视类的属性
        Map<String, ClassMetadataIndex> map = ClassMetadataIndex.fromType(project, type);

        ConfigurationMetadata metadata = generateMetadata(new ConfigurationMetadata(), property.getPropertyName(), type);
      } else if (PsiUtil.resolveClassInType(type) != null && !PropertyTypeUtil.isValueType(type)) {
        log.warn(property.getName() + " has unsupported type: " + type.getCanonicalText());
      }
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
