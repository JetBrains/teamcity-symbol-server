package jetbrains.buildServer.symbols;

/**
 * Windows Portable Executable (PE) debug type
 * @author Evgeniy.Koshkin
 */
public enum PEDebugType {
  IMAGE_DEBUG_TYPE_UNKNOWN(0),
  IMAGE_DEBUG_TYPE_COFF(1),
  IMAGE_DEBUG_TYPE_CODEVIEW(2),
  IMAGE_DEBUG_TYPE_FPO(3),
  IMAGE_DEBUG_TYPE_MISC(4);

  private final int myValue;

  PEDebugType(int value) {
    myValue = value;
  }

  private int getValue() {
    return myValue;
  }

  @Override
  public String toString() {
    return String.valueOf(myValue);
  }
}
