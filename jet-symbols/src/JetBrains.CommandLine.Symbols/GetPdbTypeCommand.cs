

using System;
using JetBrains.Metadata.Debug.Pdb;
using JetBrains.Metadata.Utils.PE.Directories;
using JetBrains.Util;

namespace JetBrains.CommandLine.Symbols
{
    public class GetPdbTypeCommand : ICommand
    {
        public const string CMD_NAME = "getPdbType";

        private readonly FileSystemPath _symbolsFile;

        public GetPdbTypeCommand(FileSystemPath symbolsFile)
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
                DebugInfoType debugInfo;
                if (!PdbUtils.TryGetPdbType(_symbolsFile, out debugInfo))
                {
                    throw new Exception("Invalid PDB file " + _symbolsFile);
                }
                Console.Out.WriteLine(FormatDebugInfoType(debugInfo));
            }
            catch (Exception e)
            {
                Console.Error.WriteLine("Unable to read PDB type from PDF file {0}: {1} ", _symbolsFile, e.Message);
                return 1;
            }

            return 0;
        }

        private static string FormatDebugInfoType(DebugInfoType debugInfo)
        {
            switch (debugInfo)
            {
                case DebugInfoType.Windows:
                    return "windows";
                case DebugInfoType.Portable:
                    return "portable";
                case DebugInfoType.EmbeddedPortable:
                    return "embeddedPortable";
                case DebugInfoType.Deterministic:
                    return "deterministic";
                default:
                    return debugInfo.ToString();
            }
        }
    }
}
