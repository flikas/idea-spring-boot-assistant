package dev.flikas.spring.boot.assistant.idea.plugin.misc;

import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface MutableReference<T> {
  @Nullable T dereference();

  static <R> MutableReference<R> immutable(R obj) {
    return () -> obj;
  }
}
