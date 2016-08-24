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

package won.protocol.model;

import javax.persistence.AttributeConverter;

/**
 * Created by fkleedorfer on 24.08.2016.
 */
public class ConnectionStateConverter implements AttributeConverter<ConnectionState, Integer>
{
  @Override
  public Integer convertToDatabaseColumn(final ConnectionState connectionState) {
    if (connectionState == null) {
      return null;
    }
    switch (connectionState) {
      case  CLOSED: return 5;
      case  CONNECTED: return 4;
      case  REQUEST_RECEIVED: return 3;
      case  SUGGESTED: return 1;
      case  REQUEST_SENT: return 2;
    }
    throw new IllegalArgumentException("Cannot convert ConnectionState '" + connectionState.toString() + "' to Integer " +
                                         "value for database");
  }

  @Override
  public ConnectionState convertToEntityAttribute(final Integer connectionStateAsNumeric) {
    if (connectionStateAsNumeric == null){
      return null;
    }
    switch (connectionStateAsNumeric) {
      case 1: return ConnectionState.SUGGESTED;
      case 2: return ConnectionState.REQUEST_SENT;
      case 3: return ConnectionState.REQUEST_RECEIVED;
      case 4: return ConnectionState.CONNECTED;
      case 5: return ConnectionState.CLOSED;
    }
    throw new IllegalArgumentException("Cannot convert short value '" + connectionStateAsNumeric + " read from db to " +
                                         "ConnectionState object");
  }
}
