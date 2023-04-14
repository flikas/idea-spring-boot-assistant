package dev.flikas.spring.boot.assistant.idea.plugin.metadata.source;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.util.TypeConversionUtil;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

public class PropertyTypeUtil {

  private PropertyTypeUtil() {
  }


  @Nullable
  public static PsiClass findClass(Project project, String sourceType) {
    return JavaPsiFacade.getInstance(project)
        .findClass(sourceType.replace('$', '.'), GlobalSearchScope.allScope(project));
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


  public static boolean isPhysical(PsiType type) {
    PsiClass psiClass = PsiTypesUtil.getPsiClass(type);
    if (psiClass == null) return false;
    return type.isValid() && psiClass.isPhysical();
  }


  public static boolean isCollection(Project project, PsiType type) {
    if (type == null) return false;
    PsiClassType collectionType = PsiType.getTypeByName(CommonClassNames.JAVA_UTIL_COLLECTION, project, GlobalSearchScope.allScope(project));
    PsiClassType mapType = PsiType.getTypeByName(CommonClassNames.JAVA_UTIL_MAP, project, GlobalSearchScope.allScope(project));
    return type instanceof PsiArrayType
        || mapType.isAssignableFrom(type)
        || collectionType.isAssignableFrom(type);
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
