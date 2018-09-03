using System;
using System.Collections.Generic;
using System.IO;
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
        Console.Error.WriteLine("Unable to read indexed sources from PDF file {0}: {1} ", _symbolsFile, e.Message);
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

      foreach (var sourceFile in GetGenericPdbSourceFiles(pdb))
      {
        yield return sourceFile;
      }

      // Windows PDB files could not contains proper section with C++ file references,
      // they reference generated files instead, so we need to iterate values from /name stream.
      DebugInfoType debugInfoType;
      if (PdbUtils.TryGetPdbType(_symbolsFile, out debugInfoType) && debugInfoType == DebugInfoType.Windows)
      {
        using (Stream pdbStream = _symbolsFile.OpenFileForReading())
        {
          var windowsPdbFile = new WindowsPdbFile(pdbStream);
          foreach (var sourceName in windowsPdbFile.NameStream.Values)
          {
            yield return sourceName;            
          }
        }
      }
    }

    private IEnumerable<string> GetGenericPdbSourceFiles(ParsedPdb pdb)
    {
      if (pdb.DocumentNameToIndex != null)
      {
        return pdb.DocumentNameToIndex.Keys;
      }
      
      var types = pdb.Type2Files;
      if (types != null)
      {
        return types.AllValues;
      }
      
      return Enumerable.Empty<string>();
    }
  }
}
