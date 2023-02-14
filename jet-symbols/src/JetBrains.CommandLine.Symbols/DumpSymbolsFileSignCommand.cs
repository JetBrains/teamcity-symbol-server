/*
 * Copyright 2000-2023 JetBrains s.r.o.
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

using JetBrains.Util;
using System;
using System.Collections.Generic;
using JetBrains.Metadata.Debug.Pdb;
using JetBrains.Metadata.Debug.Pdb.Dbi;
using JetBrains.Metadata.Utils;
using JetBrains.Metadata.Utils.PE.Directories;

namespace JetBrains.CommandLine.Symbols
{
  public class DumpSymbolsFileSignCommand : DumpFilesSignCommandBase
  {
    public const string CMD_NAME = "dumpSymbolSign";

    public DumpSymbolsFileSignCommand(FileSystemPath outputFilePath, ICollection<FileSystemPath> targetFilePaths)
      : base(outputFilePath, targetFilePaths)
    {
    }

    protected override string GetFileSignature(FileSystemPath targetFilePath)
    {
      if (!targetFilePath.ExistsFile)
      {
        Console.Error.WriteLine("PDB file does not exists " + targetFilePath);
        return null;
      }
      
      if (targetFilePath.GetFileLength() == 0)
      {
        Console.Error.WriteLine("Empty PDB file " + targetFilePath);
        return null;
      }

      if (PdbUtils.GetPdbType(targetFilePath) == DebugInfoType.Windows)
      {
        using (var pdbStream = targetFilePath.OpenFileForReading())
        {
          var pdbFile = new WindowsPdbFile(pdbStream);
          var dbiStream = pdbFile.GetDbiStream();
          var ageFromDbi = 1;
          if (dbiStream.Length > 0)
          {
            var binaryStream = new StreamBinaryReader(dbiStream);
            var dbiHeader = new DbiHeader(binaryStream);
            ageFromDbi = dbiHeader.PdbAge;
          }
          var root = pdbFile.GetRoot();
          var signature = root.PdbSignature;
          return string.Format("{0}{1:X}", signature.ToString("N").ToUpperInvariant(), ageFromDbi);
        }
      }

      var debugInfo = PdbUtils.TryGetPdbDebugInfo(targetFilePath);
      if (debugInfo == null)
      {
        Console.Error.WriteLine("Unsupport PDB file " + targetFilePath);
        return null;
      }

      return string.Format("{0}ffffffff", debugInfo.Signature.ToString("N").ToUpperInvariant());
    }
  }
}
