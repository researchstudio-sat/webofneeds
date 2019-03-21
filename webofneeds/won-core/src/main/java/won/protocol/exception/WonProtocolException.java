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
 * User: fkleedorfer
 * Date: 02.11.12
 */
public class WonProtocolException extends Exception
{
  public WonProtocolException()
  {
    super();
  }

  public WonProtocolException(final String message)
  {
    super(message);
  }

  public WonProtocolException(final String message, final Throwable cause)
  {
    super(message, cause);
  }

  public WonProtocolException(final Throwable cause)
  {
    super(cause);
  }

  protected static String safeToString(Object o){
    return o == null ? "null" : o.toString();
  }
}
