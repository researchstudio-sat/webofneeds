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

package won.protocol.message.processor.exception;

import java.net.URI;

/**
 * User: fkleedorfer
 * Date: 27.03.15
 */
public class IllegalWonMessagePropertyValue extends WonMessageNotWellFormedException {
  private URI property;
  private String value;

  private static String createExceptionMessage(URI property, String value){
    return String.format("Illegal value: %s for property %s", value, property.toString());
  }

  public IllegalWonMessagePropertyValue(URI property, String value) {
    super(createExceptionMessage(property,value));
    this.property = property;
    this.value = value;
  }

  public IllegalWonMessagePropertyValue(Throwable cause, URI property, String value) {
    super(createExceptionMessage(property,value),cause);
    this.property = property;
    this.value = value;
  }

  public IllegalWonMessagePropertyValue(Throwable cause, boolean enableSuppression, boolean writableStackTrace, URI property, String value) {
    super(createExceptionMessage(property,value), cause, enableSuppression, writableStackTrace);
    this.property = property;
    this.value = value;
  }
}
