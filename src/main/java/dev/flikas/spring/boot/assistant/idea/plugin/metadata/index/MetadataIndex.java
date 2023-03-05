package dev.flikas.spring.boot.assistant.idea.plugin.metadata.index;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PropertyUtil;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.psi.util.TypeConversionUtil;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.source.ConfigurationMetadata;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.source.PropertyName;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static dev.flikas.spring.boot.assistant.idea.plugin.metadata.source.ConfigurationPropertyName.Form.DASHED;

public class MetadataIndex {
  public static final MetadataIndex EMPTY = new MetadataIndex();
  private static final Logger log = Logger.getInstance(MetadataIndex.class);
  @Getter
  private final Map<String, ConfigurationMetadata> sourceUrlToMetadata = new ConcurrentHashMap<>();
  private final Map<PropertyName, Group> groups = new HashMap<>();
  private final Map<PropertyName, Property> properties = new HashMap<>();
  private final Map<PropertyName, Hint> hints = new HashMap<>();
  private final Project project;


  public MetadataIndex(Project project) {
    this.project = project;
  }


  private MetadataIndex() {
    this(null);
  }


  @Nullable
  public static PsiClass findClass(Project project, String sourceType) {
    return JavaPsiFacade.getInstance(project)
        .findClass(sourceType.replace('$', '.'), GlobalSearchScope.allScope(project));
  }


  public void merge(String sourceUrl, ConfigurationMetadata metadata) {
    if (this.project == null) {
      throw new UnsupportedOperationException("this instance is unmodifiable");
    }
    if (metadata.isEmpty()) return;
    final List<Property> newProperties = Collections.synchronizedList(new ArrayList<>());
    this.sourceUrlToMetadata.computeIfAbsent(sourceUrl, url -> {
      if (metadata.getGroups() != null) {
        metadata.getGroups().forEach(g -> {
          PropertyName name = PropertyName.ofIfValid(g.getName());
          if (name != null)
            this.groups.put(name, new Group(g));
          else
            log.warn("Invalid group name " + g.getName() + " in " + sourceUrl + ", skipped");
        });
      }
      if (metadata.getHints() != null) {
        metadata.getHints().forEach(h -> {
          PropertyName name = PropertyName.ofIfValid(h.getName());
          if (name != null)
            this.hints.put(name, new Hint(h));
          else
            log.warn("Invalid hint name " + h.getName() + " in " + sourceUrl + ", skipped");
        });
      }
      metadata.getProperties().forEach(p -> {
        PropertyName name = PropertyName.ofIfValid(p.getName());
        if (name != null) {
          Property property = new Property(p);
          newProperties.add(property);
          this.properties.put(name, property);
        } else
          log.warn("Invalid property name " + p.getName() + " in " + sourceUrl + ", skipped");
      });
      return metadata;
    });

    newProperties.forEach(this::resolvePropertyType);
  }


  public void merge(MetadataIndex metadata) {
    metadata.sourceUrlToMetadata.forEach(this::merge);
  }


  public boolean isEmpty() {
    return sourceUrlToMetadata.isEmpty();
  }


  @Nullable
  public Group getGroup(String name) {
    PropertyName key = PropertyName.adapt(name);
    return groups.get(key);
  }


  public Collection<Group> getGroups() {
    return groups.values();
  }


  public Property getProperty(String name) {
    PropertyName key = PropertyName.adapt(name);
    return properties.get(key);
  }


  public Property getNearstParentProperty(String name) {
    PropertyName key = PropertyName.adapt(name);
    Property property = null;
    while (key != null && !key.isEmpty() && (property = properties.get(key)) == null) {
      key = key.getParent();
    }
    return property;
  }


  public Collection<Property> getProperties() {
    return properties.values();
  }


  public Hint getHint(String name) {
    PropertyName key = PropertyName.adapt(name);
    return hints.get(key);
  }


  public Collection<Hint> getHints() {
    return hints.values();
  }


  public MetadataItem getPropertyOrGroup(String name) {
    PropertyName key = PropertyName.adapt(name);
    MetadataItem item = properties.get(key);
    return item != null ? item : groups.get(key);
  }


  /**
   * @return true if type can be converted from a single String.
   */
  public static boolean isValueType(PsiType type) {
    return isPhysical(type)
        && (TypeConversionUtil.isAssignableFromPrimitiveWrapper(type)
        || TypeConversionUtil.isPrimitiveAndNotNullOrWrapper(type)
        || TypeConversionUtil.isEnumType(type)
        || PsiTypesUtil.classNameEquals(type, "java.lang.String")
        || PsiTypesUtil.classNameEquals(type, "java.lang.Class")
        || PsiTypesUtil.classNameEquals(type, "java.nio.charset.Charset")
        || PsiTypesUtil.classNameEquals(type, "java.time.Duration")
        || PsiTypesUtil.classNameEquals(type, "java.net.URI")
        || PsiTypesUtil.classNameEquals(type, "java.net.URL")
        || PsiTypesUtil.classNameEquals(type, "java.net.InetAddress")
        || PsiTypesUtil.classNameEquals(type, "org.springframework.util.unit.DataSize")
        || PsiTypesUtil.classNameEquals(type, "org.springframework.core.io.Resource")
        || canConvertFromString(type));
  }


