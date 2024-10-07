package dev.flikas.spring.boot.assistant.idea.plugin.misc;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiUtil;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PsiElementUtils {
  public static boolean isInFileOfType(PsiElement element, FileType fileType) {
    VirtualFile virtualFile = PsiUtil.getVirtualFile(element);
    if (virtualFile == null) {
      return false;
    }
    FileTypeManager ftm = FileTypeManager.getInstance();
    return ftm.isFileOfType(virtualFile, fileType);
  }
}
