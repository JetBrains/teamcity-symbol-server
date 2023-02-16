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
