package dev.flikas.spring.boot.assistant.idea.plugin.misc;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ModuleRootUtils {
  public static VirtualFile[] getClassRootsWithoutLibrariesRecursively(Module module) {
    return ModuleRootManager.getInstance(module)
        .orderEntries().recursively().withoutLibraries().withoutSdk().productionOnly().getClassesRoots();
  }


  public static VirtualFile[] getClassRootsRecursively(Module module) {
    return ModuleRootManager.getInstance(module).orderEntries()
        .recursively().withoutSdk().productionOnly().getClassesRoots();
  }


  public static VirtualFile[] getClassRootsWithoutLibraries(Module module) {
    // We must use OrderEnumerator but not CompilerModuleExtension,
    // because Gradle uses OrderEnumerationHandler to add custom roots.
    return ModuleRootManager.getInstance(module).orderEntries()
        .withoutSdk().withoutLibraries().productionOnly().getClassesRoots();
  }
}
