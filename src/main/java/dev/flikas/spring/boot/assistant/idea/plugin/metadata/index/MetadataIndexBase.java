package dev.flikas.spring.boot.assistant.idea.plugin.metadata.index;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.util.PsiUtil;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.source.ConfigurationMetadata;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.source.PropertyName;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.source.PropertyTypeUtil;
import lombok.*;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static dev.flikas.spring.boot.assistant.idea.plugin.metadata.source.ConfigurationPropertyName.Form.DASHED;


public abstract class MetadataIndexBase implements MetadataIndex {
  private static final Logger log = Logger.getInstance(MetadataIndexBase.class);

  protected final Map<PropertyName, Group> groups = new HashMap<>();
  protected final Map<PropertyName, Property> properties = new HashMap<>();
  protected final Map<PropertyName, Hint> hints = new HashMap<>();
  protected final Project project;


  protected MetadataIndexBase(Project project) {
    this.project = project;
  }


  @Override
  public boolean isEmpty() {
    return properties.isEmpty();
  }


  @Override
  @Nullable
  public MetadataGroup getGroup(String name) {
    PropertyName key = PropertyName.adapt(name);
    return groups.get(key);
  }


  @Override
  public Collection<MetadataGroup> getGroups() {
    return (Collection) groups.values();
  }


  @Override
  public MetadataProperty getProperty(String name) {
    PropertyName key = PropertyName.adapt(name);
    return properties.get(key);
  }


  @Override
  public MetadataProperty getNearestParentProperty(String name) {
    PropertyName key = PropertyName.adapt(name);
    Property property = null;
    while (key != null && !key.isEmpty() && (property = properties.get(key)) == null) {
      key = key.getParent();
    }
    return property;
  }


  @Override
  public Collection<MetadataProperty> getProperties() {
    return (Collection) properties.values();
  }


  @Override
  public MetadataHint getHint(String name) {
    PropertyName key = PropertyName.adapt(name);
    return hints.get(key);
  }


  @Override
  public Collection<MetadataHint> getHints() {
    return (Collection) hints.values();
  }


  @Override
  public MetadataItem getPropertyOrGroup(String name) {
    PropertyName key = PropertyName.adapt(name);
    MetadataItem item = properties.get(key);
    return item != null ? item : groups.get(key);
  }


  protected void add(ConfigurationMetadata.Property p) {
    this.properties.put(PropertyName.of(p.getName()), new Property(p));
  }


  protected void add(ConfigurationMetadata.Group g) {
    this.groups.put(PropertyName.of(g.getName()), new Group(g));
  }


  protected void add(ConfigurationMetadata.Hint h) {
    this.hints.put(PropertyName.of(h.getName()), new Hint(h));
  }


  @EqualsAndHashCode(of = "metadata")
  @ToString(of = "metadata")
  protected class Property implements MetadataProperty {
    @Getter
    private final ConfigurationMetadata.Property metadata;
    @Getter(AccessLevel.PROTECTED)
    private final PropertyName propertyName;
    private final PsiType propertyType;


    public Property(ConfigurationMetadata.Property metadata) {
      this.metadata = metadata;
      this.propertyName = PropertyName.of(metadata.getName());
      if (StringUtils.isBlank(metadata.getType())) {
        this.propertyType = null;
      } else {
        PsiJavaParserFacade parser = JavaPsiFacade.getInstance(project).getParserFacade();
        // When reference an inner class, we should use A.B not A$B but spring does.
        String typeString = metadata.getType().replace('$', '.');
        this.propertyType = parser.createTypeFromText(typeString, null);
      }
    }


    @Override
    @NotNull
    public String getName() {
      return propertyName.toString();
    }


    @Override
    @Nullable
    public PsiClass getType() {
      return this.propertyType != null ? PsiUtil.resolveClassInType(this.propertyType) : null;
    }


    @Override
    @Nullable
    public PsiClass getSourceType() {
      String sourceType = metadata.getSourceType();
      if (StringUtils.isBlank(sourceType)) return null;
      return PropertyTypeUtil.findClass(project, sourceType);
    }


    @Override
    public @Nullable PsiType getFullType() {
      return this.propertyType;
    }


    @Override
    @Nullable
    public PsiField getSourceField() {
      PsiClass sourceType = getSourceType();
      if (sourceType == null) return null;
      return sourceType.findFieldByName(getCamelCaseLastName(), true);
    }


