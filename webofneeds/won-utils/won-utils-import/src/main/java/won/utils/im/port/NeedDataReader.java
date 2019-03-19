package won.utils.im.port;

import java.io.Closeable;

/**
 * User: ypanchenko Date: 04.09.2014
 */
public interface NeedDataReader<T> extends Closeable {
    public boolean hasNext();

    public T next();
}
