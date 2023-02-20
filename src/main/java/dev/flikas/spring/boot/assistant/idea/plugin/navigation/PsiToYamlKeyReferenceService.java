package dev.flikas.spring.boot.assistant.idea.plugin.navigation;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import dev.flikas.spring.boot.assistant.idea.plugin.filetype.YamlPropertiesFileType;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.index.MetadataIndex;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.service.ModuleMetadataService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.psi.*;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

@Service
public final class PsiToYamlKeyReferenceService {
  private final Project project;
  /**
   * A map for {@linkplain #getCanonicalName(PsiElement) canonical} name of a field or class to the YamlKeyValues in application.yaml
   */
  private Map<String, Set<YamlKeyToNullReference>> index = new HashMap<>();


  public PsiToYamlKeyReferenceService(Project project) {
    this.project = project;

    DumbService.getInstance(project).runReadActionInSmartMode(this::reindex);
  }


  @NotNull
  public Collection<YamlKeyToNullReference> findReference(PsiElement psiElement) {
    if (!(psiElement instanceof PsiField || psiElement instanceof PsiClass)) {
      return Collections.emptySet();
    }
    return index.getOrDefault(getCanonicalName(psiElement), Collections.emptySet());
  }


  private synchronized void reindex() {
    Map<String, Set<YamlKeyToNullReference>> index = new HashMap<>();
    Collection<VirtualFile> files = FileTypeIndex.getFiles(YamlPropertiesFileType.INSTANCE, GlobalSearchScope.projectScope(project));
    PsiManager psiManager = PsiManager.getInstance(project);
    for (VirtualFile file : files) {
      if (!file.isValid()) continue;
      PsiFile psiFile = psiManager.findFile(file);
      if (!(psiFile instanceof YAMLFile)) continue;
      Module module = ModuleUtil.findModuleForFile(psiFile);
      if (module == null) continue;
      ModuleMetadataService metadataService = module.getService(ModuleMetadataService.class);
      for (YAMLKeyValue kv : YAMLUtil.getTopLevelKeys((YAMLFile) psiFile)) {
        indexYamlKey(index, metadataService, kv);
      }
    }
    this.index = index;
  }


  private void indexYamlKey(Map<String, Set<YamlKeyToNullReference>> index, ModuleMetadataService metadataService, YAMLKeyValue kv) {
    ProgressManager.checkCanceled();
    if (kv.getKey() == null) return;
    String fullName = YAMLUtil.getConfigFullName(kv);
    // find if any property matches this key
    MetadataIndex.Property property = metadataService.getIndex().getProperty(fullName);
    if (property != null) {
      PsiField sourceField = property.getSourceField();
      if (sourceField != null) {
        // It is wierd but ReferencesSearch uses the 'source element' not the 'target element' of the returned PsiReference.
        // So here we create a YamlKey2NullReference whose source is the target YamlKey.
        index.computeIfAbsent(getCanonicalName(sourceField), key -> new ConcurrentSkipListSet<>())
            .add(new YamlKeyToNullReference(kv));
      }
    }
    // find if any group matches this key
    MetadataIndex.Group group = metadataService.getIndex().getGroup(fullName);
    if (group != null) {
      PsiClass type = group.getType();
      if (type != null) {
        index.computeIfAbsent(getCanonicalName(type), key -> new ConcurrentSkipListSet<>())
            .add(new YamlKeyToNullReference(kv));
      }
    }
    //recursive into sub-keys
    @Nullable YAMLValue val = kv.getValue();
    if (val instanceof YAMLMapping) {
      ((YAMLMapping) val).getKeyValues().forEach(k -> indexYamlKey(index, metadataService, k));
    } else if (val instanceof YAMLSequence) {
      ((YAMLSequence) val).getItems().stream().flatMap(item -> item.getKeysValues().stream())
          .forEach(k -> indexYamlKey(index, metadataService, k));
    }
  }


  @Nullable
  private static String getCanonicalName(PsiElement element) {
    if (element instanceof PsiField) {
      PsiClass containingClass = ((PsiField) element).getContainingClass();
      if (containingClass == null) {
        //Not a standard java field, should not happen
        return null;
      }
      return containingClass.getQualifiedName() + "." + ((PsiField) element).getName();
    } else if (element instanceof PsiClass) {
      return ((PsiClass) element).getQualifiedName();
    } else {
      throw new UnsupportedOperationException();
    }
  }
}
