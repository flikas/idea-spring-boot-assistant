package dev.flikas.spring.boot.assistant.idea.plugin.metadata.service;

import com.google.gson.Gson;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootModel;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.psi.PsiType;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.index.AggregatedMetadataIndex;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.index.ConfigurationMetadataIndex;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.index.MetadataIndex;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.index.MetadataProperty;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.source.ConfigurationMetadata;
import dev.flikas.spring.boot.assistant.idea.plugin.misc.PsiTypeUtils;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Service that generates {@link MetadataIndex} from one {@linkplain ModuleRootModel#getSourceRoots() SourceRoot}.
 * <p>
 * It searches and generate index from Spring Configuration Files ({@value METADATA_FILE}, {@value ADDITIONAL_METADATA_FILE})
 * in the source root and watches them for automatically update the index.
 */
@Service(Service.Level.PROJECT)
final class ProjectMetadataService {
  public static final String METADATA_FILE = "META-INF/spring-configuration-metadata.json";
  public static final String ADDITIONAL_METADATA_FILE = "META-INF/additional-spring-configuration-metadata.json";

  private final Logger log = Logger.getInstance(ProjectMetadataService.class);
  private final ThreadLocal<Gson> gson = ThreadLocal.withInitial(Gson::new);
  private final Project project;
  private final ConcurrentMap<String, MetadataFileRoot> metadataFiles = new ConcurrentHashMap<>();


  public ProjectMetadataService(Project project) {
    this.project = project;
    project.getMessageBus().connect().subscribe(VirtualFileManager.VFS_CHANGES, new FileWatcher());
  }


  @NotNull
  public MetadataIndex getMetadata(@NotNull VirtualFile sourceRoot) {
    return metadataFiles
        .computeIfAbsent(sourceRoot.getUrl(), url -> new MetadataFileRoot(sourceRoot))
        .getMetadata();
  }


  @Data
  private class MetadataFileRoot {
    private final VirtualFile root;
    private MetadataIndex metadata = MetadataIndex.EMPTY;


    MetadataFileRoot(VirtualFile root) {
      this.root = root;
      reload();
    }


    void reload() {
      @NotNull Optional<VirtualFile> metadataFile = findFile(root, METADATA_FILE);
      if (metadataFile.isEmpty()) {
        // Some package has additional metadata file only, so we have to load it,
        // otherwise, spring-configuration-processor should merge additional metadata to the main one,
        // thus, the additional metadata file should not be load.
        metadataFile = findFile(root, ADDITIONAL_METADATA_FILE);
      }
      if (metadataFile.isPresent()) {
        try {
          AggregatedMetadataIndex index = new AggregatedMetadataIndex(generateIndex(metadataFile.get()));
          // Spring does not create metadata for types in collections, we should create it by ourselves and expand our index,
          // to better support code-completion, documentation, navigation, etc.
          for (MetadataProperty property : index.getProperties()) {
            resolvePropertyType(property).ifPresent(index::addFirst);
          }
          this.metadata = index;
        } catch (IOException e) {
          log.warn("Read metadata file " + metadataFile.get().getUrl() + " failed", e);
        }
      }
    }


    /**
     * @see ConfigurationMetadata.Property#getType()
     */
    @NotNull
    private Optional<MetadataIndex> resolvePropertyType(@NotNull MetadataProperty property) {
      return property
          .getFullType()
          .filter(PsiType::isValid)
          .filter(t -> PsiTypeUtils.isCollectionOrMap(project, t))
          .flatMap(t -> project.getService(ProjectClassMetadataService.class).getMetadata(property.getName(), t));
    }


    @NotNull
    private Optional<VirtualFile> findFile(VirtualFile root, String file) {
      return Optional.ofNullable(VfsUtil.findRelativeFile(root, file.split("/")));
    }


    @NotNull
    private MetadataIndex generateIndex(VirtualFile file) throws IOException {
      ConfigurationMetadata meta = ReadAction.compute(() -> {
        try (Reader reader = new InputStreamReader(file.getInputStream(), file.getCharset())) {
          return gson.get().fromJson(reader, ConfigurationMetadata.class);
        }
      });
      return new ConfigurationMetadataIndex(project, file.getUrl(), meta);
    }
  }


  private class FileWatcher implements BulkFileListener {
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
  }
}
