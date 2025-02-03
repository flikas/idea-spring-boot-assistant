package dev.flikas.spring.boot.assistant.idea.plugin.metadata.service;

import com.intellij.openapi.compiler.CompilationStatusListener;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.task.ModuleBuildTask;
import com.intellij.task.ProjectTaskListener;
import com.intellij.task.ProjectTaskManager;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.source.MetadataFileIndex;
import dev.flikas.spring.boot.assistant.idea.plugin.misc.ModuleRootUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@SuppressWarnings("UnstableApiUsage")
class CompilationListener implements CompilationStatusListener, ProjectTaskListener {
  private final Project project;


  CompilationListener(Project project) {
    this.project = project;
  }


  /**
   * For Maven project only
   * <p>
   * {@inheritDoc}
   */
  @Override
  public void compilationFinished(boolean aborted, int errors, int warnings, @NotNull CompileContext compileContext) {
    enqueueBackgroundReloadTask(List.of(compileContext.getCompileScope().getAffectedModules()));
  }


  /**
   * For gradle delegated build
   * <p>
   * {@inheritDoc}
   */
  @Override
  public void finished(@NotNull ProjectTaskManager.Result result) {
    if (result.isAborted()) return;
    enqueueBackgroundReloadTask(getAffectedModules(result));
  }


  private Set<Module> getAffectedModules(ProjectTaskManager.Result result) {
    Set<Module> modules = new HashSet<>();
    result.anyTaskMatches((task, state) -> {
      if (task instanceof ModuleBuildTask mbt && !state.isFailed() && !state.isSkipped()) {
        modules.add(mbt.getModule());
        return true;
      } else {
        return false;
      }
    });
    return modules;
  }


  private void enqueueBackgroundReloadTask(Collection<Module> affectedModules) {
    //The index recreates too late, we have to find the generated metadata files without the index.
    new Task.Backgroundable(project, "Reloading spring configuration metadata") {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        indicator.pushState();
        indicator.setIndeterminate(false);
        List<VirtualFile> affectedClassRoots = new ArrayList<>();
        double i = 0;
        for (Module module : affectedModules) {
          assert module.getProject().equals(project);   // The 2 topics we are listening are all project-level.
          indicator.setText2(module.getName());
          indicator.setFraction(i++ / affectedModules.size());
          affectedClassRoots.addAll(Arrays.asList(ModuleRootUtils.getClassRootsWithoutLibraries(module)));
        }
        indicator.popState();
        List<VirtualFile> newMetaFiles = affectedClassRoots.stream()
            .map(MetadataFileIndex::findMetaFileInClassRoot)
            .filter(Objects::nonNull)
            .toList();
        if (newMetaFiles.isEmpty()) return;
        // Looks like the IDE won't reload the generated metadata file automatically,
        // so we have to refresh it for use by IndexFromOneFile#reSync
        newMetaFiles.forEach(vf -> vf.refresh(true, false));
        i = 0;
        for (Module module : affectedModules) {
          indicator.setText2(module.getName());
          indicator.setFraction(i++ / affectedModules.size());
          refreshModuleAndDependencies(List.of(module), newMetaFiles);
        }
      }
    }.queue();
  }


  private void refreshModuleAndDependencies(
      Iterable<Module> modules, Collection<VirtualFile> additionalMetaFiles) {
    for (Module module : modules) {
      ModuleMetadataServiceImpl mms = (ModuleMetadataServiceImpl) module.getServiceIfCreated(
          ModuleMetadataService.class);
      if (mms != null) {
        mms.refreshMetadata(additionalMetaFiles);
      }
      refreshModuleAndDependencies(ModuleManager.getInstance(project).getModuleDependentModules(module),
          additionalMetaFiles);
    }
  }
}
