package jetbrains.buildServer.symbols;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.FlowLogger;
import jetbrains.buildServer.agent.NullBuildProgressLogger;
import jetbrains.buildServer.messages.BuildMessage1;
import jetbrains.buildServer.symbols.tools.JetSymbolsExe;
import jetbrains.buildServer.util.FileUtil;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

/**
 * @author Evgeniy.Koshkin
 */
public class JetSymbolsExeTest extends BaseTestCase implements BuildProgressLogger {

  private JetSymbolsExe myExe;

  @Override
  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    File homeDir = new File("tools\\JetSymbols").getAbsoluteFile();
    assertTrue(homeDir.isDirectory());
    myExe = new JetSymbolsExe(homeDir);
  }

  @Test
  public void testCmdParametersLengthLimit() throws Exception {
    myExe.dumpGuidsToFile(getFilesCollection(500), FileUtil.createTempFile("testCmdParametersLengthLimit", ".out"), this);
  }

  private Collection<File> getFilesCollection(int count) throws IOException {
    Collection<File> result = new HashSet<File>();
    for (int i = 0; i < count; i++){
      result.add(FileUtil.createTempFile("foo", "boo"));
    }
    return result;
  }

  public void activityStarted(String activityName, String activityType) {

  }

  public void activityStarted(String activityName, String activityDescription, String activityType) {

  }

  public void activityFinished(String activityName, String activityType) {

  }

  public void targetStarted(String targetName) {

  }

  public void targetFinished(String targetName) {

  }

  public void buildFailureDescription(String message) {

  }

  public void internalError(String type, String message, Throwable throwable) {

  }

  public void progressStarted(String message) {

  }

  public void progressFinished() {

  }

  public void logMessage(BuildMessage1 message) {

  }

  public void logTestStarted(String name) {

  }

  public void logTestStarted(String name, Date timestamp) {

  }

  public void logTestFinished(String name) {

  }

  public void logTestFinished(String name, Date timestamp) {

  }

  public void logTestIgnored(String name, String reason) {

  }

  public void logSuiteStarted(String name) {

  }

  public void logSuiteStarted(String name, Date timestamp) {

  }

  public void logSuiteFinished(String name) {

  }

  public void logSuiteFinished(String name, Date timestamp) {

  }

  public void logTestStdOut(String testName, String out) {

  }

  public void logTestStdErr(String testName, String out) {

  }

  public void logTestFailed(String testName, Throwable e) {

  }

  public void logComparisonFailure(String testName, Throwable e, String expected, String actual) {

  }

  public void logTestFailed(String testName, String message, String stackTrace) {

  }

  public void flush() {

  }

  public void ignoreServiceMessages(Runnable runnable) {

  }

  public FlowLogger getFlowLogger(String flowId) {
    return null;
  }

  public FlowLogger getThreadLogger() {
    return null;
  }

  public String getFlowId() {
    return null;
  }

  public void logBuildProblem(BuildProblemData buildProblem) {

  }

  public void message(String message) {

  }

  public void error(String message) {
    fail(message);
  }

  public void warning(String message) {

  }

  public void exception(Throwable th) {
    if(th != null) {
      fail(th.toString());
    }
  }

  public void progressMessage(String message) {

  }
}
