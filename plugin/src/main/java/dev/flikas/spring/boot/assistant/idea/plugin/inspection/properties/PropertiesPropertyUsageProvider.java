package dev.flikas.spring.boot.assistant.idea.plugin.inspection.properties;

import com.intellij.lang.properties.codeInspection.unused.ImplicitPropertyUsageProvider;
import com.intellij.lang.properties.psi.Property;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.service.ModuleMetadataService;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.source.PropertyName;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

public class PropertiesPropertyUsageProvider implements ImplicitPropertyUsageProvider {
  @Override
  public boolean isUsed(@NotNull Property property) {
    Module module = ModuleUtil.findModuleForPsiElement(property);
    if (module == null) return false;
    var service = ModuleMetadataService.getInstance(module);
    String key = property.getUnescapedKey();
    if (StringUtils.isBlank(key)) return false;
    PropertyName propertyName = PropertyName.adapt(key);
    if (propertyName.isLastElementIndexed()) {
      key = propertyName.getParent().toString();
    }
    if (service.getIndex().getProperty(key) != null) return true;
    var nearestParent = service.getIndex().getNearestParentProperty(key);
    return nearestParent != null && nearestParent.canBind(key);
  }
}
