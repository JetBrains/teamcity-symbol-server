/*
 * Copyright 2000-2020 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.symbols;

import jetbrains.buildServer.serverSide.impl.BaseServerTestCase;
import jetbrains.buildServer.serverSide.metadata.BuildMetadataEntry;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class SymbolsCacheTest extends BaseServerTestCase {
  @Test
  public void testCacheNonEmptyEntry() {
    SymbolsCache symbolsCache = new SymbolsCache(myFixture.getEventDispatcher());
    Mockery m = new Mockery();
    AtomicInteger integer = new AtomicInteger();
    BuildMetadataEntry entry = m.mock(BuildMetadataEntry.class);
    String key = "key";

    Function<String, BuildMetadataEntry> entryFunction = s -> {
      integer.incrementAndGet();
      return entry;
    };

    m.checking(new Expectations(){{
      oneOf(entry).getBuildId();
      will(returnValue(123L));
    }});


    BuildMetadataEntry cacheEntry1 = symbolsCache.getEntry(key, entryFunction);
    BuildMetadataEntry cacheEntry2 = symbolsCache.getEntry(key, entryFunction);

    Assert.assertNotNull(cacheEntry1);
    Assert.assertEquals(cacheEntry1, cacheEntry2);
    Assert.assertEquals(cacheEntry2, entry);

    Assert.assertEquals(integer.get(), 1);
  }

  @Test
  public void testCacheEmptyEntry() {
    SymbolsCache symbolsCache = new SymbolsCache(myFixture.getEventDispatcher());
    AtomicInteger integer = new AtomicInteger();
    String key = "key";

    Function<String, BuildMetadataEntry> entryFunction = s -> {
      integer.incrementAndGet();
      return null;
    };

    BuildMetadataEntry cacheEntry1 = symbolsCache.getEntry(key, entryFunction);
    BuildMetadataEntry cacheEntry2 = symbolsCache.getEntry(key, entryFunction);

    Assert.assertNull(cacheEntry1);
    Assert.assertNull(cacheEntry2);

    Assert.assertEquals(integer.get(), 1);
  }

  @Test
  public void testCacheEntryInvalidation() {
    SymbolsCache symbolsCache = new SymbolsCache(myFixture.getEventDispatcher());
    Mockery m = new Mockery();
    AtomicInteger integer = new AtomicInteger();
    BuildMetadataEntry entry = m.mock(BuildMetadataEntry.class);
    String key = "key";

    Function<String, BuildMetadataEntry> entryFunction = s -> {
      integer.incrementAndGet();
      return entry;
    };

    m.checking(new Expectations(){{
      exactly(3).of(entry).getBuildId();
      will(returnValue(123L));
    }});

    // Cache entry
    BuildMetadataEntry cacheEntry1 = symbolsCache.getEntry(key, entryFunction);
    Assert.assertNotNull(cacheEntry1);
    Assert.assertEquals(cacheEntry1, entry);

    // Remove it
    symbolsCache.removeEntry(key);

    // Cache entry again
    BuildMetadataEntry cacheEntry2 = symbolsCache.getEntry(key, entryFunction);
    Assert.assertNotNull(cacheEntry1);
    Assert.assertEquals(cacheEntry2, entry);

    Assert.assertEquals(integer.get(), 2);
  }
}
