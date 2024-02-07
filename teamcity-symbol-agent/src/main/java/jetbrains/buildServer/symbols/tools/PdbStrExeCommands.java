

package jetbrains.buildServer.symbols.tools;

/**
 * @author Evgeniy.Koshkin
 */
public enum PdbStrExeCommands {
  READ {
    @Override
    public String getCmdSwitch() {
      return "-r";
    }
  },
  WRITE {
    @Override
    public String getCmdSwitch() {
      return "-w";
    }
  };

  public abstract String getCmdSwitch();
}
