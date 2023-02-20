package dev.flikas.spring.boot.assistant.idea.plugin.metadata.service;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.index.AggregatedMetadataIndex;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.index.FileMetadataSource;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.index.MetadataIndex;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.source.MetadataFileIndex;
import dev.flikas.spring.boot.assistant.idea.plugin.misc.ModuleRootUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

final class ModuleMetadataServiceImpl implements ModuleMetadataService {
  private static final Logger LOG = Logger.getInstance(ModuleMetadataServiceImpl.class);
  private final Project project;
  private final Module module;
  private MetadataIndex index;


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
    refreshMetadata(Collections.emptySet());
  }


  synchronized void refreshMetadata(Collection<VirtualFile> unIndexedMetaFiles) {
    LOG.trace("Try refreshing metadata for module " + this.module.getName());
    @NotNull GlobalSearchScope scope = new ModuleScope(this.module);
    Collection<VirtualFile> files = DumbService.getInstance(project).runReadActionInSmartMode(() -> {
      HashSet<VirtualFile> metafiles = new HashSet<>(MetadataFileIndex.getFiles(scope));
      for (VirtualFile metafile : unIndexedMetaFiles) {
        if (scope.accept(metafile)) metafiles.add(metafile);
      }
      return metafiles;
    });
    if (this.index instanceof AggregatedMetadataIndex aidx) {
      aidx.refresh();
    }
    Set<String> currentFiles = this.index.getSource().stream()
        .filter(FileMetadataSource.class::isInstance)
        .map(s -> ((FileMetadataSource) s).getSource().getUrl())
        .collect(Collectors.toSet());
    if (currentFiles.containsAll(files.stream().map(VirtualFile::getUrl).collect(Collectors.toSet()))) {
      // No new metadata files, can stop here.
      return;
    }
    // Because the MetadataFileIndex may lag of the creation of new metafiles,
    // we only accept new metafiles from the index (but won't remove files even if the index doesn't contain it),
    // the removal of the non-exists ones is done by AggregatedMetadataIndex#refresh()
    files.removeIf(vf -> currentFiles.contains(vf.getUrl()));
    LOG.info("Module \"" + this.module.getName() + "\"'s metadata needs refresh");
    LOG.info("New metadata files: " + files);
    ProjectMetadataService pms = project.getService(ProjectMetadataService.class);
    AggregatedMetadataIndex meta = this.index instanceof AggregatedMetadataIndex
        ? (AggregatedMetadataIndex) this.index
        : new AggregatedMetadataIndex();
    for (VirtualFile file : files) {
      meta.addLast(pms.getIndexForMetaFile(file));
    }
    if (!meta.isEmpty()) {
      this.index = meta;
    }
  }


  /**
   * Imitate the {@link com.intellij.openapi.module.impl.scopes.ModuleWithDependenciesScope}
   * <p>
   * Because the SDK provides scopes do not contain the compile output path.
   */
  private static class ModuleScope extends GlobalSearchScope {
    private final ProjectFileIndex projectFileIndex;
    private final Object2IntMap<VirtualFile> libraryRoots;
    private final VirtualFile[] moduleRoots;
    private final Set<Module> modulesInScope;


    private ModuleScope(Module module) {
      super(module.getProject());
      this.projectFileIndex = ProjectFileIndex.getInstance(module.getProject());
      VirtualFile[] roots = ModuleRootUtils.getClassRootsRecursively(module);
      Object2IntOpenHashMap<VirtualFile> map = new Object2IntOpenHashMap<>(roots.length);
      int i = 0;
      for (VirtualFile root : roots) {
        map.put(root, i++);
      }
      this.libraryRoots = map;
      Set<Module> modules = new HashSet<>();
      ModuleUtil.getDependencies(module, modules);
      this.modulesInScope = modules;
      this.moduleRoots = ModuleRootUtils.getClassRootsWithoutLibrariesRecursively(module);
    }


    @Override
    public boolean isSearchInModuleContent(@NotNull Module aModule) {
      return this.modulesInScope.contains(aModule);
    }


    @Override
    public boolean isSearchInModuleContent(@NotNull Module aModule, boolean testSources) {
      return !testSources && isSearchInModuleContent(aModule);
    }


    @Override
    public boolean isSearchInLibraries() {
      return true;
    }


    @Override
    public boolean contains(@NotNull VirtualFile file) {
      // The ProjectFileIndex cannot find class root for a file in the module's compile output path.
      // And the WorkspaceFileIndex is still in experimental.
      // That's why we are doing like this:

      // File is contained in this scope if and only if:
      // - File's class root can be found, and it is in the scope. (files in libraries can be found class root)
      VirtualFile root = projectFileIndex.getClassRootForFile(file);
      if (root != null) {
        return libraryRoots.containsKey(root);
      }
      // - File's class root cannot be found, but the module which belongs is in the scope.
      Module module = projectFileIndex.getModuleForFile(file, false);
      if (module != null && this.modulesInScope.contains(module)) {
        return true;
      }
      // - Or file is in the module's class roots without libraries.
      //   This is for gradle projects, for its special module structure.
      for (VirtualFile moduleRoot : this.moduleRoots) {
        if (VfsUtilCore.isAncestor(moduleRoot, file, false)) {
          return true;
        }
      }
      return false;
    }
  }
}
