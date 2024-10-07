package dev.flikas.spring.boot.assistant.idea.plugin.editing;

import com.intellij.application.options.CodeStyle;
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegateAdapter;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.codeStyle.IndentHelperImpl;
import com.intellij.psi.util.PsiTreeUtil;
import dev.flikas.spring.boot.assistant.idea.plugin.filetype.SpringBootConfigurationYamlFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLTextUtil;
import org.jetbrains.yaml.YAMLTokenTypes;
import org.jetbrains.yaml.psi.YAMLCompoundValue;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLValue;

import static com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate.Result.Continue;

public class YamlSplitKeyProcessor extends EnterHandlerDelegateAdapter {

    @Override
    public Result preprocessEnter(@NotNull PsiFile file, @NotNull Editor editor, @NotNull Ref<Integer> caretOffset,
                                  @NotNull Ref<Integer> caretAdvance, @NotNull DataContext dataContext,
                                  EditorActionHandler originalHandler) {
        VirtualFile virtualFile = file.getVirtualFile();
        if (virtualFile == null) return Continue;
        FileTypeManager ftm = FileTypeManager.getInstance();
      if (!ftm.isFileOfType(virtualFile, SpringBootConfigurationYamlFileType.INSTANCE))
            return Continue;
        if (caretOffset.get() <= 0)
            return Continue;
        PsiDocumentManager.getInstance(file.getProject()).commitDocument(editor.getDocument());
        PsiElement elementAtCaret = file.findElementAt(caretOffset.get());
        if (elementAtCaret == null)
            return Continue;

        if (YAMLTokenTypes.SCALAR_KEY.equals(elementAtCaret.getNode().getElementType())) {
            Document document = editor.getDocument();
            int offset = caretOffset.get();
            char c = document.getText().charAt(offset);
            if (c != '.') {
                //If caret is right after the dot, it should work as well.
                c = document.getText().charAt(--offset);
            }
            if (c == '.') {
                int indentSize = CodeStyle.getIndentSize(file);
                //Indent children
                YAMLKeyValue keyValue = PsiTreeUtil.getParentOfType(elementAtCaret, YAMLKeyValue.class);
                if (keyValue != null) {
                    YAMLValue valueElement = keyValue.getValue();
                    if (valueElement instanceof YAMLCompoundValue) {
                        TextRange range = valueElement.getTextRange();
                        document.replaceString(range.getStartOffset(), range.getEndOffset(),
                                YAMLTextUtil.indentText(valueElement.getText(), indentSize));
                    }
                }
                //Split the key
                String space = IndentHelperImpl.fillIndent(CodeStyle.getIndentOptions(file), indentSize);
                document.replaceString(offset, offset + 1, ":" + space);
                caretOffset.set(offset + 1);
            }
        }

        return Continue;
    }
}
