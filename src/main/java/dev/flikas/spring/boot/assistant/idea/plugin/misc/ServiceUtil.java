package dev.flikas.spring.boot.assistant.idea.plugin.misc;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import dev.flikas.spring.boot.assistant.idea.plugin.filetype.YamlPropertiesFileType;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

@UtilityClass
public class ServiceUtil {
  @Nullable
  public static <T> T getServiceFromEligibleFile(PsiFile file, FileType requiredFileType, Class<T> serviceClass) {
    VirtualFile virtualFile = file.getVirtualFile();
    if (virtualFile == null) {
      return null;
    }
    FileTypeManager ftm = FileTypeManager.getInstance();
    if (!ftm.isFileOfType(virtualFile, YamlPropertiesFileType.INSTANCE)) {
      return null;
    }
    Module module = ModuleUtil.findModuleForFile(file);
    if (module == null) {
      return null;
    }
    return module.getService(serviceClass);
  }
}