    @Override
    @Nullable
    public MetadataHint getHint() {
      return hints.getOrDefault(propertyName, hints.get(propertyName.append("values")));
    }


    @Override
    @Nullable
    public MetadataHint getKeyHint() {
      return hints.get(propertyName.append("keys"));
    }


    @Override
    public boolean canBind(@NotNull String key) {
      PropertyName keyName = PropertyName.adapt(key);
      if (this.propertyName.equals(keyName)) return true;
      if (this.propertyName.isAncestorOf(keyName)) {
        @Nullable PsiType[] keyValueTypes = getKeyValueTypes();
        if (keyValueTypes == null) return false;
        PsiType keyType = keyValueTypes[0];
        PsiType valueType = keyValueTypes[1];
        if (PsiTypesUtil.classNameEquals(keyType, CommonClassNames.JAVA_LANG_STRING) && PropertyTypeUtil.isValueType(valueType)) {
          // String key can be bound to any nested keys.
          return true;
        }
        return this.propertyName.getNumberOfElements() == keyName.getNumberOfElements() + 1 && PropertyTypeUtil.isValueType(keyType);
      }
      return false;
    }


    /**
     * @return the key and value types of this property if it is or can be converted to a java.util.Map, or null if not.
     */
    private @Nullable PsiType[] getKeyValueTypes() {
      PsiType type = getFullType();
      if (type == null) return null;
      @NotNull PsiClassType mapType = PsiType.getTypeByName(CommonClassNames.JAVA_UTIL_MAP, project, GlobalSearchScope.allScope(project));
      if (PsiTypesUtil.classNameEquals(type, CommonClassNames.JAVA_UTIL_MAP)
          || PsiTypesUtil.classNameEquals(type, CommonClassNames.JAVA_UTIL_HASH_MAP)
          || PsiTypesUtil.classNameEquals(type, CommonClassNames.JAVA_UTIL_CONCURRENT_HASH_MAP)
          || PsiTypesUtil.classNameEquals(type, CommonClassNames.JAVA_UTIL_LINKED_HASH_MAP)) {
        return type instanceof PsiClassType classType ? classType.getParameters() : null;
      } else if (PsiTypesUtil.classNameEquals(type, CommonClassNames.JAVA_UTIL_PROPERTIES)) {
        // java.util.Properties implements Map<Object,Object>, we should manually force it to string.
        PsiClassType stringType = PsiType.getJavaLangString(PsiManager.getInstance(project), GlobalSearchScope.allScope(project));
        return new PsiType[]{stringType, stringType};
      } else if (!mapType.isAssignableFrom(type)) {
        log.warn("Cannot retrieve key & value types from map type: " + type);
      }
      return null;
    }


    private String getCamelCaseLastName() {
      return PropertyName.toCamelCase(propertyName.getLastElement(DASHED));
    }
  }


  @RequiredArgsConstructor
  @EqualsAndHashCode(of = "metadata")
  @ToString(of = "metadata")
  protected class Group implements MetadataGroup {
    @Getter
    private final ConfigurationMetadata.Group metadata;


    @Override
    public @NotNull String getName() {
      return metadata.getName();
    }


    /**
     * @see ConfigurationMetadata.Group#getType()
     */
    @Override
    @Nullable
    public PsiClass getType() {
      String type = metadata.getType();
      if (StringUtils.isBlank(type)) return null;
      return PropertyTypeUtil.findClass(project, type);
    }


    /**
     * @see ConfigurationMetadata.Group#getSourceType()
     */
    @Override
    @Nullable
    public PsiClass getSourceType() {
      String sourceType = metadata.getSourceType();
      if (StringUtils.isBlank(sourceType)) return null;
      return PropertyTypeUtil.findClass(project, sourceType);
    }


    /**
     * @see ConfigurationMetadata.Group#getSourceMethod()
     */
    @Override
    @Nullable
    public PsiMethod getSourceMethod() {
      //TODO Implement this
      return null;
    }
  }


  @RequiredArgsConstructor
  @EqualsAndHashCode(of = "metadata")
  @ToString(of = "metadata")
  protected class Hint implements MetadataHint {
    @Getter
    private final ConfigurationMetadata.Hint metadata;
  }
}
