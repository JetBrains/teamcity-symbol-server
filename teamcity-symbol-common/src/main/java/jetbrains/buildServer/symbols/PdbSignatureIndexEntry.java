/*
 * Copyright 2000-2020 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
