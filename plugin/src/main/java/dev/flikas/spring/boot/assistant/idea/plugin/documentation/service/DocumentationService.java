package dev.flikas.spring.boot.assistant.idea.plugin.documentation.service;


import com.intellij.lang.documentation.DocumentationMarkup;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.HtmlBuilder;
import com.intellij.openapi.util.text.HtmlChunk;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJvmMember;
import com.intellij.psi.PsiType;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.index.MetadataGroup;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.index.MetadataItem;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.index.MetadataProperty;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.index.hint.Hint;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.source.ConfigurationMetadata;
import dev.flikas.spring.boot.assistant.idea.plugin.misc.PsiElementUtils;
import in.oneton.idea.spring.assistant.plugin.misc.GenericUtil;
import kotlin.Pair;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.util.Objects;
import java.util.Optional;

import static com.intellij.lang.documentation.DocumentationMarkup.BOTTOM_ELEMENT;
import static com.intellij.lang.documentation.DocumentationMarkup.CONTENT_ELEMENT;
import static com.intellij.lang.documentation.DocumentationMarkup.DEFINITION_ELEMENT;
import static com.intellij.lang.documentation.DocumentationMarkup.GRAYED_ELEMENT;
import static com.intellij.lang.documentation.DocumentationMarkup.SECTIONS_TABLE;
import static com.intellij.lang.documentation.DocumentationMarkup.SECTION_CONTENT_CELL;
import static com.intellij.lang.documentation.DocumentationMarkup.SECTION_HEADER_CELL;

@Service(Service.Level.PROJECT)
public final class DocumentationService {
  public static DocumentationService getInstance(Project project) {
    return project.getService(DocumentationService.class);
  }


  public String generateDoc(MetadataItem item) {
    if (item instanceof MetadataProperty property) {
      return generateDoc(property);
    } else if (item instanceof MetadataGroup group) {
      return generateDoc(group);
    } else {
      throw new IllegalArgumentException("Unsupported argument type: " + item.getClass());
    }
  }


  /**
   * Generate documentation is as follows:
   * <pre>{@code
   * <Property Type> <Property Name> = <Default Value>
   * ---
   * Deprecation Warning
   * ---
   * Description
   * Default Value: ...
   * ---
   * Declared At: ...
   * }</pre>
   */
  public String generateDoc(MetadataProperty property) {
    HtmlBuilder doc = new HtmlBuilder();
    HtmlChunk.Element def = DEFINITION_ELEMENT;
    Optional<PsiType> propertyType = property.getFullType();
    if (propertyType.isPresent()) {
      StringBuilder typeHtml = new StringBuilder();
      GenericUtil.updateClassNameAsJavadocHtml(typeHtml, propertyType.get().getCanonicalText());
      def = def.addRaw(typeHtml.toString()).child(HtmlChunk.br());
    }
    Pair<String, Icon> icon = property.getIcon();
    def = def.child(HtmlChunk.icon(icon.getFirst(), icon.getSecond()))
        .child(HtmlChunk.nbsp())
        .addText(property.getNameStr());
    Object defaultValue = property.getMetadata().getDefaultValue();
    if (defaultValue != null) {
      def = def.addText(" = ").addText(String.valueOf(defaultValue));
    }
    doc.append(def);

    HtmlChunk.Element body = CONTENT_ELEMENT;
    ConfigurationMetadata.Property.Deprecation deprecation = property.getMetadata().getDeprecation();
    if (deprecation != null) {
      HtmlChunk.Element dpc = CONTENT_ELEMENT;
      dpc = dpc.child(HtmlChunk.text(deprecation.getLevel() == ConfigurationMetadata.Property.Deprecation.Level.ERROR
          ? "ERROR: DO NOT USE THIS PROPERTY AS IT IS COMPLETELY UNSUPPORTED"
          : "WARNING: PROPERTY IS DEPRECATED").bold());
      HtmlChunk.Element table = SECTIONS_TABLE;
      if (deprecation.getReason() != null) {
        table = table.child(HtmlChunk.tag("tr")
            .children(SECTION_HEADER_CELL.addText("Reason:"), SECTION_CONTENT_CELL.addText(deprecation.getReason())));
      }
      if (deprecation.getReplacement() != null) {
        table = table.children(HtmlChunk.tag("tr").children(SECTION_HEADER_CELL.addText("Replaced by:"),
            SECTION_CONTENT_CELL.addText(deprecation.getReplacement())));
      }
      if (!table.isEmpty()) {
        dpc = dpc.child(table);
      }
      dpc = dpc.child(HtmlChunk.hr());
      body = body.child(dpc);
    }

    body = body.addRaw(property.getRenderedDescription());

    if (defaultValue != null) {
      body = body.child(SECTIONS_TABLE.child(SECTION_HEADER_CELL.addText("Default value:"))
          .child(SECTION_CONTENT_CELL.addText(String.valueOf(defaultValue))));
    }
    doc.append(body);

    property.getSourceField().ifPresent(field -> doc.hr().append(
        BOTTOM_ELEMENT.child(GRAYED_ELEMENT.addText("Declared at: "))
            .addRaw(PsiElementUtils.createLinkForDoc(field)))
    );

    return doc.toString();
  }


  /**
   * Generate documentation is as follows:
   * <pre>{@code
   * Group Name
   * ---
   * Description
   * ---
   * Declared At: ...
   * }</pre>
   */
  public String generateDoc(MetadataGroup group) {
    HtmlBuilder doc = new HtmlBuilder();
    HtmlChunk.Element def = DEFINITION_ELEMENT;
    Optional<PsiClass> type = group.getType();
    if (type.isPresent()) {
      def = def.addRaw(PsiElementUtils.createLinkForDoc(type.get())).addText("\n");
    }
    Pair<String, Icon> icon = group.getIcon();
    def = def.children(
        HtmlChunk.icon(icon.getFirst(), icon.getSecond()),
        HtmlChunk.nbsp(),
        HtmlChunk.text(group.getNameStr()));
    doc.append(def);

    doc.append(CONTENT_ELEMENT.addRaw(group.getRenderedDescription()));

    group.getSourceMethod()
        .map(m -> (PsiJvmMember) m)
        .or(group::getSourceType)
        .map(PsiElementUtils::createLinkForDoc)
        .filter(StringUtils::isNotBlank)
        .ifPresent(link -> doc.hr().append(DocumentationMarkup.BOTTOM_ELEMENT
            .child(DocumentationMarkup.GRAYED_ELEMENT.addText("Declared at: "))
            .addRaw(link)));

    return doc.toString();
  }


  public String generateDoc(Hint hint) {
    assert hint.icon() != null;
    HtmlChunk doc = HtmlChunk.fragment(
        DocumentationMarkup.DEFINITION_ELEMENT.addText(hint.value()),
        HtmlChunk.hr(),
        DocumentationMarkup.CONTENT_ELEMENT.addText(Objects.requireNonNullElse(hint.description(), "")));
    return doc.toString();
  }
}
