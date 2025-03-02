package dev.flikas.spring.boot.assistant.idea.plugin.navigation.backward;

import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.util.Processor;
import dev.flikas.spring.boot.assistant.idea.plugin.navigation.ReferenceService;
import org.jetbrains.annotations.NotNull;

public class PsiToSpringPropertyReferenceSearcher
    extends QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters> {
  protected PsiToSpringPropertyReferenceSearcher() {
    super(true);
  }


  @Override
  public void processQuery(
      @NotNull ReferencesSearch.SearchParameters queryParameters, @NotNull Processor<? super PsiReference> consumer) {
    if (!queryParameters.areValid()) return;
    SearchScope searchScope = queryParameters.getScopeDeterminedByUser();
    if (!(searchScope instanceof GlobalSearchScope)) return;
    PsiElement element = queryParameters.getElementToSearch();

    Project project = element.getProject();
    ReferenceService service = ReferenceService.getInstance(project);
    service.backwardReference(element)
        .stream()
        .filter(ref -> ref.getElement().isValid())
        .filter(ref -> searchScope.contains(ref.getElement().getContainingFile().getVirtualFile()))
        .forEach(consumer::process);
  }
}
