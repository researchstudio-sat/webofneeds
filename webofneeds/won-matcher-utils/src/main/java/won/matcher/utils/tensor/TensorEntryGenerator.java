package won.matcher.utils.tensor;

import java.io.IOException;
import java.util.Collection;

/**
 * This interface defines the structure of classes that want to act as
 * {@link TensorEntry} generators. These generators can be used to create tensor
 * entries in a generalized and structured way. Created by hfriedrich on
 * 21.04.2017.
 */
public interface TensorEntryGenerator {
    Collection<TensorEntry> generateTensorEntries() throws IOException;
}
