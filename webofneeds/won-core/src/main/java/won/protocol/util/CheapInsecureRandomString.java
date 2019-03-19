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

import java.util.Random;

/**
 * Generates a random string of specified length cheaply, but not securely. Taken from:
 * http://stackoverflow.com/questions/41107/how-to-generate-a-random-alpha-numeric-string
 */
public class CheapInsecureRandomString {

    private static final char[] symbols;

    static {
        StringBuilder tmp = new StringBuilder();
        for (char ch = '0'; ch <= '9'; ++ch)
            tmp.append(ch);
        for (char ch = 'a'; ch <= 'z'; ++ch)
            tmp.append(ch);
        symbols = tmp.toString().toCharArray();
    }

    private final Random random = new Random(System.currentTimeMillis());

    public CheapInsecureRandomString() {

    }

    public String nextString(int length) {
        StringBuilder sb = new StringBuilder();
        for (int idx = 0; idx < length; ++idx)
            sb.append(symbols[random.nextInt(symbols.length)]);
        return sb.toString();
    }
}