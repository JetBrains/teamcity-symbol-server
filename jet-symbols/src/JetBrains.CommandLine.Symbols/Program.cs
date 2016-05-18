// Decompiled with JetBrains decompiler
// Type: JetBrains.CommandLine.Symbols.Program
// Assembly: JetBrains.CommandLine.Symbols, Version=1.0.0.0, Culture=neutral, PublicKeyToken=1010a0d8d6380325
// MVID: EF046BF6-60AC-48EA-9121-8AF3D8D08853
// Assembly location: C:\Data\Work\TeamCity\misc\tc-symbol-server\tools\JetSymbols\JetBrains.CommandLine.Symbols.exe

using JetBrains.Util;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;

namespace JetBrains.CommandLine.Symbols
{
  internal static class Program
  {
    private const int ERROR_EXIT_CODE = 1;

    public static int Main(string[] args)
    {
      try
      {
        string error;
        ICommand commandToExecute = Program.GetCommandToExecute(args, out error);
        if (commandToExecute != null)
          return commandToExecute.Execute();
        Program.PrintIncorrectUsageMessage(error);
        return 1;
      }
      catch (Exception ex)
      {
        Console.Error.WriteLine((object) ex);
        return 1;
      }
    }

    private static ICommand GetCommandToExecute(string[] args, out string error)
    {
      error = string.Empty;
      string str = ((IEnumerable<string>) args).First<string>();
      FileSystemPath inputFilePath = FileSystemPath.TryParse(args[2].Substring(3));
      FileSystemPath outputFilePath = FileSystemPath.TryParse(args[1].Substring(3));
      IEnumerable<FileSystemPath> targetFilePaths = Program.LoadPathsFromFile(inputFilePath);
      switch (str)
      {
        case "dumpSymbolSign":
          return (ICommand) new DumpSymbolsFileSignCommand(outputFilePath, targetFilePaths);
        case "dumpBinSign":
          return (ICommand) new DumpBinaryFileSignCommand(outputFilePath, targetFilePaths);
        default:
          error = string.Format("{0} command is unknown.", (object) str);
          return (ICommand) null;
      }
    }

    private static IEnumerable<FileSystemPath> LoadPathsFromFile(FileSystemPath inputFilePath)
    {
      ICollection<FileSystemPath> result = (ICollection<FileSystemPath>) new HashSet<FileSystemPath>();
      inputFilePath.ReadTextStream((Action<StreamReader>) (streamReader =>
      {
        while (!streamReader.EndOfStream)
        {
          FileSystemPath fileSystemPath = FileSystemPath.TryParse(streamReader.ReadLine());
          if (!fileSystemPath.IsEmpty)
            result.Add(fileSystemPath);
        }
      }), (Encoding) null);
      return (IEnumerable<FileSystemPath>) result;
    }

    private static void PrintIncorrectUsageMessage(string error)
    {
      Console.Error.WriteLine(error);
      Console.Error.WriteLine("Usage: Symbols /<command_name> <command_parameters>");
    }
  }
}
