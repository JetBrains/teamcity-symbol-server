// Decompiled with JetBrains decompiler
// Type: JetBrains.CommandLine.Symbols.DumpSymbolsFileSignCommand
// Assembly: JetBrains.CommandLine.Symbols, Version=1.0.0.0, Culture=neutral, PublicKeyToken=1010a0d8d6380325
// MVID: EF046BF6-60AC-48EA-9121-8AF3D8D08853
// Assembly location: C:\Data\Work\TeamCity\misc\tc-symbol-server\tools\JetSymbols\JetBrains.CommandLine.Symbols.exe

using JetBrains.Metadata.Utils.Pdb;
using JetBrains.Util;
using System;
using System.Collections.Generic;
using System.IO;

namespace JetBrains.CommandLine.Symbols
{
  public class DumpSymbolsFileSignCommand : DumpFilesSignCommandBase
  {
    public const string CMD_NAME = "dumpSymbolSign";

    public DumpSymbolsFileSignCommand(FileSystemPath outputFilePath, IEnumerable<FileSystemPath> targetFilePaths)
      : base(outputFilePath, targetFilePaths)
    {
    }

    protected override string GetFileSignature(FileSystemPath targetFilePath)
    {
      try
      {
        using (Stream pdbStream = targetFilePath.OpenFileForReading())
        {
          PdbRootRecord root = new PdbFile(pdbStream).GetRoot();
          return string.Format("{0}{1:X}", (object) root.SymId.ToString("N").ToUpperInvariant(), (object) root.Age);
        }
      }
      catch (Exception ex)
      {
        Console.Error.WriteLine((object) ex);
        return (string) null;
      }
    }
  }
}
