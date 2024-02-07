

package jetbrains.buildServer.symbols;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;
import jetbrains.buildServer.serverSide.impl.BaseServerTestCase;
import jetbrains.buildServer.serverSide.metadata.BuildMetadataEntry;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.Assert;
import org.testng.annotations.Test;

public class SymbolsCacheTest extends BaseServerTestCase {
  @Test
  public void shouldNotReadCachedEntryAgainFromSource() throws TimeoutException, InterruptedException {
    //Given
    SymbolsCache symbolsCache = new SymbolsCache(myFixture.getEventDispatcher());
    Mockery m = new Mockery();

    BuildMetadataEntry entry = m.mock(BuildMetadataEntry.class);
    List<BuildMetadataEntry> entryList = Collections.singletonList(entry);
    MetadataSource metadataSource = m.mock(MetadataSource.class);
    String key = "key";
    Long buildId = 123L;

    m.checking(new Expectations(){{
      oneOf(entry).getBuildId();
      will(returnValue(buildId));

      one(entry).getKey();
      will(returnValue(key));

      one(metadataSource).getBuildIdByEntryKey(key);
      will(returnValue(buildId));

      one(metadataSource).getEntriesByBuildId(buildId);
      will(returnValue(entryList));
    }});

    // When
    BuildMetadataEntry cacheEntry1 = symbolsCache.getEntry(key, metadataSource);
    BuildMetadataEntry cacheEntry2 = symbolsCache.getEntry(key, metadataSource);

    // Then
    Assert.assertNotNull(cacheEntry1);
    Assert.assertEquals(cacheEntry1, cacheEntry2);
    Assert.assertEquals(cacheEntry2, entry);
  }

  @Test
  public void shouldSearchOnceForNonExistentKeys() throws TimeoutException, InterruptedException {
    // Given
    SymbolsCache symbolsCache = new SymbolsCache(myFixture.getEventDispatcher());
    Mockery m = new Mockery();

    MetadataSource metadataSource = m.mock(MetadataSource.class);
    String key = "key";
    Long buildId = null;

    m.checking(new Expectations(){{
      one(metadataSource).getBuildIdByEntryKey(key);
      will(returnValue(buildId));
    }});

    // When
    BuildMetadataEntry cacheEntry1 = symbolsCache.getEntry(key, metadataSource);
    BuildMetadataEntry cacheEntry2 = symbolsCache.getEntry(key, metadataSource);

    // Then
    Assert.assertNull(cacheEntry1);
    Assert.assertNull(cacheEntry2);
  }

  @Test
  public void shouldReadEntriesAfterInvalidate() throws TimeoutException, InterruptedException {
    // Given
    SymbolsCache symbolsCache = new SymbolsCache(myFixture.getEventDispatcher());
    Mockery m = new Mockery();

    BuildMetadataEntry entry = m.mock(BuildMetadataEntry.class);
    List<BuildMetadataEntry> entryList = Collections.singletonList(entry);
    MetadataSource metadataSource = m.mock(MetadataSource.class);
    String key = "key";
    Long buildId = 123L;

    m.checking(new Expectations(){{
      exactly(2).of(entry).getBuildId();
      will(returnValue(buildId));

      exactly(2).of(entry).getKey();
      will(returnValue(key));

      exactly(2).of(metadataSource).getBuildIdByEntryKey(key);
      will(returnValue(buildId));

      exactly(2).of(metadataSource).getEntriesByBuildId(buildId);
      will(returnValue(entryList));
    }});

    BuildMetadataEntry cacheEntry1 = symbolsCache.getEntry(key, metadataSource);
    Assert.assertNotNull(cacheEntry1);
    Assert.assertEquals(cacheEntry1, entry);

    // When
    symbolsCache.invalidate(buildId);

    // Then
    BuildMetadataEntry cacheEntry2 = symbolsCache.getEntry(key, metadataSource);
    Assert.assertNotNull(cacheEntry1);
    Assert.assertEquals(cacheEntry2, entry);
  }

  @Test
  public void shouldReadWholeBuild() throws TimeoutException, InterruptedException {
    // Given
    SymbolsCache symbolsCache = new SymbolsCache(myFixture.getEventDispatcher());
    Mockery m = new Mockery();

    String key = "key";
    BuildMetadataEntry entry = m.mock(BuildMetadataEntry.class, key);

    String key2 = "key2";
    BuildMetadataEntry entry2 = m.mock(BuildMetadataEntry.class, key2);
    List<BuildMetadataEntry> entryList = Arrays.asList(entry, entry2);

    MetadataSource metadataSource = m.mock(MetadataSource.class);
    Long buildId = 123L;

    m.checking(new Expectations(){{
      one(entry).getBuildId();
      will(returnValue(buildId));

      one(entry).getKey();
      will(returnValue(key));

      one(entry2).getKey();
      will(returnValue(key2));

      one(entry2).getBuildId();
      will(returnValue(buildId));

      one(metadataSource).getBuildIdByEntryKey(key);
      will(returnValue(buildId));

      one(metadataSource).getEntriesByBuildId(buildId);
      will(returnValue(entryList));
    }});

    symbolsCache.getEntry(key, metadataSource);

    // When
    BuildMetadataEntry cacheEntry2 = symbolsCache.getEntry(key2, metadataSource);

    // Then
    Assert.assertEquals(cacheEntry2, entry2);
  }
}
