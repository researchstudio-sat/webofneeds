package won.protocol.model;

import javax.persistence.AttributeConverter;

public class BooleanTFConverter implements AttributeConverter<Boolean, Character> {

  @Override
  public Character convertToDatabaseColumn(Boolean booleanValue) {
    return booleanValue == null ? null : booleanValue.booleanValue() ? 'T' : 'F';
  }

  @Override
  public Boolean convertToEntityAttribute(Character charValue) {
    if (charValue == null) {
      return null;
    }
    if (charValue.charValue() == 'T') {
      return Boolean.TRUE;
    }
    if (charValue.charValue() == 'F') {
      return Boolean.FALSE;
    }
    throw new IllegalArgumentException("Cannot map string '" + charValue + "' to Boolean");
  }

}
