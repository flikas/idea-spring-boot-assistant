package dev.flikas.spring.boot.assistant.idea.plugin.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.psi.PsiElementVisitor;
import dev.flikas.spring.boot.assistant.idea.plugin.filetype.YamlPropertiesFileType;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.index.MetadataIndex;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.service.ModuleMetadataService;
import dev.flikas.spring.boot.assistant.idea.plugin.misc.ServiceUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLBundle;
import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.psi.YAMLCompoundValue;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YamlPsiElementVisitor;

public class KeyNotDefinedInspection extends LocalInspectionTool {
  @Override
  public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    ModuleMetadataService service = ServiceUtil.getServiceFromEligibleFile(
        holder.getFile(),
        YamlPropertiesFileType.INSTANCE,
        ModuleMetadataService.class
    );
    if (service == null) {
      return PsiElementVisitor.EMPTY_VISITOR;
    }
    Module module = ModuleUtil.findModuleForFile(holder.getFile());
    assert module != null;
    return new YamlPsiElementVisitor() {
      @Override
      public void visitKeyValue(@NotNull YAMLKeyValue keyValue) {
        ProgressIndicatorProvider.checkCanceled();
        if (keyValue.getKey() == null) return;
        if (keyValue.getValue() instanceof YAMLCompoundValue) return;
        String fullName = YAMLUtil.getConfigFullName(keyValue);
        MetadataIndex.Property property = service.getIndex().getNearstParentProperty(fullName);
        if (property != null){
          
        }
        if (property == null) {
          holder.registerProblem(
              keyValue.getKey(),
              YAMLBundle.message("YamlUnknownKeysInspectionBase.unknown.key", keyValue.getKeyText())
          );
        }
      }
    };
  }
}
