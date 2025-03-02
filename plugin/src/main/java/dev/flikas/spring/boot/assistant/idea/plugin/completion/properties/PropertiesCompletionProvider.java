package dev.flikas.spring.boot.assistant.idea.plugin.completion.properties;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.lang.properties.psi.Property;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbModeBlockedFunctionality;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import dev.flikas.spring.boot.assistant.idea.plugin.completion.CompletionService;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.codeInsight.completion.CompletionUtil.DUMMY_IDENTIFIER_TRIMMED;
import static com.intellij.openapi.module.ModuleUtilCore.findModuleForPsiElement;

class PropertiesCompletionProvider extends CompletionProvider<CompletionParameters> {
  @Override
  protected void addCompletions(
      @NotNull CompletionParameters completionParameters,
      @NotNull ProcessingContext processingContext,
      @NotNull CompletionResultSet resultSet
  ) {
    PsiElement element = completionParameters.getPosition();
    if (element instanceof PsiComment) return;

    Project project = element.getProject();
    if (ReadAction.compute(() -> DumbService.isDumb(project))) {
      DumbService.getInstance(project).showDumbModeNotificationForFunctionality(
          "Spring configuration completion", DumbModeBlockedFunctionality.CodeCompletion);
      return;
    }
    Module module = findModuleForPsiElement(element);
    if (module == null) return;

    // Find context YAMLPsiElement, stop if context is not at the same line.
    @Nullable Property context = PsiTreeUtil.getParentOfType(element, Property.class, false);
    if (context == null) return;

    String originKey = context.getUnescapedKey();
    String originValue = context.getUnescapedValue();

    CompletionService service = CompletionService.getInstance(project);
    if (originKey != null && originKey.contains(DUMMY_IDENTIFIER_TRIMMED)) {
      // User is asking completion for property key
      //TODO Map key completion
      String queryString = StringUtils.truncate(originKey, originKey.indexOf(DUMMY_IDENTIFIER_TRIMMED));
      if (service.findSuggestionForKey(completionParameters, resultSet, "", queryString,
          PropertiesKeyInsertHandler.INSTANCE)) {
        resultSet.stopHere();
      }
    } else if (originValue != null && originValue.contains(DUMMY_IDENTIFIER_TRIMMED)) {
      // Value completion
      String queryString = StringUtils.truncate(originValue, originValue.indexOf(DUMMY_IDENTIFIER_TRIMMED));
      if (StringUtils.isBlank(originKey)) return;
      if (service.findSuggestionForValue(completionParameters, resultSet, originKey, queryString,
          PropertiesValueInsertHandler.INSTANCE)) {
        resultSet.stopHere();
      }
    }
  }
}
