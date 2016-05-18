package jetbrains.buildServer.symbols;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by Evgeniy.Koshkin.
 */
public class PdbSignatureIndexEntry {
    private final String myGuid;
    private final String myFileName;
    private final String myArtifactPath;

    public PdbSignatureIndexEntry(@NotNull String guid, @NotNull String filePath, @Nullable String fileName) {
        myGuid = guid;
        myArtifactPath = filePath;
        myFileName = fileName;
    }

    @NotNull
    public String getGuid() {
        return myGuid;
    }

    @NotNull
    public String getArtifactPath() {
        return myArtifactPath;
    }

    @Nullable
    public String getFileName() {
        return myFileName;
    }
}
