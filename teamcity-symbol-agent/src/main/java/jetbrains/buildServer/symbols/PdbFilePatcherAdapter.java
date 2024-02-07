

package jetbrains.buildServer.symbols;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import jetbrains.buildServer.ExecResult;
import jetbrains.buildServer.agent.BuildProgressLogger;

public interface PdbFilePatcherAdapter {
  public int serializeSourceLinks(File sourceLinksFile, Collection<File> sourceFiles) throws IOException;
  public ExecResult updatePdbSourceLinks(File symbolsFile, File sourceLinksFile);
  public Collection<File> getReferencedSourceFiles(File symbolsFile) throws IOException;
}
