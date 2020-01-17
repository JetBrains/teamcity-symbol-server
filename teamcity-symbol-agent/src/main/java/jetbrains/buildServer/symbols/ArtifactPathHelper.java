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
