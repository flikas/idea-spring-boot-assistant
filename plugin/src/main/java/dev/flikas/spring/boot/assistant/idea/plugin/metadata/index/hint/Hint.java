package dev.flikas.spring.boot.assistant.idea.plugin.metadata.index.hint;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public record Hint(
    @NotNull String value,
    @Nullable String oneLineDescription,
    @Nullable String description,
    @Nullable Icon icon,
    @Nullable PsiElement psiElement,
    @Nullable Integer priorityGroup
) implements Comparable<Hint> {
  public Hint(@NotNull String value, @NotNull PsiElement psiElement) {
    this(value, null, null, null, psiElement, null);
  }


  public Hint(@NotNull String value, @NotNull PsiElement psiElement, int priorityGroup) {
    this(value, null, null, null, psiElement, priorityGroup);
  }


  public Hint(@NotNull String value, @NotNull Icon icon) {
    this(value, null, null, icon, null, null);
  }


  public Hint(@NotNull String value, @NotNull Icon icon, int priorityGroup) {
    this(value, null, null, icon, null, priorityGroup);
  }


  public Hint(
      @NotNull String value, @Nullable String oneLineDescription, @Nullable String description, @NotNull Icon icon) {
    this(value, oneLineDescription, description, icon, null, null);
  }


  public Hint(
      @NotNull String value, @Nullable String oneLineDescription, @Nullable String description, @NotNull Icon icon,
      int priorityGroup
  ) {
    this(value, oneLineDescription, description, icon, null, priorityGroup);
  }


  @Override
  public int compareTo(@NotNull Hint o) {
    return this.value.compareTo(o.value);
  }
}
