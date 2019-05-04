package won.utils.im.port;

import java.io.Closeable;
import java.io.IOException;

/**
 * User: ypanchenko Date: 04.09.2014
 */
public interface AtomDataWriter<T> extends Closeable {
    public void write(T obj) throws IOException;
}
