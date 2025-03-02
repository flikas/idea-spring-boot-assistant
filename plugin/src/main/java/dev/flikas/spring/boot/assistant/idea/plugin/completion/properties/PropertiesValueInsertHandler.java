package dev.flikas.spring.boot.assistant.idea.plugin.completion.properties;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.lang.properties.psi.PropertiesResourceBundleUtil;
import com.intellij.lang.properties.psi.Property;
import com.intellij.lang.properties.psi.PropertyKeyValueFormat;
import com.intellij.lang.properties.psi.codeStyle.PropertiesCodeStyleSettings;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import net.jcip.annotations.ThreadSafe;
import org.jetbrains.annotations.NotNull;

@ThreadSafe
class PropertiesValueInsertHandler implements InsertHandler<LookupElement> {
  public static final PropertiesValueInsertHandler INSTANCE = new PropertiesValueInsertHandler();


  private PropertiesValueInsertHandler() {}


  @Override
  public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement lookupElement) {
    Project project = context.getProject();
    PsiElement currentElement = context.getFile().findElementAt(context.getStartOffset());
    assert currentElement != null : "no element at " + context.getStartOffset();
    Property property = PsiTreeUtil.getParentOfType(currentElement, Property.class);
    if (property == null) return;

    String escaped = escapeValue(project, lookupElement.getLookupString());
    if (!escaped.equals(lookupElement.getLookupString())) {
      property.setValue(lookupElement.getLookupString(), PropertyKeyValueFormat.MEMORY);
    }

    //TODO Add '\n' if the value is 'complete'(for example, 'classpath:' is not complete.
//    if (context.getCompletionChar() == '\n') {
//      Editor editor = context.getEditor();
//      editor.getCaretModel().moveToOffset(property.getTextOffset() + property.getTextLength());
//      EditorModificationUtil.insertStringAtCaret(editor, "\n", false, true);
//    }
  }


  private String escapeValue(Project project, String value) {
    char delimiter = PropertiesCodeStyleSettings.getInstance(project).getDelimiter();
    return PropertiesResourceBundleUtil.convertValueToFileFormat(value, delimiter, PropertyKeyValueFormat.MEMORY);
  }
}
