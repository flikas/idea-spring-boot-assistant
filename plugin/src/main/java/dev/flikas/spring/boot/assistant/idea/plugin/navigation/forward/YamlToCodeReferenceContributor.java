package dev.flikas.spring.boot.assistant.idea.plugin.navigation.forward;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.util.ProcessingContext;
import dev.flikas.spring.boot.assistant.idea.plugin.filetype.SpringBootConfigurationYamlFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLLanguage;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import static com.intellij.patterns.PlatformPatterns.psiElement;
import static com.intellij.patterns.PsiJavaPatterns.virtualFile;


/**
 * Provides references from Spring configuration file (application.yaml) to code.
 */
public class YamlToCodeReferenceContributor extends PsiReferenceContributor {

//TODO refactor by com.intellij.psi.search.searches.DefinitionsScopedSearch.EP and
// com.intellij.psi.search.searches.ReferencesSearch.EP_NAME


  @Override
  public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
    registrar.registerReferenceProvider(
        psiElement(YAMLKeyValue.class)
            .withLanguage(YAMLLanguage.INSTANCE)
            .inVirtualFile(virtualFile().ofType(SpringBootConfigurationYamlFileType.INSTANCE)),
        new AbstractReferenceProvider() {
          @Override
          protected PsiElement getRefSource(@NotNull PsiElement element, @NotNull ProcessingContext context) {
            return element instanceof YAMLKeyValue yamlKeyValue ? yamlKeyValue : null;
          }
        });
  }
}
