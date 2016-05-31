using JetBrains.Util;
using System;
using System.Collections.Generic;
using System.Linq;

namespace JetBrains.CommandLine.Symbols
{
  internal static class Program
  {
    public const int ERROR_EXIT_CODE = 1;

    public static int Main(string[] args)
    {
      AppDomain.CurrentDomain.UnhandledException += (sender, eventArgs) =>
      {
        Console.Error.WriteLine(String.Format("Unhandled excpetion: " + eventArgs.ExceptionObject));
      };

      try
      {
        string error;
        var commandToExecute = GetCommandToExecute(args, out error);
        if (commandToExecute != null)
        {
          return commandToExecute.Execute();
        }
        PrintIncorrectUsageMessage(error);
        return ERROR_EXIT_CODE;
      }
      catch (Exception ex)
      {
        Console.Error.WriteLine(ex);
        return ERROR_EXIT_CODE;
      }
    }

    private static ICommand GetCommandToExecute(string[] args, out string error)
    {
      error = Empty;
      var str = args.First();
      var inputFilePath = FileSystemPath.TryParse(args[2].Substring(3));
      var outputFilePath = FileSystemPath.TryParse(args[1].Substring(3));
      var targetFilePaths = LoadPathsFromFile(inputFilePath);
      switch (str)
      {
        case DumpSymbolsFileSignCommand.CMD_NAME:
          return new DumpSymbolsFileSignCommand(outputFilePath, targetFilePaths);
        case DumpBinaryFileSignCommand.CMD_NAME:
          return new DumpBinaryFileSignCommand(outputFilePath, targetFilePaths);
        default:
          error = String.Format("{0} command is unknown.", str);
          return null;
      }
    }

    private static IEnumerable<FileSystemPath> LoadPathsFromFile(FileSystemPath inputFilePath)
    {
      ICollection<FileSystemPath> result = new HashSet<FileSystemPath>();
      inputFilePath.ReadTextStream(streamReader =>
      {
        while (!streamReader.EndOfStream)
        {
          var fileSystemPath = FileSystemPath.TryParse(streamReader.ReadLine());
          if (!fileSystemPath.IsEmpty)
            result.Add(fileSystemPath);
        }
      });
      return result;
    }

    private static void PrintIncorrectUsageMessage(string error)
    {
      Console.Error.WriteLine(error);
      Console.Error.WriteLine("Usage: Symbols /<command_name> <command_parameters>");
    }
  }
}
