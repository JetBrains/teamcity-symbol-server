package jetbrains.buildServer.symbols;

import jetbrains.buildServer.util.ArchiveType;
import jetbrains.buildServer.util.ArchiveUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Evgeniy.Koshkin.
 */
public class ArtifactPathHelper {
    private static final String ARCHIVE_PATH_SEPARATOR = "!";
    private static final String ARCHIVE_PATH_SEPARATOR_FULL = "!/";
    private static final String FOLDER_SEPARATOR = "/";

    @NotNull
    static String concatenateArtifactPath(String fileNamePrefix, String pdbFileName) {
      final String normilizedFileNamePrefix = fileNamePrefix.replace(ARCHIVE_PATH_SEPARATOR, ARCHIVE_PATH_SEPARATOR_FULL);
      final String delimiter = (isPathToArchive(normilizedFileNamePrefix) && !normilizedFileNamePrefix.contains(ARCHIVE_PATH_SEPARATOR)) ? ARCHIVE_PATH_SEPARATOR_FULL : FOLDER_SEPARATOR;
        return normilizedFileNamePrefix + delimiter + pdbFileName;
    }

    static private boolean isPathToArchive(@NotNull final String path){
        return ArchiveUtil.getArchiveType(path) != ArchiveType.NOT_ARCHIVE;
    }
}
