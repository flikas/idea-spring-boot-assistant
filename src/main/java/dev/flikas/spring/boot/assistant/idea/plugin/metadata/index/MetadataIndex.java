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
    } else if (!isValueType(type)) {
      log.warn(property.getName() + " has unsupported type: " + type.getCanonicalText());
    }
  }


  @NotNull
  private ConfigurationMetadata generateMetadata(ConfigurationMetadata metadata, PropertyName basename, PsiType type) {
    if (PsiTypesUtil.classNameEquals(type, "java.util.Map")) {
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
    } else if (PsiTypesUtil.classNameEquals(type, "java.util.List")
        || PsiTypesUtil.classNameEquals(type, "java.util.Set")
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
    } else {
      if (isValueType(type)) return metadata;
      PsiClass valueClass = PsiUtil.resolveClassInType(type);
      if (valueClass == null) return metadata;
      for (String fieldName : PropertyUtil.getWritableProperties(valueClass, true)) {
        PsiField field = valueClass.findFieldByName(fieldName, true);
        if (field == null) continue;
        PropertyName name = basename.append(PropertyName.toKebabCase(fieldName));
        ConfigurationMetadata.Property meta = new ConfigurationMetadata.Property();
        meta.setName(name.toString());
        meta.setType(Objects.requireNonNull(PropertyUtil.getPropertyType(field)).getCanonicalText());
        meta.setSourceType(valueClass.getQualifiedName());
        PsiExpression initializer = field.getInitializer();
        if (initializer instanceof PsiLiteralExpression literal) {
          meta.setDefaultValue(literal.getValue());
        }
        metadata.getProperties().add(meta);
      }
    }
    return metadata;
  }


  private static boolean isCollection(PsiType type) {
    return PsiTypesUtil.classNameEquals(type, "java.util.List")
        || PsiTypesUtil.classNameEquals(type, "java.util.Map")
        || PsiTypesUtil.classNameEquals(type, "java.util.Set")
        || type instanceof PsiArrayType;
  }


  private static boolean isValueType(PsiType type) {
    return TypeConversionUtil.isAssignableFromPrimitiveWrapper(type)
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
        || PsiTypesUtil.classNameEquals(type, "org.springframework.core.io.Resource");

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
      return sourceType.findFieldByName(getFieldName(), true);
    }


    /**
     * get hint or value hint for this property.
     */
    @Nullable
    public Hint getHint() {
      return hints.getOrDefault(propertyName, hints.get(propertyName.append("values")));
    }


    /**
     * get key hint for this property if it is a Map.
     */
    @Nullable
    public Hint getKeyHint() {
      return hints.get(propertyName.append("keys"));
    }


    private String getFieldName() {
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
