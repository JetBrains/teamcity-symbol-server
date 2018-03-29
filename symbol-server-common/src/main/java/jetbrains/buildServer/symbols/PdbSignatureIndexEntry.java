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

    public PdbSignatureIndexEntry(@NotNull String guid, @NotNull String fileName, @Nullable String filePath) {
        myGuid = guid;
        myFileName = fileName;
        myArtifactPath = filePath;
    }

    @NotNull
    public String getGuid() {
        return myGuid;
    }

    @NotNull
    public String getFileName() {
        return myFileName;
    }

    @Nullable
    public String getArtifactPath() {
        return myArtifactPath;
    }
}
