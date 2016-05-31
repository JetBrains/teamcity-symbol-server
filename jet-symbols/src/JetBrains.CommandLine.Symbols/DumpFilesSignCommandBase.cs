using JetBrains.Util;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Xml;

namespace JetBrains.CommandLine.Symbols
{
  public abstract class DumpFilesSignCommandBase : ICommand
  {
    private readonly FileSystemPath myOutputFilePath;
    private readonly IEnumerable<FileSystemPath> myTargetFilePaths;

    protected DumpFilesSignCommandBase(FileSystemPath outputFilePath, IEnumerable<FileSystemPath> targetFilePaths)
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
        var dictionary = myTargetFilePaths.ToDictionary(targetFilePath => targetFilePath, GetFileSignature);
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
        element.CreateAttributeWithNonEmptyValue("file", signature.Key.FullPath);
        string str = signature.Value;
        if (str != null)
          element.CreateAttributeWithNonEmptyValue("sign", str);
      }
      xmlDocument.AppendChild(node);
      using (XmlWriter w = XmlWriter.Create(outputFilePath.FullPath))
        xmlDocument.WriteContentTo(w);
    }

    protected abstract string GetFileSignature(FileSystemPath targetFilePath);
  }
}
