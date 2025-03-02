package dev.flikas.spring.boot.assistant.idea.plugin.inspection.yaml;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import dev.flikas.spring.boot.assistant.idea.plugin.filetype.SpringBootConfigurationYamlFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YamlPsiElementVisitor;

public abstract class YamlInspectionBase extends LocalInspectionTool {
  public boolean isAvailableForFile(@NotNull PsiFile file) {
    VirtualFile virtualFile = file.getVirtualFile();
    if (virtualFile == null) {
      return false;
    }
    FileTypeManager ftm = FileTypeManager.getInstance();
    return ftm.isFileOfType(virtualFile, SpringBootConfigurationYamlFileType.INSTANCE);
  }


  @Override
  public @NotNull PsiElementVisitor buildVisitor(
      @NotNull ProblemsHolder holder, boolean isOnTheFly, @NotNull LocalInspectionToolSession session
  ) {
    if (!isAvailableForFile(session.getFile())) return PsiElementVisitor.EMPTY_VISITOR;
    Module module = ModuleUtil.findModuleForFile(session.getFile());
    if (module == null) return PsiElementVisitor.EMPTY_VISITOR;

    return new YamlPsiElementVisitor() {
      @Override
      public void visitKeyValue(@NotNull YAMLKeyValue keyValue) {
        ProgressIndicatorProvider.checkCanceled();
        YamlInspectionBase.this.visitKeyValue(module, keyValue, holder, isOnTheFly);
      }
    };
  }


  protected abstract void visitKeyValue(
      @NotNull Module module, @NotNull YAMLKeyValue keyValue, @NotNull ProblemsHolder holder, boolean isOnTheFly
  );
}
