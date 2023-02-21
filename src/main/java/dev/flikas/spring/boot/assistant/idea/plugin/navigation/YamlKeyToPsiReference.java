package dev.flikas.spring.boot.assistant.idea.plugin.navigation;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.index.MetadataIndex;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.index.MetadataItem;
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
  @NotNull
  private final Project project;


  public YamlKeyToPsiReference(@NotNull YAMLKeyValue yamlKeyValue) {
    super(yamlKeyValue, true);
    this.yamlKeyValue = yamlKeyValue;
    this.project = yamlKeyValue.getProject();
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
    if (propertyOrGroup instanceof MetadataIndex.Property property) {
      return property.getSourceField();
    } else {
      return propertyOrGroup.getType();
    }
  }
}
