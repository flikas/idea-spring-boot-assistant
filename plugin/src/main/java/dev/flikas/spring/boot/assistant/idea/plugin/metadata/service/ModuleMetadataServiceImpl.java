package dev.flikas.spring.boot.assistant.idea.plugin.metadata.service;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.index.AggregatedMetadataIndex;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.index.MetadataIndex;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dev.flikas.spring.boot.assistant.idea.plugin.metadata.service.ProjectMetadataService.ADDITIONAL_METADATA_FILE_NAME;
import static dev.flikas.spring.boot.assistant.idea.plugin.metadata.service.ProjectMetadataService.METADATA_FILE_NAME;

final class ModuleMetadataServiceImpl implements ModuleMetadataService {
  private static final Logger LOG = Logger.getInstance(ModuleMetadataServiceImpl.class);
  private final Project project;
  private final Module module;
  private MetadataIndex index;
  private Set<String> classRootsUrlSnapshot = new HashSet<>();


  public ModuleMetadataServiceImpl(Module module) {
    this.module = module;
    this.project = module.getProject();
    this.index = this.project.getService(ProjectMetadataService.class).getEmptyIndex();
    // read metadata for the first time
    refreshMetadata();
  }


  @Override
  public @NotNull MetadataIndex getIndex() {
    return index;
  }


  synchronized void refreshMetadata() {
    LOG.info("Try refreshing metadata for module " + this.module.getName());
    @NotNull ProjectFileIndex pfi = ProjectFileIndex.getInstance(project);
    @NotNull GlobalSearchScope scope = module.getModuleWithDependenciesAndLibrariesScope(false);
    VirtualFile[] roots = DumbService.getInstance(project).runReadActionInSmartMode(() -> Stream.concat(
            FilenameIndex.getVirtualFilesByName(METADATA_FILE_NAME, scope).stream(),
            FilenameIndex.getVirtualFilesByName(ADDITIONAL_METADATA_FILE_NAME, scope).stream())
        .map(pfi::getClassRootForFile)
        .distinct()
        .filter(Objects::nonNull)
        .toArray(VirtualFile[]::new));
    Set<String> classRootsUrl = Arrays.stream(roots).map(VirtualFile::getUrl).collect(Collectors.toSet());

    if (classRootsUrlSnapshot.equals(classRootsUrl)) {
      // No dependency changed, no need to refresh metadata.
      return;
    }
    LOG.info("Module \"" + this.module.getName() + "\"'s metadata needs refresh");
    LOG.info("Class root candidates: " + Arrays.toString(roots));
    ProjectMetadataService pms = project.getService(ProjectMetadataService.class);
    AggregatedMetadataIndex meta = new AggregatedMetadataIndex();
    for (VirtualFile root : roots) {
      pms.getIndexForClassRoot(root)
          .filter(p -> !Objects.requireNonNull(p.dereference()).isEmpty())
          .ifPresent(meta::addLast);
    }
    if (!meta.isEmpty()) {
      this.index = meta;
      this.classRootsUrlSnapshot = classRootsUrl;
    }
  }
}
