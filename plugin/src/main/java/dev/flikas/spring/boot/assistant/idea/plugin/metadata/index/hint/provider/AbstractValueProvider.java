package dev.flikas.spring.boot.assistant.idea.plugin.metadata.index.hint.provider;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.PrefixMatcher;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.source.ConfigurationMetadata;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.core.convert.ConversionService;

import java.util.Map;
import java.util.Objects;

abstract class AbstractValueProvider implements ValueProvider {
  private static final ConversionService conversionService = ApplicationConversionService.getSharedInstance();
  private final ConfigurationMetadata.Hint.ValueProvider metadata;


  AbstractValueProvider(ConfigurationMetadata.Hint.ValueProvider metadata) {
    this.metadata = metadata;
  }


  @Override
  public ConfigurationMetadata.Hint.ValueProvider getMetadata() {
    return this.metadata;
  }


  @NotNull
  protected PrefixMatcher getPrefixMatcher(@Nullable PrefixMatcher prefixMatcher) {
    return Objects.requireNonNullElse(prefixMatcher, PrefixMatcher.ALWAYS_TRUE);
  }


  @NotNull
  protected Project getProject(@NotNull CompletionParameters completionParameters) {
    return completionParameters.getPosition().getProject();
  }


  @NotNull
  protected Module getModule(@NotNull CompletionParameters completionParameters) {
    return Objects.requireNonNull(ModuleUtilCore.findModuleForPsiElement(completionParameters.getPosition()));
  }


  @Contract("_,_,!null->!null")
  @Nullable
  protected <T> T getParameter(String key, Class<T> type, @Nullable T defaultValue) {
    Map<String, Object> parameters = metadata.getParameters();
    if (parameters != null) {
      Object value = parameters.get(key);
      if (value == null) return defaultValue;
      if (conversionService.canConvert(value.getClass(), type)) {
        return conversionService.convert(value, type);
      } else {
        throw new IllegalArgumentException("Cannot convert value [" + value + "] to required type: " + type);
      }
    } else {
      return defaultValue;
    }
  }


  protected <T> T getRequiredParameter(String key, Class<T> type) {
    return Objects.requireNonNull(getParameter(key, type, null),
        "Parameter " + key + " is mandatory");
  }
}