  /**
   * Spring does not create metadata for types in collections, we should create it by ourselves and expand our index.
   *
   * @see ConfigurationMetadata.Property#getType()
   */
  private void resolvePropertyType(Property property) {
    @Nullable PsiType type = property.getFullType();
    if (type == null || !type.isValid()) return;
    if (isCollection(type)) {
      ConfigurationMetadata metadata = generateMetadata(new ConfigurationMetadata(), property.propertyName, type);
      merge("(generated from " + property.propertyName + ")", metadata);
    } else if (PsiUtil.resolveClassInType(type) != null && !isValueType(type)) {
      log.warn(property.getName() + " has unsupported type: " + type.getCanonicalText());
    }
  }


  @NotNull
  private ConfigurationMetadata generateMetadata(ConfigurationMetadata metadata, PropertyName basename, PsiType type) {
    if (isValueType(type)) {
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
        if (!isValueType(parameters[0])) {
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
    } else if (!isCollection(type)) {
      PsiClass valueClass = PsiUtil.resolveClassInType(type);
      if (valueClass == null) return metadata;
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
        if (isValueType(propertyType)) {
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
    } else if (!isValueType(type)) {
      log.warn("Property \"" + basename + "\" has unsupported collection type: " + type);
    }
    return metadata;
  }


  private boolean isCollection(PsiType type) {
    if (type == null) return false;
    PsiClassType collectionType = PsiType.getTypeByName(CommonClassNames.JAVA_UTIL_COLLECTION, project, GlobalSearchScope.allScope(project));
    PsiClassType mapType = PsiType.getTypeByName(CommonClassNames.JAVA_UTIL_MAP, project, GlobalSearchScope.allScope(project));
    return type instanceof PsiArrayType
        || mapType.isAssignableFrom(type)
        || collectionType.isAssignableFrom(type);
  }


  private static boolean isPhysical(PsiType type) {
    PsiClass psiClass = PsiTypesUtil.getPsiClass(type);
    if (psiClass == null) return false;
    return type.isValid() && psiClass.isPhysical();
  }


  private static boolean canConvertFromString(PsiType type) {
    if (type instanceof PsiClassType classType) {
      PsiClass psiClass = classType.resolve();
      if (psiClass == null) return false;
      return Stream.concat(
              Arrays.stream(psiClass.getConstructors()),
              Arrays.stream(psiClass.getMethods())
                  .filter(m -> m.hasModifierProperty(PsiModifier.STATIC))
                  .filter(m -> PsiTypesUtil.compareTypes(m.getReturnType(), type, true))
          ).map(PsiMethod::getParameterList)
          .filter(list -> list.getParametersCount() == 1)
          .map(list -> list.getParameter(0))
          .filter(Objects::nonNull)
          .map(PsiParameter::getType)
          .anyMatch(t -> PsiTypesUtil.classNameEquals(t, CommonClassNames.JAVA_LANG_STRING));
    }
    return false;
  }


  @EqualsAndHashCode(of = "metadata")
  @ToString(of = "metadata")
  public class Property implements MetadataProperty {
    @Getter
    private final ConfigurationMetadata.Property metadata;
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
      return findClass(project, sourceType);
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
    public Hint getHint() {
      return hints.getOrDefault(propertyName, hints.get(propertyName.append("values")));
    }


    @Override
    @Nullable
    public Hint getKeyHint() {
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
        if (PsiTypesUtil.classNameEquals(keyType, CommonClassNames.JAVA_LANG_STRING) && isValueType(valueType)) {
          // String key can be bound to any nested keys.
          return true;
        }
        return this.propertyName.getNumberOfElements() == keyName.getNumberOfElements() + 1 && isValueType(keyType);
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
  public class Group implements MetadataItem {
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
      return findClass(project, type);
    }


    /**
     * @see ConfigurationMetadata.Group#getSourceType()
     */
    @Override
    @Nullable
    public PsiClass getSourceType() {
      String sourceType = metadata.getSourceType();
      if (StringUtils.isBlank(sourceType)) return null;
      return findClass(project, sourceType);
    }


    /**
     * @see ConfigurationMetadata.Group#getSourceMethod()
     */
    @Nullable
    public PsiMethod getSourceMethod() {
      //TODO Implement this
      return null;
    }
  }


  @RequiredArgsConstructor
  @EqualsAndHashCode(of = "metadata")
  @ToString(of = "metadata")
  public class Hint {
    @Getter
    private final ConfigurationMetadata.Hint metadata;
  }
}
