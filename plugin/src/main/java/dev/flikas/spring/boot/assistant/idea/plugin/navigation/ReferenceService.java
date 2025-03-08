package dev.flikas.spring.boot.assistant.idea.plugin.navigation;

import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.lang.properties.psi.Property;
import com.intellij.lang.properties.psi.impl.PropertyKeyImpl;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.DumbModeBlockedFunctionality;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiTreeUtil;
import dev.flikas.spring.boot.assistant.idea.plugin.filetype.SpringBootConfigurationPropertiesFileType;
import dev.flikas.spring.boot.assistant.idea.plugin.filetype.SpringBootConfigurationYamlFileType;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.index.MetadataGroup;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.index.MetadataProperty;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.service.ModuleMetadataService;
import org.apache.commons.collections4.MultiMapUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.collection.CompositeCollection;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YamlRecursivePsiElementVisitor;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@Service(Service.Level.PROJECT)
public final class ReferenceService {
  private final Project project;
  private final YamlIndexHolder yamlIndexHolder = new YamlIndexHolder();
  private final PropertiesIndexHolder propertiesIndexHolder = new PropertiesIndexHolder();


  public ReferenceService(Project project) {
    this.project = project;
  }


  public static ReferenceService getInstance(Project project) {
    return project.getService(ReferenceService.class);
  }

  public PsiReference forwardReference(PsiElement source) {
    return switch (source) {
      case YAMLKeyValue yamlKeyValue -> forwardReference(yamlKeyValue);
      case PropertyKeyImpl propertyKey -> forwardReference(propertyKey);
      default -> throw new IllegalStateException("Unexpected value: " + source);
    };
  }

  public PsiReference forwardReference(YAMLKeyValue yamlKeyValue) {
    return CachedValuesManager.getCachedValue(yamlKeyValue, () ->
        CachedValueProvider.Result.create(new YamlToPsiReference(yamlKeyValue), yamlKeyValue));
  }

  public PsiReference forwardReference(PropertyKeyImpl property) {
    return CachedValuesManager.getCachedValue(property, () ->
        CachedValueProvider.Result.create(new PropertiesToPsiReference(property), property));
  }


  @NotNull
  public Collection<PsiReference> backwardReference(PsiElement psiElement) {
    if (!(psiElement instanceof PsiField || psiElement instanceof PsiClass)) {
      return Collections.emptySet();
    }
    if (DumbService.isDumb(project)) {
      DumbService.getInstance(project)
          .showDumbModeNotificationForFunctionality("Index is not ready", DumbModeBlockedFunctionality.FindUsages);
      return Collections.emptySet();
    }
    String canonicalName = getCanonicalName(psiElement);

    MultiValuedMap<String, PsiReference> yamlIndex = refreshYamlIndex();
    Collection<PsiReference> yamlReferences = yamlIndex != null ? yamlIndex.get(canonicalName) : Set.of();
    MultiValuedMap<String, PsiReference> propertiesIndex = refreshPropertiesIndex();
    Collection<PsiReference> propertiesReferences =
        propertiesIndex != null ? propertiesIndex.get(canonicalName) : Set.of();
    return new CompositeCollection<>(yamlReferences, propertiesReferences);
  }


  private MultiValuedMap<String, PsiReference> refreshYamlIndex() {
    Collection<VirtualFile> files = DumbService.getInstance(project).runReadActionInSmartMode(
        () -> FileTypeIndex.getFiles(SpringBootConfigurationYamlFileType.INSTANCE,
            GlobalSearchScope.projectScope(project)));
    return this.yamlIndexHolder.getIndex(files);
  }

  private MultiValuedMap<String, PsiReference> refreshPropertiesIndex() {
    Collection<VirtualFile> files = DumbService.getInstance(project).runReadActionInSmartMode(
        () -> FileTypeIndex.getFiles(SpringBootConfigurationPropertiesFileType.INSTANCE,
            GlobalSearchScope.projectScope(project)));
    return this.propertiesIndexHolder.getIndex(files);
  }


  @Nullable
  private static String getCanonicalName(PsiElement element) {
    if (element instanceof PsiField) {
      PsiClass containingClass = ReadAction.compute(() -> ((PsiField) element).getContainingClass());
      if (containingClass == null) {
        //Not a standard java field, should not happen
        return null;
      }
      return ReadAction.compute(() -> containingClass.getQualifiedName() + "." + ((PsiField) element).getName());
    } else if (element instanceof PsiClass) {
      return ReadAction.compute(() -> ((PsiClass) element).getQualifiedName());
    } else {
      throw new UnsupportedOperationException();
    }
  }


