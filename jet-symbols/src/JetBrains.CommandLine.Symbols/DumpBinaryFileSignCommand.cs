using System;
using System.Collections.Generic;
using System.IO;
using JetBrains.Metadata.Utils;
using JetBrains.Metadata.Utils.PE;
using JetBrains.Util;

namespace JetBrains.CommandLine.Symbols
{
  public class DumpBinaryFileSignCommand : DumpFilesSignCommandBase
  {
    public const string CMD_NAME = "dumpBinSign";

    public DumpBinaryFileSignCommand(FileSystemPath outputFilePath, ICollection<FileSystemPath> targetFilePaths)
      : base(outputFilePath, targetFilePaths)
    {
    }

    protected override string GetFileSignature(FileSystemPath targetFilePath)
    {
      try
      {
        using (Stream stream = targetFilePath.OpenFileForReading())
        {
          var peFile = new PEFile(new StreamBinaryReader(stream));
          return string.Format("{0:X}{1:X}", peFile.COFFheader.TimeDateStamp, peFile.NTHeader.SizeOfImage);
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
