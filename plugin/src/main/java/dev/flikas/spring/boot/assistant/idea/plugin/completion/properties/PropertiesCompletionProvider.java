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

    String originKey = context.getKey();
    String originValue = context.getValue();


    CompletionService service = CompletionService.getInstance(project);
    //TODO 考虑在key中间键入时的情况，yaml也是，最好不要直接 remove 掉 DUMMY_IDENTIFIER，而是找到它的起始位置。
    if (originKey != null && originKey.contains(DUMMY_IDENTIFIER_TRIMMED)) {
      // User is asking completion for property key
      String queryString = StringUtils.truncate(originKey, originKey.indexOf(DUMMY_IDENTIFIER_TRIMMED));
      service.findSuggestionForKey(completionParameters, resultSet, "", queryString,
          PropertiesKeyInsertHandler.INSTANCE);
    } else if (originValue != null && originValue.contains(DUMMY_IDENTIFIER_TRIMMED)) {
      // Value completion
      String queryString = StringUtils.truncate(originValue, originValue.indexOf(DUMMY_IDENTIFIER_TRIMMED));
      service.findSuggestionForValue(completionParameters, resultSet, "", queryString,
          PropertiesValueInsertHandler.INSTANCE);
    }
  }
}
