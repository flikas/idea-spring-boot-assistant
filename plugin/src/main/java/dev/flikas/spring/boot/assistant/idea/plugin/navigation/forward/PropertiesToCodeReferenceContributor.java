package dev.flikas.spring.boot.assistant.idea.plugin.navigation.forward;

import com.intellij.lang.properties.PropertiesLanguage;
import com.intellij.lang.properties.psi.impl.PropertyKeyImpl;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.util.ProcessingContext;
import dev.flikas.spring.boot.assistant.idea.plugin.filetype.SpringBootConfigurationPropertiesFileType;
import org.jetbrains.annotations.NotNull;

import static com.intellij.patterns.PsiJavaPatterns.virtualFile;


/**
 * Provides references from Spring configuration file (application.properties) to code.
 */
public class PropertiesToCodeReferenceContributor extends PsiReferenceContributor {

//TODO refactor by com.intellij.psi.search.searches.DefinitionsScopedSearch.EP and
// com.intellij.psi.search.searches.ReferencesSearch.EP_NAME


  @Override
  public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
    registrar.registerReferenceProvider(
        PlatformPatterns.psiElement(PropertyKeyImpl.class)
            .withLanguage(PropertiesLanguage.INSTANCE)
            .inVirtualFile(virtualFile().ofType(SpringBootConfigurationPropertiesFileType.INSTANCE)),
        new AbstractReferenceProvider() {
          @Override
          protected PsiElement getRefSource(@NotNull PsiElement element, @NotNull ProcessingContext context) {
            return element instanceof PropertyKeyImpl key ? key : null;
          }
        });
  }
}
