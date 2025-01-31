package dev.flikas.spring.boot.assistant.idea.plugin.editing;

import com.intellij.codeInsight.editorActions.JoinRawLinesHandlerDelegate;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import dev.flikas.spring.boot.assistant.idea.plugin.filetype.SpringBootConfigurationYamlFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLTokenTypes;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import java.util.Collections;

/**
 * <pre>{@code
 * server:
 *   port: 8080
 * ==>
 * spring.port: 8080
 * }</pre>
 */
public class YamlJoinLinesHandler implements JoinRawLinesHandlerDelegate {
  @Override
  public int tryJoinLines(@NotNull Document document, @NotNull PsiFile file, int start, int end) {
    return CANNOT_JOIN;
  }

  /**
   * {@inheritDoc}
   *
   * @param start offset right after the last non-space char of first line;
   * @param end   offset of first non-space char since the next line.
   */
  @Override
  public int tryJoinRawLines(@NotNull Document document, @NotNull PsiFile file, int start, int end) {
    // Take effects only in Spring YAML Configuration files.
    VirtualFile virtualFile = file.getVirtualFile();
    if (virtualFile == null) return CANNOT_JOIN;
    FileTypeManager ftm = FileTypeManager.getInstance();
    if (!ftm.isFileOfType(virtualFile, SpringBootConfigurationYamlFileType.INSTANCE)) return CANNOT_JOIN;
    // Take effects only when first line represents a key
    PsiElement elementAtStartLineEnd = file.findElementAt(start - 1);
    if (elementAtStartLineEnd == null || !YAMLTokenTypes.COLON.equals(elementAtStartLineEnd.getNode().getElementType()))
      return CANNOT_JOIN;
    // Take effects only when next line represents a key
    PsiElement elementAtEndLineStart = file.findElementAt(end);
    if (elementAtEndLineStart == null ||
            !YAMLTokenTypes.SCALAR_KEY.equals(elementAtEndLineStart.getNode().getElementType())) {
      return CANNOT_JOIN;
    }
    // Take effects only when the key at next line is child of first line
    YAMLKeyValue parentKeyValue = PsiTreeUtil.getParentOfType(elementAtStartLineEnd, YAMLKeyValue.class);
    if (parentKeyValue == null) return CANNOT_JOIN;
    YAMLKeyValue childKeyValue = PsiTreeUtil.getParentOfType(elementAtEndLineStart, YAMLKeyValue.class);
    if (childKeyValue == null) return CANNOT_JOIN;
    if (!PsiTreeUtil.isAncestor(parentKeyValue, childKeyValue, false)) {
      return CANNOT_JOIN;
    }

    // Join parent and child key
    start = elementAtStartLineEnd.getTextOffset();
    document.replaceString(start, end, ".");

    // Reformat joined key value
    PsiDocumentManager pdm = PsiDocumentManager.getInstance(file.getProject());
    pdm.commitDocument(document);
    YAMLKeyValue joinedKeyValue = PsiTreeUtil.getParentOfType(file.findElementAt(start), YAMLKeyValue.class);
    if (joinedKeyValue != null) {
      CodeStyleManager csm = CodeStyleManager.getInstance(joinedKeyValue.getManager());
      csm.reformatText(file, Collections.singletonList(joinedKeyValue.getTextRange()));
      pdm.doPostponedOperationsAndUnblockDocument(document);
    }

    return start;
  }
}
