/*
 * Copyright 2000-2023 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
      error = String.Empty;
      var commandName = args.First();
      switch (commandName)
      {
          case DumpSymbolsFileSignCommand.CMD_NAME:
          {
              var targetFilePaths = GetTargetFilePaths(args);
              var outputFilePath = FileSystemPath.TryParse(args[1].Substring(3));
              return new DumpSymbolsFileSignCommand(outputFilePath, targetFilePaths);
          }

          case DumpBinaryFileSignCommand.CMD_NAME:
          {
              var targetFilePaths = GetTargetFilePaths(args);
              var outputFilePath = FileSystemPath.TryParse(args[1].Substring(3));
              return new DumpBinaryFileSignCommand(outputFilePath, targetFilePaths);
          }

          case ListReferencesSourcesCommand.CMD_NAME:
              return new ListReferencesSourcesCommand(FileSystemPath.TryParse(args[1]));

          case GetPdbTypeCommand.CMD_NAME:
              return new GetPdbTypeCommand(FileSystemPath.TryParse(args[1]));

          case UpdateSourceUrlsCommand.CMD_NAME:
          {
              var symbolsFile = FileSystemPath.TryParse(args[1]);
              var sourceDescriptorFilePath = FileSystemPath.TryParse(args[2].Substring(3));
              return new UpdateSourceUrlsCommand(symbolsFile, sourceDescriptorFilePath);
          }

          default:
              error = String.Format("{0} command is unknown.", commandName);
              return null;
      }
    }

    private static ICollection<FileSystemPath> GetTargetFilePaths(string[] args)
    {
      var inputFilePath = FileSystemPath.TryParse(args[2].Substring(3));
      var targetFilePaths = LoadPathsFromFile(inputFilePath);
      return targetFilePaths;
    }

    private static ICollection<FileSystemPath> LoadPathsFromFile(FileSystemPath inputFilePath)
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
