using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using JetBrains.Metadata.Debug.Pdb;
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
        var pdb = PdbReader.ReadPdb(_symbolsFile, PdbParseLevel.None, null);
        if (pdb == null)
        {
          Console.Error.WriteLine("Invalid PDB file " + _symbolsFile);
          return 1;
        }
        
        foreach (var sourceFile in GetSourceFiles(pdb))
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

    private IEnumerable<string> GetSourceFiles(ParsedPdb pdb)
    {
      if (pdb.DocumentNameToIndex != null)
      {
        return pdb.DocumentNameToIndex.Keys;
      }
      
      var types = pdb.Type2Files;
      if (types != null)
      {
        return types.AllValues.Distinct();
      }
      
      return Enumerable.Empty<string>();
    }
  }
}
