package jetbrains.buildServer.symbols.tools;

public enum PdbType {
  Undefined,
  Windows,
  Portable,
  EmbeddedPortable,
  Deterministic;

  public static PdbType parse(String value) {
    switch (value) {
      case "windows":
        return PdbType.Windows;
      case "portable":
        return PdbType.Portable;
      case "embeddedPortable":
        return PdbType.EmbeddedPortable;
      case "deterministic":
        return PdbType.Deterministic;
      default:
        return PdbType.Undefined;
    }
  }
}
