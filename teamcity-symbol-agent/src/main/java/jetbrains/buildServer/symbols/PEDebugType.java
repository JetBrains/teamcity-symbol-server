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
