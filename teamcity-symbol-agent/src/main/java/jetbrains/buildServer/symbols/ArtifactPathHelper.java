package jetbrains.buildServer.symbols;

import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.agent.impl.artifacts.ArchivePreprocessor;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by Evgeniy.Koshkin.
 */
public class ArtifactPathHelper {
  private static final String ARCHIVE_PATH_SEPARATOR = "!";
  private static final String ARCHIVE_PATH_SEPARATOR_FULL = "!/";
  private static final String FOLDER_SEPARATOR = "/";

  private final ExtensionHolder myExtensions;

  public ArtifactPathHelper(@NotNull final ExtensionHolder extensions) {
    myExtensions = extensions;
  }

  @NotNull
  String concatenateArtifactPath(@NotNull final String fileNamePrefix, @NotNull final String pdbFileName) {
    final String normalizedFileNamePrefix = fileNamePrefix.replace(ARCHIVE_PATH_SEPARATOR, ARCHIVE_PATH_SEPARATOR_FULL);
    if (StringUtil.isEmpty(normalizedFileNamePrefix)) {
      return pdbFileName;
    }

    final String archivePath = getArchivePath(normalizedFileNamePrefix);
    if (archivePath == null || normalizedFileNamePrefix.contains(ARCHIVE_PATH_SEPARATOR_FULL)) {
      return normalizedFileNamePrefix + FOLDER_SEPARATOR + pdbFileName;
    }

    return archivePath + ARCHIVE_PATH_SEPARATOR +
      StringUtil.trimStart(normalizedFileNamePrefix, archivePath) + FOLDER_SEPARATOR +
      pdbFileName;
  }

  @Nullable
  private String getArchivePath(@NotNull final String path) {
    for (ArchivePreprocessor preprocessor : myExtensions.getExtensions(ArchivePreprocessor.class)) {
      final String targetPath = preprocessor.getTargetKey(path);
      if (StringUtil.isEmpty(targetPath)) continue;
      return targetPath;
    }
    return null;
  }
}
