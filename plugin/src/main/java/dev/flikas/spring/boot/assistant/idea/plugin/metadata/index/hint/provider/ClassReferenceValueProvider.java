package dev.flikas.spring.boot.assistant.idea.plugin.metadata.index.hint.provider;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.JavaInheritorsGetter;
import com.intellij.codeInsight.completion.PrefixMatcher;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.index.hint.Hint;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.source.ConfigurationMetadata;
import dev.flikas.spring.boot.assistant.idea.plugin.misc.PsiTypeUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @see ConfigurationMetadata.Hint.ValueProvider.Type#CLASS_REFERENCE
 */
public class ClassReferenceValueProvider extends AbstractValueProvider {
  private final String targetFQN;
  /**
   * Specify whether only concrete classes are to be considered as valid candidates. Default: true.
   */
  private final boolean concrete;


  ClassReferenceValueProvider(ConfigurationMetadata.Hint.ValueProvider metadata) {
    super(metadata);

    this.targetFQN = getParameter("target", String.class, null);
    this.concrete = getParameter("concrete", Boolean.class, true);
  }


  /**
   * The fully qualified name of the class that should be assignable to the chosen value.
   * Typically used to filter out non-candidate classes.
   * Note that this information can be provided by the type itself by exposing a class with the appropriate upper bound.
   */
  public Optional<PsiClassType> getUpperBoundClass(Project project) {
    return Optional.ofNullable(targetFQN)
        .map(fqn -> PsiTypeUtils.getJavaTypeByName(project, fqn));
  }


  @Override
  public Collection<Hint> provideValues(
      @NotNull CompletionParameters completionParameters, @Nullable PrefixMatcher prefixMatcher
  ) {
    return getUpperBoundClass(completionParameters.getEditor().getProject()).map(baseClass -> {
      List<Hint> values = new ArrayList<>();
      // We do not give the specified prefixMatcher to JavaInheritorsGetter, because it will use it with the simple name
      // of the class, that will filter out too many candidates we need.
      JavaInheritorsGetter.processInheritors(completionParameters, Collections.singleton(baseClass),
          PrefixMatcher.ALWAYS_TRUE,
          t -> {
            PsiClass c = PsiTypeUtils.resolveClassInType(t);
            if (c != null) {
              if (this.concrete && !PsiTypeUtils.isConcrete(t)) return;
              String name = c.getQualifiedName();
              if (StringUtils.isBlank(name)) return;
              if (prefixMatcher != null && !prefixMatcher.prefixMatches(name)) return;
              values.add(new Hint(name, c));
            }
          });
      return values;
    }).orElseGet(Collections::emptyList);
  }
}
