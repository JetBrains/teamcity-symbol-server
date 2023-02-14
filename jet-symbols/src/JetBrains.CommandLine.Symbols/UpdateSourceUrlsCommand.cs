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
using System.Collections.Generic;
using System.Runtime.Serialization;
using System.Runtime.Serialization.Json;
using JetBrains.Metadata.Debug.Pdb;
using JetBrains.Metadata.Debug.Pdb.SymbolWriter;
using JetBrains.Metadata.Utils.PE.Directories;
using JetBrains.Util;

namespace JetBrains.CommandLine.Symbols
{
    public class UpdateSourceUrlsCommand: ICommand
    {
        public const string CMD_NAME = "updateSourceUrls";

        private readonly FileSystemPath _symbolsFile;
        private readonly FileSystemPath _sourceDescriptorFile;

        public UpdateSourceUrlsCommand(FileSystemPath symbolsFile, FileSystemPath sourceDescriptorFile)
        {
            _symbolsFile = symbolsFile;
            _sourceDescriptorFile = sourceDescriptorFile;
        }
        public int Execute()
        {
            if (!_symbolsFile.ExistsFile)
            {
                Console.Error.WriteLine("PDB file {0} does not exist.", _symbolsFile);
                return 1;
            }

            if (!_sourceDescriptorFile.ExistsFile)
            {
                Console.Error.WriteLine("Source descriptor file {0} does not exist.", _sourceDescriptorFile);
                return 1;
            }

            try
            {
                DebugInfoType debugInfoType;
                if (!PdbUtils.TryGetPdbType(_symbolsFile, out debugInfoType))
                {
                    throw new Exception("Invalid PDB file " + _symbolsFile);
                }

                if (debugInfoType != DebugInfoType.Portable)
                {
                    throw new Exception(string.Format("Cannot update PDB file ${0}. PDB Type {1} is not supported",
                        _symbolsFile, debugInfoType));
                }

                var sourceDescriptor = ReadSourceDescriptor(_sourceDescriptorFile);
                if (sourceDescriptor.Documents == null)
                {
                    throw new Exception(string.Format("Source Link file ${0} is empty", _sourceDescriptorFile));
                }

                var symbolsFile = _symbolsFile.AddSuffix(".original");
                _symbolsFile.MoveFile(symbolsFile, true);

                if (!PortablePdbModifier.AddSourceLink(symbolsFile, _sourceDescriptorFile, _symbolsFile))
                {
                    throw new Exception(string.Format("Cannot update PDB file ${0}. Internal error", symbolsFile));
                }
            }
            catch (Exception e)
            {
                Console.Error.WriteLine("Unable to update PDF file {0}: {1} ", _symbolsFile, e.Message);
                return 1;
            }

            return 0;

        }

        private static SourcesDescriptor ReadSourceDescriptor(FileSystemPath sourceDescriptorFile)
        {
            var settings = new DataContractJsonSerializerSettings();
            settings.UseSimpleDictionaryFormat = true;

            var serializer = new DataContractJsonSerializer(typeof(SourcesDescriptor), settings);
            using (var stream = sourceDescriptorFile.OpenFileForReading())
            {
                return (SourcesDescriptor)serializer.ReadObject(stream);
            }
        }
    }

    [DataContract]
    internal sealed class SourcesDescriptor
    {
        [DataMember(Name = "documents")]
        public Dictionary<string, string> Documents { get; private set; }
    }
}
