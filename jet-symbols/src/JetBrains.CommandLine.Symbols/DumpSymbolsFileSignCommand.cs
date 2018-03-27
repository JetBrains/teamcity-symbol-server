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
      
      var debugInfo = PdbUtils.TryGetPdbDebugInfo(targetFilePath);
      if (debugInfo == null)
      {
        Console.Error.WriteLine("Unsupport PDB file " + targetFilePath);
        return null;
      }

      return string.Format("{0}{1:X}", debugInfo.Signature.ToString("N").ToUpperInvariant(), debugInfo.AgeOrTimestamp);
    }
  }
}
