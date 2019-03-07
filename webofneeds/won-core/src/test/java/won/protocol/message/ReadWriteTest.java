package won.protocol.message;

import org.apache.jena.query.Dataset;
import org.apache.jena.riot.Lang;
import org.apache.jena.sparql.util.IsoMatcher;
import org.junit.Assert;
import org.junit.Test;

import won.protocol.util.RdfUtils;

/**
 * Created by yanapanchenko on 05.08.14.
 */
public class ReadWriteTest
{

  //"/01_OA_to_WN1.trig";
  //"/02_WN1.trig";
  //"/01_WN2_to_WN1.trig";
  private static final String RESOURCE_FILE = "/need-lifecycle_with_message_02adj/04_deactivate_(by_owner)" +
    "/02_WN1.trig";


  @Test
  //@Ignore
  public void testTrigRoundTrip() throws Exception {

    Dataset datasetIn = Utils.createTestDataset(RESOURCE_FILE);
    String datasetInString = RdfUtils.writeDatasetToString(datasetIn, Lang.TRIG);

    System.out.println("TRIG IN");
    System.out.println(datasetInString);
    System.out.println();

    Dataset datasetOut = RdfUtils.readDatasetFromString(datasetInString, Lang.TRIG);
    String datasetOutString = RdfUtils.writeDatasetToString(datasetOut, Lang.TRIG);

    System.out.println("TRIG OUT");
    System.out.println(datasetOutString);

    Assert.assertTrue(IsoMatcher.isomorphic(datasetIn.asDatasetGraph(), datasetOut.asDatasetGraph()));
  }
}
