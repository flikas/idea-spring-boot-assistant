package dev.flikas.spring.boot.assistant.idea.plugin.metadata.index.platform;

import com.google.gson.Gson;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.indexing.DataIndexer;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.FileBasedIndexExtension;
import com.intellij.util.indexing.FileContent;
import com.intellij.util.indexing.ID;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.KeyDescriptor;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.index.MetadataProperty;
import dev.flikas.spring.boot.assistant.idea.plugin.metadata.source.PropertyName;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MetadataPropertyIndex extends FileBasedIndexExtension<PropertyName, MetadataProperty> {
  public static final String METADATA_FILE = "META-INF/spring-configuration-metadata.json";
  public static final String ADDITIONAL_METADATA_FILE = "META-INF/additional-spring-configuration-metadata.json";
  public static final ID<PropertyName, MetadataProperty> NAME = ID.create(
      MetadataPropertyIndex.class.getCanonicalName());

  private static final Logger LOG = Logger.getInstance(MetadataPropertyIndex.class);

  private final ThreadLocal<Gson> gson = ThreadLocal.withInitial(Gson::new);


  @Override
  public @NotNull ID<PropertyName, MetadataProperty> getName() {
    return NAME;
  }


  @Override
  public int getVersion() {
    return 0;
  }


  @Override
  public boolean dependsOnFileContent() {
    return true;
  }


  @Override
  public @NotNull DataIndexer<PropertyName, MetadataProperty, FileContent> getIndexer() {
    return new DataIndexer<>() {
      @Override
      public @NotNull Map<PropertyName, MetadataProperty> map(@NotNull FileContent inputData) {
        List<PsiDirectory> parents = PsiTreeUtil.collectParents(inputData.getPsiFile(), PsiDirectory.class,
            false, Objects::isNull);
        if (parents.size() == 1 && parents.getFirst().getName().equals("META-INF")) {

        }
        return Map.of();
      }
    };
  }


  @Override
  public @NotNull KeyDescriptor<PropertyName> getKeyDescriptor() {
    return new PropertyNameDescriptor();
  }


  @Override
  public @NotNull DataExternalizer<MetadataProperty> getValueExternalizer() {
    //TODO Implement this.
    return null;
  }


  @Override
  public @NotNull FileBasedIndex.InputFilter getInputFilter() {
    return file -> {
      Path path = file.toNioPath();
      if (file.isInLocalFileSystem()) {
        final String parent = "src/main/resources/";
        return path.endsWith(parent + METADATA_FILE) || path.endsWith(parent + ADDITIONAL_METADATA_FILE);
      } else {
        return path.endsWith(METADATA_FILE) || path.endsWith(ADDITIONAL_METADATA_FILE);
      }
    };
  }
}
