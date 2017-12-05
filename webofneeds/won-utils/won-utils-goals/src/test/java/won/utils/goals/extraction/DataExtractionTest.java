package won.utils.goals.extraction;

import org.apache.jena.query.Dataset;
import org.junit.Test;

import java.io.IOException;

public class DataExtractionTest {

    @Test
    public void test() throws IOException {
        DataExtraction ex = new DataExtraction();
        Dataset ds = null;
        Dataset result = ex.extract(ds);
        System.out.println(result);
    }
}
