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
          var root = new PdbFile(pdbStream).GetRoot();
          return string.Format("{0}{1:X}", root.SymId.ToString("N").ToUpperInvariant(), root.Age);
        }
      }
      catch (Exception ex)
      {
        Console.Error.WriteLine(ex);
        return null;
      }
    }
  }
}
