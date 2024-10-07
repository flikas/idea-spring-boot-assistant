package dev.flikas.spring.boot.assistant.idea.plugin.navigation;

import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.util.ProcessingContext;
import dev.flikas.spring.boot.assistant.idea.plugin.filetype.SpringBootConfigurationYamlFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLLanguage;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import static com.intellij.patterns.PlatformPatterns.psiElement;
import static com.intellij.patterns.PsiJavaPatterns.virtualFile;

//TODO refactor by com.intellij.psi.search.searches.DefinitionsScopedSearch.EP and
// com.intellij.psi.search.searches.ReferencesSearch.EP_NAME
public class YamlReferenceContributor extends PsiReferenceContributor {

  @Override
  public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
    PsiElementPattern.Capture<YAMLKeyValue> pattern =
        psiElement(YAMLKeyValue.class)
            .withLanguage(YAMLLanguage.INSTANCE)
            .inVirtualFile(virtualFile().ofType(SpringBootConfigurationYamlFileType.INSTANCE));
    registrar.registerReferenceProvider(pattern, new PsiReferenceProvider() {
      @Override
      public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                             @NotNull ProcessingContext context) {
        if (element instanceof YAMLKeyValue yamlKeyValue) {
          return new YamlKeyToPsiReference[]{new YamlKeyToPsiReference(yamlKeyValue)};
        } else {
          return PsiReference.EMPTY_ARRAY;
        }
      }
    });
  }

}
