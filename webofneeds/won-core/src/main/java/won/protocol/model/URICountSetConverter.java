package won.protocol.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Set;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Converter
public class URICountSetConverter implements AttributeConverter<Set<URICount>, byte[]> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public byte[] convertToDatabaseColumn(Set<URICount> attribute) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(out);
            oos.writeObject(attribute);
        } catch (IOException e) {
            logger.error("Cannot convert Set<URICount> to byte array", e);
        }
        return out.toByteArray();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<URICount> convertToEntityAttribute(byte[] data) {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream ois;
        try {
            ois = new ObjectInputStream(in);
            return (Set<URICount>) ois.readObject();
        } catch (Exception e) {
            logger.error("Cannot convert byte array to Set<URICount>", e);
        }
        return null;
    }
}
