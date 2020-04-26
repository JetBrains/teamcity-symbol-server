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

    protected override FileSignature GetFileSignature(FileSystemPath targetFilePath)
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
          return FileSignature.Create(root.PdbSignature, ageFromDbi);
        }
      }

      var debugInfo = PdbUtils.TryGetPdbDebugInfo(targetFilePath);
      if (debugInfo != null)
      {
        return FileSignature.Create(debugInfo.Signature, 1);
      }

      Console.Error.WriteLine("Unsupported PDB file " + targetFilePath);
      return null;
    }
  }
}
