package dev.flikas.spring.boot.assistant.idea.plugin.documentation;

import com.intellij.codeInsight.documentation.DocumentationManager;
import com.intellij.codeInsight.documentation.DocumentationManagerUtil;
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
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.index.MetadataGroup;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.source.ConfigurationMetadata;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Collectors;

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
public abstract class GroupDocumentationTarget implements ProjectDocumentationTarget {
  private final MetadataGroup group;
  private final @NotNull Project project;


  public static GroupDocumentationTarget[] createTargets(MetadataGroup group) {
    //Unfortunately, even though there is a 'description' field for the group metadata, `spring boot configuration processor` will never fill it.
    //So, it is better to use group type's document instead.
    return new GroupDocumentationTarget[]{new FromSource(group), new FromMeta(group)};
  }


  public GroupDocumentationTarget(MetadataGroup group) {
    this.group = group;
    this.project = group.getIndex().getProject();
  }


  @Override
  public @NotNull Project getProject() {
    return this.project;
  }


  @Override
  public @NotNull TargetPresentation computePresentation() {
    String locationText = group
        .getSourceType()
        .map(PsiElement::getContainingFile)
        .map(PsiFile::getVirtualFile)
        .map(f -> ProjectFileIndex.getInstance(project).getOrderEntriesForFile(f))
        .map(l -> l.stream().map(OrderEntry::getPresentableName).distinct().collect(Collectors.joining(", ")))
        .orElse(null);
    return TargetPresentation.builder(group.getName())
        .icon(PlatformIcons.PROPERTY_ICON)
        .containerText(group.getMetadata().getSourceType())
        .locationText(locationText, AllIcons.Nodes.PpLibFolder)
        .presentation();
  }


  public static class FromSource extends GroupDocumentationTarget {
    public FromSource(MetadataGroup group) {
      super(group);
    }


    @Override
    public @NotNull Pointer<? extends DocumentationTarget> createPointer() {
      return Pointer.delegatingPointer(Pointer.hardPointer(super.group), FromSource::new);
    }


    @SuppressWarnings("removal")
    @Override
    public @Nullable DocumentationResult computeDocumentation() {
      StringBuilder doc = new StringBuilder();
      super.group.getSourceMethod()
          .<PsiElement>map(m -> m)
          .or(super.group::getSourceType)
          .ifPresent(e -> doc.append(DocumentationManager.getProviderFromElement(e).generateDoc(e, null)));
      if (doc.isEmpty()) return null;
      return DocumentationResult.documentation(doc.toString());
    }
  }


  public static class FromMeta extends GroupDocumentationTarget {

    public FromMeta(MetadataGroup group) {
      super(group);
    }


    @Override
    public @NotNull Pointer<? extends DocumentationTarget> createPointer() {
      return Pointer.delegatingPointer(Pointer.hardPointer(super.group), FromMeta::new);
    }


    @Override
    public @Nullable DocumentationResult computeDocumentation() {
      ConfigurationMetadata.Group meta = super.group.getMetadata();
      StringBuilder doc = new StringBuilder();
      // Otherwise, format for the documentation is as follows
      /*
       * {@link com.acme.Generic}<{@link com.acme.Class1}, {@link com.acme.Class2}>
       * a.b.c
       * ---
       * Long description
       */
      doc.append(DEFINITION_START);
      String className = meta.getType();
      if (className != null) {
        int l = updateClassNameAsJavadocHtml(doc, className);
        if (l > 20) {
          doc.append('\n');
        } else {
          doc.append(' ');
        }
      }
      doc.append(super.group.getName())
          .append(DEFINITION_END);
      if (meta.getDescription() != null) {
        doc.append(CONTENT_START).append(meta.getDescription()).append(CONTENT_END);
      }

      // Append "Declared at" section as follows:
      // Declared at: {@link com.acme.GenericRemovedClass#method}> <-- only for groups with method info
      String sourceType = meta.getSourceType();
      if (sourceType != null) {
        String sourceTypeInJavadocFormat = removeGenerics(sourceType);
        String sourceMethod = meta.getSourceMethod();
        if (sourceMethod != null) {
          sourceTypeInJavadocFormat += "." + sourceMethod;
        }

        // lets show declaration point only if does not match the type
        if (!sourceTypeInJavadocFormat.equals(removeGenerics(className))) {
          StringBuilder buffer = new StringBuilder();
          DocumentationManagerUtil.createHyperlink(
              buffer,
              methodForDocumentationNavigation(sourceTypeInJavadocFormat),
              sourceTypeInJavadocFormat,
              false
          );
          sourceTypeInJavadocFormat = buffer.toString();
          doc.append(SECTIONS_START)
              .append(SECTION_HEADER_START)
              .append("<span style='white-space:nowrap'>Declared at:</span>")
              .append(SECTION_SEPARATOR)
              .append(sourceTypeInJavadocFormat)
              .append(SECTION_END)
              .append(SECTIONS_END);
        }
      }

      return DocumentationResult.documentation(doc.toString());
    }
  }
}
