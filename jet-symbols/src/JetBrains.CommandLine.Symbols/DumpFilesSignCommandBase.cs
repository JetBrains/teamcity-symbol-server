﻿

using JetBrains.Util;
using System;
using System.Collections.Generic;
using System.Xml;

namespace JetBrains.CommandLine.Symbols
{
  public abstract class DumpFilesSignCommandBase : ICommand
  {
    private readonly FileSystemPath myOutputFilePath;
    private readonly ICollection<FileSystemPath> myTargetFilePaths;

    protected DumpFilesSignCommandBase(FileSystemPath outputFilePath, ICollection<FileSystemPath> targetFilePaths)
    {
      myOutputFilePath = outputFilePath;
      myTargetFilePaths = targetFilePaths;
    }

    public int Execute()
    {
      try
      {
        if (myOutputFilePath.IsEmpty)
        {
          Console.Error.WriteLine("Output file path is empty.");
          return 1;
        }

        var dictionary = new Dictionary<FileSystemPath, string>(myTargetFilePaths.Count);
        foreach (var targetFilePath in myTargetFilePaths)
        {
          var fileSignature = GetFileSignature(targetFilePath);
          if (string.IsNullOrEmpty(fileSignature)) continue;
          dictionary.Add(targetFilePath, fileSignature);
        }
        
        if (!dictionary.IsEmpty())
        {
          WriteToFile(myOutputFilePath, dictionary);
          Console.Out.WriteLine("Dumped {0} signature entries to the file {1}", dictionary.Count, myOutputFilePath);
          return 0;
        }
        
        Console.Error.WriteLine("Nothing to dump.");
        return 1;
      }
      catch (Exception ex)
      {
        Console.Error.WriteLine(ex.Message);
        Console.Error.WriteLine(ex.StackTrace);
        return Program.ERROR_EXIT_CODE;
      }
    }

    private static void WriteToFile(FileSystemPath outputFilePath, Dictionary<FileSystemPath, string> signatures)
    {
      XmlDocument xmlDocument = new XmlDocument();
      XmlNode node = xmlDocument.CreateNode(XmlNodeType.Element, "file-signs", "");
      foreach (KeyValuePair<FileSystemPath, string> signature in signatures)
      {
        XmlElement element = node.CreateElement("file-sign-entry");
        element.CreateAttributeWithNonEmptyValue("file-path", signature.Key.FullPath);
        element.CreateAttributeWithNonEmptyValue("file", signature.Key.Name);
        element.CreateAttributeWithNonEmptyValue("sign", signature.Value);
      }
      xmlDocument.AppendChild(node);
      using (XmlWriter w = XmlWriter.Create(outputFilePath.FullPath))
        xmlDocument.WriteContentTo(w);
    }

    protected abstract string GetFileSignature(FileSystemPath targetFilePath);
  }
}
