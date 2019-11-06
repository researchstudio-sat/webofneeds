/*
 * Copyright 2012 Research Studios Austria Forschungsges.m.b.H. Licensed under
 * the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package won.protocol.exception;

import java.text.MessageFormat;

/**
 * Exception indicating that an entity does not have the required number of
 * properties.
 */
public class IncorrectPropertyCountException extends DataIntegrityException {
    private int expectedCount;
    private int actualCount;

    public IncorrectPropertyCountException(final int expectedCount, final int actualCount) {
        this(MessageFormat.format("Incorrect property count, expected: {0}, actual: {1} (at least)", expectedCount,
                        actualCount), expectedCount, actualCount);
    }

    public IncorrectPropertyCountException(final String message, final int expectedCount, final int actualCount) {
        super(message + ", expected: " + expectedCount + ", actual: " + actualCount);
        this.expectedCount = expectedCount;
        this.actualCount = actualCount;
    }

    public IncorrectPropertyCountException(final String message, final Throwable cause, final int expectedCount,
                    final int actualCount) {
        super(message + ", expected: " + expectedCount + ", actual: " + actualCount, cause);
        this.expectedCount = expectedCount;
        this.actualCount = actualCount;
    }

    public IncorrectPropertyCountException(final Throwable cause, final int expectedCount, final int actualCount) {
        super(cause);
        this.expectedCount = expectedCount;
        this.actualCount = actualCount;
    }

    public int getExpectedCount() {
        return expectedCount;
    }

    public int getActualCount() {
        return actualCount;
    }
}
