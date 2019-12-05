package won.protocol.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.util.Set;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Converter
public class URISetConverter implements AttributeConverter<Set<URI>, byte[]> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public byte[] convertToDatabaseColumn(Set<URI> attribute) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(out);
            oos.writeObject(attribute);
        } catch (IOException e) {
            logger.error("Cannot convert Set<URI> to byte array", e);
        }
        return out.toByteArray();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<URI> convertToEntityAttribute(byte[] data) {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream ois;
        try {
            ois = new ObjectInputStream(in);
            return (Set<URI>) ois.readObject();
        } catch (Exception e) {
            logger.error("Cannot convert byte array to Set<URI>", e);
        }
        return null;
    }
}
