package dev.flikas.spring.boot.assistant.idea.plugin.filetype;

import com.intellij.openapi.fileTypes.LanguageFileType;
import icons.Icons;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLLanguage;

import javax.swing.*;

public class SpringBootConfigurationYamlFileType extends LanguageFileType {
  public static final SpringBootConfigurationYamlFileType INSTANCE = new SpringBootConfigurationYamlFileType();


  private SpringBootConfigurationYamlFileType() {
    super(YAMLLanguage.INSTANCE, true);
  }


  @Override
  public @NonNls @NotNull String getName() {
    return "spring-boot-properties-yaml";
  }


  @Override
  public @Nls @NotNull String getDisplayName() {
    return "Spring Boot Configuration YAML";
  }


  @Override
  public @NotNull String getDescription() {
    return "Spring configuration yaml file";
  }


  @Override
  public @NotNull String getDefaultExtension() {
    return "yaml";
  }


  @Override
  public @Nullable Icon getIcon() {
    return Icons.Yaml;
  }
}
