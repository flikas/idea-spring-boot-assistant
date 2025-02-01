package dev.flikas.spring.boot.assistant.idea.plugin.metadata.index.hint.provider;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.PrefixMatcher;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.index.hint.Hint;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.source.ConfigurationMetadata;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLMapping;
import org.jetbrains.yaml.psi.YAMLValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.intellij.openapi.project.DumbModeBlockedFunctionality.CodeCompletion;

/**
 * @see ConfigurationMetadata.Hint.ValueProvider.Type#LOGGER_NAME
 */
public class LoggerNameValueProvider extends AbstractValueProvider {
  private static final Logger LOG = Logger.getInstance(LoggerNameValueProvider.class);
  private static final String[] SPRING_PREDEFINED_LOGGER_GROUPS = {"web", "sql"};
  /**
   * Specify whether known groups should be considered. Default: true.
   */
  private final boolean group;


  LoggerNameValueProvider(ConfigurationMetadata.Hint.ValueProvider metadata) {
    super(metadata);
    this.group = getParameter("group", Boolean.class, true);
  }


  @Override
  public Collection<Hint> provideValues(
      @NotNull CompletionParameters completionParameters, @Nullable PrefixMatcher prefixMatcher
  ) {
    @NotNull PrefixMatcher matcher = getPrefixMatcher(prefixMatcher);
    // According to Spring's doc: "Typically, package and class names available in the current project can be auto-completed."
    Project project = getProject(completionParameters);
    DumbService dumbService = DumbService.getInstance(project);
    if (dumbService.isDumb()) {
      dumbService.showDumbModeNotificationForFunctionality("Completion", CodeCompletion);
      return Set.of();
    }
    enum Type {PACKAGE, CLASS, GROUP}
    Map<String, Type> candidates = new HashMap<>();
    //TODO We need an index to accelerate query all the packages and classes in the world,
    // this way takes too many time to give suggestions.
//    dumbService.runReadActionInSmartMode(() -> {
//      JavaShortClassNameIndex fqcnIndex = JavaShortClassNameIndex.getInstance();
//      fqcnIndex.processAllKeys(project, key -> {
//        ProgressManager.checkCanceled();
//        Collection<PsiClass> classes = fqcnIndex.getClasses(key, project, ProjectScope.getAllScope(project));
//        for (PsiClass aClass : classes) {
//          String fqcn = aClass.getQualifiedName();
//          if (fqcn == null) continue;
//          try {
//            if (!matcher.prefixMatches(fqcn)) continue;
//            candidates.put(fqcn, Type.CLASS);
//            char[] chars = fqcn.toCharArray();
//            for (int i = 0; i < chars.length; i++) {
//              if (chars[i] == '.') {
//                String pkg = new String(chars, 0, i);
//                if (!matcher.prefixMatches(pkg)) return true;
//                candidates.put(pkg, Type.PACKAGE);
//              }
//            }
//          } catch (Exception e) {
//            LOG.warn("class name " + fqcn + " cannot be processed", e);
//          }
//        }
//        return true;
//      });
//    });
    if (this.group) {
      // Spring predefined logger groups is in the value hint, we don't need add them here.
      // TODO Create an index for user-defined logger groups from yaml/properties configurations.
      // find logger groups in this yaml document, I know it is not good enough,
      // but this is easy to carry out and suitable for most of the use cases.
      getLoggerGroups(completionParameters).stream()
          .filter(matcher::prefixMatches)
          .forEach(g -> candidates.put(g, Type.GROUP));
    }
    List<Hint> suggestions = new ArrayList<>(candidates.size());
    candidates.forEach((name, type) -> {
      switch (type) {
        case PACKAGE -> suggestions.add(new Hint(name, AllIcons.Nodes.Package));
        case CLASS -> suggestions.add(new Hint(name, AllIcons.Nodes.Class));
        case GROUP -> suggestions.add(new Hint(name, "User-defined log group", null, AllIcons.Nodes.WebFolder));
      }
    });
    return suggestions;
  }


  private static Set<String> getLoggerGroups(CompletionParameters completionParameters) {
    if (completionParameters.getOriginalFile() instanceof YAMLFile yamlFile) {
      Set<String> groups = new HashSet<>();
      for (YAMLDocument document : yamlFile.getDocuments()) {
        YAMLKeyValue loggingGroup = YAMLUtil.getQualifiedKeyInDocument(document, List.of("logging", "group"));
        if (loggingGroup == null) continue;
        YAMLValue groupValue = loggingGroup.getValue();
        if (groupValue instanceof YAMLMapping groupMapping) {
          groupMapping.getKeyValues().stream().map(YAMLKeyValue::getKeyText).forEach(groups::add);
        }
      }
      return groups;
    }
    return Set.of();
  }
}
