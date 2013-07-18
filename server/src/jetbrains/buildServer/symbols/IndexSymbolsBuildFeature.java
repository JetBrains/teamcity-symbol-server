package jetbrains.buildServer.symbols;

import jetbrains.buildServer.serverSide.BuildFeature;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Evgeniy.Koshkin
 */
public class IndexSymbolsBuildFeature extends BuildFeature {

  @NotNull
  @Override
  public String getType() {
    return SymbolsConstants.BUILD_FEATURE_TYPE;
  }

  @NotNull
  @Override
  public String getDisplayName() {
    return "Symbol files (.pdb) indexer";
  }

  @Nullable
  @Override
  public String getEditParametersUrl() {
    return null;
  }
}
