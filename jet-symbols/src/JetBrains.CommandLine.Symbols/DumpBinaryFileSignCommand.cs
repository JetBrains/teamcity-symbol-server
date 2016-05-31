// Decompiled with JetBrains decompiler
// Type: JetBrains.CommandLine.Symbols.DumpBinaryFileSignCommand
// Assembly: JetBrains.CommandLine.Symbols, Version=1.0.0.0, Culture=neutral, PublicKeyToken=1010a0d8d6380325
// MVID: EF046BF6-60AC-48EA-9121-8AF3D8D08853
// Assembly location: C:\Data\Work\TeamCity\misc\tc-symbol-server\tools\JetSymbols\JetBrains.CommandLine.Symbols.exe

using JetBrains.Metadata.Utils;
using JetBrains.Metadata.Utils.PE;
using JetBrains.Util;
using System;
using System.Collections.Generic;
using System.IO;

namespace JetBrains.CommandLine.Symbols
{
  public class DumpBinaryFileSignCommand : DumpFilesSignCommandBase
  {
    public const string CMD_NAME = "dumpBinSign";

    public DumpBinaryFileSignCommand(FileSystemPath outputFilePath, IEnumerable<FileSystemPath> targetFilePaths)
      : base(outputFilePath, targetFilePaths)
    {
    }

    protected override string GetFileSignature(FileSystemPath targetFilePath)
    {
      try
      {
        using (Stream stream = targetFilePath.OpenFileForReading())
        {
          PEFile peFile = new PEFile((IBinaryReader) new StreamBinaryReader(stream));
          return string.Format("{0:X}{1:X}", (object) peFile.COFFheader.TimeStamp, (object) peFile.NTHeader.ImageSize);
        }
      }
      catch (Exception ex)
      {
        Console.Error.WriteLine(ex.Message);
        return null;
      }
    }
  }
}
