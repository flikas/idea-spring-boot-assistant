package dev.flikas.spring.boot.assistant.idea.plugin.metadata.service;

import com.intellij.ProjectTopics;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.index.FileMetadataIndex;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.index.MetadataIndex;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
class ModuleMetadataServiceImpl implements ModuleMetadataService {
  private final Module module;
  private MetadataIndex index = MetadataIndex.EMPTY;
  private Set<String> classRootsUrlSnapshot = new HashSet<>();


  public ModuleMetadataServiceImpl(Module module) {
    this.module = module;
    // add change listener
    // TODO WorkspaceModuleChangeListener is better, but it is in experimental for now.
    module.getProject().getMessageBus().connect().subscribe(ProjectTopics.PROJECT_ROOTS, new ModuleRootListener() {
      @Override
      public void rootsChanged(@NotNull ModuleRootEvent event) {
        ReadAction.run(ModuleMetadataServiceImpl.this::refreshMetadata);
      }
    });
    // read metadata for the first time
    ReadAction.run(this::refreshMetadata);
  }


  @Override
  public MetadataIndex getIndex() {
    return index;
  }


  private synchronized void refreshMetadata() {
    VirtualFile[] roots = ModuleRootManager.getInstance(this.module).orderEntries().withoutSdk().classes().getRoots();
    Set<String> classRootsUrl = Arrays.stream(roots).map(VirtualFile::getUrl).collect(Collectors.toSet());
    if (classRootsUrlSnapshot.equals(classRootsUrl)) {
      // No dependency changed, no need to refresh metadata.
      return;
    }
    Project project = this.module.getProject();
    ProjectMetadataService pms = project.getService(ProjectMetadataService.class);
    FileMetadataIndex meta = new FileMetadataIndex(project);
    for (VirtualFile root : roots) {
      meta.merge(pms.getMetadata(root));
    }
    if (!meta.isEmpty()) {
      this.index = meta;
      this.classRootsUrlSnapshot = classRootsUrl;
    }
  }
}
