package dev.flikas.spring.boot.assistant.idea.plugin.completion;

import com.intellij.lang.Language;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.light.LightElement;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.index.MetadataItem;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.index.hint.Hint;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * A PsiElement that carrying a source object.
 * <p>
 * Created by Completion for each LookupElement, useful for InsertHandler, Documentation, etc.
 */
@ToString(of = "source")
public class SourceContainer extends LightElement {
  private final Object source;


  SourceContainer(@NotNull MetadataItem metadata, @NotNull Project project) {
    this(metadata, PsiManager.getInstance(project));
  }


  SourceContainer(@NotNull Hint metadata, @NotNull Project project) {
    this(metadata, PsiManager.getInstance(project));
  }


  private SourceContainer(@NotNull Object metadata, @NotNull PsiManager psiManager) {
    super(psiManager, Language.ANY);
    this.source = metadata;
    assert metadata instanceof MetadataItem || metadata instanceof Hint;
  }


  public Optional<MetadataItem> getSourceMetadataItem() {
    return source instanceof MetadataItem mi ? Optional.of(mi) : Optional.empty();
  }


  public Optional<Hint> getSourceHint() {
    return source instanceof Hint h ? Optional.of(h) : Optional.empty();
  }
}