  private abstract class AbstractIndexHolder<E extends PsiElement> extends UserDataHolderBase {
    private Collection<VirtualFile> files = Set.of();

    /**
     * @return A map for {@linkplain #getCanonicalName(PsiElement) canonical} name of a field or class to the YamlKeyValues in application.yaml
     */
    public MultiValuedMap<String, PsiReference> getIndex(Collection<VirtualFile> files) {
      dropCacheIfCollectionChanged(files);
      // If the files collection is empty, CachedValueProvider.Result.create will throw an error.
      if (files.isEmpty()) return MultiMapUtils.emptyMultiValuedMap();
      return CachedValuesManager.getManager(project).getCachedValue(this, () -> {
        // In this block, we return null at any error, makes this function is **Result equivalence**.
        MultiValuedMap<String, PsiReference> index = new HashSetValuedHashMap<>();
        PsiManager psiManager = PsiManager.getInstance(project);
        for (VirtualFile file : files) {
          if (!file.isValid()) continue;
          PsiFile psiFile = ReadAction.compute(() -> psiManager.findFile(file));
          if (psiFile == null) continue;
          Module module = ModuleUtil.findModuleForFile(psiFile);
          if (module == null) continue;
          ModuleMetadataService metadataService = module.getService(ModuleMetadataService.class);
          indexFile(psiFile, (key, src) -> {
            // find if any property matches this key
            MetadataProperty property = metadataService.getIndex().getProperty(key);
            if (property != null) {
              property.getSourceField()
                  .map(ReferenceService::getCanonicalName)
                  .filter(Objects::nonNull)
                  .ifPresent(cn -> index.put(cn, createReference(src)));
            }
            // find if any group matches this key
            MetadataGroup group = metadataService.getIndex().getGroup(key);
            if (group != null) {
              group.getType()
                  .map(ReferenceService::getCanonicalName)
                  .filter(Objects::nonNull)
                  .ifPresent(cn -> index.put(cn, createReference(src)));
            }
          });
        }
        return CachedValueProvider.Result.create(index, files);
      });
    }

    private void dropCacheIfCollectionChanged(Collection<VirtualFile> files) {
      Set<String> current = this.files.stream()
          .filter(VirtualFile::isValid)
          .map(VirtualFile::getUrl)
          .collect(Collectors.toSet());
      Set<String> newFiles = files.stream().filter(VirtualFile::isValid).map(VirtualFile::getUrl)
          .collect(Collectors.toSet());
      if (!newFiles.equals(current)) {
        this.clearUserData();
        this.files = files;
      }
    }


    protected abstract void indexFile(PsiFile file, BiConsumer<String, E> sourceConsumer);

    protected abstract PsiReference createReference(E source);
  }


  private class YamlIndexHolder extends AbstractIndexHolder<YAMLKeyValue> {
    @Override
    protected void indexFile(PsiFile file, BiConsumer<String, YAMLKeyValue> sourceConsumer) {
      assert file instanceof YAMLFile;
      file.accept(new YamlRecursivePsiElementVisitor() {
        @Override
        public void visitKeyValue(@NotNull YAMLKeyValue keyValue) {
          sourceConsumer.accept(ReadAction.compute(() -> YAMLUtil.getConfigFullName(keyValue)), keyValue);
          super.visitKeyValue(keyValue);
        }
      });
    }


    @Override
    protected PsiReference createReference(YAMLKeyValue source) {
      return forwardReference(source);
    }
  }


  private class PropertiesIndexHolder extends AbstractIndexHolder<Property> {
    @Override
    protected void indexFile(PsiFile file, BiConsumer<String, Property> sourceConsumer) {
      if (file instanceof PropertiesFile propertiesFile) {
        propertiesFile.getProperties().forEach(p -> {
          if (p instanceof Property property) sourceConsumer.accept(property.getUnescapedKey(), property);
        });
      }
    }


    @Override
    protected PsiReference createReference(Property source) {
      return forwardReference(PsiTreeUtil.findChildOfType(source, PropertyKeyImpl.class));
    }
  }
}
