package dev.flikas.spring.boot.assistant.idea.plugin.navigation;

import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;

public class PsiToYamlKeySearcher extends QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters> {
  protected PsiToYamlKeySearcher() {
    super(true);
  }


  @Override
  public void processQuery(
      @NotNull ReferencesSearch.SearchParameters queryParameters, @NotNull Processor<? super PsiReference> consumer) {
    PsiElement element = queryParameters.getElementToSearch();
    if (!element.isValid()) return;

    Project project = element.getProject();
    PsiToYamlKeyReferenceService service = project.getService(PsiToYamlKeyReferenceService.class);
    service.findReference(element).stream().filter(ref -> ref.getElement().isValid()).forEach(consumer::process);
  }

}
