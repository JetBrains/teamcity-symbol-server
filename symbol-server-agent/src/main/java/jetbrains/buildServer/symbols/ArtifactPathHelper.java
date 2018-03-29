package jetbrains.buildServer.symbols;

import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.agent.impl.artifacts.ArchivePreprocessor;
import jetbrains.buildServer.util.CollectionsUtil;
import jetbrains.buildServer.util.filters.Filter;
import org.jetbrains.annotations.NotNull;

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
    String concatenateArtifactPath(String fileNamePrefix, String pdbFileName) {
      final String normilizedFileNamePrefix = fileNamePrefix.replace(ARCHIVE_PATH_SEPARATOR, ARCHIVE_PATH_SEPARATOR_FULL);
      final String delimiter = (isPathToArchive(normilizedFileNamePrefix) && !normilizedFileNamePrefix.contains(ARCHIVE_PATH_SEPARATOR)) ? ARCHIVE_PATH_SEPARATOR_FULL : FOLDER_SEPARATOR;
        return normilizedFileNamePrefix + delimiter + pdbFileName;
    }

    private boolean isPathToArchive(@NotNull final String path){
        return CollectionsUtil.contains(myExtensions.getExtensions(ArchivePreprocessor.class), new Filter<ArchivePreprocessor>() {
            @Override
            public boolean accept(@NotNull ArchivePreprocessor data) {
                return data.shouldProcess(path);
            }
        });
    }
}
