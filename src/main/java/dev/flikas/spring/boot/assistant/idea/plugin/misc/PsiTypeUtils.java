package dev.flikas.spring.boot.assistant.idea.plugin.misc;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.CommonClassNames;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.ProjectScope;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.util.TypeConversionUtil;
import jakarta.validation.constraints.Size;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

@UtilityClass
public class PsiTypeUtils {
  private static final Logger log = Logger.getInstance(PsiTypeUtils.class);


  public static PsiClassType getJavaLangString(Project project) {
    return PsiType.getJavaLangString(PsiManager.getInstance(project), ProjectScope.getLibrariesScope(project));
  }


  public static PsiClassType getJavaTypeByName(Project project, String typeName) {
    return PsiType.getTypeByName(typeName, project, ProjectScope.getLibrariesScope(project));
  }


  @Nullable
  public static PsiClass findClass(Project project, String sourceType) {
    return JavaPsiFacade
        .getInstance(project)
        .findClass(sourceType.trim().replace('$', '.'), GlobalSearchScope.allScope(project));
  }


  /**
   * @return true if type can be converted from a single String.
   */
  public static boolean isValueType(PsiType type) {
    // From Spring 'org.springframework.boot.convert.ApplicationConversionService'
    return isPhysical(type)
        && (TypeConversionUtil.isAssignableFromPrimitiveWrapper(type)
                || TypeConversionUtil.isPrimitiveAndNotNullOrWrapper(type)
                || TypeConversionUtil.isEnumType(type)
                || PsiTypesUtil.classNameEquals(type, CommonClassNames.JAVA_LANG_STRING)
                || PsiTypesUtil.classNameEquals(type, CommonClassNames.JAVA_LANG_CLASS)
                || PsiTypesUtil.classNameEquals(type, CommonClassNames.JAVA_NIO_CHARSET_CHARSET)
                || PsiTypesUtil.classNameEquals(type, "java.util.Locale")
                || PsiTypesUtil.classNameEquals(type, "java.nio.charset.Charset")
                || PsiTypesUtil.classNameEquals(type, "java.util.Currency")
                || PsiTypesUtil.classNameEquals(type, "java.util.UUID")
                || PsiTypesUtil.classNameEquals(type, "java.util.regex.Pattern")
                || PsiTypesUtil.classNameEquals(type, "kotlin.text.Regex")
                || PsiTypesUtil.classNameEquals(type, "java.util.TimeZone")
                || PsiTypesUtil.classNameEquals(type, "java.time.ZoneId")
                || PsiTypesUtil.classNameEquals(type, "java.time.ZonedDateTime")
                || PsiTypesUtil.classNameEquals(type, "java.util.Calendar")
                || PsiTypesUtil.classNameEquals(type, "java.time.Duration")
                || PsiTypesUtil.classNameEquals(type, "java.time.Period")
                || PsiTypesUtil.classNameEquals(type, "org.springframework.util.unit.DataSize")
                || PsiTypesUtil.classNameEquals(type, "java.io.File")
                || PsiTypesUtil.classNameEquals(type, CommonClassNames.JAVA_NET_URI)
                || PsiTypesUtil.classNameEquals(type, CommonClassNames.JAVA_NET_URL)
                || PsiTypesUtil.classNameEquals(type, "java.net.InetAddress")
                || PsiTypesUtil.classNameEquals(type, "org.springframework.core.io.Resource")
                || PsiTypesUtil.classNameEquals(type, "org.springframework.http.MediaType")
                || canConvertFromString(type));
  }


  public static boolean isPhysical(PsiType type) {
    PsiClass psiClass = PsiTypesUtil.getPsiClass(type);
    if (psiClass == null) return false;
    return type.isValid() && psiClass.isPhysical();
  }


  public static boolean isCollectionOrMap(Project project, @Nullable PsiType type) {
    return isCollection(project, type) || isMap(project, type);
  }


  public static boolean isCollection(Project project, @Nullable PsiType type) {
    if (type == null) return false;
    if (type instanceof PsiArrayType) return true;
    return PsiType.getTypeByName(
            CommonClassNames.JAVA_UTIL_COLLECTION, project, GlobalSearchScope.allScope(project))
        .isAssignableFrom(type);
  }


