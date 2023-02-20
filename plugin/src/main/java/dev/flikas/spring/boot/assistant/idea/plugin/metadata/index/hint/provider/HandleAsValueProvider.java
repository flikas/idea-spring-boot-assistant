package dev.flikas.spring.boot.assistant.idea.plugin.metadata.index.hint.provider;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.PrefixMatcher;
import com.intellij.codeInsight.completion.impl.CamelHumpMatcher;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiEnumConstant;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileInfoManager;
import com.intellij.util.ReflectionUtil;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.index.hint.Hint;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.source.ConfigurationMetadata;
import dev.flikas.spring.boot.assistant.idea.plugin.misc.PsiTypeUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.java.JavaResourceRootType;

import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * @see ConfigurationMetadata.Hint.ValueProvider.Type#HANDLE_AS
 */
public class HandleAsValueProvider extends AbstractValueProvider {
  private static final Pattern COLLECTION_REMOVAL_PATTERN = Pattern.compile("[\\w.]+<([\\w.]+)>");
  private static final Map<String, Handler> handlers = Map.of(
      "java.nio.charset.Charset", new CharsetHandler(),
      "java.util.Locale", new LocaleHandler(),
      "org.springframework.util.MimeType", new MimeTypeHandler(),
      "org.springframework.core.io.Resource", new ResourceHandler());
  @NotNull private final String targetFQN;


  HandleAsValueProvider(ConfigurationMetadata.Hint.ValueProvider metadata) {
    super(metadata);

    this.targetFQN = getRequiredParameter("target", String.class);
  }


  public static Handler getHandler(String type) {
    // Removes potential collection or array type, we only need the value part of the type.
    Matcher matcher = COLLECTION_REMOVAL_PATTERN.matcher(type);
    if (matcher.matches()) {
      type = matcher.group(1);
    } else {
      type = StringUtils.removeEnd(type, "[]");
    }
    return handlers.getOrDefault(type, new DefaultHandler(type));
  }


  @Override
  public Collection<Hint> provideValues(
      @NotNull CompletionParameters completionParameters, @Nullable PrefixMatcher prefixMatcher
  ) {
    return getHandler(this.targetFQN).handle(completionParameters, prefixMatcher);
  }


  @FunctionalInterface
  public interface Handler {
    Collection<Hint> handle(@NotNull CompletionParameters completionParameters, @Nullable PrefixMatcher prefixMatcher);
  }


  private static class CharsetHandler implements Handler {
    private static final int TOP_PRIORITY = Integer.MAX_VALUE;
    private static final List<Hint> STANDARD_CHARSETS = ReflectionUtil.collectFields(StandardCharsets.class)
        .stream()
        .filter(f -> Modifier.isStatic(f.getModifiers()) && f.canAccess(null))
        .filter(f -> f.getType().equals(Charset.class))
        .map(f -> (Charset) ReflectionUtil.getFieldValue(f, null))
        .filter(Objects::nonNull)
        .map(cs -> toHint(cs, TOP_PRIORITY))
        .toList();


    @Override
    public Collection<Hint> handle(
        @NotNull CompletionParameters completionParameters, @Nullable PrefixMatcher prefixMatcher) {
      List<Hint> result = new ArrayList<>();
      PrefixMatcher matcher = Objects.requireNonNullElse(prefixMatcher, PrefixMatcher.ALWAYS_TRUE);
      // Standard charsets is the first priority
      STANDARD_CHARSETS.stream().filter(h -> matcher.prefixMatches(h.value())).forEach(result::add);
      // Try non-standard charsets, but it maybe not suitable for user
      // Because this is from the IDE's charset, not the user's spring application
      // Try Charsets that is IANA registered first, then all the others
      Charset.availableCharsets().values().stream()
          .filter(cs -> matcher.prefixMatches(cs.name()))
          .filter(Charset::isRegistered)
          .map(cs -> toHint(cs, TOP_PRIORITY - (cs.isRegistered() ? 1 : 2)))
          .forEach(result::add);
      return result;
    }


    private static Hint toHint(Charset cs, int priorityGroup) {
      return new Hint(cs.name(), cs.displayName(), null, AllIcons.Nodes.Class, priorityGroup);
    }
  }


  private static class LocaleHandler implements Handler {
    @Override
    public Collection<Hint> handle(
        @NotNull CompletionParameters completionParameters, @Nullable PrefixMatcher prefixMatcher) {
      // The return value must be convertible by {@link org.springframework.core.convert.support.StringToLocaleConverter}
      PrefixMatcher matcher = Objects.requireNonNullElse(prefixMatcher, PrefixMatcher.ALWAYS_TRUE);
      // But this is the locals that installed on the developer's computer,
      // may not suitable for he/she's application. Consider using all the ISO standard locale names?
      return Locale.availableLocales()
          .map(Locale::stripExtensions)
          .distinct()
          .filter(l -> StringUtils.isNotBlank(l.getLanguage()) && matcher.prefixMatches(l.toString()))
          .map(l -> {
            int priority;
            if (!StringUtils.isAllBlank(l.getVariant(), l.getScript())) {
              priority = 2;
            } else if (StringUtils.isNoneBlank(l.getLanguage(), l.getCountry())) {
              priority = 0;
            } else {
              priority = 1;
            }
            return new Hint(l.toString(), l.getDisplayName(), l.getDisplayName(), AllIcons.Nodes.Enum,
                Integer.MAX_VALUE - priority);
          }).toList();
    }
  }


