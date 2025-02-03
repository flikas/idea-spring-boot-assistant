package dev.flikas.spring.boot.assistant.idea.plugin.documentation;

import com.intellij.lang.documentation.AbstractDocumentationProvider;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import dev.flikas.spring.boot.assistant.idea.plugin.completion.SourceContainer;
import dev.flikas.spring.boot.assistant.idea.plugin.documentation.service.DocumentationService;
import dev.flikas.spring.boot.assistant.idea.plugin.filetype.SpringBootConfigurationYamlFileType;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.index.MetadataItem;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.service.ModuleMetadataService;
import dev.flikas.spring.boot.assistant.idea.plugin.misc.PsiElementUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import static com.intellij.openapi.module.ModuleUtilCore.findModuleForPsiElement;

public class YamlDocumentationProvider extends AbstractDocumentationProvider {
  @Nullable
  @Nls
  @Override
  public String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
    if (element instanceof SourceContainer ce) {
      return generateDocumentHtml(ce);
    }

    if (originalElement != null) element = originalElement;
    if (!PsiElementUtils.isInFileOfType(element, SpringBootConfigurationYamlFileType.INSTANCE)) {
      return null;
    }
    Module module = findModuleForPsiElement(element);
    if (module == null) {
      return null;
    }
    // Find context YAMLKeyValue, stop if context is not at the same line.
    YAMLKeyValue keyValue = PsiTreeUtil.getContextOfType(element, false, YAMLKeyValue.class);
    if (keyValue == null) return null;
    if (!YAMLUtil.psiAreAtTheSameLine(element, keyValue)) return null;

    String propertyName = YAMLUtil.getConfigFullName(keyValue);
    ModuleMetadataService service = module.getService(ModuleMetadataService.class);
    @Nullable MetadataItem propertyOrGroup = service.getIndex().getPropertyOrGroup(propertyName);
    if (propertyOrGroup == null) return null;

    return DocumentationService.getInstance(module.getProject()).generateDoc(propertyOrGroup);
  }


  @NotNull
  private String generateDocumentHtml(SourceContainer sc) {
    DocumentationService docSvc = DocumentationService.getInstance(sc.getProject());
    return sc.getSourceMetadataItem().map(docSvc::generateDoc).orElseGet(() ->
        sc.getSourceHint().map(docSvc::generateDoc).orElseThrow());
  }
}