  public static boolean isMap(Project project, @Nullable PsiType type) {
    if (type == null) return false;
    return PsiType.getTypeByName(
            CommonClassNames.JAVA_UTIL_MAP, project, GlobalSearchScope.allScope(project))
        .isAssignableFrom(type);
  }


  /**
   * @return element type of specified collectionOrArrayType, or null if specified type is not a collection or array, or is null.
   */
  @Nullable
  public static PsiType getElementType(Project project, PsiType collectionOrArrayType) {
    if (collectionOrArrayType instanceof PsiClassType type && isCollection(project, type)) {
      if (PsiTypesUtil.classNameEquals(type, CommonClassNames.JAVA_UTIL_LIST)
          || PsiTypesUtil.classNameEquals(type, CommonClassNames.JAVA_UTIL_ARRAY_LIST)
          || PsiTypesUtil.classNameEquals(type, CommonClassNames.JAVA_UTIL_LINKED_LIST)
          || PsiTypesUtil.classNameEquals(type, CommonClassNames.JAVA_UTIL_SET)
          || PsiTypesUtil.classNameEquals(type, CommonClassNames.JAVA_UTIL_HASH_SET)
          || PsiTypesUtil.classNameEquals(type, CommonClassNames.JAVA_UTIL_LINKED_HASH_SET)
          || PsiTypesUtil.classNameEquals(type, CommonClassNames.JAVA_UTIL_SORTED_SET)) {
        return type.getParameters()[0];
      } else {
        PsiType[] parameters = type.getParameters();
        if (parameters.length == 1) {
          log.warn("Try to retrieve element type from sub-classes of Collection \""
              + type + "\", this may be a wrong result");
          return parameters[0];
        } else {
          //TODO Support of sub-classes of Collection
          log.warn("(Unsupported)Cannot retrieve element type from sub-class of Collection: " + type);
          return null;
        }
      }
    } else if (collectionOrArrayType instanceof PsiArrayType arrayType) {
      return arrayType.getComponentType();
    } else {
      throw new IllegalArgumentException("Unsupported type: " + collectionOrArrayType);
    }
  }


  /**
   * @return the key and value types of this property if it is or can be converted to a java.util.Map, or else null.
   */
  @Nullable
  @Size(min = 2, max = 2)
  public static PsiType[] getKeyValueType(Project project, @Nullable PsiType mapType) {
    if (mapType == null) return null;
    if (PsiTypesUtil.classNameEquals(mapType, CommonClassNames.JAVA_UTIL_MAP)
        || PsiTypesUtil.classNameEquals(mapType, CommonClassNames.JAVA_UTIL_HASH_MAP)
        || PsiTypesUtil.classNameEquals(mapType, CommonClassNames.JAVA_UTIL_CONCURRENT_HASH_MAP)
        || PsiTypesUtil.classNameEquals(mapType, CommonClassNames.JAVA_UTIL_LINKED_HASH_MAP)) {
      return mapType instanceof PsiClassType classType ? classType.getParameters() : null;
    } else if (PsiTypesUtil.classNameEquals(mapType, CommonClassNames.JAVA_UTIL_PROPERTIES)) {
      // java.util.Properties implements Map<Object,Object>, we should manually force it to string.
      PsiType stringType = PsiTypeUtils.getJavaLangString(project);
      return new PsiType[]{stringType, stringType};
    } else if (isMap(project, mapType)) {
      //TODO Support sub-classes of Map, with generics.
      if (mapType instanceof PsiClassType classType) {
        PsiType[] parameters = classType.getParameters();
        if (parameters.length == 2) {
          log.warn("Try to retrieve key & value types from sub-classes of Map \""
              + mapType + "\", this may be a wrong result");
          return parameters;
        }
      }
      log.warn("(Unsupported)Cannot retrieve key & value types from sub-class of Map: " + mapType);
      return null;
    } else {
      throw new IllegalArgumentException("Unsupported type: " + mapType);
    }
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
}
