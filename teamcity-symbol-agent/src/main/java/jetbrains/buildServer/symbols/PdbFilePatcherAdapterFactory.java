

package jetbrains.buildServer.symbols;

import jetbrains.buildServer.symbols.tools.PdbType;

public interface PdbFilePatcherAdapterFactory {
  PdbFilePatcherAdapter create(PdbType type);
}
