package dev.flikas.spring.boot.assistant.idea.plugin.documentation;

import com.intellij.icons.AllIcons;
import com.intellij.model.Pointer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.platform.backend.documentation.DocumentationResult;
import com.intellij.platform.backend.documentation.DocumentationTarget;
import com.intellij.platform.backend.presentation.TargetPresentation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.PlatformIcons;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.index.MetadataProperty;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.source.ConfigurationMetadata.Property.Deprecation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Collectors;

import static com.intellij.codeInsight.documentation.DocumentationManagerUtil.createHyperlink;
import static com.intellij.lang.documentation.DocumentationMarkup.CONTENT_END;
import static com.intellij.lang.documentation.DocumentationMarkup.CONTENT_START;
import static com.intellij.lang.documentation.DocumentationMarkup.DEFINITION_END;
import static com.intellij.lang.documentation.DocumentationMarkup.DEFINITION_START;
import static com.intellij.lang.documentation.DocumentationMarkup.SECTIONS_END;
import static com.intellij.lang.documentation.DocumentationMarkup.SECTIONS_START;
import static com.intellij.lang.documentation.DocumentationMarkup.SECTION_END;
import static com.intellij.lang.documentation.DocumentationMarkup.SECTION_HEADER_START;
import static com.intellij.lang.documentation.DocumentationMarkup.SECTION_SEPARATOR;
import static in.oneton.idea.spring.assistant.plugin.misc.GenericUtil.methodForDocumentationNavigation;
import static in.oneton.idea.spring.assistant.plugin.misc.GenericUtil.removeGenerics;
import static in.oneton.idea.spring.assistant.plugin.misc.GenericUtil.updateClassNameAsJavadocHtml;

@SuppressWarnings("UnstableApiUsage")
public class PropertyDocumentationTarget implements ProjectDocumentationTarget {
  private final MetadataProperty property;
  private final Project project;


  public PropertyDocumentationTarget(MetadataProperty property) {
    this.property = property;
    this.project = property.getIndex().getProject();
  }


  @Override
  public Project getProject() {
    return this.project;
  }


  @Override
  public @NotNull Pointer<? extends DocumentationTarget> createPointer() {
    return Pointer.delegatingPointer(Pointer.hardPointer(property), PropertyDocumentationTarget::new);
  }


  @Override
  public @NotNull TargetPresentation computePresentation() {
    String locationText = property
        .getSourceType()
        .map(PsiElement::getContainingFile)
        .map(PsiFile::getVirtualFile)
        .map(f -> ProjectFileIndex.getInstance(project).getOrderEntriesForFile(f))
        .map(l -> l.stream().map(OrderEntry::getPresentableName).distinct().collect(Collectors.joining(", ")))
        .orElse(null);
    return TargetPresentation.builder(property.getName())
        .icon(PlatformIcons.PROPERTY_ICON)
        .containerText(property.getMetadata().getSourceType())
        .locationText(locationText, AllIcons.Nodes.PpLibFolder)
        .presentation();
  }


  @Override
  public @Nullable DocumentationResult computeDocumentation() {
    // Format for the documentation is as follows
    /*
     * <p><b>a.b.c</b> ({@link com.acme.Generic}<{@link com.acme.Class1}, {@link com.acme.Class2}>)</p>
     * <p><em>Default Value</em> default value</p>
     * <p>Long description</p>
     * or of this type
     * <p><b>Type</b> {@link com.acme.Array}[]</p>
     * <p><b>Declared at</b>{@link com.acme.GenericRemovedClass#method}></p> <-- only for groups with method info
     * <b>WARNING:</b>
     * @deprecated Due to something something. Replaced by <b>c.d.e</b>
     */
    StringBuilder doc = new StringBuilder(DEFINITION_START);
    String className = property.getMetadata().getType();
    if (className != null) {
      int l = updateClassNameAsJavadocHtml(doc, className);
      if (l > 20) {
        doc.append('\n');
      } else {
        doc.append(' ');
      }
    }
    doc.append(property.getName());
    Object defaultValue = property.getMetadata().getDefaultValue();
    if (defaultValue != null) {
      doc.append(" = ").append(defaultValue);
    }
    doc.append(DEFINITION_END);

    String description = property.getMetadata().getDescription();
    if (description != null) {
      doc.append(CONTENT_START).append(description).append(CONTENT_END);
    }

    doc.append(SECTIONS_START);
    if (defaultValue != null) {
      doc.append(SECTION_HEADER_START)
          .append("<p style='white-space:nowrap'>Default value:</p>")
          .append(SECTION_SEPARATOR)
          .append(defaultValue)
          .append(SECTION_END);
    }

    String sourceType = property.getMetadata().getSourceType();
    if (sourceType != null) {
      String sourceTypeInJavadocFormat = removeGenerics(sourceType);
      // lets show declaration point only if does not match the type
      if (!sourceTypeInJavadocFormat.equals(removeGenerics(className))) {
        StringBuilder buffer = new StringBuilder();
        createHyperlink(buffer, methodForDocumentationNavigation(sourceTypeInJavadocFormat),
            sourceTypeInJavadocFormat, false
        );
        sourceTypeInJavadocFormat = buffer.toString();
        doc.append(SECTION_HEADER_START)
            .append("<p style='white-space:nowrap'>Declared at:</p>")
            .append(SECTION_SEPARATOR)
            .append(sourceTypeInJavadocFormat)
            .append(SECTION_END);
      }
    }

    Deprecation deprecation = property.getMetadata().getDeprecation();
    if (deprecation != null) {
      doc.append(SECTION_HEADER_START)
          .append("Deprecation:")
          .append(SECTION_SEPARATOR)
          .append("<p><b>")
          .append(deprecation.getLevel() == Deprecation.Level.ERROR ?
              "ERROR: DO NOT USE THIS PROPERTY AS IT IS COMPLETELY UNSUPPORTED" :
              "WARNING: PROPERTY IS DEPRECATED")
          .append("</b></p>");
      if (deprecation.getReason() != null) {
        doc.append("<p><b>Reason:</b> ").append(deprecation.getReason()).append("</p>");
      }
      if (deprecation.getReplacement() != null) {
        doc.append("<p>Replaced by property:<b> ").append(deprecation.getReplacement())
            .append("</b></p>");
      }
      doc.append(SECTION_END);
    }
    doc.append(SECTIONS_END);

    return DocumentationResult.documentation(doc.toString());
  }
}
