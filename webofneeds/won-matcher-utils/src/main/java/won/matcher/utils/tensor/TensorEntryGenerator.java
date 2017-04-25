package won.matcher.utils.tensor;

import java.io.IOException;
import java.util.Collection;

/**
 * Created by hfriedrich on 21.04.2017.
 */
public interface TensorEntryGenerator {

    Collection<TensorEntry> generateTensorEntries() throws IOException;
}
