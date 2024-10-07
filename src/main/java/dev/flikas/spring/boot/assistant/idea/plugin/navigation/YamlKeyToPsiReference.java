package dev.flikas.spring.boot.assistant.idea.plugin.navigation;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.index.MetadataIndex;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.index.MetadataItem;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.index.MetadataProperty;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.service.ModuleMetadataService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.psi.YAMLKeyValue;

public class YamlKeyToPsiReference extends PsiReferenceBase<PsiElement> {
  @NotNull
  private final YAMLKeyValue yamlKeyValue;
  @Nullable
  private final Module module;


  public YamlKeyToPsiReference(@NotNull YAMLKeyValue yamlKeyValue) {
    super(yamlKeyValue, true);
    this.yamlKeyValue = yamlKeyValue;
    this.module = ModuleUtil.findModuleForPsiElement(yamlKeyValue);
  }


  @Override
  public @Nullable PsiElement resolve() {
    if (module == null) {
      return null;
    }

    ModuleMetadataService metadataService = module.getService(ModuleMetadataService.class);
    MetadataIndex metadata = metadataService.getIndex();
    String fullName = YAMLUtil.getConfigFullName(yamlKeyValue);

    MetadataItem propertyOrGroup = metadata.getPropertyOrGroup(fullName);
    if (propertyOrGroup == null) return null;
    if (propertyOrGroup instanceof MetadataProperty property) {
      return property.getSourceField().orElse(null);
    } else {
      return propertyOrGroup.getType().orElse(null);
    }
  }
}
