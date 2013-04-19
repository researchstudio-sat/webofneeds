/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package won.protocol.exception;

/**
 * Created with IntelliJ IDEA.
 * User: gabriel
 * Date: 15.02.13
 * Time: 15:40
 * To change this template use File | Settings | File Templates.
 */
public class RDFStorageException extends RuntimeException {
    private boolean enableSuppression = false;
    private boolean writeableStacktrace;
    public RDFStorageException() {
    }

    public RDFStorageException(String message) {
        super(message);
    }

    public RDFStorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public RDFStorageException(Throwable cause) {
        super(cause);
    }

    public RDFStorageException(String message, Throwable cause, boolean enableSuppression, boolean writeableStackTrace) {
        super(message, cause);
      this.enableSuppression = enableSuppression;
      this.writeableStacktrace = writeableStackTrace;
    }
}
