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
import com.intellij.util.text.CharArrayUtil;
import net.jcip.annotations.ThreadSafe;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

@ThreadSafe
class PropertiesValueInsertHandler implements InsertHandler<LookupElement> {
  public static final PropertiesValueInsertHandler INSTANCE = new PropertiesValueInsertHandler();

  private static final char SINGLE_QUOTE = '\'';
  private static final char DOUBLE_QUOTE = '"';

  private static final char[] RESERVED_YAML_CHARS =
      {':', '{', '}', '[', ']', ',', '&', '*', '#', '?', '|', '-', '<', '>', '=', '!', '%', '@',
          '`'};


  private PropertiesValueInsertHandler() {}


  public static String unescapeValue(String value) {
    return value.replaceAll("^['\"]", "").replaceAll("['\"]$", "");
  }


  @Override
  public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement lookupElement) {
    Project project = context.getProject();
    PsiElement currentElement = context.getFile().findElementAt(context.getStartOffset());
    assert currentElement != null : "no element at " + context.getStartOffset();
    Property property = PsiTreeUtil.getParentOfType(currentElement, Property.class);
    if (property == null) return;

    String escaped = escapeValue(project, lookupElement.getLookupString());
    if (escaped.equals(lookupElement.getLookupString())) return;

    property.setValue(lookupElement.getLookupString(), PropertyKeyValueFormat.MEMORY);
  }


  private String escapeValue(Project project, String value) {
    char delimiter = PropertiesCodeStyleSettings.getInstance(project).getDelimiter();
    return PropertiesResourceBundleUtil.convertValueToFileFormat(value, delimiter, PropertyKeyValueFormat.MEMORY);
  }


  private boolean shouldUseQuotes(final LookupElement lookupElement) {
    return StringUtils.containsAny(lookupElement.getLookupString(), RESERVED_YAML_CHARS);
  }


  private boolean hasStartingOrEndingQuoteOfType(
      final InsertionContext insertionContext,
      final LookupElement lookupElement, final char quoteType
  ) {
    final int caretOffset = insertionContext.getEditor().getCaretModel().getOffset();
    final int startOfLookupStringOffset = caretOffset - lookupElement.getLookupString().length();


    final boolean hasStartingQuote =
        hasStartingQuote(insertionContext, quoteType, startOfLookupStringOffset);
    final boolean hasEndingQuote = hasEndingQuote(insertionContext, caretOffset, quoteType);

    return hasStartingQuote || hasEndingQuote;
  }


  private boolean hasEndingQuote(
      final InsertionContext insertionContext, final int caretOffset,
      final char quoteType
  ) {
    final CharSequence chars = insertionContext.getDocument().getCharsSequence();

    return CharArrayUtil.regionMatches(chars, caretOffset, String.valueOf(quoteType));
  }


  private boolean hasStartingQuote(
      final InsertionContext insertionContext, final char quoteType,
      final int startOfLookupStringOffset
  ) {
    return insertionContext.getDocument().getText().charAt(startOfLookupStringOffset - 1)
        == quoteType;
  }


  private void handleStartingQuote(
      final InsertionContext insertionContext,
      final LookupElement lookupElement, final char quoteType
  ) {
    final int caretOffset = insertionContext.getEditor().getCaretModel().getOffset();
    final int startOfLookupStringOffset = caretOffset - lookupElement.getLookupString().length();

    final boolean hasStartingQuote =
        hasStartingQuote(insertionContext, quoteType, startOfLookupStringOffset);

    if (!hasStartingQuote) {
      insertionContext.getDocument()
          .insertString(startOfLookupStringOffset, String.valueOf(quoteType));
    }
  }


  private void handleEndingQuote(final InsertionContext insertionContext, final char quoteType) {
    final int caretOffset = insertionContext.getEditor().getCaretModel().getOffset();

    final boolean hasEndingQuote = hasEndingQuote(insertionContext, caretOffset, quoteType);

    if (!hasEndingQuote) {
      insertionContext.getDocument().insertString(caretOffset, String.valueOf(quoteType));
    }
  }
}
