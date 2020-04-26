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

import java.util.Iterator;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PdbSignatureEntry implements Iterable<PdbSignatureDescriptor> {
  private final String mySignature;
  private final String myFullSignature;

  public PdbSignatureEntry(@NotNull String signature, @Nullable String fullSignature) {
    mySignature = signature;
    myFullSignature = fullSignature;
  }

  @NotNull
  public String getSignature() { return mySignature; }

  @Nullable
  public String getFullSignature() { return myFullSignature; }

  @NotNull
  @Override
  public Iterator<PdbSignatureDescriptor> iterator() {
    return new Iterator<PdbSignatureDescriptor>() {
      private int currentIndex = -1;

      @Override
      public boolean hasNext() {
        return (currentIndex < 0 && StringUtil.isNotEmpty(mySignature))
               || (currentIndex == 0 && StringUtil.isNotEmpty(myFullSignature));
      }

      @Override
      public PdbSignatureDescriptor next() {
        currentIndex++;
        if (currentIndex == 0) {
          return new PdbSignatureDescriptor(mySignature, PdbSignatureType.GUID);
        }
        if (currentIndex == 1) {
          return new PdbSignatureDescriptor(mySignature, PdbSignatureType.FULL);
        }
        throw new IndexOutOfBoundsException();
      }
    };
  }

}
