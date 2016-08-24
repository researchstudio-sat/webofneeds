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
public class NeedStateConverter implements AttributeConverter<NeedState, Character>
{

  @Override
  public Character convertToDatabaseColumn(final NeedState needState) {
    if (needState == null) {
      return null;
    }
    switch (needState){
      case ACTIVE: return 'A';
      case INACTIVE: return 'I';
    }
    throw new IllegalArgumentException("Cannot convert needState " + needState.toString() + " to character " +
                                         "representation for database");
  }

  @Override
  public NeedState convertToEntityAttribute(final Character needStateAsCharacter) {
    if (needStateAsCharacter == null) {
      return null;
    }
    switch(needStateAsCharacter){
      case 'A' : return NeedState.ACTIVE;
      case 'I' : return NeedState.INACTIVE;
    }
    throw new IllegalArgumentException("cannot convert character '"+ needStateAsCharacter + " to NeedState");
  }
}
