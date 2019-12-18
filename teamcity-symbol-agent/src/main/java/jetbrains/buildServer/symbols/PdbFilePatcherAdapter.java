package jetbrains.buildServer.symbols;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import jetbrains.buildServer.ExecResult;

public interface PdbFilePatcherAdapter {
  public int serializeSourceLinks(File sourceLinksFile, Collection<File> sourceFiles) throws IOException;
  public ExecResult updatePdbSourceLinks(File symbolsFile, File sourceLinksFile);
}
