// Decompiled with JetBrains decompiler
// Type: JetBrains.CommandLine.Symbols.DumpFilesSignCommandBase
// Assembly: JetBrains.CommandLine.Symbols, Version=1.0.0.0, Culture=neutral, PublicKeyToken=1010a0d8d6380325
// MVID: EF046BF6-60AC-48EA-9121-8AF3D8D08853
// Assembly location: C:\Data\Work\TeamCity\misc\tc-symbol-server\tools\JetSymbols\JetBrains.CommandLine.Symbols.exe

using JetBrains.Util;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Xml;

namespace JetBrains.CommandLine.Symbols
{
  public abstract class DumpFilesSignCommandBase : ICommand
  {
    private FileSystemPath myOutputFilePath;
    private IEnumerable<FileSystemPath> myTargetFilePaths;

    protected DumpFilesSignCommandBase(FileSystemPath outputFilePath, IEnumerable<FileSystemPath> targetFilePaths)
    {
      this.myOutputFilePath = outputFilePath;
      this.myTargetFilePaths = targetFilePaths;
    }

    public int Execute()
    {
      try
      {
        if (this.myOutputFilePath.IsEmpty)
        {
          Console.Error.WriteLine("Output file path is empty.");
          return 1;
        }
        Dictionary<FileSystemPath, string> dictionary = this.myTargetFilePaths.ToDictionary<FileSystemPath, FileSystemPath, string>((Func<FileSystemPath, FileSystemPath>) (targetFilePath => targetFilePath), new Func<FileSystemPath, string>(this.GetFileSignature));
        if (!dictionary.IsEmpty<KeyValuePair<FileSystemPath, string>>())
        {
          DumpFilesSignCommandBase.WriteToFile(this.myOutputFilePath, dictionary);
          Console.Out.WriteLine("Dumped {0} signature entries to the file {1}", (object) dictionary.Count, (object) this.myOutputFilePath);
          return 0;
        }
        Console.Error.WriteLine("Nothing to dump.");
        return 1;
      }
      catch (Exception ex)
      {
        Console.Error.WriteLine((object) ex);
        return 1;
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
