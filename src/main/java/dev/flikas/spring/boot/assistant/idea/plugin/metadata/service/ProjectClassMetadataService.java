package dev.flikas.spring.boot.assistant.idea.plugin.metadata.service;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PropertyUtil;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.util.PsiUtil;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.index.MetadataIndex;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.source.ConfigurationMetadata;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.source.PropertyName;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.source.PropertyTypeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Service
public final class ProjectClassMetadataService implements Disposable {
  private static final Logger log = Logger.getInstance(ProjectClassMetadataService.class);

  private final Project project;
  private final Map<String,>


  public ProjectClassMetadataService(Project project) {
    this.project = project;
    //TODO Watch Psi changes
    PsiManager.getInstance(project).addPsiTreeChangeListener(
        new PsiTreeChangeAdapter() {
        }, this);
  }


  @NotNull
  public Optional<MetadataIndex> getMetadata(PropertyName basename, PsiType type) {
    if (PropertyTypeUtil.isValueType(type)) return Optional.empty();
    return Optional.ofNullable(

    )
  }


  @NotNull
  private ConfigurationMetadata generateMetadata(ConfigurationMetadata metadata, PropertyName basename, PsiType type) {
    if (PropertyTypeUtil.isValueType(type)) {
      // This method is only for complex types.
      return metadata;
    }
    if (PsiTypesUtil.classNameEquals(type, CommonClassNames.JAVA_UTIL_MAP)
        || PsiTypesUtil.classNameEquals(type, CommonClassNames.JAVA_UTIL_HASH_MAP)
        || PsiTypesUtil.classNameEquals(type, CommonClassNames.JAVA_UTIL_CONCURRENT_HASH_MAP)
        || PsiTypesUtil.classNameEquals(type, CommonClassNames.JAVA_UTIL_LINKED_HASH_MAP)) {
      if (type instanceof PsiClassType classType) {
        PsiType[] parameters = classType.getParameters();
        if (parameters.length != 2) {
          log.warn(basename + " has illegal Map type: " + type);
          return metadata;
        }
        if (!PropertyTypeUtil.isValueType(parameters[0])) {
          log.warn(basename + " has unsupported Map key type: " + type);
          return metadata;
        }
        generateMetadata(metadata, basename.appendAnyMapKey(), parameters[1]);
      }
    } else if (PsiTypesUtil.classNameEquals(type, CommonClassNames.JAVA_UTIL_PROPERTIES)) {
      generateMetadata(metadata, basename.appendAnyMapKey(), PsiType.getJavaLangString(
          PsiManager.getInstance(project), GlobalSearchScope.allScope(project)));
    } else if (PsiTypesUtil.classNameEquals(type, CommonClassNames.JAVA_UTIL_LIST)
        || PsiTypesUtil.classNameEquals(type, CommonClassNames.JAVA_UTIL_ARRAY_LIST)
        || PsiTypesUtil.classNameEquals(type, CommonClassNames.JAVA_UTIL_LINKED_LIST)
        || PsiTypesUtil.classNameEquals(type, CommonClassNames.JAVA_UTIL_SET)
        || PsiTypesUtil.classNameEquals(type, CommonClassNames.JAVA_UTIL_HASH_SET)
        || PsiTypesUtil.classNameEquals(type, CommonClassNames.JAVA_UTIL_LINKED_HASH_SET)
        || PsiTypesUtil.classNameEquals(type, CommonClassNames.JAVA_UTIL_SORTED_SET)
        || type instanceof PsiArrayType) {
      PsiType valueType;
      if (type instanceof PsiClassType classType) {
        PsiType[] parameters = classType.getParameters();
        if (parameters.length != 1) {
          log.warn(basename + " has illegal Collection type: " + type);
          return metadata;
        }
        valueType = parameters[0];
      } else if (type instanceof PsiArrayType arrayType) {
        valueType = arrayType.getComponentType();
      } else {
        // We should not be here
        return metadata;
      }
      generateMetadata(metadata, basename.appendAnyNumericalIndex(), valueType);
    } else if (!PropertyTypeUtil.isCollection(project, type)) {
      PsiClass valueClass = PsiUtil.resolveClassInType(type);
      if (valueClass == null) return metadata;
      //TODO Watch class's modification & update metadata
      for (String fieldName : PropertyUtil.getWritableProperties(valueClass, true)) {
        PsiField field = valueClass.findFieldByName(fieldName, true);
        if (field == null) continue;
        PropertyName name = basename.append(PropertyName.toKebabCase(fieldName));
        ConfigurationMetadata.Property meta = new ConfigurationMetadata.Property();
        meta.setName(name.toString());
        PsiType propertyType = PropertyUtil.getPropertyType(field);
        if (propertyType instanceof PsiPrimitiveType primitiveType) {
          propertyType = primitiveType.getBoxedType(field);
        }
        if (propertyType == null) continue;
        if (PropertyTypeUtil.isValueType(propertyType)) {
          // Leaf property, which type can be converted from a single string
          meta.setType(propertyType.getCanonicalText());
          meta.setSourceType(valueClass.getQualifiedName());
          PsiExpression initializer = field.getInitializer();
          if (initializer instanceof PsiLiteralExpression literal) {
            meta.setDefaultValue(literal.getValue());
          }
          metadata.getProperties().add(meta);
        } else {
          // Nested class, recursive in.
          generateMetadata(metadata, name, propertyType);
        }
      }
    } else if (!PropertyTypeUtil.isValueType(type)) {
      log.warn("Property \"" + basename + "\" has unsupported collection type: " + type);
    }
    return metadata;
  }


  @Override
  public void dispose() {
  }
}
