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
    private static final String ARCHIVE_DELIMITER = "!";
    private static final String FOLDER_DELIMITER = "/";

    private final ExtensionHolder myExtensions;

    public ArtifactPathHelper(@NotNull final ExtensionHolder extensions) {
        myExtensions = extensions;
    }

    @NotNull
    String concatenateArtifactPath(String fileNamePrefix, String pdbFileName) {
        final String delimiter = (isPathToArchive(fileNamePrefix) && !fileNamePrefix.contains(ARCHIVE_DELIMITER)) ? ARCHIVE_DELIMITER : FOLDER_DELIMITER;
        return fileNamePrefix + delimiter + pdbFileName;
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