  private static class MimeTypeHandler implements Handler {
    //Use IANA MediaTypes? https://www.iana.org/assignments/media-types/media-types.xhtml
    private static final List<String> COMMON_MIME_TYPES = List.of(
        "application/atom+xml",
        "application/cbor",
        "application/x-www-form-urlencoded",
        "application/graphql+json",
        "application/graphql-response+json",
        "application/json",
        "application/octet-stream",
        "application/pdf",
        "application/problem+json",
        "application/problem+xml",
        "application/x-protobuf",
        "application/rss+xml",
        "application/x-ndjson",
        "application/stream+json",
        "application/xhtml+xml",
        "application/xml",
        "image/gif",
        "image/jpeg",
        "image/png",
        "multipart/form-data",
        "multipart/mixed",
        "multipart/related",
        "text/event-stream",
        "text/html",
        "text/markdown",
        "text/plain",
        "text/xml");


    @Override
    public Collection<Hint> handle(
        @NotNull CompletionParameters completionParameters, @Nullable PrefixMatcher prefixMatcher
    ) {
      PrefixMatcher matcher = Objects.requireNonNullElse(prefixMatcher, PrefixMatcher.ALWAYS_TRUE);
      return COMMON_MIME_TYPES.stream()
          .filter(matcher::prefixMatches)
          .map(t -> new Hint(t, null, null, AllIcons.Nodes.Enum))
          .toList();
    }
  }


  /**
   * @see org.springframework.core.io.ResourceLoader#getResource(String)
   * @see org.springframework.core.io.support.ResourcePatternResolver
   */
  private static class ResourceHandler implements Handler {
    @Override
    public Collection<Hint> handle(
        @NotNull CompletionParameters completionParameters, @Nullable PrefixMatcher prefixMatcher
    ) {
      PrefixMatcher matcher = Objects.requireNonNullElse(prefixMatcher, PrefixMatcher.ALWAYS_TRUE);
      // If user ask for suggestion with empty String, we provide suggestion of 'classpath',
      // this the only kind of resource we can provide completion.
      String queryString = matcher.getPrefix();
      if (StringUtils.isBlank(queryString) || "classpath".startsWith(queryString.toLowerCase())) {
        List<String> matches = Stream.of("classpath:", "classpath*:").filter(matcher::prefixMatches).toList();
        if (!matches.isEmpty()) {
          return matches.stream().map(s -> new Hint(s, null, null, AllIcons.Nodes.Enum)).toList();
        }
      }
      // Otherwise, we provide suggestion of files in resource roots
      Module module = ModuleUtilCore.findModuleForPsiElement(completionParameters.getPosition());
      if (module == null) return List.of();
      HashSet<Module> allModules = new HashSet<>();
      ModuleUtilCore.getDependencies(module, allModules);
      List<VirtualFile> sourceRoots = allModules.stream()
          .flatMap(m -> ModuleRootManager.getInstance(m).getSourceRoots(JavaResourceRootType.RESOURCE).stream())
          .toList();
      String prefix;
      if (queryString.startsWith("classpath")) {
        int delimiterIndex = queryString.indexOf(':');
        if (delimiterIndex > 0) {
          prefix = queryString.substring(0, delimiterIndex + 1);
          queryString = queryString.substring(delimiterIndex + 1);
        } else {
          prefix = "classpath:";
        }
      } else {
        prefix = "classpath:";
      }
      queryString = StringUtils.removeStart(queryString, '/');
      PrefixMatcher filePathMatcher = new CamelHumpMatcher(queryString, false);
      PsiManager psiManager = completionParameters.getPosition().getManager();
      Set<Hint> hints = Collections.newSetFromMap(new ConcurrentHashMap<>());
      sourceRoots.parallelStream()
          .forEach(root -> VfsUtil.iterateChildrenRecursively(root, VirtualFile::isValid, fileOrDir -> {
            if (fileOrDir.isDirectory()) return true;
            String relPath = VfsUtilCore.getRelativePath(fileOrDir, root);
            assert relPath != null;
            if (filePathMatcher.prefixMatches(relPath)) {
              PsiFile f = ReadAction.compute(() -> psiManager.findFile(fileOrDir));
              if (f != null) {
                ReadAction.run(() -> {
                  String fileAdditionalInfo = FileInfoManager.getFileAdditionalInfo(f);
                  hints.add(new Hint(prefix + relPath, fileAdditionalInfo, fileAdditionalInfo, f.getIcon(0)));
                });
              }
            }
            return true;
          }));
      return hints;
    }
  }


  /**
   * Handle suggestions without any hint.
   * <p>
   * For example, by enum fields or boolean(true/false).
   */
  @SuppressWarnings("ClassCanBeRecord")
  private static class DefaultHandler implements Handler {
    private final String type;


    private DefaultHandler(String type) {
      this.type = type;
    }


    @Override
    public Collection<Hint> handle(
        @NotNull CompletionParameters completionParameters, @Nullable PrefixMatcher prefixMatcher
    ) {
      PrefixMatcher matcher = Objects.requireNonNullElse(prefixMatcher, PrefixMatcher.ALWAYS_TRUE);
      if ("java.lang.Boolean".equals(this.type)) {
        return Stream.of("true", "false")
            .filter(matcher::prefixMatches)
            .map(s -> new Hint(s, AllIcons.Nodes.Constant))
            .toList();
      } else {
        Optional<PsiClass> propType = getUpperBoundClass(completionParameters.getEditor().getProject());
        if (propType.filter(PsiClass::isEnum).isPresent()) {
          return Arrays.stream(propType.get().getFields())
              .filter(PsiEnumConstant.class::isInstance)
              .filter(f -> matcher.prefixMatches(f.getName()))
              .map(f -> new Hint(f.getName(), f))
              .toList();
        }
        //As the last handler, we return empty list when we don't support this type.
        return List.of();
      }
    }


    private Optional<PsiClass> getUpperBoundClass(Project project) {
      return Optional.ofNullable(PsiTypeUtils.findClass(project, this.type));
    }
  }
}
