package dev.flikas.spring.boot.assistant.idea.plugin.metadata.service;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import com.intellij.openapi.vfs.VirtualFile;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.source.MetadataFileIndex;
import dev.flikas.spring.boot.assistant.idea.plugin.misc.ModuleRootUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class ModuleDependenciesWatcher implements ModuleRootListener {
  private final Project project;


  public ModuleDependenciesWatcher(Project project) {
    this.project = project;
  }


  @Override
  public void rootsChanged(@NotNull ModuleRootEvent event) {
    // Have to retrieve metadata files without index, because the update of the index has a lag.
    //noinspection DialogTitleCapitalization
    new Task.Backgroundable(project, "Loading Spring Boot metadata") {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        refreshAllWithoutIndex(indicator);
      }
    }.queue();
  }


  private void refreshAllWithoutIndex(ProgressIndicator indicator) {
    for (Module module : ModuleManager.getInstance(project).getModules()) {
      @Nullable ModuleMetadataService svc = module.getServiceIfCreated(ModuleMetadataService.class);
      if (svc instanceof ModuleMetadataServiceImpl impl) {  // also filtered out null
        indicator.setText2(module.getName());
        impl.refreshMetadata(getMetaFiles(module));
      }
    }
  }


  private Collection<VirtualFile> getMetaFiles(Module module) {
    return Stream.of(ModuleRootUtils.getClassRootsRecursively(module))
        .map(MetadataFileIndex::findMetaFileInClassRoot)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }
}
