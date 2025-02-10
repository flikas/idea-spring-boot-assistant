package dev.flikas.spring.boot.assistant.idea.plugin.completion.properties;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.lang.properties.PropertiesLanguage;
import com.intellij.patterns.PlatformPatterns;
import dev.flikas.spring.boot.assistant.idea.plugin.filetype.SpringBootConfigurationPropertiesFileType;

import static com.intellij.patterns.PlatformPatterns.virtualFile;

public class SpringPropertiesCompletionContributor extends CompletionContributor {
  public SpringPropertiesCompletionContributor() {
    extend(
        CompletionType.BASIC,
        PlatformPatterns.psiElement().withLanguage(PropertiesLanguage.INSTANCE)
            .inVirtualFile(virtualFile().ofType(SpringBootConfigurationPropertiesFileType.INSTANCE)),
        new PropertiesCompletionProvider()
    );
  }
}
