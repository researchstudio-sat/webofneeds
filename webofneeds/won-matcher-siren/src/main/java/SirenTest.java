import com.hp.hpl.jena.query.Dataset;
import common.service.HttpRequestService;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;

import java.io.IOException;
import java.io.StringWriter;

/**
 * Created by hfriedrich on 11.09.2015.
 */
public class SirenTest
{
  public static void main(String[] args) throws IOException {

    HttpRequestService httpService = new HttpRequestService();
    Dataset ds = httpService.requestDataset("http://satsrv04.researchstudio" +
                                           ".at:8889/won/resource/need/3846967518561904600");

    StringWriter sw = new StringWriter();
    RDFDataMgr.write(sw, ds, RDFFormat.JSONLD);
    String jsonData = sw.toString();
    //httpService.postRequest("http://localhost:8983/solr/won/siren/add?commit=true", jsonData);
    httpService.postRequest("http://192.168.59.103:8983/solr/won/siren/add?commit=true", jsonData);
    System.out.println(sw.toString());
  }
}
