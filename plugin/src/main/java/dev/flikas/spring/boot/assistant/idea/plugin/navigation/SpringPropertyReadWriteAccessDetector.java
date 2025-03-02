package dev.flikas.spring.boot.assistant.idea.plugin.navigation;

import com.intellij.codeInsight.highlighting.JavaReadWriteAccessDetector;
import com.intellij.lang.properties.parsing.PropertiesTokenTypes;
import com.intellij.lang.properties.psi.Property;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import dev.flikas.spring.boot.assistant.idea.plugin.filetype.SpringBootConfigurationPropertiesFileType;
import dev.flikas.spring.boot.assistant.idea.plugin.filetype.SpringBootConfigurationYamlFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import java.util.Optional;

/**
 * This detector have to extends {@link JavaReadWriteAccessDetector},
 * because KotlinReadWriteAccessDetector decorate it.
 */
public class SpringPropertyReadWriteAccessDetector extends JavaReadWriteAccessDetector {
  @Override
  public boolean isReadWriteAccessible(@NotNull PsiElement element) {
    return element instanceof PsiField || super.isReadWriteAccessible(element);
  }

  @Override
  public @NotNull Access getReferenceAccess(@NotNull PsiElement referencedElement, @NotNull PsiReference reference) {
    return reference instanceof SpringPropertyToPsiReference
        ? Access.Write
        : super.getReferenceAccess(referencedElement, reference);
  }

  @Override
  public @NotNull Access getExpressionAccess(@NotNull PsiElement expression) {
    return isSpringProperty(expression)
        ? Access.Write
        : super.getExpressionAccess(expression);
  }

  private boolean isSpringProperty(PsiElement element) {
    VirtualFile vf = Optional.ofNullable(element.getContainingFile()).map(PsiFile::getVirtualFile).orElse(null);
    if (vf == null) return false;
    var vfm = FileTypeManager.getInstance();
    return (vfm.isFileOfType(vf, SpringBootConfigurationYamlFileType.INSTANCE) && element instanceof YAMLKeyValue)
        || (vfm.isFileOfType(vf, SpringBootConfigurationPropertiesFileType.INSTANCE)
                && (element instanceof Property
                        || element.getNode().getElementType() == PropertiesTokenTypes.KEY_CHARACTERS));
  }
}
