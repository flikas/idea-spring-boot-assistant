package dev.flikas.spring.boot.assistant.idea.plugin.inspection;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.module.Module;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.index.MetadataProperty;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.service.ModuleMetadataService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLBundle;
import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.psi.YAMLCompoundValue;
import org.jetbrains.yaml.psi.YAMLKeyValue;

public class KeyNotDefinedInspection extends YamlInspectionBase {
  @Override
  protected void visitKeyValue(
      @NotNull Module module, @NotNull YAMLKeyValue keyValue, @NotNull ProblemsHolder holder, boolean isOnTheFly
  ) {
    if (keyValue.getKey() == null) return;
    if (keyValue.getValue() instanceof YAMLCompoundValue) return; //only validate leaf nodes

    ModuleMetadataService service = module.getService(ModuleMetadataService.class);
    String fullName = YAMLUtil.getConfigFullName(keyValue);
    MetadataProperty property = service.getIndex().getProperty(fullName);
    if (property != null) {
      // Property is defined
      return;
    }
    // Property is not defined, but maybe its parent has a Map<String,String> or Properties type.
    property = service.getIndex().getNearestParentProperty(fullName);
    if (property == null || !property.canBind(fullName)) {
      holder.registerProblem(
          keyValue.getKey(),
          YAMLBundle.message("YamlUnknownKeysInspectionBase.unknown.key", keyValue.getKeyText())
      );
    }
  }
}
