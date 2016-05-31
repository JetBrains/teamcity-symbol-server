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
