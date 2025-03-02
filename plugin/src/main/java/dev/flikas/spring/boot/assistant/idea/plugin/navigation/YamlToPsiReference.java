package dev.flikas.spring.boot.assistant.idea.plugin.navigation;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.source.PropertyName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import java.util.Iterator;

class YamlToPsiReference extends SpringPropertyToPsiReference<YAMLKeyValue> {
  YamlToPsiReference(@NotNull YAMLKeyValue source) {
    super(source);
  }

  @Override
  protected Iterator<String> candidateKeys(YAMLKeyValue key) {
    PropertyName fullKey = PropertyName.adapt(YAMLUtil.getConfigFullName(key));
    PropertyName thisKey = PropertyName.adapt(key.getKeyText());
    assert fullKey.toString().endsWith(thisKey.toString());
    PropertyName prefix = fullKey.chop(fullKey.getNumberOfElements() - thisKey.getNumberOfElements());
    return new Iterator<>() {
      private PropertyName next = thisKey;

      @Override
      public boolean hasNext() {
        return !next.isEmpty();
      }

      @Override
      public String next() {
        PropertyName n = next;
        next = next.getParent();
        return prefix.append(n).toString();
      }
    };
  }


  @Override
  protected TextRange calculateDefaultRangeInElement() {
    PsiElement key = getElement().getKey();
    if (key != null) {
      return key.getTextRangeInParent();
    }
    return super.calculateDefaultRangeInElement();
  }
}
