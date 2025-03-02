package dev.flikas.spring.boot.assistant.idea.plugin.filetype;

import com.intellij.icons.AllIcons;
import com.intellij.lang.properties.PropertiesLanguage;
import com.intellij.openapi.fileTypes.LanguageFileType;
import dev.flikas.spring.boot.assistant.idea.plugin.misc.CompositeIconUtils;
import icons.Icons;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class SpringBootConfigurationPropertiesFileType extends LanguageFileType {
  public static final SpringBootConfigurationPropertiesFileType INSTANCE = new SpringBootConfigurationPropertiesFileType();


  private SpringBootConfigurationPropertiesFileType() {
    super(PropertiesLanguage.INSTANCE, true);
  }


  @Override
  public @NonNls @NotNull String getName() {
    return "sba-spring-boot-configuration-properties";
  }


  @Override
  public @Nls @NotNull String getDisplayName() {
    return "Spring Boot Configuration Properties";
  }


  @Override
  public @NotNull String getDescription() {
    return "Spring Boot configuration properties file";
  }


  @Override
  public @NotNull String getDefaultExtension() {
    return "properties";
  }


  @Override
  public @Nullable Icon getIcon() {
    return CompositeIconUtils.createWithModifier(Icons.SpringBoot, AllIcons.FileTypes.Properties);
  }
}
