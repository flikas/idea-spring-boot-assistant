package dev.flikas.spring.boot.assistant.idea.plugin.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import dev.flikas.spring.boot.assistant.idea.plugin.filetype.YamlPropertiesFileType;
import dev.flikas.spring.boot.assistant.idea.plugin.misc.ServiceUtil;
import in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNode;
import in.oneton.idea.spring.assistant.plugin.suggestion.service.SuggestionService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLBundle;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YamlPsiElementVisitor;

import java.util.ArrayList;
import java.util.List;

import static in.oneton.idea.spring.assistant.plugin.misc.GenericUtil.truncateIdeaDummyIdentifier;
import static java.util.Objects.requireNonNull;

/**
 * Key
 *
 * <table style=">td{border:1px;}border: 1px solid; td border: 1px;">
 *     <tr><td>abc</td></tr>
 * </table>
 */
public class KeyNotDefinedInspection extends LocalInspectionTool {
  @Override
  public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    SuggestionService service = ServiceUtil.getServiceFromEligibleFile(
        holder.getFile(),
        YamlPropertiesFileType.INSTANCE,
        SuggestionService.class
    );
    if (service == null) {
      return PsiElementVisitor.EMPTY_VISITOR;
    }
    Module module = ModuleUtil.findModuleForFile(holder.getFile());
    assert module != null;
    return new YamlPsiElementVisitor() {
      @Override
      public void visitKeyValue(@NotNull YAMLKeyValue keyValue) {
        ProgressIndicatorProvider.checkCanceled();
        List<String> ancestralKeys = new ArrayList<>();
        PsiElement context = keyValue;
        do {
          if (context instanceof YAMLKeyValue) {
            ancestralKeys.add(0, truncateIdeaDummyIdentifier(((YAMLKeyValue) context).getKeyText()));
          }
          context = requireNonNull(context).getParent();
        } while (context != null);
        List<SuggestionNode> matchedNodesFromRootTillLeaf = service.findMatchedNodesRootTillEnd(ancestralKeys);
        if (matchedNodesFromRootTillLeaf == null) {
          assert keyValue.getKey() != null;
          holder.registerProblem(
              keyValue.getKey(),
              YAMLBundle.message("YamlUnknownKeysInspectionBase.unknown.key", keyValue.getKeyText())
          );
        }
      }
    };
  }
}
