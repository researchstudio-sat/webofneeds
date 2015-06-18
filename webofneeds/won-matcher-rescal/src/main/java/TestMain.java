import data.RescalMatchingData;
import data.RescalSparqlService;

import java.io.IOException;

/**
 * User: hfriedrich
 * Date: 13.06.2015
 */
public class TestMain
{
  public static void main(String[] args) throws IOException {

    String endpoint = "http://localhost:9999/bigdata/namespace/needtest3/sparql";
    RescalSparqlService sparqlService = new RescalSparqlService(endpoint);
    RescalMatchingData data = new RescalMatchingData();
    sparqlService.updateMatchingDataWithActiveNeeds(data, 0, 10000000000000l);
    sparqlService.updateMatchingDataWithConnections(data, 0, 10000000000000l);
    data.writeCleanedOutputFiles("C:/dev/temp/tensor");
    System.out.println("done");
  }

}
