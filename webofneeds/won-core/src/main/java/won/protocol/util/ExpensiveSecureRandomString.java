/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package won.protocol.util;

import java.security.SecureRandom;

/**
 * Generates a random string of specified length quite securely, but not cheaply
 * The String always starts with a letter We do this so that we generate URIs
 * for which prefixing will always work with N3.js Based on:
 * http://stackoverflow.com/questions/41107/how-to-generate-a-random-alpha-numeric-string
 */
public class ExpensiveSecureRandomString {

  private static final char[] symbols;
  private static final char[] letters;

  static {
    StringBuilder tmp = new StringBuilder();
    for (char ch = 'a'; ch <= 'z'; ++ch) {
      tmp.append(ch);
    }
    letters = tmp.toString().toCharArray();

    for (char ch = '0'; ch <= '9'; ++ch) {
      tmp.append(ch);
    }
    symbols = tmp.toString().toCharArray();
  }

  private final SecureRandom random = new SecureRandom();

  /**
   * Beware, instance creation is expensive.
   */
  public ExpensiveSecureRandomString() {
  }

  public String nextString(int length) {
    StringBuilder sb = new StringBuilder();
    sb.append(letters[random.nextInt(letters.length)]); // make sure the first symbol is always a letter

    for (int i = 1; i < length; i++) {
      sb.append(symbols[random.nextInt(symbols.length)]);
    }
    return sb.toString();
  }
}