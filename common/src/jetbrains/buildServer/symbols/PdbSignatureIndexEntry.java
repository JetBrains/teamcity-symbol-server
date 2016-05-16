package jetbrains.buildServer.symbols;

import org.jetbrains.annotations.NotNull;

/**
 * Created by Evgeniy.Koshkin.
 */
public class PdbSignatureIndexEntry {
    private final String myGuid;
    private final String myArtifactPath;

    public PdbSignatureIndexEntry(@NotNull String guid, @NotNull String artifactPath) {
        myGuid = guid;
        myArtifactPath = artifactPath;
    }

    @NotNull
    public String getGuid() {
        return myGuid;
    }

    @NotNull
    public String getArtifactPath() {
        return myArtifactPath;
    }
}
