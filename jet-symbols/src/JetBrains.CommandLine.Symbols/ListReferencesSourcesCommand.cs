

using System;
using System.Collections.Generic;
using System.Linq;
using JetBrains.Metadata.Debug.Pdb;
using JetBrains.Metadata.Utils.PE.Directories;
using JetBrains.Util;

namespace JetBrains.CommandLine.Symbols
{
  public class ListReferencesSourcesCommand : ICommand
  {
    private readonly FileSystemPath _symbolsFile;
    public const string CMD_NAME = "listSources";

    public ListReferencesSourcesCommand(FileSystemPath symbolsFile)
    {
      _symbolsFile = symbolsFile;
    }

    public int Execute()
    {
      if (!_symbolsFile.ExistsFile)
      {
        Console.Error.WriteLine("PDB file {0} does not exist.", _symbolsFile);
        return 1;
      }

      try
      {
        var sourceFiles = GetSourceFiles(_symbolsFile)
            .Where(p => p.IsNotEmpty())
            .Distinct(StringComparer.OrdinalIgnoreCase);

        foreach (var sourceFile in sourceFiles)
        {
          Console.Out.WriteLine(sourceFile);
        }
      }
      catch (Exception e)
      {
        Console.Error.WriteLine("Unable to read indexed sources from PDF file {0}: {1} ", _symbolsFile,
            e.Message);
        return 1;
      }

      return 0;
    }

    private IEnumerable<string> GetSourceFiles(FileSystemPath symbolsFile)
    {
      var pdb = PdbReader.ReadPdb(_symbolsFile, PdbParseLevel.None, null);
      if (pdb == null)
      {
        throw new Exception("Invalid PDB file " + _symbolsFile);
      }

      return pdb.DocumentNameToIndex.Keys
        .Concat(
          GetPdbSourceFiles(symbolsFile)
          .SelectMany(x => x));
    }

    private IEnumerable<IEnumerable<string>> GetPdbSourceFiles(FileSystemPath symbolsFile)
    {
      DebugInfoType debugInfoType;
      if (!PdbUtils.TryGetPdbType(_symbolsFile, out debugInfoType)) yield break;

      switch (debugInfoType)
      {
        case DebugInfoType.Windows:
        {
          using (var fileStream = symbolsFile.OpenFileForReading())
          {
            var windowsPdbFile = new WindowsPdbFile(fileStream);
            // Windows PDB files could not contains proper section with C++ file references,
            // they reference generated files instead, so we need to iterate values from /name stream.
            yield return windowsPdbFile.NameStream.Values;

            yield return PdbUtils.GetTypeToFilesMapping(windowsPdbFile, null).AllValues;
          }

          break;
        }

        case DebugInfoType.Portable:
        case DebugInfoType.EmbeddedPortable:
        {
          using (var cookie = PortablePdbFileCookie.Create(symbolsFile, debugInfoType, null))
          {
            yield return cookie.Pdb?.Type2Files?.AllValues ?? Enumerable.Empty<string>();
          }

          break;
        }
      }
    }
  }
}
