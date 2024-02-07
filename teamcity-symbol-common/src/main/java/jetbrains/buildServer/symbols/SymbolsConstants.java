

package jetbrains.buildServer.symbols;

/**
 * @author Evgeniy.Koshkin
 */
public class SymbolsConstants {
  public static final String BUILD_FEATURE_TYPE = "symbol-indexer";

  public static final String SOURCES_SERVER_URL_PARAM_NAME = "symbols.sources-server-url";
  public static final String SERVER_OWN_URL_PARAM_NAME = "symbols.server-own-url";
  public static final String INDEXING_ENABLED_PARAM_NAME = "symbols.indexing.enabled";

  public static final String APP_SYMBOLS = "/app/symbols";
  public static final String APP_SOURCES = "/app/sources";

  public static final String SYMBOL_SIGNATURES_FILE_NAME_PREFIX = "symbol-signatures-artifacts-";
  public static final String BINARY_SIGNATURES_FILE_NAME_PREFIX = "binary-signatures-artifacts-";

  public static final String SYMBOLS_SERVER_CACHE_ENTRIES_SIZE = "teamcity.symbolServer.cache.entriesSize";
  public static final String SYMBOLS_SERVER_CACHE_EXPIRATION_TIME_SEC = "teamcity.symbolServer.cache.expirationTime.sec";
  public static final String SYMBOLS_SERVER_MISS_CACHE_ENTRIES_SIZE = "teamcity.symbolServer.miss.cache.size";
  public static final String SYMBOLS_SERVER_MISS_CACHE_EXPIRATION_TIME_SEC = "teamcity.symbolServer.miss.cache.expirationTime.sec";
  public static final String SYMBOLS_SERVER_CACHE_MAXREADREQUESTS = "teamcity.symbolServer.cache.maxReadRequests";
  public static final String SYMBOLS_SERVER_CACHE_ACQUIRE_LOCK_TIMEOUT ="teamcity.symbolServer.cache.acqure.lock.timeout";

  public static final String BRANCH_FILTER = "teamcity.symbols.branchFilter";
}
